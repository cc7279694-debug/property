/* Real C allocator tests. Run with ASan/LSan to verify releases and bounds. */
#include "budget.h"

#include <inttypes.h>
#include <stdio.h>
#include <string.h>

static unsigned assertions;
static unsigned failures;
static unsigned tests;

#define CHECK(expression) do { \
  ++assertions; \
  if (!(expression)) { \
    ++failures; \
    fprintf(stderr, "%s:%d: FAIL: %s\n", __FILE__, __LINE__, #expression); \
  } \
} while (0)

static void check_empty(const struct e01_budget *budget)
{
  CHECK(budget->live == 0);
  CHECK(budget->live_blocks == 0);
  CHECK(budget->head == NULL);
}

/* Catches omitted header charging, insufficient alignment, or fake storage. */
static void allocation_and_release(void)
{
  struct e01_budget budget;
  unsigned char *pointer;
  size_t charge;
  ++tests;
  e01_budget_init(&budget, 4096, 0, 0);
  pointer = e01_alloc(&budget, 129, E01_CALLER);
  CHECK(pointer != NULL);
  if (pointer == NULL) return;
  CHECK((uintptr_t)pointer % _Alignof(max_align_t) == 0);
  memset(pointer, 0xA5, 129);
  CHECK(pointer[0] == 0xA5 && pointer[128] == 0xA5);
  charge = budget.live;
  CHECK(charge > 129);
  CHECK(budget.peak == charge);
  CHECK(budget.live_blocks == 1);
  CHECK(budget.head != NULL);
  CHECK(budget.attempts == 1 && budget.calls[E01_CALLER] == 1);
  CHECK(e01_free(&budget, pointer, 129, E01_CALLER) == 1);
  CHECK(budget.frees[E01_CALLER] == 1);
  CHECK(budget.peak == charge);
  CHECK(budget.bad_frees == 0);
  check_empty(&budget);
}

/* Catches >= instead of > at the limit, or budget bypass (CI RED mutation). */
static void exact_limit_boundary(void)
{
  struct e01_budget probe, exact, short_budget;
  void *pointer;
  size_t charge;
  ++tests;
  e01_budget_init(&probe, 4096, 0, 0);
  pointer = e01_alloc(&probe, 64, E01_LARGE);
  CHECK(pointer != NULL);
  if (pointer == NULL) return;
  charge = probe.live;
  CHECK(e01_free(&probe, pointer, 64, E01_LARGE) == 1);

  e01_budget_init(&exact, charge, 0, 0);
  pointer = e01_alloc(&exact, 64, E01_LARGE);
  CHECK(pointer != NULL);
  CHECK(exact.live == charge && exact.peak == charge);
  if (pointer != NULL) CHECK(e01_free(&exact, pointer, 64, E01_LARGE) == 1);
  check_empty(&exact);

  e01_budget_init(&short_budget, charge - 1, 0, 0);
  pointer = e01_alloc(&short_budget, 64, E01_LARGE);
  CHECK(pointer == NULL);
  CHECK(short_budget.denied == 1);
  CHECK(short_budget.live == 0 && short_budget.peak == 0);
  if (pointer != NULL) CHECK(e01_free(&short_budget, pointer, 64, E01_LARGE) == 1);
  check_empty(&short_budget);
}

/* Catches limit checks that forget already-live caller or codec blocks. */
static void mixed_kinds_share_one_budget(void)
{
  struct e01_budget probe, budget;
  void *caller, *small, *large;
  size_t charge;
  ++tests;
  e01_budget_init(&probe, 4096, 0, 0);
  caller = e01_alloc(&probe, 16, E01_CALLER);
  CHECK(caller != NULL);
  if (caller == NULL) return;
  charge = probe.live;
  CHECK(e01_free(&probe, caller, 16, E01_CALLER) == 1);
  e01_budget_init(&budget, charge * 2, 0, 0);
  caller = e01_alloc(&budget, 16, E01_CALLER);
  small = e01_alloc(&budget, 16, E01_SMALL);
  large = e01_alloc(&budget, 16, E01_LARGE);
  CHECK(caller != NULL && small != NULL && large == NULL);
  CHECK(budget.live == charge * 2 && budget.live_blocks == 2);
  CHECK(budget.attempts == 3 && budget.denied == 1);
  CHECK(budget.calls[0] == 1 && budget.calls[1] == 1 && budget.calls[2] == 1);
  if (caller != NULL) CHECK(e01_free(&budget, caller, 16, E01_CALLER) == 1);
  if (small != NULL) CHECK(e01_free(&budget, small, 16, E01_SMALL) == 1);
  if (large != NULL) CHECK(e01_free(&budget, large, 16, E01_LARGE) == 1);
  check_empty(&budget);
}

