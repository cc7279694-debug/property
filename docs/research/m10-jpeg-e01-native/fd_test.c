/* HOST kernel-FD ownership tests. Link strict_probe.c with E01_NO_MAIN and
 * the SAME locked/budgeted libjpeg as the CLI. argv[1] is the committed real
 * complete JPEG; no substitute, encoded stub or simulated decoder is used. */
#define _POSIX_C_SOURCE 200809L
#define _FILE_OFFSET_BITS 64

#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include "strict_core.h"

static uint64_t checks, failed, cases;

#define CHECK(expression) do { \
  checks++; \
  if (!(expression)) { \
    failed++; \
    fprintf(stderr, "FD_CHECK_FAILED line=%d check=%s\n", __LINE__, #expression); \
  } \
} while (0)

static void check_cleanup(const struct e01_result *result, int caller_fd,
                           int expect_acquired)
{
  CHECK(result->live == 0);
  CHECK(result->live_blocks == 0);
  CHECK(result->bad_frees == 0);
  CHECK(result->decoder_mem_null);
  CHECK(!result->create_attempted || result->destroy_called);
  CHECK(result->fd_acquired == expect_acquired);
  CHECK(result->fd_closed);
  if (expect_acquired) {
    CHECK(result->owned_fd_number >= 0);
    CHECK(result->owned_fd_number != caller_fd);
    /* Independent syscall evidence after the API returned, in addition to
     * its own close check. No intervening open can reuse this descriptor. */
    errno = 0;
    CHECK(fcntl(result->owned_fd_number, F_GETFD) == -1 && errno == EBADF);
  } else CHECK(result->owned_fd_number == -1);
}

static int validate_preserving_caller(int fd, const struct e01_options *options,
                                       struct e01_result *result,
                                       int expect_acquired)
{
  int flags_before = fcntl(fd, F_GETFL);
  int descriptor_flags_before = fcntl(fd, F_GETFD);
  struct stat before = {0}, after = {0};
  off_t offset_before = lseek(fd, 0, SEEK_CUR);
  int rc;

  CHECK(flags_before >= 0);
  CHECK(descriptor_flags_before >= 0);
  CHECK(fstat(fd, &before) == 0);
  cases++;
  rc = e01_validate_fd(fd, options, result);
  CHECK(rc == (int)result->status);
  /* A healthy return alone does not prove FD ownership: verify that caller's
   * actual kernel descriptor, file identity, offset and flags all survive. */
  CHECK(fcntl(fd, F_GETFD) == descriptor_flags_before);
  CHECK(fcntl(fd, F_GETFL) == flags_before);
  CHECK(fstat(fd, &after) == 0);
  CHECK(before.st_dev == after.st_dev && before.st_ino == after.st_ino);
  CHECK(lseek(fd, 0, SEEK_CUR) == offset_before);
  check_cleanup(result, fd, expect_acquired);
  return rc;
}

