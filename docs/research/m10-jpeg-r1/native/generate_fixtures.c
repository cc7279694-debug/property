/* Isolated research fixtures. Never use this program on the locked JPEGs. */
#include <errno.h>
#include <inttypes.h>
#include <setjmp.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jpeglib.h>

typedef struct {
  const char *name;
  unsigned width;
  unsigned height;
  int grayscale;
  int native_rgb;
  int sample_444;
  int progressive;
  unsigned restart_interval;
  unsigned orientation;
  int quality;
  uint32_t seed;
} Fixture;

typedef struct {
  struct jpeg_error_mgr base;
  jmp_buf target;
  char message[JMSG_LENGTH_MAX];
} GeneratorError;

/* All objects mutated after setjmp live on the heap, not in automatic locals. */
typedef struct {
  struct jpeg_compress_struct jpeg;
  GeneratorError error;
  FILE *file;
  JSAMPLE *row;
  char *path;
  int jpeg_created;
  int file_created;
  int complete;
  long length;
} GeneratorState;

static void generator_error_exit(j_common_ptr jpeg) {
  GeneratorError *error = (GeneratorError *)jpeg->err;
  (*jpeg->err->format_message)(jpeg, error->message);
  longjmp(error->target, 1);
}

static void generator_emit_message(j_common_ptr jpeg, int level) {
  if (level < 0) generator_error_exit(jpeg);
}

static void cleanup(GeneratorState *state) {
  if (!state) return;
  if (state->jpeg_created) jpeg_destroy_compress(&state->jpeg);
  if (state->file) fclose(state->file);
  if (state->file_created && !state->complete) remove(state->path);
  free(state->row);
  free(state->path);
  free(state);
}

/* Intentional uint32_t modular arithmetic is part of the pixel specification. */
static uint32_t coordinate_hash(uint32_t seed, uint32_t x, uint32_t y) {
  uint32_t value = seed ^ (x * UINT32_C(0x9e3779b9)) ^
                   (y * UINT32_C(0x85ebca6b));
  value ^= value >> 16;
  value *= UINT32_C(0x7feb352d);
  value ^= value >> 15;
  value *= UINT32_C(0x846ca68b);
  return value ^ (value >> 16);
}

static void fill_row(JSAMPLE *row, const Fixture *fixture, unsigned y) {
  unsigned x;
  for (x = 0; x < fixture->width; ++x) {
    uint32_t hash = coordinate_hash(fixture->seed, x, y);
    if (fixture->grayscale) {
      row[x] = (JSAMPLE)((3u * x + 5u * y + (hash & 15u)) & 255u);
    } else {
      size_t offset = (size_t)x * 3u;
      row[offset] = (JSAMPLE)((3u * x + 5u * y + (hash & 15u)) & 255u);
      row[offset + 1u] =
          (JSAMPLE)((x + 7u * y + ((hash >> 8) & 15u)) & 255u);
      row[offset + 2u] =
          (JSAMPLE)((11u * x + 2u * y + ((hash >> 16) & 15u)) & 255u);
    }
  }
}