/* Catches wrapping header+payload arithmetic and accepting zero-byte blocks. */
static void overflow_zero_and_invalid_kind(void)
{
  struct e01_budget budget;
  void *pointer;
  int unknown = 7;
  ++tests;
  e01_budget_init(&budget, SIZE_MAX, 0, 0);
  CHECK(e01_alloc(&budget, SIZE_MAX, E01_CALLER) == NULL);
  CHECK(e01_alloc(&budget, SIZE_MAX - 1, E01_SMALL) == NULL);
  CHECK(e01_alloc(&budget, 0, E01_LARGE) == NULL);
  CHECK(e01_alloc(&budget, 8, (enum e01_kind)3) == NULL);
  CHECK(budget.attempts == 4 && budget.denied == 4);
  CHECK(budget.calls[0] == 1 && budget.calls[1] == 1 && budget.calls[2] == 1);
  CHECK(budget.injected == 0 && budget.peak == 0);
  check_empty(&budget);

  /* Exhausted accounting must fail closed rather than wrapping past an
   * injection boundary. Failure counters themselves also saturate. */
  e01_budget_init(&budget, 4096, 0, UINT64_MAX);
  budget.attempts = UINT64_MAX;
  budget.denied = UINT64_MAX;
  CHECK(e01_alloc(&budget, 8, E01_CALLER) == NULL);
  CHECK(budget.attempts == UINT64_MAX && budget.denied == UINT64_MAX);
  CHECK(budget.calls[E01_CALLER] == 1);
  check_empty(&budget);

  e01_budget_init(&budget, 4096, 0, 0);
  budget.calls[E01_SMALL] = UINT64_MAX;
  CHECK(e01_alloc(&budget, 8, E01_SMALL) == NULL);
  CHECK(budget.calls[E01_SMALL] == UINT64_MAX);
  CHECK(budget.attempts == 1 && budget.denied == 1);
  check_empty(&budget);

  e01_budget_init(&budget, 4096, 1, 0);
  budget.injected = UINT64_MAX;
  CHECK(e01_alloc(&budget, 8, E01_CALLER) == NULL);
  CHECK(budget.injected == UINT64_MAX && budget.denied == 1);
  check_empty(&budget);

  e01_budget_init(&budget, 4096, 0, 0);
  pointer = e01_alloc(&budget, 8, E01_CALLER);
  CHECK(pointer != NULL);
  budget.bad_frees = SIZE_MAX;
  budget.frees[E01_CALLER] = UINT64_MAX;
  CHECK(e01_free(&budget, &unknown, sizeof unknown, E01_CALLER) == 0);
  CHECK(budget.bad_frees == SIZE_MAX && unknown == 7);
  CHECK(e01_alloc(&budget, 8, E01_SMALL) == NULL);
  if (pointer != NULL) CHECK(e01_free(&budget, pointer, 8, E01_CALLER) == 1);
  CHECK(budget.frees[E01_CALLER] == UINT64_MAX);
  check_empty(&budget);
}

/* Catches fail-at counting only codec requests, or continuing to fail at N+1. */
static void fail_at_one_attempt(void)
{
  struct e01_budget budget;
  void *first, *second, *third;
  size_t first_live;
  ++tests;
  e01_budget_init(&budget, 4096, 2, 0);
  first = e01_alloc(&budget, 8, E01_CALLER);
  first_live = budget.live;
  second = e01_alloc(&budget, 8, E01_SMALL);
  CHECK(first != NULL && second == NULL);
  CHECK(budget.live == first_live && budget.live_blocks == 1);
  third = e01_alloc(&budget, 8, E01_LARGE);
  CHECK(third != NULL);
  CHECK(budget.attempts == 3 && budget.injected == 1 && budget.denied == 1);
  if (first != NULL) CHECK(e01_free(&budget, first, 8, E01_CALLER) == 1);
  if (second != NULL) CHECK(e01_free(&budget, second, 8, E01_SMALL) == 1);
  if (third != NULL) CHECK(e01_free(&budget, third, 8, E01_LARGE) == 1);
  check_empty(&budget);
}

