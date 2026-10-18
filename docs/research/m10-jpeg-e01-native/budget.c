#include "budget.h"

#include <stdlib.h>

/* sizeof(struct e01_block) is a multiple of max_align_t's alignment. The
 * malloc request charges this entire intrusive ledger header plus payload.
 * libc allocator bookkeeping, the caller's stack root and codec stack are
 * outside this metric; it is not a process RSS cap. */
struct e01_block {
  struct e01_block *next;
  struct e01_budget *owner;
  size_t bytes, charge;
  enum e01_kind kind;
  max_align_t alignment;
};

static void increment_saturated(uint64_t *counter)
{
  if (*counter != UINT64_MAX) ++*counter;
}

void e01_budget_init(struct e01_budget *budget, size_t limit,
                     uint64_t fail_at, uint64_t fail_from)
{
  *budget = (struct e01_budget){
    .limit = limit, .fail_at = fail_at, .fail_from = fail_from
  };
}

void *e01_alloc(struct e01_budget *budget, size_t bytes, enum e01_kind kind)
{
  struct e01_block *block;
  size_t charge;
  int valid_kind = kind >= E01_CALLER && kind <= E01_LARGE;
  int exhausted;

  if (budget == NULL) return NULL;
  exhausted = budget->attempts == UINT64_MAX ||
              (valid_kind && budget->calls[kind] == UINT64_MAX);
  increment_saturated(&budget->attempts);
  if (valid_kind) increment_saturated(&budget->calls[kind]);
  /* Once request accounting is exhausted, no allocation may bypass its
   * injection boundary. Existing blocks remain available for valid cleanup. */
  if (exhausted) {
    increment_saturated(&budget->denied);
    return NULL;
  }

  if ((budget->fail_at != 0 && budget->attempts == budget->fail_at) ||
      (budget->fail_from != 0 && budget->attempts >= budget->fail_from)) {
    increment_saturated(&budget->injected);
    increment_saturated(&budget->denied);
    return NULL;
  }
  if (budget->bad_frees != 0 || !valid_kind || bytes == 0 ||
      bytes > SIZE_MAX - sizeof(struct e01_block)) {
    increment_saturated(&budget->denied);
    return NULL;
  }
  charge = sizeof(struct e01_block) + bytes;
  if (charge > SIZE_MAX - budget->live || budget->live_blocks == SIZE_MAX) {
    increment_saturated(&budget->denied);
    return NULL;
  }

  /* Deliberate CI RED mutation only: prove the real boundary tests fail if
   * cap enforcement is removed. Every normal build includes this check. */
#ifndef E01_RED_BACKEND
  if (budget->live > budget->limit || charge > budget->limit - budget->live) {
    increment_saturated(&budget->denied);
    return NULL;
  }
#endif

  block = malloc(charge);
  if (block == NULL) {
    increment_saturated(&budget->denied);
    return NULL;
  }
  block->next = budget->head;
  block->owner = budget;
  block->bytes = bytes;
  block->charge = charge;
  block->kind = kind;
  budget->head = block;
  budget->live += charge;
  ++budget->live_blocks;
  if (budget->live > budget->peak) budget->peak = budget->live;
  return block + 1;
}

int e01_free(struct e01_budget *budget, void *pointer,
             size_t expected_bytes, enum e01_kind kind)
{
  struct e01_block **link;
  struct e01_block *block;

  if (budget == NULL) return 0;
  if (pointer == NULL) return 1;

  /* Search only known live metadata. Do not derive/read a header from an
   * unknown, wrong-owner or already-freed pointer. */
  link = &budget->head;
  while (*link != NULL && (void *)(*link + 1) != pointer)
    link = &(*link)->next;
  block = *link;
  if (block == NULL || block->owner != budget ||
      block->bytes != expected_bytes || block->kind != kind ||
      budget->live < block->charge || budget->live_blocks == 0) {
    if (budget->bad_frees != SIZE_MAX) ++budget->bad_frees;
    return 0;
  }

  *link = block->next;
  budget->live -= block->charge;
  --budget->live_blocks;
  increment_saturated(&budget->frees[kind]);
  free(block);
  return 1;
}
