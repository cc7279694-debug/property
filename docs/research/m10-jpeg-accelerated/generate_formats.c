/* Original deterministic research fixtures; stock libjpeg-turbo 3.2.0 only.
 * Usage: generate_formats EXISTING_OUTPUT_DIRECTORY
 * Generation is not decoder verification. No production images are inputs. */
#define _POSIX_C_SOURCE 200809L
#include <errno.h>
#include <fcntl.h>
#include <setjmp.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <jpeglib.h>

#define WIDTH 257
#define HEIGHT 193
#define COMPONENTS 4
#define SEED UINT32_C(0x31415926)

struct error_state {
  struct jpeg_error_mgr base;
  jmp_buf jump;
  char message[JMSG_LENGTH_MAX];
};

/* Mutable ownership and error state remain on the heap across longjmp. */
struct generation {
  struct jpeg_compress_struct codec;
  struct error_state error;
  FILE *file;
  JSAMPROW row;
  int fd, owned, create_attempted, destroy_attempted, ok;
};

struct format {
  const char *name;
  int ycck, progressive;
};

static void fatal_error(j_common_ptr codec)
{
  struct error_state *error = (struct error_state *)codec->err;
  (*error->base.format_message)(codec, error->message);
  longjmp(error->jump, 1);
}

static void strict_message(j_common_ptr codec, int level)
{
  if (level < 0) fatal_error(codec);
}

static void fail(struct generation *state, const char *message)
{
  snprintf(state->error.message, sizeof(state->error.message), "%s", message);
  longjmp(state->error.jump, 1);
}

static void fill_row(JSAMPROW row, JDIMENSION y)
{
  for (uint32_t x = 0; x < WIDTH; ++x) {
    for (uint32_t channel = 0; channel < COMPONENTS; ++channel) {
      uint32_t value = SEED ^ (x * UINT32_C(0x9e3779b1)) ^
                       ((uint32_t)y * UINT32_C(0x85ebca6b)) ^
                       (channel * UINT32_C(0xc2b2ae35));
      value ^= value >> 16;
      value *= UINT32_C(0x7feb352d);
      value ^= value >> 15;
      row[(size_t)x * COMPONENTS + channel] = (JSAMPLE)(value >> 24);
    }
  }
}

static int generate_one(const char *path, const struct format *format)
{
  struct generation *state = calloc(1, sizeof(*state));
  size_t row_bytes;
  if (!state) { fprintf(stderr, "generator state allocation failed\n"); return 0; }
  state->fd = -1;
  if ((size_t)WIDTH > SIZE_MAX / COMPONENTS / sizeof(JSAMPLE)) {
    fprintf(stderr, "generator row size overflow\n"); free(state); return 0;
  }
  row_bytes = (size_t)WIDTH * COMPONENTS * sizeof(JSAMPLE);
  state->row = malloc(row_bytes);
  if (!state->row) { fprintf(stderr, "generator row allocation failed\n"); free(state); return 0; }
  state->codec.err = jpeg_std_error(&state->error.base);
  state->error.base.error_exit = fatal_error;
  state->error.base.emit_message = strict_message;

  if (setjmp(state->error.jump) == 0) {
    state->fd = open(path, O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC | O_NOFOLLOW, 0600);
    if (state->fd < 0) fail(state, strerror(errno));
    state->owned = 1;
    state->file = fdopen(state->fd, "wb");
    if (!state->file) fail(state, strerror(errno));
    state->fd = -1; /* FILE now owns this descriptor. */
    state->create_attempted = 1;
    jpeg_create_compress(&state->codec);
    jpeg_stdio_dest(&state->codec, state->file);
    state->codec.image_width = WIDTH;
    state->codec.image_height = HEIGHT;
    state->codec.input_components = COMPONENTS;
    state->codec.in_color_space = JCS_CMYK;
    jpeg_set_defaults(&state->codec);
    jpeg_set_colorspace(&state->codec, format->ycck ? JCS_YCCK : JCS_CMYK);
    jpeg_set_quality(&state->codec, 85, TRUE);
    state->codec.data_precision = 8;
    state->codec.arith_code = FALSE;
    state->codec.optimize_coding = FALSE;
    state->codec.dct_method = JDCT_ISLOW;
    if (format->progressive) jpeg_simple_progression(&state->codec);
    jpeg_start_compress(&state->codec, TRUE);
    while (state->codec.next_scanline < state->codec.image_height) {
      fill_row(state->row, state->codec.next_scanline);
      if (jpeg_write_scanlines(&state->codec, &state->row, 1) != 1)
        fail(state, "generator scanline made no progress");
    }
    jpeg_finish_compress(&state->codec);
    state->destroy_attempted = 1;
    jpeg_destroy_compress(&state->codec);
    if (fflush(state->file) != 0 || ferror(state->file) ||
        fsync(fileno(state->file)) != 0) fail(state, "generator output sync failed");
    int closed = fclose(state->file);
    state->file = NULL;
    if (closed != 0) fail(state, "generator output close failed");
    state->ok = 1;
  }

  if (state->create_attempted && !state->destroy_attempted) {
    state->destroy_attempted = 1;
    jpeg_destroy_compress(&state->codec);
  }
  if (state->file) fclose(state->file);
  if (state->fd >= 0) close(state->fd);
  if (!state->ok) {
    fprintf(stderr, "%s: %s\n", format->name, state->error.message);
    if (state->owned && unlink(path) != 0)
      fprintf(stderr, "%s: failed output cleanup: %s\n", format->name, strerror(errno));
  } else {
    printf("{\"file\":\"%s\",\"status\":\"GENERATED_NOT_DECODE_VERIFIED\","
           "\"width\":257,\"height\":193,\"input_components\":4,"
           "\"input_color_space\":\"CMYK\",\"jpeg_color_space\":\"%s\","
           "\"progressive\":%s,\"quality\":85,\"seed\":\"0x31415926\"}\n",
           format->name, format->ycck ? "YCCK" : "CMYK",
           format->progressive ? "true" : "false");
  }
  int ok = state->ok;
  free(state->row);
  free(state);
  return ok;
}

int main(int argc, char **argv)
{
  static const struct format formats[] = {
    {"cmyk_baseline.jpg", 0, 0}, {"cmyk_progressive.jpg", 0, 1},
    {"ycck_baseline.jpg", 1, 0}, {"ycck_progressive.jpg", 1, 1}
  };
  if (argc != 2) { fprintf(stderr, "usage: generate_formats EXISTING_OUTPUT_DIRECTORY\n"); return 2; }
  for (size_t index = 0; index < sizeof(formats) / sizeof(formats[0]); ++index) {
    char path[4096];
    int length = snprintf(path, sizeof(path), "%s/%s", argv[1], formats[index].name);
    if (length < 0 || (size_t)length >= sizeof(path)) {
      fprintf(stderr, "generator output path too long\n"); return 2;
    }
    if (!generate_one(path, &formats[index])) return 1;
  }
  return ferror(stdout) ? 1 : 0;
}
