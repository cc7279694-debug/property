/*
 * E01 HOST-only libjpeg A2 research probe; no Android/JNI integration.
 *
 * Build only against the locked libjpeg-turbo 3.2.0 source with jmem_budget.c.
 * All caller and libjpeg small/large allocations use one e01_budget.  The
 * stack budget root, libc/runtime internals, and the input file are outside
 * that allocation accounting.  The 64 MiB default heap budget, 64 MiB source
 * bound, 40 MP bound, and 8-bit Huffman lossy 1/3-component coding scope are
 * research controls, not a product compatibility or Android resource claim.
 *
 * The source never synthesizes an EOI; every libjpeg warning is fatal to this
 * decode.  Acceptance requires all real output rows and successful finish.
 * No signal handler converts a crash into an image rejection.
 */
#define _POSIX_C_SOURCE 200809L
#define _FILE_OFFSET_BITS 64

#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <setjmp.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include <jpeglib.h>
#include <jerror.h>

#include "budget.h"

#define DEFAULT_LIMIT ((size_t)64 * 1024 * 1024)
#define INPUT_LIMIT (UINT64_C(64) * 1024 * 1024)
#define PIXEL_LIMIT UINT64_C(40000000)
#define SOURCE_CHUNK 8192

enum probe_status {
  PROBE_ACCEPT = 0,
  PROBE_IMAGE = 10,
  PROBE_RESOURCE = 20,
  PROBE_INFRA = 40
};

enum probe_phase {
  PHASE_CREATE, PHASE_HEADER, PHASE_START,
  PHASE_ROW, PHASE_SCANLINE, PHASE_FINISH
};

enum probe_error {
  ERROR_ARGUMENTS = -2001,
  ERROR_INPUT_TYPE = -2002,
  ERROR_SOURCE_CHANGED = -2003,
  ERROR_INPUT_LIMIT = -2004,
  ERROR_CODING_SCOPE = -2005,
  ERROR_PIXEL_LIMIT = -2006,
  ERROR_CALLER_ALLOCATION = -2007,
  ERROR_SUSPENDED = -2008,
  ERROR_ROW_SIZE = -2009,
  ERROR_INVARIANT = -2010,
  ERROR_CLEANUP = -2011
};

struct options {
  const char *path;
  size_t limit;
  uint64_t fail_at;
  uint64_t fail_from;
};

struct result {
  enum probe_status status;
  enum probe_phase phase;
  int error_code;
  int create_attempted;
  int destroy_called;
  int decoder_mem_null;
  int fd_closed;
  int finish_complete;
  uint64_t rows_read;
  uint64_t height;
  uint64_t warnings;
  uint64_t compressed_read;
};

struct context;

struct strict_error {
  struct jpeg_error_mgr pub;
  jmp_buf jump;
  struct context *owner;
};

struct bounded_source {
  struct jpeg_source_mgr pub;
  JOCTET buffer[SOURCE_CHUNK];
};

/* Mutable decode/cleanup state survives longjmp in this budgeted heap block.
 * The budget root is in main(), which contains no setjmp.  Functions that do
 * contain setjmp never modify their automatic context-pointer argument. */
struct context {
  struct e01_budget *budget;
  struct result result;
  struct jpeg_decompress_struct decoder;
  struct strict_error error;
  struct bounded_source source;
  JSAMPROW row;
  size_t row_bytes;
  uint64_t input_bytes;
  int fd;
  int cleanup_active;
};

static void initialize_result(struct result *result)
{
  memset(result, 0, sizeof(*result));
  result->status = PROBE_INFRA;
  result->phase = PHASE_CREATE;
  result->decoder_mem_null = 1;
  result->fd_closed = 1; /* also true when no descriptor was acquired */
}

static struct context *from_jpeg(j_common_ptr decoder)
{
  return ((struct strict_error *)decoder->err)->owner;
}

static _Noreturn void abort_decode(struct context *context,
                                  enum probe_status status, int error_code)
{
  context->result.status = context->cleanup_active ? PROBE_INFRA : status;
  context->result.error_code = error_code;
  longjmp(context->error.jump, 1);
}

static void strict_fatal(j_common_ptr decoder)
{
  struct context *context = from_jpeg(decoder);
  int code = decoder->err->msg_code;
  enum probe_status status = PROBE_IMAGE;

  if (code == JERR_OUT_OF_MEMORY || code == JERR_NO_BACKING_STORE)
    status = PROBE_RESOURCE;
  else if (context->result.phase == PHASE_CREATE ||
           code == JERR_BAD_LIB_VERSION || code == JERR_BAD_STRUCT_SIZE ||
           code == JERR_BAD_ALIGN_TYPE || code == JERR_BAD_ALLOC_CHUNK ||
           code == JERR_BAD_STATE || code == JERR_BAD_POOL_ID ||
           code == JERR_BAD_VIRTUAL_ACCESS || code == JERR_VIRTUAL_BUG)
    status = PROBE_INFRA;
  abort_decode(context, status, code);
}

