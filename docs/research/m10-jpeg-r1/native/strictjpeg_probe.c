/*
 * M10-R1 isolated research CLI, not an App/JNI integration.
 *
 * Acceptance/compile contract (execution belongs to the external runner):
 *   link against the same fixed libjpeg-turbo 3.2.0 libjpeg + libturbojpeg.
 *   strictjpeg_probe --mode a2|a1 --memory-mib 384 --scan-limit 100 FILE
 *   locked complete.jpg: exit 0 / ACCEPT / complete decode / no warnings.
 *   locked entropy_cut_eoi_retained.jpg: exit 10 / REJECT, not a crash.
 *   A2 must read every output row and finish; A1 must finish tj3Decompress8.
 * No result in this source is evidence that those experiments were executed.
 *
 * The upstream intermediate-memory setting is NOT a total native-heap cap.
 * trackedCallerBytesAfter covers this harness's context/input/output mallocs
 * only.  ru_maxrss is the process lifetime high-water mark, not decoder heap.
 * Decoder/library/stdio allocations are explicitly outside caller tracking.
 * No signal/crash handler attempts to turn a native crash into an image reject.
 */
#define _POSIX_C_SOURCE 200809L
#define _FILE_OFFSET_BITS 64

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <inttypes.h>
#include <stddef.h>
#include <string.h>
#include <limits.h>
#include <errno.h>
#include <setjmp.h>
#include <signal.h>
#include <time.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/resource.h>
#include <unistd.h>

#include <jpeglib.h>
#include <jerror.h>
#include <turbojpeg.h>

#define STRINGIFY_INNER(x) #x
#define STRINGIFY(x) STRINGIFY_INNER(x)
#define INPUT_LIMIT (UINT64_C(64) * 1024 * 1024)
#define PIXEL_LIMIT UINT64_C(40000000)
#define SOURCE_CHUNK 8192
#define MESSAGE_CAPACITY 1024
#define REPEAT_LIMIT 1000

enum probe_status {
  PROBE_ACCEPT = 0, PROBE_REJECT = 10, PROBE_RESOURCE = 20,
  PROBE_CANCELLED = 30, PROBE_INFRA = 40
};

struct options {
  const char *mode;
  const char *path;
  long memory_mib;
  int scan_limit;
  uint64_t cancel_after_progress;
  int cancel_progress_enabled;
  double cancel_after_ms;
  int fail_row_alloc;
  unsigned int repeat;
};

struct result {
  enum probe_status status;
  char message[MESSAGE_CAPACITY];
  char operation[64];
  int error_code;
  uint64_t width, height, input_bytes, rows_read;
  uint64_t output_width, output_height, output_buffer_bytes;
  uint64_t caller_peak, caller_after, compressed_read, compressed_consumed;
  uint64_t progress_checks, cancellation_checks;
  unsigned int warning_count, fatal_count;
  int components, output_components, precision, progressive, scan_number;
  int finish_complete, header_complete, destroy_called, file_closed;
  int compressed_consumed_known, injected_alloc_failure;
  int native_cancel_supported, signal_cancel_seen;
  long peak_rss_kib, rss_after_kib;
  long first_rss_after_kib, min_rss_after_kib, max_rss_after_kib;
  int fds_before, fds_after;
  double wall_ms, cpu_ms, started_ms;
  double max_check_gap_ms, last_check_ms, cancellation_lateness_ms;
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
  struct context *owner;
};

struct strict_progress {
  struct jpeg_progress_mgr pub;
  struct context *owner;
};

/* All mutable state used after longjmp lives in the heap context.  No C++
 * frame, VLA, or destructor responsibility is crossed by longjmp. */
struct context {
  struct options options;
  struct result result;
  FILE *file;
  unsigned char *input;
  unsigned char *output;
  uint64_t tracked;
  struct jpeg_decompress_struct decoder;
  struct strict_error error;
  struct bounded_source source;
  struct strict_progress progress;
  int create_attempted;
  tjhandle turbo;
};

static volatile sig_atomic_t cancellation_signal = 0;

static void on_cancel_signal(int signo)
{
  (void)signo;
  cancellation_signal = 1;
}

static double clock_ms(clockid_t clock_id)
{
  struct timespec value;
  if (clock_gettime(clock_id, &value) != 0) return -1.0;
  return (double)value.tv_sec * 1000.0 + (double)value.tv_nsec / 1000000.0;
}

static int count_fds(void)
{
  DIR *directory = opendir("/proc/self/fd");
  struct dirent *entry;
  int count = 0;
  if (!directory) return -1;
  while ((entry = readdir(directory)) != NULL)
    if (strcmp(entry->d_name, ".") && strcmp(entry->d_name, "..")) count++;
  if (closedir(directory) != 0) return -1;
  return count - 1; /* exclude the directory handle used by this observation */
}

