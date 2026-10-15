# Task6 subscription read-state coverage audit

Scope: read-only inspection of existing Android test assertions while root's targeted 65-method input is frozen. This report does not claim execution PASS. No source, build, device or Git input was changed; no shell/build/ADB command was executed for this audit.

## Existing assertions by screen

All paths below are under `app/src/androidTest/java/com/cooper/lithic/`.

| Screen / state | Existing test and precise assertions | Coverage limit |
| --- | --- | --- |
| Current list / normal | `feature/subscription/M09SubscriptionScreensTest.kt:78-90`, `current_management_has_independent_active_paused_expired_groups`: ACTIVE/PAUSED/EXPIRED headings and each corresponding row displayed. New `UIC1SubscriptionVisualTest` adds compact-header/back/create callbacks and original identity retained with an unreadable cover. | Existing real lifecycle flow tests additionally exercise list-to-detail navigation; grouping is not a replacement for read-state assertions. |
| Current list / loading | `feature/subscription/SubscriptionScreensTest.kt:193-200`, `list_loading_and_read_failure_do_not_claim_empty`: initial default loading state renders list screen; "暂无订阅服务" does not exist. | Does not assert the explicit "正在读取订阅服务" label, and does not render existing rows during loading. |
| Current list / genuine empty | `SubscriptionScreensTest.kt:209-218`, `genuine_empty_list_exposes_create_without_fake_metrics`: totalCount=0/finished read; empty text displayed, fake ¥0.00 absent, create invokes callback once. | Direct UI coverage exists. |
| Current list / read error | `SubscriptionScreensTest.kt:201-206`: transitions the same empty loading host to a read error; error text contains 无法读取, empty text absent, retry invokes callback once. | Direct empty-error UI coverage exists. |
| Current list / stale rows plus error | No direct screen assertion found. `feature/subscription/SubscriptionViewModelTest.kt:292-309`, `failed_list_refresh_retains_real_rows_and_an_explicit_read_error`: injects an actual repository read failure after a valid row; error becomes non-null, original rows and count remain equal. | VM-level evidence only; this class is outside the stated 65-method targeted set. It does not prove stale warning/row/retry are displayed. |
| Detail / normal | `M09SubscriptionScreensTest.kt:26-75`: active pause/stop, paused resume/paid history, stopped same-identity reopen/no ordinary renewal, scheduled-stop correction/cancel, per-payment correction/delete. New visual test independently compares 7.50 historical / 5.00 current / 60 purchased / 25 elapsed and a future period. | These use valid details and preserve original business operations. |
| Detail / loading | No dedicated positive loading-label/UI state assertion found. Lifecycle helpers wait until loading is false; `M09SubscriptionLifecycleFlowTest.kt:225-232` recognizes "正在读取订阅" only to avoid treating transient transitions as assertion failures. | A wait/helper is not affirmative loading-state verification. |
| Detail / empty or missing identity | `SubscriptionScreensTest.kt:221-234`, `detail_read_failure_and_missing_are_distinct`: initial read error has no missing/empty-payment claim; then missing=true displays missing and removes error. | An existing identity has at least one confirmed payment under repository invariants. Whole-detail "empty" means missing identity; it is not a zero-cost/zero-payment detail. No valid missing state should be replaced by fake metrics. |
| Detail / read error | Same method, lines 227-231: actual error text displayed; missing and empty-payment text absent; retry invokes callback once. | The fixture has no retained detail. |
| Detail / stale detail plus error | No direct screen assertion found for retained name/metrics, stale warning, retry and disabled mutation controls together. | Existing source has these branches, but source inspection is not a rendered check. Detail payment-history loading/error/stale branches likewise lack direct screen assertions in the inspected tests. |
| Stopped history / normal | `M09SubscriptionLifecycleFlowTest.kt:89-112`, `pause_is_not_history`: pauses a real record, opens history, later stops it, opens `subscription-history-row-ID`, reopens the same identity and confirms only one subscription remains. `M09SubscriptionNavigationFlowTest.kt:63-76` separately asserts original stopped row navigation, but that class is outside the stated 65-method target. | Real identity/history route coverage exists. The extra navigation class was inspected but is not counted as part of the current targeted run. |
| Stopped history / genuine empty | `M09SubscriptionLifecycleFlowTest.kt:97-100`: after pausing, opens stopped history, waits for read completion and asserts `subscription-history-empty` displayed. | This meaningfully proves paused records are excluded from stopped history. |
| Stopped history / loading | No direct screen loading-label/no-false-empty assertion found across Android test sources. | Missing. |
| Stopped history / read error | No direct screen error/retry/no-false-empty assertion found across Android test sources. | Missing. |
| Stopped history / stale rows plus error | No direct screen stale-warning/retained-original-row/retry assertion found across Android test sources. | Missing. |

## Minimal supplemental checks before Task7

1. Highest-value new method: a controlled `SubscriptionHistoryScreen` transition journey using a mutable existing `SubscriptionListUiState`. Exercise loading → failed empty read → genuine empty → one real engine-derived STOPPED detail → failed refresh retaining that same detail. Assert the loading/error/genuine-empty states are distinct; retry callbacks fire; the stale warning and original history row remain visible; clicking it forwards the original subscription ID. This single test closes the stopped-history gaps without duplicating the existing end-to-end stop/reopen flow.
2. List/detail still need rendered stale-data verification. A small additional screen test or explicitly recorded root-owned manual/screenshot inspection can cover a retained list row/error/warning/retry and retained detail/name/metrics/error/warning with business actions disabled and safe return enabled. Do not count the ViewModel test or successful compilation as this UI evidence.
3. Positively inspect detail's initial loading label and absence of false missing/empty content. This can be a short transition in the same supplemental detail test/manual sequence; no large scenario matrix is required. If the independent payment-history read states are claimed, inspect them explicitly rather than treating whole-detail checks as equivalent.

No new method was added during the freeze. These are recommendations for root to authorize after the current run, or satisfy through documented rendered checks.

## Confirmed findings during the frozen targeted run

Root reported two observed runtime failures while this audit was underway:

- `M09SubscriptionScreensTest#paused_expired_coverage_keeps_paused_status_and_explains_expiry`.
- `M09SubscriptionLifecycleFlowTest#expiry_needs_no_worker`.

Both assert real visible status text on `subscription-detail-status`. The new caller attaches that tag to the StatusBadge Surface, whose child Text is not merged. This breaks the original text semantics contract even though the label remains visually present. Root owns the minimum caller-only merge-descendants repair after the run; no old assertion or shared approved component should be weakened. Final status requires the repaired input to be rerun.

The separate review finding for `M10CoverEntryScreensTest#subscription_without_cover_exposes_safe_placeholder_and_add_cover_entry` is also retained: its original assertion checks visible "添加封面" text on `subscription-cover-edit`. A content description alone does not satisfy this original contract. Root plans a labelled 48dp cover action in a separate wrapping row after the current run; this audit does not mark the predicted failure as an independently executed result.

M10 JPEG failures, old form timeout uncertainty, full regression and Task8 checks remain inherited and outside this audit.