static void strict_message(j_common_ptr decoder, int level)
{
  struct context *context = from_jpeg(decoder);
  if (level < 0) {
    context->result.warnings++;
    decoder->err->num_warnings++;
    abort_decode(context, PROBE_IMAGE, decoder->err->msg_code);
  }
  /* Trace messages do not indicate a corrupt image.  Do not print them. */
}

static void source_init(j_decompress_ptr decoder)
{
  (void)decoder;
}

static boolean source_fill(j_decompress_ptr decoder)
{
  struct context *context = from_jpeg((j_common_ptr)decoder);
  uint64_t remaining = context->input_bytes - context->result.compressed_read;
  size_t wanted = remaining < SOURCE_CHUNK ? (size_t)remaining : SOURCE_CHUNK;
  ssize_t received;

  if (!wanted) {
    decoder->err->msg_code = JWRN_JPEG_EOF;
    strict_message((j_common_ptr)decoder, -1);
    return FALSE;
  }
  do {
    received = read(context->fd, context->source.buffer, wanted);
  } while (received < 0 && errno == EINTR);
  if (received < 0)
    abort_decode(context, PROBE_INFRA, errno);
  if (!received)
    abort_decode(context, PROBE_INFRA, ERROR_SOURCE_CHANGED);

  context->result.compressed_read += (uint64_t)received;
  context->source.pub.next_input_byte = context->source.buffer;
  context->source.pub.bytes_in_buffer = (size_t)received;
  return TRUE;
}

static void source_skip(j_decompress_ptr decoder, long bytes)
{
  struct jpeg_source_mgr *source = decoder->src;
  if (bytes <= 0) return;
  while ((uint64_t)bytes > source->bytes_in_buffer) {
    bytes -= (long)source->bytes_in_buffer;
    source->bytes_in_buffer = 0;
    if (!source_fill(decoder)) return;
  }
  source->next_input_byte += (size_t)bytes;
  source->bytes_in_buffer -= (size_t)bytes;
}

static void source_term(j_decompress_ptr decoder)
{
  (void)decoder;
}

static int open_input(struct context *context, const char *path)
{
  struct stat information;

  /* O_NONBLOCK avoids blocking on a non-regular input before fstat rejects it.
   * O_NOFOLLOW prevents accidentally following a changed final path link. */
  context->fd = open(path, O_RDONLY | O_CLOEXEC | O_NONBLOCK | O_NOFOLLOW);
  if (context->fd < 0) {
    context->result.error_code = errno;
    return 0;
  }
  context->result.fd_closed = 0;
  if (fstat(context->fd, &information) != 0) {
    context->result.error_code = errno;
    return 0;
  }
  if (!S_ISREG(information.st_mode) || information.st_size < 0) {
    context->result.error_code = ERROR_INPUT_TYPE;
    return 0;
  }
  context->input_bytes = (uint64_t)information.st_size;
  context->result.phase = PHASE_HEADER;
  if (context->input_bytes > INPUT_LIMIT) {
    context->result.status = PROBE_RESOURCE;
    context->result.error_code = ERROR_INPUT_LIMIT;
    return 0;
  }
  if (!context->input_bytes) {
    context->result.status = PROBE_IMAGE;
    context->result.error_code = JERR_INPUT_EMPTY;
    return 0;
  }
  return 1;
}

