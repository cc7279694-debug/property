#ifndef E01_STRICT_CORE_H
#define E01_STRICT_CORE_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Experimental host/request accounting bound, NOT a product or RSS cap. */
#define E01_DEFAULT_LIMIT ((size_t)512 * 1024 * 1024)

enum e01_status {
  E01_ACCEPT = 0, E01_IMAGE = 10, E01_RESOURCE = 20, E01_INFRA = 40
};

enum e01_phase {
  E01_PHASE_NONE = 0, E01_PHASE_CREATE, E01_PHASE_HEADER, E01_PHASE_START,
  E01_PHASE_ROW, E01_PHASE_SCANLINE, E01_PHASE_FINISH, E01_PHASE_RELEASE
};

enum e01_error {
  E01_ERROR_ARGUMENTS = -2001, E01_ERROR_INPUT_TYPE = -2002,
  E01_ERROR_SOURCE_CHANGED = -2003, E01_ERROR_INPUT_LIMIT = -2004,
  E01_ERROR_CODING_SCOPE = -2005, E01_ERROR_PIXEL_LIMIT = -2006,
  E01_ERROR_CALLER_ALLOCATION = -2007, E01_ERROR_SUSPENDED = -2008,
  E01_ERROR_ROW_SIZE = -2009, E01_ERROR_INVARIANT = -2010,
  E01_ERROR_CLEANUP = -2011, E01_ERROR_FD_ACCESS = -2012,
  E01_ERROR_PHASE_FAULT = -2100
};

struct e01_options {
  size_t limit;
  uint64_t fail_at, fail_from;
  enum e01_phase fault_phase;
};

struct e01_result {
  enum e01_status status;
  enum e01_phase phase;
  int error_code;
  int create_attempted, destroy_called, decoder_mem_null;
  int fd_acquired, owned_fd_number, fd_closed;
  int finish_complete, phase_fault_injected;
  uint64_t rows_read, width, height, warnings, compressed_read;
  int data_precision, input_components, output_components;
  int jpeg_color_space, output_color_space, progressive;
  size_t limit, peak, live, live_blocks, bad_frees;
  uint64_t attempts, injected, denied, small_calls, large_calls, caller_calls;
  /* End after decoder/row/context release. Cleanup includes actual context
   * free and final ledger/FD bookkeeping, not just jpeg_destroy_decompress. */
  uint64_t wall_ns, cpu_ns, decode_wall_ns, decode_cpu_ns;
  uint64_t cleanup_wall_ns, cleanup_cpu_ns;
  int timings_available;
  /* Linux/Android getrusage(RUSAGE_SELF) process high-water RSS, in KiB;
   * may include earlier requests and is NOT the charged heap peak. */
  uint64_t peak_rss_kib;
  int rss_available;
};

void e01_options_init(struct e01_options *options);

/* Synchronous C-contained error/longjmp boundary; no callback escapes here.
 * input_fd must be a regular O_RDONLY descriptor. The core duplicates it with
 * F_DUPFD_CLOEXEC, uses its own pread offset, never closes input_fd, and checks
 * actual release of its duplicate. The caller must keep input_fd/file stable.
 * options == NULL uses defaults; otherwise use e01_options_init then override.
 * Returns result->status. Saves no decoded pixels or normalized output. */
int e01_validate_fd(int input_fd, const struct e01_options *options,
                    struct e01_result *result);

#ifdef __cplusplus
}
#endif
#endif
