/*
 * E01 experimental libjpeg-turbo 3.2.0 FD validator, with optional HOST CLI.
 * E01_NO_MAIN removes the CLI for the reusable C/JNI error boundary. No pixels
 * are retained/normalized. This is not Android/product compatibility proof.
 *
 * All caller/libjpeg heap requests share one e01_budget. Its stack root,
 * libc/runtime internals, codec stack and input file are outside accounting.
 * Experimental 512 MiB request default is NOT a product/RSS bound. Source is
 * bounded to 64 MiB, image to 40 MP; 8-bit Huffman lossy grayscale/RGB/YCbCr/
 * CMYK/YCCK (including progressive) only. Other precision/lossless/arithmetic
 * modes are outside research scope. Every warning is fatal; no synthetic EOI
 * and no signal/crash-to-image-rejection path.
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
#include <sys/resource.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

#include <jpeglib.h>
#include <jerror.h>

#include "budget.h"
#include "strict_core.h"

#define INPUT_LIMIT (UINT64_C(64) * 1024 * 1024)
#define PIXEL_LIMIT UINT64_C(40000000)
#define SOURCE_CHUNK 8192

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

/* Every mutable value used across a libjpeg longjmp is in this charged heap
 * block or the budget it points to. setjmp frames never mutate their automatic
 * context-pointer argument. Error callbacks never jump across this C API. */
struct context {
  struct e01_budget *budget;
  struct e01_result result;
  struct jpeg_decompress_struct decoder;
  struct strict_error error;
  struct bounded_source source;
  JSAMPROW row;
  size_t row_bytes;
  uint64_t input_bytes;
  int fd, cleanup_active;
  enum e01_phase fault_phase;
};

struct timestamp {
  struct timespec wall, cpu;
  int valid;
};

static struct timestamp timestamp_now(void)
{
  struct timestamp stamp;
  memset(&stamp, 0, sizeof(stamp));
  stamp.valid = clock_gettime(CLOCK_MONOTONIC, &stamp.wall) == 0 &&
                clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &stamp.cpu) == 0;
  return stamp;
}

static uint64_t elapsed_ns(struct timespec begin, struct timespec end)
{
  int64_t seconds = (int64_t)end.tv_sec - (int64_t)begin.tv_sec;
  int64_t nanos = (int64_t)end.tv_nsec - (int64_t)begin.tv_nsec;
  if (nanos < 0) { seconds--; nanos += INT64_C(1000000000); }
  if (seconds < 0 || (uint64_t)seconds > UINT64_MAX / UINT64_C(1000000000))
    return 0;
  return (uint64_t)seconds * UINT64_C(1000000000) + (uint64_t)nanos;
}

void e01_options_init(struct e01_options *options)
{
  if (!options) return;
  memset(options, 0, sizeof(*options));
  options->limit = E01_DEFAULT_LIMIT;
}

static void initialize_result(struct e01_result *result)
{
  memset(result, 0, sizeof(*result));
  result->status = E01_INFRA;
  result->phase = E01_PHASE_CREATE;
  result->decoder_mem_null = 1;
  result->owned_fd_number = -1;
  result->fd_closed = 1; /* vacuous only if fd_acquired remains false */
}

static struct context *from_jpeg(j_common_ptr decoder)
{
  return ((struct strict_error *)decoder->err)->owner;
}

static _Noreturn void abort_decode(struct context *context,
                                   enum e01_status status, int error_code)
{
  context->result.status = context->cleanup_active ? E01_INFRA : status;
  context->result.error_code = error_code;
  longjmp(context->error.jump, 1);
}

static void inject_phase(struct context *context, enum e01_phase phase)
{
  if (context->fault_phase != phase) return;
  context->result.phase_fault_injected = 1;
  abort_decode(context, E01_INFRA, E01_ERROR_PHASE_FAULT);
}