static long current_rss_kib(void)
{
  FILE *file = fopen("/proc/self/status", "r");
  char line[256];
  long result = -1;
  if (!file) return -1;
  while (fgets(line, sizeof(line), file))
    if (sscanf(line, "VmRSS: %ld kB", &result) == 1) break;
  fclose(file);
  return result;
}

static const char *status_name(enum probe_status status)
{
  switch (status) {
    case PROBE_ACCEPT: return "ACCEPT";
    case PROBE_REJECT: return "REJECT";
    case PROBE_RESOURCE: return "RESOURCE_REJECT";
    case PROBE_CANCELLED: return "CANCELLED";
    default: return "INFRA_ERROR";
  }
}

static void set_result(struct context *context, enum probe_status status,
                       int code, const char *operation, const char *message)
{
  context->result.status = status;
  context->result.error_code = code;
  snprintf(context->result.operation, sizeof(context->result.operation), "%s",
           operation ? operation : "");
  snprintf(context->result.message, sizeof(context->result.message), "%s",
           message ? message : "");
}

static int checked_multiply(uint64_t a, uint64_t b, uint64_t *out)
{
  if (b != 0 && a > UINT64_MAX / b) return 0;
  *out = a * b;
  return *out <= SIZE_MAX;
}

static void *caller_allocate(struct context *context, uint64_t bytes)
{
  void *allocation;
  if (!bytes || bytes > SIZE_MAX || context->tracked > UINT64_MAX - bytes)
    return NULL;
  allocation = malloc((size_t)bytes);
  if (!allocation) return NULL;
  context->tracked += bytes;
  if (context->tracked > context->result.caller_peak)
    context->result.caller_peak = context->tracked;
  return allocation;
}

static int open_input(struct context *context)
{
  struct stat info;
  context->file = fopen(context->options.path, "rb");
  if (!context->file) {
    set_result(context, PROBE_INFRA, errno, "open", strerror(errno));
    return 0;
  }
  if (fstat(fileno(context->file), &info) != 0) {
    set_result(context, PROBE_INFRA, errno, "fstat", strerror(errno));
    return 0;
  }
  if (!S_ISREG(info.st_mode) || info.st_size < 0) {
    set_result(context, PROBE_INFRA, -1004, "fstat", "Input must be a regular file");
    return 0;
  }
  context->result.input_bytes = (uint64_t)info.st_size;
  if (context->result.input_bytes > INPUT_LIMIT) {
    set_result(context, PROBE_RESOURCE, -1001, "input-limit", "Input exceeds 64 MiB");
    return 0;
  }
  if (!context->result.input_bytes) {
    set_result(context, PROBE_REJECT, -1002, "input-limit", "Empty input");
    return 0;
  }
  return 1;
}

static struct context *from_jpeg(j_common_ptr decoder)
{
  return ((struct strict_error *)decoder->err)->owner;
}

static void abort_a2(struct context *context, enum probe_status status,
                     int code, const char *operation, const char *message)
{
  set_result(context, status, code, operation, message);
  longjmp(context->error.jump, 1);
}

static void strict_fatal(j_common_ptr decoder)
{
  struct context *context = from_jpeg(decoder);
  char message[JMSG_LENGTH_MAX];
  enum probe_status status = PROBE_REJECT;
  int code = decoder->err->msg_code;
  (*decoder->err->format_message)(decoder, message);
  context->result.fatal_count++;
  if (code == JERR_OUT_OF_MEMORY || code == JERR_NO_BACKING_STORE)
    status = PROBE_RESOURCE;
  abort_a2(context, status, code, "libjpeg-fatal", message);
}

static void strict_message(j_common_ptr decoder, int level)
{
  struct context *context = from_jpeg(decoder);
  if (level < 0) {
    char message[JMSG_LENGTH_MAX];
    (*decoder->err->format_message)(decoder, message);
    context->result.warning_count++;
    decoder->err->num_warnings++;
    abort_a2(context, PROBE_REJECT, decoder->err->msg_code,
             "libjpeg-warning", message);
  }
  /* Trace messages (level >= 0) are not corruption warnings. */
}

static void check_cancellation(struct context *context)
{
  double now = clock_ms(CLOCK_MONOTONIC);
  context->result.cancellation_checks++;
  if (context->result.last_check_ms > 0 && now > 0) {
    double gap = now - context->result.last_check_ms;
    if (gap > context->result.max_check_gap_ms)
      context->result.max_check_gap_ms = gap;
  }
  context->result.last_check_ms = now;
  if (cancellation_signal ||
      (context->options.cancel_progress_enabled &&
       context->result.progress_checks >= context->options.cancel_after_progress) ||
      (context->options.cancel_after_ms >= 0 && now >= 0 &&
       now - context->result.started_ms >= context->options.cancel_after_ms)) {
    context->result.signal_cancel_seen = cancellation_signal ? 1 : 0;
    if (context->options.cancel_after_ms >= 0 && now >= 0)
      context->result.cancellation_lateness_ms =
          now - context->result.started_ms - context->options.cancel_after_ms;
    abort_a2(context, PROBE_CANCELLED, -1003, "cooperative-cancel",
             "Native decoder stopped at an actual source/progress/row checkpoint");
  }
}