static void decode(struct context *context)
{
  context->decoder.err = jpeg_std_error(&context->error.pub);
  context->error.owner = context;
  context->error.pub.error_exit = strict_fatal;
  context->error.pub.emit_message = strict_message;
  /* jpeg_CreateDecompress preserves client_data before its first allocation. */
  context->decoder.client_data = context->budget;

  if (setjmp(context->error.jump)) return;

  context->result.phase = PHASE_CREATE;
  context->result.create_attempted = 1;
  jpeg_create_decompress(&context->decoder);
  context->source.pub.init_source = source_init;
  context->source.pub.fill_input_buffer = source_fill;
  context->source.pub.skip_input_data = source_skip;
  context->source.pub.resync_to_restart = jpeg_resync_to_restart;
  context->source.pub.term_source = source_term;
  context->decoder.src = &context->source.pub;

  context->result.phase = PHASE_HEADER;
  if (jpeg_read_header(&context->decoder, TRUE) != JPEG_HEADER_OK)
    abort_decode(context, PROBE_IMAGE, ERROR_SUSPENDED);
  context->result.height = context->decoder.image_height;
  if (!context->decoder.image_width || !context->decoder.image_height)
    abort_decode(context, PROBE_IMAGE, JERR_EMPTY_IMAGE);
  if ((uint64_t)context->decoder.image_width >
      PIXEL_LIMIT / context->decoder.image_height)
    abort_decode(context, PROBE_RESOURCE, ERROR_PIXEL_LIMIT);

  /* This preserves R1's explicitly limited research coding scope. */
  if (context->decoder.data_precision != 8 || context->decoder.arith_code ||
      (context->decoder.num_components != 1 &&
       context->decoder.num_components != 3) ||
      (context->decoder.jpeg_color_space != JCS_GRAYSCALE &&
       context->decoder.jpeg_color_space != JCS_RGB &&
       context->decoder.jpeg_color_space != JCS_YCbCr) ||
      (!context->decoder.progressive_mode &&
       (context->decoder.Ss != 0 || context->decoder.Se != 63)))
    abort_decode(context, PROBE_IMAGE, ERROR_CODING_SCOPE);

  context->decoder.out_color_space =
    context->decoder.num_components == 1 ? JCS_GRAYSCALE : JCS_RGB;
  context->decoder.scale_num = 1;
  context->decoder.scale_denom = 1;
  context->result.phase = PHASE_START;
  if (!jpeg_start_decompress(&context->decoder))
    abort_decode(context, PROBE_IMAGE, ERROR_SUSPENDED);
  if (context->decoder.output_width != context->decoder.image_width ||
      context->decoder.output_height != context->decoder.image_height ||
      context->decoder.output_components != context->decoder.num_components)
    abort_decode(context, PROBE_INFRA, ERROR_INVARIANT);

  context->result.phase = PHASE_ROW;
  if ((size_t)context->decoder.output_width >
      SIZE_MAX / (size_t)context->decoder.output_components)
    abort_decode(context, PROBE_RESOURCE, ERROR_ROW_SIZE);
  context->row_bytes = (size_t)context->decoder.output_width *
                       (size_t)context->decoder.output_components;
  context->row = e01_alloc(context->budget, context->row_bytes, E01_CALLER);
  if (!context->row)
    abort_decode(context, PROBE_RESOURCE, ERROR_CALLER_ALLOCATION);

  context->result.phase = PHASE_SCANLINE;
  while (context->decoder.output_scanline < context->decoder.output_height) {
    if (jpeg_read_scanlines(&context->decoder, &context->row, 1) != 1)
      abort_decode(context, PROBE_IMAGE, ERROR_SUSPENDED);
    context->result.rows_read++;
  }
  context->result.phase = PHASE_FINISH;
  if (!jpeg_finish_decompress(&context->decoder))
    abort_decode(context, PROBE_IMAGE, ERROR_SUSPENDED);
  context->result.finish_complete = 1;
  if (context->result.warnings ||
      context->result.rows_read != context->result.height)
    abort_decode(context, PROBE_INFRA, ERROR_INVARIANT);
  context->result.status = PROBE_ACCEPT;
  context->result.error_code = 0;
}

static void cleanup(struct context *context)
{
  context->cleanup_active = 1;
  /* A cleanup callback must not jump into decode() after that frame returned.
   * A failure here is infrastructure failure, never a clean image rejection. */
  if (setjmp(context->error.jump) == 0) {
    if (context->result.create_attempted) {
      context->result.destroy_called = 1;
      jpeg_destroy_decompress(&context->decoder);
    }
  }
  context->result.decoder_mem_null = context->decoder.mem == NULL;
  if (!context->result.decoder_mem_null) {
    context->result.status = PROBE_INFRA;
    context->result.error_code = ERROR_CLEANUP;
  }
  if (context->fd >= 0) {
    /* On Linux close() releases the descriptor even when interrupted; do not
     * retry a failed close and risk closing a subsequently reused descriptor. */
    if (close(context->fd) == 0)
      context->result.fd_closed = 1;
    else {
      context->result.status = PROBE_INFRA;
      context->result.error_code = errno;
    }
    context->fd = -1;
  }
  if (context->row) {
    (void)e01_free(context->budget, context->row,
                   context->row_bytes, E01_CALLER);
    context->row = NULL;
  }
}

static const char *status_name(enum probe_status status)
{
  switch (status) {
    case PROBE_ACCEPT: return "ACCEPT";
    case PROBE_IMAGE: return "IMAGE_REJECT";
    case PROBE_RESOURCE: return "RESOURCE_REJECT";
    default: return "INFRA_ERROR";
  }
}

static const char *phase_name(enum probe_phase phase)
{
  switch (phase) {
    case PHASE_HEADER: return "header";
    case PHASE_START: return "start";
    case PHASE_ROW: return "row";
    case PHASE_SCANLINE: return "scanline";
    case PHASE_FINISH: return "finish";
    default: return "create";
  }
}

static const char *json_boolean(int value)
{
  return value ? "true" : "false";
}