/* Catches fail-from using equality instead of a sticky >= boundary. */
static void fail_from_all_later_attempts(void)
{
  struct e01_budget budget;
  void *first, *second, *third, *fourth;
  size_t first_live;
  ++tests;
  e01_budget_init(&budget, 4096, 0, 2);
  first = e01_alloc(&budget, 8, E01_CALLER);
  first_live = budget.live;
  second = e01_alloc(&budget, 8, E01_SMALL);
  third = e01_alloc(&budget, 8, E01_LARGE);
  fourth = e01_alloc(&budget, 8, E01_CALLER);
  CHECK(first != NULL && second == NULL && third == NULL && fourth == NULL);
  CHECK(budget.live == first_live && budget.live_blocks == 1);
  CHECK(budget.attempts == 4 && budget.injected == 3 && budget.denied == 3);
  if (first != NULL) CHECK(e01_free(&budget, first, 8, E01_CALLER) == 1);
  if (second != NULL) CHECK(e01_free(&budget, second, 8, E01_SMALL) == 1);
  if (third != NULL) CHECK(e01_free(&budget, third, 8, E01_LARGE) == 1);
  if (fourth != NULL) CHECK(e01_free(&budget, fourth, 8, E01_CALLER) == 1);
  check_empty(&budget);
}

/* Catches fail injection beginning at zero or excluding the first attempt. */
static void fail_first_and_combined(void)
{
  struct e01_budget budget;
  void *pointer;
  ++tests;
  e01_budget_init(&budget, 4096, 1, 3);
  CHECK(e01_alloc(&budget, 8, E01_CALLER) == NULL);
  pointer = e01_alloc(&budget, 8, E01_SMALL);
  CHECK(pointer != NULL);
  CHECK(e01_alloc(&budget, 8, E01_LARGE) == NULL);
  CHECK(e01_alloc(&budget, 8, E01_CALLER) == NULL);
  CHECK(budget.attempts == 4 && budget.injected == 3 && budget.denied == 3);
  if (pointer != NULL) CHECK(e01_free(&budget, pointer, 8, E01_SMALL) == 1);
  check_empty(&budget);
}

/* Catches dereferencing an unowned pointer or releasing another owner's block. */
static void wrong_owner_does_not_release(void)
{
  struct e01_budget owner, other;
  unsigned char *pointer;
  size_t live;
  ++tests;
  e01_budget_init(&owner, 4096, 0, 0);
  e01_budget_init(&other, 4096, 0, 0);
  pointer = e01_alloc(&owner, 32, E01_SMALL);
  CHECK(pointer != NULL);
  if (pointer == NULL) return;
  live = owner.live;
  pointer[0] = 17;
  CHECK(e01_free(&other, pointer, 32, E01_SMALL) == 0);
  CHECK(other.bad_frees == 1 && owner.bad_frees == 0);
  CHECK(owner.live == live && owner.live_blocks == 1 && pointer[0] == 17);
  CHECK(e01_alloc(&other, 8, E01_CALLER) == NULL);
  CHECK(other.denied == 1);
  CHECK(e01_free(&owner, pointer, 32, E01_SMALL) == 1);
  check_empty(&owner);
  check_empty(&other);
}

/* Catches size mismatch releases and errors that prevent valid cleanup. */
static void wrong_size_preserves_valid_cleanup(void)
{
  struct e01_budget budget;
  void *pointer;
  size_t live;
  ++tests;
  e01_budget_init(&budget, 4096, 0, 0);
  pointer = e01_alloc(&budget, 32, E01_SMALL);
  CHECK(pointer != NULL);
  if (pointer == NULL) return;
  live = budget.live;
  CHECK(e01_free(&budget, pointer, 31, E01_SMALL) == 0);
  CHECK(budget.live == live && budget.live_blocks == 1 && budget.bad_frees == 1);
  CHECK(budget.frees[E01_SMALL] == 0);
  CHECK(e01_alloc(&budget, 8, E01_CALLER) == NULL);
  CHECK(e01_free(&budget, pointer, 32, E01_SMALL) == 1);
  CHECK(budget.frees[E01_SMALL] == 1);
  check_empty(&budget);
}