static void strict_fatal(j_common_ptr decoder)
{
  struct context *context = from_jpeg(decoder);
  int code = decoder->err->msg_code;
  enum e01_status status = E01_IMAGE;

  if (code == JERR_OUT_OF_MEMORY || code == JERR_NO_BACKING_STORE)
    status = E01_RESOURCE;
  else if (context->result.phase == E01_PHASE_CREATE ||
           code == JERR_BAD_LIB_VERSION || code == JERR_BAD_STRUCT_SIZE ||
           code == JERR_BAD_ALIGN_TYPE || code == JERR_BAD_ALLOC_CHUNK ||
           code == JERR_BAD_STATE || code == JERR_BAD_POOL_ID ||
           code == JERR_BAD_VIRTUAL_ACCESS || code == JERR_VIRTUAL_BUG)
    status = E01_INFRA;
  abort_decode(context, status, code);
}

static void strict_message(j_common_ptr decoder, int level)
{
  struct context *context = from_jpeg(decoder);
  if (level < 0) {
    context->result.warnings++;
    decoder->err->num_warnings++;
    abort_decode(context, E01_IMAGE, decoder->err->msg_code);
  }
}

static void source_init(j_decompress_ptr decoder) { (void)decoder; }

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
    received = pread(context->fd, context->source.buffer, wanted,
                     (off_t)context->result.compressed_read);
  } while (received < 0 && errno == EINTR);
  if (received < 0) abort_decode(context, E01_INFRA, errno);
  if (!received) abort_decode(context, E01_INFRA, E01_ERROR_SOURCE_CHANGED);

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

static void source_term(j_decompress_ptr decoder) { (void)decoder; }

