# Task6 subscription presentation implementation

Status: source ready for root-owned compilation, targeted GREEN verification and screenshot review. No test PASS is claimed here.

## Owned paths

- Modified `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt`.
- Modified `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionHistoryScreen.kt`.
- Modified `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt`.
- Added `app/src/androidTest/java/com/cooper/lithic/feature/subscription/UIC1SubscriptionVisualTest.kt` (four new methods; original tests untouched).

## Presentation changes

List and stopped history use the approved project-list spacing, compact header, existing EntryCard/StatusBadge visual language and subscription icon. New trailing default `mediaRepository` arguments preserve existing call sites. The shared subscription-only card renderer uses real bounded CoverThumbnail for referenced assets; absent references keep the subscription placeholder and unreadable references keep the existing distinct error icon. Root owns navigation injection of the current facade.

Detail uses existing LithicCard/StatusBadge surfaces, actual CoverDisplay at a 2.2 aspect ratio for a referenced image, or a compact 48dp service icon if absent. Back retains the real visible `backLabel` and its text semantics so frozen source-origin assertions remain valid. The back area has bounded weighted text; the cover-management icon is 48dp with a complete Chinese description and its original tag/enable condition.

## Metric and fact mapping

| Existing fact | Visible destination / preserved tag |
| --- | --- |
| `cost.historicalAverage` | Overview, `subscription-detail-historical-average`, original AmountText formatter |
| `cost.standardPurchasedDays` | Overview, `subscription-detail-purchased-days`; exact original 30/90/365/future-period qualification retained |
| `cost.elapsedPaidDays` | Separate explicit elapsed-paid-days line in the same overview |
| `detail.asOf` | Explicit result observation date in overview |
| `cost.currentRate`, `currentPrice`, `currentRenewal` | Independent `subscription-detail-current-plan`, original `subscription-detail-current-rate` and no-current-plan branch |
| Future renewals | Existing future-payment explanation and per-payment `subscription-renewal-future-ID`; never substituted for current renewal |
| Confirmed paid total, category, note | Basic-information card, original `subscription-detail-total-paid` |
| Last actual payment | Existing `renewals.maxOfOrNull(paidOn)` in current list; same real-fact display added to stopped history |

## Preserved operations and state handling

| Operations | Preserved callbacks / tags / conditions |
| --- | --- |
| List create/back/open | Same callbacks and `subscription-list-create`, `subscription-list-back`, `subscription-row-ID`; 48dp header controls |
| Grouping | Existing ACTIVE / PAUSED / EXPIRED sequence and order within each filtered group; stopped records retain the separate repository-fed history route |
| List/history pagination | Same `hasMore`, loading/loadingMore conditions, load-more tags and callbacks; no query or cursor changes |
| Read states | Existing loading, error, stale-data text, count, genuine empty and retry branches; detail missing remains separate |
| Renewal | `subscription-detail-renewal`, same non-STOPPED visibility and loading/error guard |
| Pause/resume/reopen | Original state switch, tags and callbacks, same loading/error guard |
| Stop/early-stop entry | Original `onStop`, `subscription-detail-stop`, same STOPPED/scheduled-stop eligibility; provider-cancellation disclaimer visible in operation card before stop |
| Scheduled stop | Original earliest future STOPPED selection, fixed-date text, correct/cancel callbacks and tags |
| State correction | All original `subscription-state-row-ID`, reason/note and `subscription-state-correct-ID` controls |
| Payment correction/delete | Original per-fact IDs, callbacks and detail/history error guards; original payment dates, boundaries and reminder facts |
| Cover management/view | Existing `subscription-cover-edit` callback and enable guard; existing CoverDisplay view/enlarge/error behavior reused |

No ViewModel, route, SavedState envelope, owner, coordinator, repository, schema, migration, domain formula, media protocol, dependency, configuration, permission, approved stage1 screen, historical source or old test file was edited by this subtask.

## Verification

- Root's preserved `task6-red-summary.json` reports compilation succeeded, inputs stable, and the four new subscription methods all failed against unchanged production for the intended missing compact-header/cover/overview/disclaimer behavior. The combined targeted run had 7 FAIL / 0 PASS, exit code 1; the other three cases are root-owned secondary pages.
- Read-only `git diff --check` over these owned files: exit code 0, no whitespace errors.
- GREEN compilation, Android execution, screenshots, full regression, device/data preservation and final APK validation: NOT_RUN by this agent; root exclusively owns these steps.
- No Gradle, ADB, commit, push or other Git mutation was run by this agent.

## Concerns retained

The historical three M10 JPEG integrity failures remain outside scope. A successful thumbnail is not an input-integrity claim. Full 360dp/200% rendered layout, all real callback journeys, themes and screenshots still require root evidence. Root must include its navigation media-facade injection in the same verification input. This report grants neither UI-C1 nor M10 acceptance.
