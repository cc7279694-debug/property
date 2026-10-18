#ifndef E01_BUDGET_H
#define E01_BUDGET_H

#include <stddef.h>
#include <stdint.h>

enum e01_kind { E01_CALLER = 0, E01_SMALL = 1, E01_LARGE = 2 };

struct e01_block;

/* This root lives on the caller's stack; it is outside the charged heap. */
struct e01_budget {
  size_t limit, live, peak, live_blocks, bad_frees;
  uint64_t attempts, injected, denied, fail_at, fail_from;
  uint64_t calls[3], frees[3];
  struct e01_block *head;
};

void e01_budget_init(struct e01_budget *budget, size_t limit,
                     uint64_t fail_at, uint64_t fail_from);
void *e01_alloc(struct e01_budget *budget, size_t bytes, enum e01_kind kind);

/* Returns 1 on release or a NULL no-op, 0 on an invalid free. */
int e01_free(struct e01_budget *budget, void *pointer,
             size_t expected_bytes, enum e01_kind kind);

#endif