int main(int argc, char **argv)
{
  struct e01_options options;
  struct e01_result result, baseline;
  int fd, writable, readwrite, directory, pipes[2];
  enum e01_phase phase;
  uint64_t index;
  unsigned char magic[2] = {0};

  if (argc != 2) {
    fprintf(stderr, "Usage: fd_test COMMITTED_COMPLETE_JPEG\n");
    return 2;
  }
  fd = open(argv[1], O_RDONLY | O_CLOEXEC | O_NOFOLLOW);
  if (fd < 0) { perror("open complete JPEG"); return 2; }
  CHECK(pread(fd, magic, sizeof(magic), 0) == 2);
  CHECK(magic[0] == 0xff && magic[1] == 0xd8);
  CHECK(lseek(fd, 7, SEEK_SET) == 7);
  e01_options_init(&options);
  CHECK(options.limit == E01_DEFAULT_LIMIT);
  CHECK(validate_preserving_caller(fd, NULL, &baseline, 1) == E01_ACCEPT);
  CHECK(baseline.width > 0 && baseline.height > 0);
  CHECK(baseline.rows_read == baseline.height && baseline.finish_complete);
  CHECK(baseline.small_calls > 0 && baseline.large_calls > 0);
  CHECK(baseline.compressed_read > 0);
  CHECK(baseline.timings_available && baseline.wall_ns > 0);
  CHECK(baseline.cleanup_wall_ns <= baseline.wall_ns);
  CHECK(baseline.decode_wall_ns <= baseline.wall_ns);
  CHECK(baseline.rss_available && baseline.peak_rss_kib > 0);
  /* Still a usable caller FD after full successful decode, not just fcntl. */
  CHECK(pread(fd, magic, sizeof(magic), 0) == 2);
  CHECK(magic[0] == 0xff && magic[1] == 0xd8);
  CHECK(lseek(fd, 0, SEEK_CUR) == 7);

  for (phase = E01_PHASE_CREATE; phase <= E01_PHASE_RELEASE; phase++) {
    e01_options_init(&options);
    options.fault_phase = phase;
    CHECK(validate_preserving_caller(fd, &options, &result, 1) == E01_INFRA);
    CHECK(result.phase == phase);
    CHECK(result.error_code == E01_ERROR_PHASE_FAULT);
    CHECK(result.phase_fault_injected);
    CHECK(result.create_attempted && result.destroy_called);
    CHECK(!result.warnings); /* operational injection is not corrupt input */
    if (phase == E01_PHASE_SCANLINE) CHECK(result.rows_read == 1);
    if (phase == E01_PHASE_FINISH) {
      CHECK(result.rows_read == result.height && result.height > 0);
      CHECK(!result.finish_complete);
    }
    if (phase == E01_PHASE_RELEASE)
      CHECK(result.rows_read == result.height && result.finish_complete);
  }

  /* Exhaust every real allocation point for this same real JPEG. A transient
   * fail-at may be handled by libjpeg's normal pool retry; fail-from cannot. */
  for (index = 1; index <= baseline.attempts; index++) {
    e01_options_init(&options);
    options.fail_at = index;
    (void)validate_preserving_caller(fd, &options, &result, index != 1);
    CHECK(result.injected == 1);
    CHECK(result.status == E01_RESOURCE || result.status == E01_ACCEPT);
    e01_options_init(&options);
    options.fail_from = index;
    CHECK(validate_preserving_caller(fd, &options, &result,
                                     index != 1) == E01_RESOURCE);
    CHECK(result.injected > 0);
  }
  e01_options_init(&options);
  options.limit = 0;
  CHECK(validate_preserving_caller(fd, &options, &result, 0) == E01_RESOURCE);
  CHECK(result.denied > 0);

  cases++;
  CHECK(e01_validate_fd(-1, NULL, &result) == E01_INFRA);
  CHECK(result.error_code == EBADF);
  CHECK(!result.create_attempted);
  check_cleanup(&result, fd, 0);

  /* Opening without O_TRUNC never alters the committed fixture. Both writable
   * access modes must be refused, with caller ownership retained. */
  writable = open(argv[1], O_WRONLY | O_CLOEXEC | O_NOFOLLOW);
  if (writable < 0) { perror("open O_WRONLY"); return 2; }
  CHECK(lseek(writable, 9, SEEK_SET) == 9);
  CHECK(validate_preserving_caller(writable, NULL, &result, 1) == E01_INFRA);
  CHECK(result.error_code == E01_ERROR_FD_ACCESS && !result.create_attempted);
  CHECK(close(writable) == 0);
  readwrite = open(argv[1], O_RDWR | O_CLOEXEC | O_NOFOLLOW);
  if (readwrite < 0) { perror("open O_RDWR"); return 2; }
  CHECK(validate_preserving_caller(readwrite, NULL, &result, 1) == E01_INFRA);
  CHECK(result.error_code == E01_ERROR_FD_ACCESS && !result.create_attempted);
  CHECK(close(readwrite) == 0);

  if (pipe(pipes) != 0) { perror("pipe"); return 2; }
  CHECK(validate_preserving_caller(pipes[0], NULL, &result, 1) == E01_INFRA);
  CHECK(result.error_code == E01_ERROR_INPUT_TYPE && !result.create_attempted);
  CHECK(close(pipes[0]) == 0);
  CHECK(close(pipes[1]) == 0);
  directory = open(".", O_RDONLY | O_DIRECTORY | O_CLOEXEC);
  if (directory < 0) { perror("open directory"); return 2; }
  CHECK(validate_preserving_caller(directory, NULL, &result, 1) == E01_INFRA);
  CHECK(result.error_code == E01_ERROR_INPUT_TYPE && !result.create_attempted);
  CHECK(close(directory) == 0);

  CHECK(pread(fd, magic, sizeof(magic), 0) == 2);
  CHECK(magic[0] == 0xff && magic[1] == 0xd8);
  CHECK(lseek(fd, 0, SEEK_CUR) == 7);
  CHECK(close(fd) == 0);
  printf("{\"tests\":%" PRIu64 ",\"cases\":%" PRIu64
         ",\"failed\":%" PRIu64 ",\"realJpeg\":true"
         ",\"callerOwnershipVerified\":true,\"ownedCloseSyscallVerified\":true}\n",
         checks, cases, failed);
  return failed ? 1 : 0;
}