/* Catches wrong-kind acceptance even when owner, address and size match. */
static void wrong_kind_does_not_release(void)
{
  struct e01_budget budget;
  void *pointer;
  size_t live;
  ++tests;
  e01_budget_init(&budget, 4096, 0, 0);
  pointer = e01_alloc(&budget, 24, E01_LARGE);
  CHECK(pointer != NULL);
  if (pointer == NULL) return;
  live = budget.live;
  CHECK(e01_free(&budget, pointer, 24, E01_SMALL) == 0);
  CHECK(budget.live == live && budget.bad_frees == 1);
  CHECK(e01_free(&budget, pointer, 24, E01_LARGE) == 1);
  CHECK(budget.frees[E01_LARGE] == 1 && budget.frees[E01_SMALL] == 0);
  check_empty(&budget);
}

/* Catches pointer-header subtraction before a safe ledger membership lookup. */
static void unknown_and_duplicate_are_safe(void)
{
  struct e01_budget budget;
  void *pointer;
  int stack_value = 42;
  ++tests;
  e01_budget_init(&budget, 4096, 0, 0);
  pointer = e01_alloc(&budget, 24, E01_CALLER);
  CHECK(pointer != NULL);
  if (pointer == NULL) return;
  CHECK(e01_free(&budget, &stack_value, sizeof stack_value, E01_CALLER) == 0);
  CHECK(stack_value == 42 && budget.bad_frees == 1 && budget.live_blocks == 1);
  CHECK(e01_free(&budget, pointer, 24, E01_CALLER) == 1);
  /* No new allocation occurs between release and the duplicate request. */
  CHECK(e01_free(&budget, pointer, 24, E01_CALLER) == 0);
  CHECK(budget.bad_frees == 2 && budget.frees[E01_CALLER] == 1);
  check_empty(&budget);
  CHECK(e01_free(&budget, NULL, 0, E01_CALLER) == 1);
  CHECK(budget.bad_frees == 2);
}

/* Catches broken list unlinking for middle/head/tail and stale live counters. */
static void non_lifo_and_repeated_cleanup(void)
{
  struct e01_budget budget;
  unsigned iteration;
  ++tests;
  e01_budget_init(&budget, 4096, 0, 0);
  for (iteration = 0; iteration < 64; ++iteration) {
    void *a = e01_alloc(&budget, 1, E01_CALLER);
    void *b = e01_alloc(&budget, 2, E01_SMALL);
    void *c = e01_alloc(&budget, 3, E01_LARGE);
    CHECK(a != NULL && b != NULL && c != NULL);
    CHECK(budget.live_blocks == 3 && budget.live <= budget.limit);
    if (b != NULL) CHECK(e01_free(&budget, b, 2, E01_SMALL) == 1);
    if (c != NULL) CHECK(e01_free(&budget, c, 3, E01_LARGE) == 1);
    if (a != NULL) CHECK(e01_free(&budget, a, 1, E01_CALLER) == 1);
    check_empty(&budget);
  }
  CHECK(budget.attempts == 192 && budget.denied == 0 && budget.bad_frees == 0);
  CHECK(budget.frees[0] == 64 && budget.frees[1] == 64 && budget.frees[2] == 64);
}

int main(void)
{
  allocation_and_release();
  exact_limit_boundary();
  mixed_kinds_share_one_budget();
  overflow_zero_and_invalid_kind();
  fail_at_one_attempt();
  fail_from_all_later_attempts();
  fail_first_and_combined();
  wrong_owner_does_not_release();
  wrong_size_preserves_valid_cleanup();
  wrong_kind_does_not_release();
  unknown_and_duplicate_are_safe();
  non_lifo_and_repeated_cleanup();
  printf("{\"suite\":\"e01_budget\",\"tests\":%u,\"assertions\":%u,"
         "\"failed\":%u,\"status\":\"%s\"}\n",
         tests, assertions, failures, failures == 0 ? "PASS" : "FAIL");
  return failures == 0 ? 0 : 1;
}