static void print_result(const struct result *result,
                         const struct e01_budget *budget)
{
  printf("{\"status\":\"%s\",\"phase\":\"%s\"",
         status_name(result->status), phase_name(result->phase));
  printf(",\"attempts\":%" PRIu64 ",\"injected\":%" PRIu64
         ",\"denied\":%" PRIu64 ",\"peak\":%zu,\"live\":%zu"
         ",\"liveBlocks\":%zu,\"badFrees\":%zu",
         budget->attempts, budget->injected, budget->denied, budget->peak,
         budget->live, budget->live_blocks, budget->bad_frees);
  printf(",\"smallCalls\":%" PRIu64 ",\"largeCalls\":%" PRIu64
         ",\"callerCalls\":%" PRIu64,
         budget->calls[E01_SMALL], budget->calls[E01_LARGE],
         budget->calls[E01_CALLER]);
  printf(",\"createAttempted\":%s,\"destroyCalled\":%s"
         ",\"decoderMemNull\":%s,\"fdClosed\":%s",
         json_boolean(result->create_attempted),
         json_boolean(result->destroy_called),
         json_boolean(result->decoder_mem_null), json_boolean(result->fd_closed));
  printf(",\"rowsRead\":%" PRIu64 ",\"height\":%" PRIu64
         ",\"finishComplete\":%s,\"warnings\":%" PRIu64
         ",\"errorCode\":%d,\"compressedRead\":%" PRIu64 "}\n",
         result->rows_read, result->height, json_boolean(result->finish_complete),
         result->warnings, result->error_code, result->compressed_read);
}

static int parse_unsigned(const char *text, uint64_t maximum, uint64_t *out)
{
  const unsigned char *cursor = (const unsigned char *)text;
  char *end;
  uintmax_t value;

  if (!cursor || !*cursor) return 0;
  for (; *cursor; cursor++)
    if (*cursor < '0' || *cursor > '9') return 0;
  errno = 0;
  value = strtoumax(text, &end, 10);
  if (errno || *end || value > maximum) return 0;
  *out = (uint64_t)value;
  return 1;
}

static int parse_options(int argc, char **argv, struct options *options)
{
  unsigned int seen = 0;
  uint64_t value;
  int index;

  memset(options, 0, sizeof(*options));
  options->limit = DEFAULT_LIMIT;
  if (argc < 2 || !argv[1][0] || argv[1][0] == '-') return 0;
  options->path = argv[1];
  for (index = 2; index < argc; index++) {
    if (!strcmp(argv[index], "--limit") && !(seen & 1) && index + 1 < argc) {
      if (!parse_unsigned(argv[++index], SIZE_MAX, &value)) return 0;
      options->limit = (size_t)value;
      seen |= 1;
    } else if (!strcmp(argv[index], "--fail-at") && !(seen & 2) &&
               index + 1 < argc) {
      if (!parse_unsigned(argv[++index], UINT64_MAX, &value)) return 0;
      options->fail_at = value;
      seen |= 2;
    } else if (!strcmp(argv[index], "--fail-from") && !(seen & 4) &&
               index + 1 < argc) {
      if (!parse_unsigned(argv[++index], UINT64_MAX, &value)) return 0;
      options->fail_from = value;
      seen |= 4;
    } else return 0;
  }
  return 1;
}

int main(int argc, char **argv)
{
  struct options options;
  struct e01_budget budget;
  struct result result;
  struct context *context;
  int valid_arguments;

  initialize_result(&result);
  valid_arguments = parse_options(argc, argv, &options);
  e01_budget_init(&budget, options.limit, options.fail_at, options.fail_from);
  if (!valid_arguments) {
    result.error_code = ERROR_ARGUMENTS;
    fprintf(stderr, "Usage: strict_probe FILE [--limit BYTES] "
                    "[--fail-at N] [--fail-from N]\n");
  } else {
    context = e01_alloc(&budget, sizeof(*context), E01_CALLER);
    if (!context) {
      result.status = PROBE_RESOURCE;
      result.error_code = ERROR_CALLER_ALLOCATION;
    } else {
      memset(context, 0, sizeof(*context));
      context->budget = &budget;
      context->fd = -1;
      initialize_result(&context->result);
      if (open_input(context, options.path)) decode(context);
      cleanup(context);
      result = context->result;
      (void)e01_free(&budget, context, sizeof(*context), E01_CALLER);
    }
  }
  /* Counters are observed after actual row, decoder, and context frees.
   * Never reset accounting to make an incomplete cleanup appear balanced. */
  if (budget.live || budget.live_blocks || budget.bad_frees || budget.head ||
      !result.decoder_mem_null || !result.fd_closed) {
    result.status = PROBE_INFRA;
    result.error_code = ERROR_CLEANUP;
  }
  print_result(&result, &budget);
  return result.status;
}