static void source_init(j_decompress_ptr decoder)
{
  (void)decoder;
}

static boolean source_fill(j_decompress_ptr decoder)
{
  struct context *context = from_jpeg((j_common_ptr)decoder);
  size_t wanted = SOURCE_CHUNK;
  size_t received;
  uint64_t remaining;
  check_cancellation(context);
  remaining = context->result.input_bytes - context->result.compressed_read;
  if (remaining < wanted) wanted = (size_t)remaining;
  if (!wanted) {
    int extra = fgetc(context->file);
    if (extra != EOF)
      abort_a2(context, PROBE_INFRA, -1004, "source-changed",
               "Input grew after fstat; research input identity is no longer stable");
    if (ferror(context->file))
      abort_a2(context, PROBE_INFRA, errno, "source-read", "Input read failed");
    decoder->err->msg_code = JWRN_JPEG_EOF;
    strict_message((j_common_ptr)decoder, -1);
    return FALSE;
  }
  received = fread(context->source.buffer, 1, wanted, context->file);
  context->result.compressed_read += received;
  context->source.pub.next_input_byte = context->source.buffer;
  context->source.pub.bytes_in_buffer = received;
  check_cancellation(context);
  if (ferror(context->file))
    abort_a2(context, PROBE_INFRA, errno, "source-read", "Input read failed");
  if (!received) {
    abort_a2(context, PROBE_INFRA, -1004, "source-changed",
             "Input became shorter than its fstat length");
  }
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

static void monitor_progress(j_common_ptr decoder)
{
  struct context *context = from_jpeg(decoder);
  j_decompress_ptr decompressor = (j_decompress_ptr)decoder;
  context->result.progress_checks++;
  context->result.scan_number = decompressor->input_scan_number;
  if (decompressor->input_scan_number > context->options.scan_limit)
    abort_a2(context, PROBE_RESOURCE, -1001, "scan-limit",
             "JPEG scan count exceeded the configured research limit");
  check_cancellation(context);
}

static void run_a2(struct context *context)
{
  uint64_t row_size;
  JSAMPROW row;
  context->result.native_cancel_supported = 1;
  context->decoder.err = jpeg_std_error(&context->error.pub);
  context->error.owner = context;
  context->error.pub.error_exit = strict_fatal;
  context->error.pub.emit_message = strict_message;
  if (setjmp(context->error.jump)) return;
  check_cancellation(context);
  context->create_attempted = 1;
  jpeg_create_decompress(&context->decoder);
  context->decoder.mem->max_memory_to_use = context->options.memory_mib * 1048576L;
  context->source.owner = context;
  context->source.pub.init_source = source_init;
  context->source.pub.fill_input_buffer = source_fill;
  context->source.pub.skip_input_data = source_skip;
  context->source.pub.resync_to_restart = jpeg_resync_to_restart;
  context->source.pub.term_source = source_term;
  context->decoder.src = &context->source.pub;
  context->progress.owner = context;
  context->progress.pub.progress_monitor = monitor_progress;
  context->decoder.progress = &context->progress.pub;
  if (jpeg_read_header(&context->decoder, TRUE) != JPEG_HEADER_OK)
    abort_a2(context, PROBE_REJECT, -1002, "header", "Incomplete JPEG header");
  context->result.header_complete = 1;
  context->result.width = context->decoder.image_width;
  context->result.height = context->decoder.image_height;
  context->result.components = context->decoder.num_components;
  context->result.precision = context->decoder.data_precision;
  context->result.progressive = context->decoder.progressive_mode ? 1 : 0;
  if (!context->result.width || !context->result.height ||
      !checked_multiply(context->result.width, context->result.height, &row_size) ||
      row_size > PIXEL_LIMIT)
    abort_a2(context, PROBE_RESOURCE, -1001, "pixel-limit", "Image exceeds 40 MP");
  if (context->decoder.data_precision != 8 || context->decoder.arith_code ||
      (context->decoder.num_components != 1 && context->decoder.num_components != 3) ||
      (context->decoder.jpeg_color_space != JCS_GRAYSCALE &&
       context->decoder.jpeg_color_space != JCS_RGB &&
       context->decoder.jpeg_color_space != JCS_YCbCr) ||
      (!context->decoder.progressive_mode &&
       (context->decoder.Ss != 0 || context->decoder.Se != 63)))
    abort_a2(context, PROBE_REJECT, -1002, "supported-coding",
             "Only 8-bit Huffman lossy grayscale/RGB/YCbCr JPEG is in this PoC scope");
  context->decoder.out_color_space =
      context->decoder.num_components == 1 ? JCS_GRAYSCALE : JCS_RGB;
  context->decoder.scale_num = 1;
  context->decoder.scale_denom = 1;
  check_cancellation(context);
  if (!jpeg_start_decompress(&context->decoder))
    abort_a2(context, PROBE_REJECT, -1002, "start", "Suspended decompression");
  context->result.output_width = context->decoder.output_width;
  context->result.output_height = context->decoder.output_height;
  context->result.output_components = context->decoder.output_components;
  if (context->decoder.output_components < 1 ||
      !checked_multiply(context->decoder.output_width,
                        (uint64_t)context->decoder.output_components, &row_size))
    abort_a2(context, PROBE_RESOURCE, -1001, "row-size", "Output row size overflow");
  context->result.output_buffer_bytes = row_size;
  if (context->options.fail_row_alloc) {
    context->result.injected_alloc_failure = 1;
    abort_a2(context, PROBE_RESOURCE, -1006, "row-allocation-injection",
             "Research fault injection: caller row allocation deliberately failed");
  }
  context->output = caller_allocate(context, row_size);
  if (!context->output)
    abort_a2(context, PROBE_RESOURCE, -1006, "row-allocation", "Caller row malloc failed");
  while (context->decoder.output_scanline < context->decoder.output_height) {
    check_cancellation(context);
    row = context->output;
    if (jpeg_read_scanlines(&context->decoder, &row, 1) != 1)
      abort_a2(context, PROBE_REJECT, -1002, "scanline", "Incomplete output scanline");
    context->result.rows_read++;
  }
  check_cancellation(context);
  if (!jpeg_finish_decompress(&context->decoder))
    abort_a2(context, PROBE_REJECT, -1002, "finish", "Incomplete JPEG finish");
  context->result.finish_complete = 1;
  context->result.scan_number = context->decoder.input_scan_number;
  if (context->result.warning_count || context->result.fatal_count ||
      context->result.rows_read != context->result.output_height)
    abort_a2(context, PROBE_INFRA, -1005, "accept-invariant", "Decode completion invariant failed");
  set_result(context, PROBE_ACCEPT, 0, "jpeg_finish_decompress", "All output scanlines read; finish succeeded without warnings");
}

static void turbo_error(struct context *context, const char *operation,
                        int infrastructure)
{
  int code = context->turbo ? tj3GetErrorCode(context->turbo) : TJERR_FATAL;
  const char *message = tj3GetErrorStr(context->turbo);
  enum probe_status status = infrastructure ? PROBE_INFRA : PROBE_REJECT;
  if (!message) message = "TurboJPEG supplied no error string";
  if (code == TJERR_WARNING) context->result.warning_count++;
  else context->result.fatal_count++;
  if (!infrastructure &&
      (strstr(message, "Memory limit exceeded") ||
       strstr(message, "Insufficient memory") ||
       strstr(message, "Memory allocation failure") ||
       strstr(message, "Progressive JPEG image has more than") ||
       strstr(message, "Image is too large"))) status = PROBE_RESOURCE;
  set_result(context, status, code, operation, message);
}

static int turbo_parameter(struct context *context, int parameter, int value)
{
  if (tj3Set(context->turbo, parameter, value) != 0) {
    turbo_error(context, "tj3Set", 1);
    return 0;
  }
  if (tj3Get(context->turbo, parameter) != value) {
    set_result(context, PROBE_INFRA, -1005, "tj3Get-parameter",
               "TurboJPEG parameter read-back did not equal the requested value");
    return 0;
  }
  return 1;
}

static void run_a1(struct context *context)
{
  uint64_t pixels, output_size, scaled_width, scaled_height;
  int width, height, colorspace, precision, lossless, progressive, arithmetic;
  int pixel_format, output_components;
  int extra;
  tjscalingfactor scaling = { 1, 8 };
  context->result.native_cancel_supported = 0;
  if (context->options.cancel_progress_enabled || context->options.cancel_after_ms >= 0) {
    set_result(context, PROBE_INFRA, -1005, "cancel-unsupported-a1",
               "The public TurboJPEG API exposes no cooperative decode cancellation callback; not tested as cancelled");
    return;
  }
  context->input = caller_allocate(context, context->result.input_bytes);
  if (!context->input) {
    set_result(context, PROBE_RESOURCE, -1006, "input-allocation", "Bounded input malloc failed");
    return;
  }
  if (fread(context->input, 1, (size_t)context->result.input_bytes, context->file) !=
      (size_t)context->result.input_bytes) {
    set_result(context, PROBE_INFRA, -1004, "input-read", "Input read failed or file length changed");
    return;
  }
  context->result.compressed_read = context->result.input_bytes;
  extra = fgetc(context->file);
  if (extra != EOF || ferror(context->file)) {
    set_result(context, PROBE_INFRA, -1004, "input-read", "Input grew or read failed after fstat");
    return;
  }
  context->turbo = tj3Init(TJINIT_DECOMPRESS);
  if (!context->turbo) {
    turbo_error(context, "tj3Init", 1);
    return;
  }
  if (!turbo_parameter(context, TJPARAM_STOPONWARNING, 1) ||
      !turbo_parameter(context, TJPARAM_MAXMEMORY, (int)context->options.memory_mib) ||
      !turbo_parameter(context, TJPARAM_MAXPIXELS, (int)PIXEL_LIMIT) ||
      !turbo_parameter(context, TJPARAM_SCANLIMIT, context->options.scan_limit) ||
      !turbo_parameter(context, TJPARAM_SAVEMARKERS, 0)) return;
  if (tj3DecompressHeader(context->turbo, context->input,
                          (size_t)context->result.input_bytes) != 0) {
    turbo_error(context, "tj3DecompressHeader", 0);
    return;
  }
  context->result.header_complete = 1;
  width = tj3Get(context->turbo, TJPARAM_JPEGWIDTH);
  height = tj3Get(context->turbo, TJPARAM_JPEGHEIGHT);
  colorspace = tj3Get(context->turbo, TJPARAM_COLORSPACE);
  precision = tj3Get(context->turbo, TJPARAM_PRECISION);
  lossless = tj3Get(context->turbo, TJPARAM_LOSSLESS);
  progressive = tj3Get(context->turbo, TJPARAM_PROGRESSIVE);
  arithmetic = tj3Get(context->turbo, TJPARAM_ARITHMETIC);
  if (width < 1 || height < 1 || precision < 0 || colorspace < 0 || lossless < 0 || progressive < 0 || arithmetic < 0) {
    set_result(context, PROBE_INFRA, -1005, "tj3Get-header", "Missing TurboJPEG header parameters");
    return;
  }
  context->result.width = (uint64_t)width;
  context->result.height = (uint64_t)height;
  context->result.components = colorspace == TJCS_GRAY ? 1 :
      (colorspace == TJCS_CMYK || colorspace == TJCS_YCCK ? 4 : 3);
  context->result.precision = precision;
  context->result.progressive = progressive;
  if (!checked_multiply((uint64_t)width, (uint64_t)height, &pixels) || pixels > PIXEL_LIMIT) {
    set_result(context, PROBE_RESOURCE, -1001, "pixel-limit", "Image exceeds 40 MP");
    return;
  }
  if (precision != 8 || lossless || arithmetic ||
      (colorspace != TJCS_GRAY && colorspace != TJCS_RGB && colorspace != TJCS_YCbCr)) {
    set_result(context, PROBE_REJECT, -1002, "supported-coding",
               "Only 8-bit Huffman lossy grayscale/RGB/YCbCr JPEG is in this PoC scope");
    return;
  }
  if (tj3SetScalingFactor(context->turbo, scaling) != 0) {
    turbo_error(context, "tj3SetScalingFactor", 1);
    return;
  }
  /* RGB-coded JPEG uses RGB output rather than an unsupported RGB-to-gray
   * conversion.  Both paths are 1/8 scale, never full-resolution RGBA. */
  pixel_format = colorspace == TJCS_RGB ? TJPF_RGB : TJPF_GRAY;
  output_components = colorspace == TJCS_RGB ? 3 : 1;
  scaled_width = ((uint64_t)width + 7) / 8;
  scaled_height = ((uint64_t)height + 7) / 8;
  context->result.output_width = scaled_width;
  context->result.output_height = scaled_height;
  context->result.output_components = output_components;
  if (!checked_multiply(scaled_width, scaled_height, &output_size) ||
      !checked_multiply(output_size, (uint64_t)output_components, &output_size)) {
    set_result(context, PROBE_RESOURCE, -1001, "output-size", "Scaled output capacity overflow");
    return;
  }
  context->result.output_buffer_bytes = output_size;
  if (context->options.fail_row_alloc) {
    context->result.injected_alloc_failure = 1;
    set_result(context, PROBE_RESOURCE, -1006, "output-allocation-injection",
               "Research fault injection: caller scaled output allocation deliberately failed");
    return;
  }
  context->output = caller_allocate(context, output_size);
  if (!context->output) {
    set_result(context, PROBE_RESOURCE, -1006, "output-allocation", "Bounded scaled output malloc failed");
    return;
  }
  if (tj3Decompress8(context->turbo, context->input,
                    (size_t)context->result.input_bytes, context->output, 0,
                    pixel_format) != 0) {
    turbo_error(context, "tj3Decompress8", 0);
    return;
  }
  /* These fields reflect the public call's success contract.  A1 exposes
   * neither per-row counters nor a separate jpeg_finish_decompress result. */
  context->result.rows_read = scaled_height;
  context->result.finish_complete = 1;
  set_result(context, PROBE_ACCEPT, 0, "tj3Decompress8",
             "Full public decompression call succeeded without a warning return; row/finish completion inferred from upstream API implementation");
}

static struct result run_once(const struct options *options)
{
  struct result result;
  struct context *context;
  struct rusage usage;
  double start = clock_ms(CLOCK_MONOTONIC), cpu_start = clock_ms(CLOCK_PROCESS_CPUTIME_ID);
  memset(&result, 0, sizeof(result));
  result.fds_before = count_fds();
  if (start < 0 || cpu_start < 0) {
    result.status = PROBE_INFRA;
    result.error_code = errno;
    result.wall_ms = -1;
    result.cpu_ms = -1;
    result.peak_rss_kib = -1;
    result.rss_after_kib = -1;
    result.fds_after = count_fds();
    snprintf(result.operation, sizeof(result.operation), "clock_gettime");
    snprintf(result.message, sizeof(result.message), "Required monotonic/CPU clock unavailable; no timing or cancellation deadline claim is valid");
    return result;
  }
  context = calloc(1, sizeof(*context));
  if (!context) {
    result.status = PROBE_RESOURCE;
    result.error_code = -1006;
    snprintf(result.message, sizeof(result.message), "Research context calloc failed");
    snprintf(result.operation, sizeof(result.operation), "context-allocation");
  } else {
    context->options = *options;
    context->result.fds_before = result.fds_before;
    context->result.started_ms = start;
    context->result.caller_peak = sizeof(*context);
    context->tracked = sizeof(*context);
    context->result.status = PROBE_INFRA;
    if (open_input(context)) {
      if (!strcmp(options->mode, "a2")) run_a2(context);
      else run_a1(context);
    }
    if (context->create_attempted) {
      /* Destroy has a void API.  The initialized/partial initialized memory
       * manager is destroyed even after a warning, fatal, or cancellation. */
      context->create_attempted = 0;
      if (context->decoder.src) {
        context->result.compressed_consumed = context->result.compressed_read -
            context->source.pub.bytes_in_buffer;
        context->result.compressed_consumed_known = 1;
      }
      jpeg_destroy_decompress(&context->decoder);
      context->result.destroy_called = 1;
    }
    if (context->turbo) {
      tj3Destroy(context->turbo); /* no return value in the official API */
      context->turbo = NULL;
      context->result.destroy_called = 1;
    }
    if (context->output) {
      free(context->output);
      context->tracked -= context->result.output_buffer_bytes;
    }
    if (context->input) {
      free(context->input);
      context->tracked -= context->result.input_bytes;
    }
    if (context->file) {
      if (fclose(context->file) != 0)
        set_result(context, PROBE_INFRA, errno, "fclose", "Input fclose failed during cleanup");
      else context->result.file_closed = 1;
    }
    context->tracked -= sizeof(*context);
    context->result.caller_after = context->tracked;
    result = context->result;
    free(context);
  }
  result.fds_after = count_fds();
  if (getrusage(RUSAGE_SELF, &usage) == 0) result.peak_rss_kib = usage.ru_maxrss;
  else result.peak_rss_kib = -1;
  result.rss_after_kib = current_rss_kib();
  result.wall_ms = clock_ms(CLOCK_MONOTONIC) - start;
  result.cpu_ms = clock_ms(CLOCK_PROCESS_CPUTIME_ID) - cpu_start;
  if (result.caller_after != 0 ||
      (result.fds_before >= 0 && result.fds_after >= 0 && result.fds_before != result.fds_after)) {
    result.status = PROBE_INFRA;
    result.error_code = -1005;
    snprintf(result.operation, sizeof(result.operation), "cleanup-invariant");
    snprintf(result.message, sizeof(result.message), "Caller allocation or FD balance changed after cleanup");
  }
  return result;
}

static void json_string(const char *text)
{
  const unsigned char *cursor = (const unsigned char *)(text ? text : "");
  putchar('"');
  while (*cursor) {
    unsigned char value = *cursor++;
    if (value == '"' || value == '\\') { putchar('\\'); putchar(value); }
    else if (value < 0x20) printf("\\u%04x", value);
    else putchar(value);
  }
  putchar('"');
}

static void print_result(const struct result *result, const struct options *options,
                         unsigned int completed, uint64_t warning_total,
                         uint64_t fatal_total, double total_wall, double total_cpu,
                         int repeated_cleanup_ok, int status_consistent)
{
  printf("{\"status\":"); json_string(status_name(result->status));
  printf(",\"exitCode\":%d,\"mode\":", result->status); json_string(options->mode);
  printf(",\"libraryVersion\":"); json_string(STRINGIFY(LIBJPEG_TURBO_VERSION));
  printf(",\"input\":"); json_string(options->path);
  printf(",\"operation\":"); json_string(result->operation);
  printf(",\"message\":"); json_string(result->message);
  printf(",\"errorCode\":%d,\"warningCount\":%u,\"fatalCount\":%u",
         result->error_code, result->warning_count, result->fatal_count);
  printf(",\"width\":%" PRIu64 ",\"height\":%" PRIu64 ",\"components\":%d,\"precision\":%d,\"progressive\":%s",
         result->width, result->height, result->components, result->precision,
         result->progressive ? "true" : "false");
  printf(",\"inputBytes\":%" PRIu64 ",\"compressedBytesRead\":%" PRIu64 ",\"compressedBytesConsumed\":",
         result->input_bytes, result->compressed_read);
  if (result->compressed_consumed_known) printf("%" PRIu64, result->compressed_consumed);
  else printf("null");
  printf(",\"headerComplete\":%s,\"finishComplete\":%s,\"rowsRead\":%" PRIu64,
         result->header_complete ? "true" : "false", result->finish_complete ? "true" : "false", result->rows_read);
  printf(",\"rowFinishObservation\":");
  json_string(!strcmp(options->mode, "a2") ? "direct-libjpeg-return-and-row-counter" : "inferred-from-tj3Decompress8-success-contract");
  printf(",\"outputWidth\":%" PRIu64 ",\"outputHeight\":%" PRIu64 ",\"outputComponents\":%d,\"outputBufferBytes\":%" PRIu64,
         result->output_width, result->output_height, result->output_components, result->output_buffer_bytes);
  printf(",\"wallMs\":%.3f,\"cpuMs\":%.3f,\"peakRssKiB\":%ld,\"rssAfterKiB\":%ld",
         result->wall_ms, result->cpu_ms, result->peak_rss_kib, result->rss_after_kib);
  printf(",\"firstIterationRssAfterKiB\":%ld,\"minIterationRssAfterKiB\":%ld,\"maxIterationRssAfterKiB\":%ld",
         result->first_rss_after_kib, result->min_rss_after_kib, result->max_rss_after_kib);
  printf(",\"rssMeasurementScope\":\"process-lifetime-high-water-not-native-heap\"");
  printf(",\"libraryMemorySettingBytes\":%" PRIu64 ",\"libraryMemorySettingIsTotalHeapCap\":false",
         (uint64_t)options->memory_mib * 1048576);
  printf(",\"trackedCallerPeakBytes\":%" PRIu64 ",\"trackedCallerBytesAfter\":%" PRIu64,
         result->caller_peak, result->caller_after);
  printf(",\"callerTrackingScope\":\"context-input-output-only-excludes-libc-and-decoder\"");
  printf(",\"openFdsBefore\":%d,\"openFdsAfter\":%d,\"destroyCalled\":%s,\"fileClosed\":%s",
         result->fds_before, result->fds_after, result->destroy_called ? "true" : "false", result->file_closed ? "true" : "false");
  printf(",\"scanLimit\":%d,\"scanNumber\":%d,\"progressChecks\":%" PRIu64 ",\"cancellationChecks\":%" PRIu64,
         options->scan_limit, result->scan_number, result->progress_checks, result->cancellation_checks);
  printf(",\"nativeCooperativeCancellationSupported\":%s,\"signalCancellationSeen\":%s,\"maxCancellationCheckGapMs\":%.3f,\"cancellationLatenessMs\":",
         result->native_cancel_supported ? "true" : "false", result->signal_cancel_seen ? "true" : "false",
         result->max_check_gap_ms);
  if (options->cancel_after_ms >= 0 && result->status == PROBE_CANCELLED)
    printf("%.3f", result->cancellation_lateness_ms);
  else printf("null");
  printf(",\"injectedCallerAllocationFailure\":%s,\"iterationsRequested\":%u,\"iterationsCompleted\":%u",
         result->injected_alloc_failure ? "true" : "false", options->repeat, completed);
  printf(",\"perDecodeFieldsScope\":\"last-iteration\",\"repeatedWarningsTotal\":%" PRIu64 ",\"repeatedFatalsTotal\":%" PRIu64 ",\"totalWallMs\":%.3f,\"totalCpuMs\":%.3f,\"allIterationsCallerCleanupBalanced\":%s,\"allIterationsStatusConsistent\":%s}\n",
         warning_total, fatal_total, total_wall, total_cpu,
         repeated_cleanup_ok ? "true" : "false", status_consistent ? "true" : "false");
}

static int parse_unsigned(const char *text, uint64_t maximum, uint64_t *out)
{
  char *end = NULL;
  unsigned long long value;
  if (!text || !*text || *text == '-' || *text == '+') return 0;
  errno = 0;
  value = strtoull(text, &end, 10);
  if (errno || !end || *end || value > maximum) return 0;
  *out = (uint64_t)value;
  return 1;
}

int main(int argc, char **argv)
{
  struct options options;
  struct result result;
  struct sigaction action;
  uint64_t value, warning_total = 0, fatal_total = 0;
  double total_wall = 0, total_cpu = 0;
  unsigned int completed = 0, iteration;
  long first_rss = -1, min_rss = -1, max_rss = -1;
  int cleanup_ok = 1, status_consistent = 1;
  enum probe_status first_status = PROBE_INFRA;
  memset(&options, 0, sizeof(options));
  memset(&result, 0, sizeof(result));
  options.mode = "a2";
  options.path = "";
  options.memory_mib = 384;
  options.scan_limit = 100;
  options.cancel_after_ms = -1;
  options.repeat = 1;
  for (int index = 1; index < argc; index++) {
    const char *argument = argv[index];
    if (!strcmp(argument, "--fail-row-alloc")) options.fail_row_alloc = 1;
    else if (!strcmp(argument, "--mode") && index + 1 < argc) options.mode = argv[++index];
    else if (!strcmp(argument, "--memory-mib") && index + 1 < argc) {
      if (!parse_unsigned(argv[++index], (uint64_t)LONG_MAX / 1048576, &value) || value < 1) goto bad_arguments;
      options.memory_mib = (long)value;
    } else if (!strcmp(argument, "--scan-limit") && index + 1 < argc) {
      if (!parse_unsigned(argv[++index], INT_MAX, &value) || value < 1) goto bad_arguments;
      options.scan_limit = (int)value;
    } else if (!strcmp(argument, "--cancel-after-progress") && index + 1 < argc) {
      if (!parse_unsigned(argv[++index], UINT64_MAX, &value)) goto bad_arguments;
      options.cancel_progress_enabled = 1;
      options.cancel_after_progress = value;
    } else if (!strcmp(argument, "--cancel-after-ms") && index + 1 < argc) {
      if (!parse_unsigned(argv[++index], UINT32_MAX, &value)) goto bad_arguments;
      options.cancel_after_ms = (double)value;
    } else if (!strcmp(argument, "--repeat") && index + 1 < argc) {
      if (!parse_unsigned(argv[++index], REPEAT_LIMIT, &value) || value < 1) goto bad_arguments;
      options.repeat = (unsigned int)value;
    } else if (argument[0] == '-' || options.path[0]) goto bad_arguments;
    else options.path = argument;
  }
  if (!options.path[0] || (strcmp(options.mode, "a2") && strcmp(options.mode, "a1")) ||
      options.memory_mib > INT_MAX) goto bad_arguments;
  memset(&action, 0, sizeof(action));
  action.sa_handler = on_cancel_signal;
  sigemptyset(&action.sa_mask);
  if (!strcmp(options.mode, "a2") && sigaction(SIGUSR1, &action, NULL) != 0) {
    result.status = PROBE_INFRA;
    result.error_code = errno;
    snprintf(result.operation, sizeof(result.operation), "sigaction");
    snprintf(result.message, sizeof(result.message), "%s", strerror(errno));
    print_result(&result, &options, 0, 0, 0, 0, 0, 1, 1);
    return PROBE_INFRA;
  }
  cancellation_signal = 0;
  for (iteration = 0; iteration < options.repeat; iteration++) {
    result = run_once(&options);
    completed++;
    if (!iteration) first_rss = result.rss_after_kib;
    if (result.rss_after_kib >= 0) {
      if (min_rss < 0 || result.rss_after_kib < min_rss) min_rss = result.rss_after_kib;
      if (max_rss < 0 || result.rss_after_kib > max_rss) max_rss = result.rss_after_kib;
    }
    if (!iteration) first_status = result.status;
    else if (first_status != result.status) status_consistent = 0;
    warning_total += result.warning_count;
    fatal_total += result.fatal_count;
    total_wall += result.wall_ms;
    total_cpu += result.cpu_ms;
    if (result.caller_after || (result.fds_before >= 0 && result.fds_after >= 0 &&
        result.fds_before != result.fds_after)) cleanup_ok = 0;
    if (result.status == PROBE_INFRA || !cleanup_ok || !status_consistent) break;
  }
  result.first_rss_after_kib = first_rss;
  result.min_rss_after_kib = min_rss;
  result.max_rss_after_kib = max_rss;
  if (!cleanup_ok || !status_consistent) {
    result.status = PROBE_INFRA;
    result.error_code = -1005;
    snprintf(result.operation, sizeof(result.operation), "repeat-invariant");
    snprintf(result.message, sizeof(result.message), "Repeated status or caller cleanup invariant failed");
  }
  print_result(&result, &options, completed, warning_total, fatal_total,
               total_wall, total_cpu, cleanup_ok, status_consistent);
  return result.status;

bad_arguments:
  result.status = PROBE_INFRA;
  result.error_code = -1005;
  snprintf(result.operation, sizeof(result.operation), "arguments");
  snprintf(result.message, sizeof(result.message), "Usage: strictjpeg_probe --mode a2|a1 [--memory-mib N] [--scan-limit N] [--cancel-after-progress N] [--cancel-after-ms N] [--fail-row-alloc] [--repeat 1..1000] FILE");
  print_result(&result, &options, 0, 0, 0, 0, 0, 1, 1);
  return PROBE_INFRA;
}