static int acquire_input(struct context *context, int input_fd)
{
  struct stat information;
  int flags;

  /* Inspect the owned duplicate to avoid a check-then-dup descriptor race.
   * pread later preserves the shared open-file-description offset. */
  context->fd = fcntl(input_fd, F_DUPFD_CLOEXEC, 0);
  if (context->fd < 0) { context->result.error_code = errno; return 0; }
  context->result.fd_acquired = 1;
  context->result.owned_fd_number = context->fd;
  context->result.fd_closed = 0;
  flags = fcntl(context->fd, F_GETFL);
  if (flags < 0) { context->result.error_code = errno; return 0; }
  if ((flags & O_ACCMODE) != O_RDONLY) {
    context->result.error_code = E01_ERROR_FD_ACCESS;
    return 0;
  }
  if (fstat(context->fd, &information) != 0) {
    context->result.error_code = errno;
    return 0;
  }
  if (!S_ISREG(information.st_mode) || information.st_size < 0) {
    context->result.error_code = E01_ERROR_INPUT_TYPE;
    return 0;
  }
  context->input_bytes = (uint64_t)information.st_size;
  context->result.phase = E01_PHASE_HEADER;
  if (context->input_bytes > INPUT_LIMIT) {
    context->result.status = E01_RESOURCE;
    context->result.error_code = E01_ERROR_INPUT_LIMIT;
    return 0;
  }
  if (!context->input_bytes) {
    context->result.status = E01_IMAGE;
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
  context->decoder.client_data = context->budget;
  if (setjmp(context->error.jump)) return;

  context->result.phase = E01_PHASE_CREATE;
  context->result.create_attempted = 1;
  jpeg_create_decompress(&context->decoder);
  inject_phase(context, E01_PHASE_CREATE);
  context->source.pub.init_source = source_init;
  context->source.pub.fill_input_buffer = source_fill;
  context->source.pub.skip_input_data = source_skip;
  context->source.pub.resync_to_restart = jpeg_resync_to_restart;
  context->source.pub.term_source = source_term;
  context->decoder.src = &context->source.pub;

  context->result.phase = E01_PHASE_HEADER;
  if (jpeg_read_header(&context->decoder, TRUE) != JPEG_HEADER_OK)
    abort_decode(context, E01_IMAGE, E01_ERROR_SUSPENDED);
  context->result.width = context->decoder.image_width;
  context->result.height = context->decoder.image_height;
  context->result.data_precision = context->decoder.data_precision;
  context->result.input_components = context->decoder.num_components;
  context->result.jpeg_color_space = context->decoder.jpeg_color_space;
  context->result.progressive = context->decoder.progressive_mode;
  inject_phase(context, E01_PHASE_HEADER);
  if (!context->decoder.image_width || !context->decoder.image_height)
    abort_decode(context, E01_IMAGE, JERR_EMPTY_IMAGE);
  if ((uint64_t)context->decoder.image_width >
      PIXEL_LIMIT / context->decoder.image_height)
    abort_decode(context, E01_RESOURCE, E01_ERROR_PIXEL_LIMIT);

  if (context->decoder.data_precision != 8 || context->decoder.arith_code ||
      (context->decoder.num_components != 1 &&
       context->decoder.num_components != 3 &&
       context->decoder.num_components != 4) ||
      (context->decoder.jpeg_color_space != JCS_GRAYSCALE &&
       context->decoder.jpeg_color_space != JCS_RGB &&
       context->decoder.jpeg_color_space != JCS_YCbCr &&
       context->decoder.jpeg_color_space != JCS_CMYK &&
       context->decoder.jpeg_color_space != JCS_YCCK) ||
      (!context->decoder.progressive_mode &&
       (context->decoder.Ss != 0 || context->decoder.Se != 63)))
    /* A valid unsupported mode is a research capability gap, not corruption. */
    abort_decode(context, E01_INFRA, E01_ERROR_CODING_SCOPE);

  context->decoder.out_color_space = context->decoder.num_components == 1
    ? JCS_GRAYSCALE : (context->decoder.num_components == 4 ? JCS_CMYK : JCS_RGB);
  context->decoder.scale_num = 1;
  context->decoder.scale_denom = 1;
  context->result.phase = E01_PHASE_START;
  if (!jpeg_start_decompress(&context->decoder))
    abort_decode(context, E01_IMAGE, E01_ERROR_SUSPENDED);
  context->result.output_components = context->decoder.output_components;
  context->result.output_color_space = context->decoder.out_color_space;
  inject_phase(context, E01_PHASE_START);
  if (context->decoder.output_width != context->decoder.image_width ||
      context->decoder.output_height != context->decoder.image_height ||
      (context->decoder.out_color_space == JCS_GRAYSCALE &&
       context->decoder.output_components != 1) ||
      (context->decoder.out_color_space == JCS_RGB &&
       context->decoder.output_components != 3) ||
      (context->decoder.out_color_space == JCS_CMYK &&
       context->decoder.output_components != 4))
    abort_decode(context, E01_INFRA, E01_ERROR_INVARIANT);

  context->result.phase = E01_PHASE_ROW;
  if (context->decoder.output_components <= 0 ||
      (size_t)context->decoder.output_width >
      SIZE_MAX / (size_t)context->decoder.output_components)
    abort_decode(context, E01_RESOURCE, E01_ERROR_ROW_SIZE);
  context->row_bytes = (size_t)context->decoder.output_width *
                       (size_t)context->decoder.output_components;
  context->row = e01_alloc(context->budget, context->row_bytes, E01_CALLER);
  if (!context->row)
    abort_decode(context, E01_RESOURCE, E01_ERROR_CALLER_ALLOCATION);
  inject_phase(context, E01_PHASE_ROW);

  context->result.phase = E01_PHASE_SCANLINE;
  while (context->decoder.output_scanline < context->decoder.output_height) {
    if (jpeg_read_scanlines(&context->decoder, &context->row, 1) != 1)
      abort_decode(context, E01_IMAGE, E01_ERROR_SUSPENDED);
    context->result.rows_read++;
    inject_phase(context, E01_PHASE_SCANLINE);
  }
  context->result.phase = E01_PHASE_FINISH;
  inject_phase(context, E01_PHASE_FINISH); /* all real rows were consumed */
  if (!jpeg_finish_decompress(&context->decoder))
    abort_decode(context, E01_IMAGE, E01_ERROR_SUSPENDED);
  context->result.finish_complete = 1;
  if (context->result.warnings ||
      context->result.rows_read != context->result.height)
    abort_decode(context, E01_INFRA, E01_ERROR_INVARIANT);
  context->result.status = E01_ACCEPT;
  context->result.error_code = 0;
}

static void cleanup(struct context *context)
{
  context->cleanup_active = 1;
  /* Never jump into the returned decode frame. A destroy callback also has
   * a live C-local boundary and is classified as infrastructure failure. */
  if (setjmp(context->error.jump) == 0) {
    if (context->result.create_attempted) {
      context->result.destroy_called = 1;
      jpeg_destroy_decompress(&context->decoder);
    }
  }
  context->result.decoder_mem_null = context->decoder.mem == NULL;
  if (!context->result.decoder_mem_null) {
    context->result.status = E01_INFRA;
    context->result.error_code = E01_ERROR_CLEANUP;
  }
  if (context->fd >= 0) {
    int owned_fd = context->fd;
    int close_status = close(owned_fd);
    int close_error = errno;
    /* Linux close is not retried after EINTR. Check actual kernel state;
     * assigning -1 to our local variable is not ownership evidence. */
    errno = 0;
    context->result.fd_closed = fcntl(owned_fd, F_GETFD) == -1 && errno == EBADF;
    if (close_status != 0 || !context->result.fd_closed) {
      context->result.status = E01_INFRA;
      context->result.error_code = close_status != 0 ? close_error
                                                    : E01_ERROR_CLEANUP;
    }
    context->fd = -1;
  }
  if (context->row) {
    (void)e01_free(context->budget, context->row,
                   context->row_bytes, E01_CALLER);
    context->row = NULL;
  }
  /* Inject after destroy/row/FD release. Context free and accounting still
   * run in e01_validate_fd; a real earlier failure is never masked. */
  if (context->fault_phase == E01_PHASE_RELEASE &&
      context->result.status == E01_ACCEPT) {
    context->result.phase = E01_PHASE_RELEASE;
    context->result.phase_fault_injected = 1;
    context->result.status = E01_INFRA;
    context->result.error_code = E01_ERROR_PHASE_FAULT;
  }
}

static void copy_budget(struct e01_result *result,
                         const struct e01_budget *budget)
{
  result->limit = budget->limit;
  result->attempts = budget->attempts;
  result->injected = budget->injected;
  result->denied = budget->denied;
  result->peak = budget->peak;
  result->live = budget->live;
  result->live_blocks = budget->live_blocks;
  result->bad_frees = budget->bad_frees;
  result->small_calls = budget->calls[E01_SMALL];
  result->large_calls = budget->calls[E01_LARGE];
  result->caller_calls = budget->calls[E01_CALLER];
}

int e01_validate_fd(int input_fd, const struct e01_options *options,
                    struct e01_result *result)
{
  struct e01_options defaults;
  struct e01_budget budget;
  struct context *context;
  struct timestamp begin = timestamp_now(), decode_begin = {0}, decode_end = {0};
  struct timestamp cleanup_begin, end;
  struct rusage usage;
  int decoded = 0;

  if (!result) return E01_INFRA;
  initialize_result(result);
  e01_options_init(&defaults);
  if (!options) options = &defaults;
  e01_budget_init(&budget, options->limit, options->fail_at, options->fail_from);
  if ((unsigned int)options->fault_phase > (unsigned int)E01_PHASE_RELEASE) {
    result->error_code = E01_ERROR_ARGUMENTS;
    cleanup_begin = timestamp_now();
  } else {
    context = e01_alloc(&budget, sizeof(*context), E01_CALLER);
    if (!context) {
      result->status = E01_RESOURCE;
      result->error_code = E01_ERROR_CALLER_ALLOCATION;
      cleanup_begin = timestamp_now();
    } else {
      memset(context, 0, sizeof(*context));
      context->budget = &budget;
      context->fd = -1;
      context->fault_phase = options->fault_phase;
      initialize_result(&context->result);
      if (acquire_input(context, input_fd)) {
        decode_begin = timestamp_now();
        decode(context);
        decode_end = timestamp_now();
        decoded = 1;
      }
      cleanup_begin = timestamp_now();
      cleanup(context);
      *result = context->result;
      (void)e01_free(&budget, context, sizeof(*context), E01_CALLER);
    }
  }
  /* Observe counters only after all actual frees, including context. Never
   * reset accounting to disguise an incomplete cleanup as balanced. */
  if (budget.live || budget.live_blocks || budget.bad_frees || budget.head ||
      !result->decoder_mem_null || !result->fd_closed) {
    result->status = E01_INFRA;
    result->error_code = E01_ERROR_CLEANUP;
  }
  copy_budget(result, &budget);
  end = timestamp_now();
  result->timings_available = begin.valid && cleanup_begin.valid && end.valid &&
    (!decoded || (decode_begin.valid && decode_end.valid));
  if (result->timings_available) {
    result->wall_ns = elapsed_ns(begin.wall, end.wall);
    result->cpu_ns = elapsed_ns(begin.cpu, end.cpu);
    result->cleanup_wall_ns = elapsed_ns(cleanup_begin.wall, end.wall);
    result->cleanup_cpu_ns = elapsed_ns(cleanup_begin.cpu, end.cpu);
    if (decoded) {
      result->decode_wall_ns = elapsed_ns(decode_begin.wall, decode_end.wall);
      result->decode_cpu_ns = elapsed_ns(decode_begin.cpu, decode_end.cpu);
    }
  }
  if (getrusage(RUSAGE_SELF, &usage) == 0 && usage.ru_maxrss >= 0) {
    result->peak_rss_kib = (uint64_t)usage.ru_maxrss;
    result->rss_available = 1;
  }
  return result->status;
}

#ifndef E01_NO_MAIN
static const char *status_name(enum e01_status status)
{
  switch (status) {
    case E01_ACCEPT: return "ACCEPT";
    case E01_IMAGE: return "IMAGE_REJECT";
    case E01_RESOURCE: return "RESOURCE_REJECT";
    default: return "INFRA_ERROR";
  }
}

static const char *phase_name(enum e01_phase phase)
{
  switch (phase) {
    case E01_PHASE_HEADER: return "header";
    case E01_PHASE_START: return "start";
    case E01_PHASE_ROW: return "row";
    case E01_PHASE_SCANLINE: return "scanline";
    case E01_PHASE_FINISH: return "finish";
    case E01_PHASE_RELEASE: return "release";
    default: return "create";
  }
}

static const char *json_boolean(int value) { return value ? "true" : "false"; }

static void print_result(const struct e01_result *result)
{
  printf("{\"status\":\"%s\",\"phase\":\"%s\"",
         status_name(result->status), phase_name(result->phase));
  printf(",\"attempts\":%" PRIu64 ",\"injected\":%" PRIu64
         ",\"denied\":%" PRIu64 ",\"peak\":%zu,\"live\":%zu"
         ",\"liveBlocks\":%zu,\"badFrees\":%zu",
         result->attempts, result->injected, result->denied, result->peak,
         result->live, result->live_blocks, result->bad_frees);
  printf(",\"smallCalls\":%" PRIu64 ",\"largeCalls\":%" PRIu64
         ",\"callerCalls\":%" PRIu64,
         result->small_calls, result->large_calls, result->caller_calls);
  printf(",\"createAttempted\":%s,\"destroyCalled\":%s"
         ",\"decoderMemNull\":%s,\"fdClosed\":%s",
         json_boolean(result->create_attempted),
         json_boolean(result->destroy_called),
         json_boolean(result->decoder_mem_null), json_boolean(result->fd_closed));
  printf(",\"rowsRead\":%" PRIu64 ",\"height\":%" PRIu64
         ",\"finishComplete\":%s,\"warnings\":%" PRIu64
         ",\"errorCode\":%d,\"compressedRead\":%" PRIu64,
         result->rows_read, result->height, json_boolean(result->finish_complete),
         result->warnings, result->error_code, result->compressed_read);
  printf(",\"width\":%" PRIu64 ",\"dataPrecision\":%d,\"inputComponents\":%d"
         ",\"outputComponents\":%d,\"jpegColorSpace\":%d,\"outputColorSpace\":%d"
         ",\"progressive\":%s,\"phaseFaultInjected\":%s,\"fdAcquired\":%s"
         ",\"ownedFdNumber\":%d,\"limit\":%zu,\"limitIsProductCap\":false",
         result->width, result->data_precision, result->input_components,
         result->output_components, result->jpeg_color_space,
         result->output_color_space, json_boolean(result->progressive),
         json_boolean(result->phase_fault_injected),
         json_boolean(result->fd_acquired), result->owned_fd_number, result->limit);
  printf(",\"wallNs\":%" PRIu64 ",\"cpuNs\":%" PRIu64
         ",\"decodeWallNs\":%" PRIu64 ",\"decodeCpuNs\":%" PRIu64
         ",\"cleanupWallNs\":%" PRIu64 ",\"cleanupCpuNs\":%" PRIu64
         ",\"timingsAvailable\":%s,\"peakRssKiB\":%" PRIu64
         ",\"rssAvailable\":%s,\"rssIsProcessHighWater\":true}\n",
         result->wall_ns, result->cpu_ns, result->decode_wall_ns,
         result->decode_cpu_ns, result->cleanup_wall_ns, result->cleanup_cpu_ns,
         json_boolean(result->timings_available), result->peak_rss_kib,
         json_boolean(result->rss_available));
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

static int parse_options(int argc, char **argv, struct e01_options *options)
{
  unsigned int seen = 0;
  uint64_t value;
  int index;
  e01_options_init(options);
  if (argc < 2 || !argv[1][0] || argv[1][0] == '-') return 0;
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
    } else if (!strcmp(argv[index], "--fault-phase") && !(seen & 8) &&
               index + 1 < argc) {
      enum e01_phase phase;
      const char *name = argv[++index];
      for (phase = E01_PHASE_CREATE; phase <= E01_PHASE_RELEASE; phase++)
        if (!strcmp(name, phase_name(phase))) break;
      if (phase > E01_PHASE_RELEASE) return 0;
      options->fault_phase = phase;
      seen |= 8;
    } else return 0;
  }
  return 1;
}

int main(int argc, char **argv)
{
  struct e01_options options;
  struct e01_result result;
  int input_fd;
  initialize_result(&result);
  if (!parse_options(argc, argv, &options)) {
    result.error_code = E01_ERROR_ARGUMENTS;
    result.limit = options.limit;
    fprintf(stderr, "Usage: strict_probe FILE [--limit BYTES] "
                    "[--fail-at N] [--fail-from N] "
                    "[--fault-phase create|header|start|row|scanline|finish|release]\n");
  } else {
    /* CLI owns this path descriptor. Core only owns its duplicate. */
    input_fd = open(argv[1], O_RDONLY | O_CLOEXEC | O_NONBLOCK | O_NOFOLLOW);
    if (input_fd < 0) {
      result.error_code = errno;
      result.limit = options.limit;
    } else {
      (void)e01_validate_fd(input_fd, &options, &result);
      if (close(input_fd) != 0) {
        result.status = E01_INFRA;
        result.error_code = errno;
      }
    }
  }
  print_result(&result);
  return result.status;
}
#endif
