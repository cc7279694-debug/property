/* Research-only replacement for libjpeg-turbo 3.2.0 src/jmemnobs.c.
 * The caller must set cinfo->client_data to its per-decode e01_budget before
 * jpeg_create_decompress. Never patch jmemmgr.c: its small-pool retry remains.
 */
#define JPEG_INTERNALS
#include "jinclude.h"
#include "jpeglib.h"
#include "jmemsys.h"

#include "budget.h"

GLOBAL(void *)
jpeg_get_small(j_common_ptr cinfo, size_t sizeofobject)
{
  return e01_alloc((struct e01_budget *)cinfo->client_data,
                   sizeofobject, E01_SMALL);
}

GLOBAL(void)
jpeg_free_small(j_common_ptr cinfo, void *object, size_t sizeofobject)
{
  (void)e01_free((struct e01_budget *)cinfo->client_data,
                 object, sizeofobject, E01_SMALL);
}

GLOBAL(void *)
jpeg_get_large(j_common_ptr cinfo, size_t sizeofobject)
{
  return e01_alloc((struct e01_budget *)cinfo->client_data,
                   sizeofobject, E01_LARGE);
}

GLOBAL(void)
jpeg_free_large(j_common_ptr cinfo, void *object, size_t sizeofobject)
{
  (void)e01_free((struct e01_budget *)cinfo->client_data,
                 object, sizeofobject, E01_LARGE);
}

GLOBAL(size_t)
jpeg_mem_available(j_common_ptr cinfo, size_t min_bytes_needed,
                   size_t max_bytes_needed, size_t already_allocated)
{
  (void)cinfo;
  (void)min_bytes_needed;
  (void)already_allocated;
  /* Preserve the default no-spill policy. Actual malloc requests meet the
   * shared ledger's cap; do not introduce a new artificial codec limit. */
  return max_bytes_needed;
}

GLOBAL(void)
jpeg_open_backing_store(j_common_ptr cinfo, backing_store_ptr info,
                        long total_bytes_needed)
{
  (void)info;
  (void)total_bytes_needed;
  ERREXIT(cinfo, JERR_NO_BACKING_STORE);
}

GLOBAL(long)
jpeg_mem_init(j_common_ptr cinfo)
{
  (void)cinfo;
  return 0;
}

GLOBAL(void)
jpeg_mem_term(j_common_ptr cinfo)
{
  (void)cinfo;
}