/* Little-endian TIFF: one SHORT orientation tag, no thumbnail or GPS fields. */
static void write_exif_orientation(j_compress_ptr jpeg, unsigned orientation) {
  JOCTET exif[32] = {
      'E', 'x', 'i', 'f', 0, 0, 'I', 'I', 42, 0, 8, 0, 0, 0,
      1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  exif[24] = (JOCTET)orientation;
  jpeg_write_marker(jpeg, JPEG_APP0 + 1, exif, (unsigned int)sizeof(exif));
}

static int generate_one(const char *directory, const Fixture *fixture) {
  GeneratorState *state;
  size_t directory_length = strlen(directory);
  size_t name_length = strlen(fixture->name);
  size_t components = fixture->grayscale ? 1u : 3u;
  size_t row_bytes;
  int close_result;

  if (!directory_length || directory_length > SIZE_MAX - name_length - 2u ||
      fixture->width == 0 || fixture->height == 0 ||
      fixture->width > JPEG_MAX_DIMENSION ||
      fixture->height > JPEG_MAX_DIMENSION ||
      (size_t)fixture->width > SIZE_MAX / components ||
      fixture->orientation > 8u) {
    fprintf(stderr, "Invalid fixture/path/size: %s\n", fixture->name);
    return 1;
  }
  row_bytes = (size_t)fixture->width * components;
  state = (GeneratorState *)calloc(1u, sizeof(*state));
  if (!state) {
    fprintf(stderr, "Cannot allocate generator state: %s\n", fixture->name);
    return 1;
  }
  state->path = (char *)malloc(directory_length + name_length + 2u);
  state->row = (JSAMPLE *)malloc(row_bytes);
  if (!state->path || !state->row) {
    fprintf(stderr, "Cannot allocate path/row: %s\n", fixture->name);
    cleanup(state);
    return 1;
  }
  memcpy(state->path, directory, directory_length);
  state->path[directory_length] = '/';
  memcpy(state->path + directory_length + 1u, fixture->name, name_length + 1u);
  /* C11 exclusive creation refuses to replace even earlier synthetic output. */
  state->file = fopen(state->path, "wbx");
  if (!state->file) {
    fprintf(stderr, "Cannot exclusively create %s: %s\n", fixture->name,
            strerror(errno));
    cleanup(state);
    return 1;
  }
  state->file_created = 1;
  state->jpeg.err = jpeg_std_error(&state->error.base);
  state->error.base.error_exit = generator_error_exit;
  state->error.base.emit_message = generator_emit_message;
  if (setjmp(state->error.target)) {
    fprintf(stderr, "JPEG generation failed for %s: %s\n", fixture->name,
            state->error.message);
    cleanup(state);
    return 1;
  }
  /* The zeroed jpeg struct is safe to destroy after partial initialization. */
  state->jpeg_created = 1;
  jpeg_create_compress(&state->jpeg);
  jpeg_stdio_dest(&state->jpeg, state->file);
  state->jpeg.image_width = (JDIMENSION)fixture->width;
  state->jpeg.image_height = (JDIMENSION)fixture->height;
  state->jpeg.input_components = (int)components;
  state->jpeg.in_color_space = fixture->grayscale ? JCS_GRAYSCALE : JCS_RGB;
  jpeg_set_defaults(&state->jpeg);
  if (fixture->native_rgb) jpeg_set_colorspace(&state->jpeg, JCS_RGB);
  if (!fixture->grayscale) {
    int component;
    for (component = 0; component < state->jpeg.num_components; ++component) {
      state->jpeg.comp_info[component].h_samp_factor = 1;
      state->jpeg.comp_info[component].v_samp_factor = 1;
    }
    if (!fixture->sample_444 && !fixture->native_rgb) {
      state->jpeg.comp_info[0].h_samp_factor = 2;
      state->jpeg.comp_info[0].v_samp_factor = 2;
    }
  }
  jpeg_set_quality(&state->jpeg, fixture->quality, TRUE);
  state->jpeg.dct_method = JDCT_ISLOW;
  state->jpeg.optimize_coding = FALSE;
  state->jpeg.restart_interval = fixture->restart_interval;
  if (fixture->progressive) jpeg_simple_progression(&state->jpeg);
  jpeg_start_compress(&state->jpeg, TRUE);
  if (fixture->orientation) {
    write_exif_orientation(&state->jpeg, fixture->orientation);
  }
  while (state->jpeg.next_scanline < state->jpeg.image_height) {
    JSAMPROW rows[1];
    fill_row(state->row, fixture, (unsigned)state->jpeg.next_scanline);
    rows[0] = state->row;
    if (jpeg_write_scanlines(&state->jpeg, rows, 1u) != 1u) {
      fprintf(stderr, "Unexpected suspended encoder: %s\n", fixture->name);
      cleanup(state);
      return 1;
    }
  }
  jpeg_finish_compress(&state->jpeg);
  if (fflush(state->file) != 0 || (state->length = ftell(state->file)) < 0) {
    fprintf(stderr, "Output flush/size failed: %s\n", fixture->name);
    cleanup(state);
    return 1;
  }
  close_result = fclose(state->file);
  state->file = NULL;
  if (close_result != 0) {
    fprintf(stderr, "Output close failed: %s\n", fixture->name);
    cleanup(state);
    return 1;
  }
  state->complete = 1;
  if (printf("{\"fixture\":\"%s\",\"status\":\"GENERATED_NOT_DECODE_VERIFIED\","
             "\"pixel_algorithm\":\"lithic-coordinate-v1\",\"seed\":%" PRIu32 ","
             "\"width\":%u,\"height\":%u,\"components\":%zu,"
             "\"jpeg_color_space\":\"%s\",\"sampling\":\"%s\","
             "\"quality\":%d,\"dct\":\"ISLOW\",\"optimize_coding\":false,"
             "\"progressive\":%s,\"restart_interval_mcus\":%u,"
             "\"exif_orientation\":%u,\"row_buffer_bytes\":%zu,\"size_bytes\":%ld}\n",
             fixture->name, fixture->seed, fixture->width, fixture->height,
             components,
             fixture->grayscale ? "GRAYSCALE" :
                 (fixture->native_rgb ? "RGB" : "YCbCr"),
             fixture->grayscale ? "GRAY" : (fixture->sample_444 ? "4:4:4" : "4:2:0"),
             fixture->quality, fixture->progressive ? "true" : "false",
             fixture->restart_interval, fixture->orientation, row_bytes,
             state->length) < 0) {
    fprintf(stderr, "Manifest output failed after file completion\n");
    cleanup(state);
    return 1;
  }
  cleanup(state);
  return 0;
}

int main(int argc, char **argv) {
  const uint32_t seed = UINT32_C(0x13579bdf);
  static const Fixture ordinary[] = {
      {"baseline_rgb_420.jpg", 257, 193, 0, 0, 0, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_rgb_444.jpg", 257, 193, 0, 0, 1, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_native_rgb_444.jpg", 257, 193, 0, 1, 1, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_gray.jpg", 259, 197, 1, 0, 1, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_1x1.jpg", 1, 1, 0, 0, 0, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_1x40000.jpg", 1, 40000, 0, 0, 0, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_40000x1.jpg", 40000, 1, 0, 0, 0, 0, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"progressive_rgb_420.jpg", 257, 193, 0, 0, 0, 1, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"progressive_rgb_444.jpg", 257, 193, 0, 0, 1, 1, 0, 0, 90, UINT32_C(0x13579bdf)},
      {"baseline_restart_420.jpg", 257, 193, 0, 0, 0, 0, 7, 0, 90, UINT32_C(0x13579bdf)},
      {"progressive_restart_420.jpg", 257, 193, 0, 0, 0, 1, 7, 0, 90, UINT32_C(0x13579bdf)}};
  static const Fixture large[] = {
      {"large_40mp_baseline_420.jpg", 8000, 5000, 0, 0, 0, 0, 0, 0, 85, UINT32_C(0x13579bdf)},
      {"large_40mp_progressive_420.jpg", 8000, 5000, 0, 0, 0, 1, 0, 0, 85, UINT32_C(0x13579bdf)},
      {"large_40mp_progressive_444.jpg", 8000, 5000, 0, 0, 1, 1, 0, 0, 85, UINT32_C(0x13579bdf)}};
  size_t index;
  unsigned orientation;
  int include_large = argc == 3 && strcmp(argv[2], "--large") == 0;
  if (argc < 2 || argc > 3 || (argc == 3 && !include_large)) {
    fprintf(stderr, "Usage: generate_fixtures <existing-empty-output-dir> [--large]\n");
    return 2;
  }
  for (index = 0; index < sizeof(ordinary) / sizeof(ordinary[0]); ++index) {
    if (generate_one(argv[1], &ordinary[index])) return 1;
  }
  for (orientation = 1u; orientation <= 8u; ++orientation) {
    char name[32];
    Fixture fixture = {name, 257, 193, 0, 0, 0, 0, 0, orientation, 90, seed};
    int length = snprintf(name, sizeof(name), "exif_orientation_%u.jpg", orientation);
    if (length < 0 || (size_t)length >= sizeof(name) ||
        generate_one(argv[1], &fixture)) return 1;
  }
  if (include_large) {
    for (index = 0; index < sizeof(large) / sizeof(large[0]); ++index) {
      if (generate_one(argv[1], &large[index])) return 1;
    }
  }
  if (fflush(stdout) != 0) return 1;
  return 0;
}
