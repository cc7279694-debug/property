/* Compile-only research bridge. No App caller or library-loading side effect. */
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include "strict_core.h"

static jlong metric(uint64_t value, int available)
{
  return available && value <= INT64_MAX ? (jlong)value : (jlong)-1;
}

JNIEXPORT jlongArray JNICALL
Java_com_cooper_lithic_research_StrictJpegCandidate_validateReadonlyFd(
    JNIEnv *environment, jclass type, jint descriptor, jlong limit)
{
  struct e01_options options;
  struct e01_result result;
  jlong values[16];
  jlongArray output;
  (void)type;
  if ((*environment)->ExceptionCheck(environment)) return NULL;
  memset(&result, 0, sizeof(result));
  result.status = E01_INFRA;
  result.error_code = E01_ERROR_ARGUMENTS;
  result.fd_closed = 1; /* No duplicate was acquired on argument rejection. */
  result.decoder_mem_null = 1;

  if (limit <= 0 || (uint64_t)limit > (uint64_t)SIZE_MAX) {
    result.status = E01_RESOURCE;
  } else if (descriptor >= 0) {
    e01_options_init(&options);
    options.limit = (size_t)limit;
    (void)e01_validate_fd((int)descriptor, &options, &result);
  }
  values[0] = (jlong)result.status;
  values[1] = (jlong)result.error_code;
  values[2] = (jlong)result.peak;
  values[3] = (jlong)result.live;
  values[4] = (jlong)result.live_blocks;
  values[5] = (jlong)result.bad_frees;
  values[6] = (jlong)result.fd_closed;
  values[7] = (jlong)result.decoder_mem_null;
  values[8] = (jlong)result.rows_read;
  values[9] = (jlong)result.height;
  values[10] = (jlong)result.finish_complete;
  values[11] = metric(result.wall_ns, result.timings_available);
  values[12] = metric(result.cpu_ns, result.timings_available);
  values[13] = metric(result.peak_rss_kib, result.rss_available);
  values[14] = (jlong)result.create_attempted;
  values[15] = (jlong)result.destroy_called;
  if ((*environment)->ExceptionCheck(environment)) return NULL;
  output = (*environment)->NewLongArray(environment, 16);
  if (!output || (*environment)->ExceptionCheck(environment)) return NULL;
  (*environment)->SetLongArrayRegion(environment, output, 0, 16, values);
  if ((*environment)->ExceptionCheck(environment)) return NULL;
  return output;
}
