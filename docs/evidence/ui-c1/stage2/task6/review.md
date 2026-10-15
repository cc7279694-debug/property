# Task6 scoped Codex code review

Reviewed 2026-10-15 against source baseline `f000639c62fce71be0a9240425c8e27b1a5ed974`, after subscription agent reported SOURCE_READY/frozen. This is a **Codex subagent source review**, not ChatGPT independent review, visual acceptance, test execution, or UI-C1/M10 acceptance.

Read-only scope: current `ReviewShell.kt`, `SettingsShell.kt`, `ProjectHistoryScreen.kt`, `LithicNavigation.kt`; subsequently all three frozen subscription screens, their diff, existing relevant tests, and new Task6 tests. This report is the only file written by this review task. No source changes, ADB, Gradle/build/test, Git mutations, or device/settings changes were performed. Root's targeted GREEN run was ongoing during the final source review; this report does not claim its result.

## Findings

### P2 — Restore the existing visible subscription add-cover label

**Location:** `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt:81` (header `subscription-cover-edit` IconButton).

The old `CoverSection` exposed an actual text button labeled “添加封面” / “管理封面”. The replacement is an icon-only IconButton with the old tag and callback, but only a `contentDescription`. The preserved old method `M10CoverEntryScreensTest#subscription_without_cover_exposes_safe_placeholder_and_add_cover_entry` at `app/src/androidTest/java/com/cooper/lithic/feature/media/M10CoverEntryScreensTest.kt:54` explicitly requires `subscription-cover-edit.assertTextContains("添加封面")` (assertion continues on line55). A content description is separate from Compose text semantics and cannot satisfy that assertion. This is an introduced mismatch with an existing retained UI contract; a static source finding, **not an observed runtime test failure in this review**.

Keep the compact header, original tag, `onEditCover` callback, min48dp touch target, and original `!state.loading && state.error == null` predicate, while restoring an actual visible add/manage label in a compact text action. Do not alter/remove/weaken the existing assertion or synthesize hidden text semantics to pass it. Re-run the unchanged M10CoverEntryScreensTest along with the targeted Task6 tests after the fix. Root and subscription agent were notified; source stayed frozen during the ongoing run.

The no-cover placeholder itself is **not missing**: it is preserved in `SubscriptionName` as `subscription-cover-empty` with “尚未设置封面” description. The finding concerns the add/manage action's label only.

No other actionable source regression was found within the inspected changes. Task6 should not be called ready to commit until this finding is repaired and verified.

## Verified source contracts

### Root-owned shells/history/navigation

| File | Source review result |
|---|---|
| `feature/review/ReviewShell.kt` | Only the two real history destinations remain; their original callback parameters and tags remain. No inferred history counts, whole-ledger empty claim, fake controls or future placeholders. Cards are actual onClick targets, min48dp, wrapping text in a scrollable parent. Unused projectCount is retained for call-site compatibility and explicitly not used to invent history counts. |
| `feature/settings/SettingsShell.kt` | Version is read from `LocalContext.current`'s own installed package via PackageManager, with nullable versionName fallback; no design-version hardcode. Actual provided `recordZone.id` remains. No OnClick/switch/arrow is attached to read-only rows. Error-colored first warning retains “开发体验版”, “完整备份与恢复尚未开放”, and “请勿将重要记录仅保存在本App中。” It is not diluted to a footer. Existing `record-zone`, `settings-screen` and backup-warning text contracts remain. |
| `feature/project/ProjectHistoryScreen.kt` | Existing state object/callbacks remain. LazyColumn retains stable project IDs, dates, heldDays, `asOf`, dailyRate, error/retry/loading/empty/loadMore conditions. Empty is suppressed when loading/error; retained rows get explicit stale-history text when read fails. Pagination enable predicate stays `!loading && !loadingMore`. Optional trailing mediaRepository preserves old call sites; actual CoverThumbnail is used only for a real asset reference, while EntryCard provides no-cover placeholder. Back is min48dp with original tag and full Chinese description. |
| `app/LithicNavigation.kt` | Diff contains exactly three additional `mediaRepository = state.mediaRepository` arguments at the project-history, subscription-list and stopped-subscription-history call sites. `requestLeave`, shared `onBack`, globalLeaveDecision invocation, bottom navigation callbacks, NavDisplay/system Back, route IDs/origin, start/load effects, commit/deletion consume+navigation ordering, dirty/discard, pending/recovery destinations, and media cleanup logic are unchanged by this scoped diff. No new guard bypass was introduced. |

Existing `NavigationShellTest#settings_reads_the_persisted_zone_without_fake_controls` still finds exact backup warning text and zero switches. The new UIC1SecondaryPagesTest adds real callbacks, no guessed absence, read-only version/zone semantics and prominent warning checks; it does not replace old tests.

### Subscription management and history

- `SubscriptionListScreen` retains original ACTIVE / PAUSED / EXPIRED grouping and order within each filtered group; STOPPED is not added to current management. Existing tags, fixed original IDs, onOpen callback and pagination/error/retry/empty guards remain.
- Extracted `SubscriptionEntryCard` retains existing `renewals.maxOfOrNull { paidOn }`, historicalAverage and standardPurchasedDays facts. No payment ordering query or engine calculation was changed. Both current/history lists reuse this same visual presentation.
- Optional trailing mediaRepository keeps previous function call compatibility and is provided from the existing navigation owner. `CoverThumbnail` and approved EntryCard placeholders reuse the current loader/provider; no new source-URI decode, media lifecycle owner or cache was introduced.
- `SubscriptionHistoryScreen` retains source-provided stopped-history items, same identity/open callback, error/stale/loading/empty distinctions and original continuation predicate. History count is still actual `state.totalCount`, not a fabricated value. Reenable continues through the original detail route.

### Subscription detail facts and actions

| Contract | Compared result |
|---|---|
| Four distinct cost/time concepts | `historicalAverage`, `currentRate/currentPrice`, `standardPurchasedDays`, `elapsedPaidDays` still use the same authoritative fields. Elapsed days moved into the summary but are not relabeled standard days. Existing metric tags remain. |
| Future coverage | Current plan still uses `detail.currentRenewal`, never `renewals.last()`. Original no-current-plan and confirmed-future-period explanation remain. Each future renewal retains its `effectiveFrom.isAfter(asOf)` tag/text. 30/90/365 explanation and exclusive paidUntil boundary are unchanged. |
| Aggregate money | `confirmedPaidCents` still uses BigDecimal.valueOf(cents,2) and AmountUnit.YUAN. No UI rounding/cost calculation was substituted. |
| Main state operation | PAUSED -> onResume; STOPPED -> onReopen; else -> onPause. Same loading/error enable predicate and tags. |
| Real renewal | Same non-STOPPED visibility, onRenewal callback, tag and loading/error guard; moved upward into a full-width primary button. |
| Stop plan | Same earliest future STOPPED fact selection and fixed-date explanation; same onStop/correct/cancel callbacks and IDs. New required provider-cancellation disclaimer is placed before stopping. |
| State facts | All existing dates/state/reason/note rows and original onCorrectState(row.id) remain, with same enable predicate. |
| Payment history | Same `state.history.items`, stable renewal IDs, factual amount/cycle/standardDays/four dates, future marker, correct/delete callbacks, loading/error/empty/loadMore branches and enable predicates. |
| Cover management | Existing tag/callback/enable predicate retained, but visible label regression is the P2 finding above. Real CoverDisplay is reused at approved 2.2 aspect ratio; its viewer/loading/error behaviors were not rewritten. |
| Navigation/unknown submit | Screen still only invokes passed callbacks. No direct VM, route, SavedState, state-reset, auto-pop or business write was introduced. |

## Layout review limits and required runtime follow-up

The inspected source uses variable-height cards, wrapping text, LazyColumn/verticalScroll, min48dp controls, and a fontScale>=1.5/narrow-width vertical branch in the detail summary. It does not shrink system font to fit. These are appropriate static safeguards, not proof that every long name/large amount/screen/dialog is reachable at360dp/font2.

Root should verify the unchanged existing M10 cover-entry method after the visible-label fix. Continue targeted new/old subscription, shell, project-history, guard and lifecycle tests; actual light/dark/360dp/200% screenshots and business callback journeys remain required. MainActivity safe insets differ from synthetic createComposeRule hosts, so their screenshot/test results must remain separately labeled. No conclusion about real IME, TalkBack, device restore, M10 JPEG integrity or final all-test result follows from this review.

## Reviewed input hashes

SHA256 of frozen files inspected (lowercase):

| Source | SHA256 |
|---|---|
| ReviewShell.kt | `f0985461fda0411534808bc4e72ec8d1902d6766af2b4c056139fc75c99a10aa` |
| SettingsShell.kt | `04e84b575cf9eb32f88cdd92acad7232a098bd043e8c8b4c0dba9ed9b065f5dd` |
| ProjectHistoryScreen.kt | `eea06f1c08d692231de6b51bee99b68625efbfa670d3210801771beb2c4c8a8c` |
| LithicNavigation.kt | `7d871ca1a6de0313402fb85cca16cfa8184f49fdd71bce8a2fe32e259c9c111c` |
| SubscriptionListScreen.kt | `3d092557cc144ab8b744eae3c05e120ccbc75a527f2ff82aca44c04a32317756` |
| SubscriptionHistoryScreen.kt | `b83e1f326b3a744974aad406b614c4e013838d676f683380e1f29696b6ad3cfb` |
| SubscriptionDetailScreen.kt | `458dab29970039f3e79beea5c2e770d815628123f207d9fb27aab5031b58aa2d` |

If files change to repair the finding, this review remains a record of the above inputs; append/review the repaired diff and its test evidence rather than silently treating this report as reviewing a different input.

## Follow-up review — visible cover label repaired

Read-only source re-review on 2026-10-15, requested after the root repaired the P2 finding. Original finding and original input hashes above are retained. No shell/process, Gradle, ADB, Git command, source edit, or test execution was performed during this follow-up; files were read directly and hashed in memory. Only this addendum was written.

**Source-level disposition: ADDRESSED; runtime verification pending.** The repaired `SubscriptionDetailScreen.kt:81` uses a real `TextButton` with actual visible “添加封面” / “管理封面” text, `softWrap = true`, `widthIn(max = 136.dp)` and `heightIn(min = 48.dp)`. It retains `subscription-cover-edit`, `onEditCover`, the original `!state.loading && state.error == null` enable predicate, and the `!state.missing && state.detail != null` visibility condition. The neighboring weighted Back button retains the actual `backLabel` text and original guarded callback. The fix adds no hidden text semantics, route mutation or write behavior.

The exact delta from the previously reviewed detail file is limited to removing the IconButton import, adding widthIn, changing that one cover action from IconButton to the bounded TextButton, and replacing its image icon with the visible conditional text. Reconstructing the pre-fix file from those changes produced SHA256 `458dab29970039f3e79beea5c2e770d815628123f207d9fb27aab5031b58aa2d`, exactly matching the original reviewed input. This confirms the remaining detail source was not changed by this repair.

- Repaired SubscriptionDetailScreen SHA256: `a7afefe7ea76fafb59baad3fb59aa8c8e63d8fd2f5b8a4c6949208afe0400ba9`.
- The other six reviewed production files still match all six original hashes listed above.
- Existing `M10CoverEntryScreensTest.kt` SHA256 remains `06d2b409d143dd566f6da4a2ecaebca1380a09a57d85579b8aaf8b772e178303`, identical to the prior refinement input manifest; original method identity and `assertTextContains("添加封面")` lines54–55 remain unchanged.

No further actionable source issue was found in this narrow repair. The root's next targeted run should include that unchanged M10CoverEntryScreensTest. A run which executes zero methods is not runtime verification; no GREEN or rendered360dp/font2 outcome is claimed in this addendum. The original review's device/layout/full-regression limits remain.

## Follow-up review — status badge semantics and actual 65-method result

Read-only review of root's two-line status-label fix and the completed preceding targeted run. No build, device operation or Git mutation was performed by this reviewer. The original source findings and hashes remain intact.

### Actual pre-fix run, independently read from retained files

`build/ui-c1-stage2/task6-green-after-restart-summary.json` reports **65 actual Android methods, 63 PASS / 2 FAIL / 0 skipped, exit1**, duration544.191s, inputsStable=PASS. Raw `task6-green-after-restart-android-0.xml` has exactly65 testcase elements and2 failure elements, matching the summary and XML suite header. Its suite execution time is394.42s; this is distinct from total command duration. This is a failed targeted run, not final full regression.

The two failures are:

1. `M09SubscriptionScreensTest#paused_expired_coverage_keeps_paused_status_and_explains_expiry`: `assertTextContains("已暂停")`, test source line46. XML shows tagged node `subscription-detail-status` with `IsContainer=true`, one child, and no Text/InputText/EditableText property.
2. `M09SubscriptionLifecycleFlowTest#expiry_needs_no_worker`: `assertTextContains("已到期")`, test source line59. The same tagged container has one child and no text property.

This confirms the observed failure mechanism is the status text's **semantic ownership**: f000 tagged the actual Text; Task6 tags the StatusBadge's Surface. The existing shared StatusBadge (`UiPrimitives.kt:80`) renders `Text(label)` as its child, without merging it into the externally tagged Surface. The failure is not an observed missing/incorrect domain status value. The unchanged status-label mapping still supplies the actual visible label to that child.

The earlier cover-label finding also has runtime evidence now: **both unchanged M10CoverEntryScreensTest methods PASS in this65-method run**, including the subscription add-cover text assertion. That verifies the repaired visible label for this run's input; no broader media-integrity conclusion follows.

Evidence fingerprints:

- XML SHA256: `048cd81da89c404dc38a712b58083d6de716bf87aa73bdc9189c6f43c3519f06` (matches report entry in summary).
- Summary SHA256: `b18459bda0e291b3d9504fab603028b43fad1d4a5260bc6be7ef1d3eb43c8353`.
- That run's SubscriptionDetailScreen input is `a7afefe7ea76fafb59baad3fb59aa8c8e63d8fd2f5b8a4c6949208afe0400ba9`, the previous addendum's reviewed label fix, before the semantics repair.

### Exact status repair

Both responsive layout branches now use:

`Modifier.testTag("subscription-detail-status").semantics(mergeDescendants = true) {}`

at current `SubscriptionDetailScreen.kt:113` and `:121`. Merging exposes the existing actual visible child Text to the original tagged-node assertions and accessibility consumers. It does not inject fabricated/hidden text, override the display label, change domain status, add a callback, or modify the shared StatusBadge used elsewhere.

**Source-level disposition: ADDRESSED; post-fix runtime verification pending.** Removing exactly those two added `.semantics(mergeDescendants = true) {}` calls in memory reconstructs SHA256 `a7afefe7ea76fafb59baad3fb59aa8c8e63d8fd2f5b8a4c6949208afe0400ba9`, matching the actual65-method input and proving there are no other detail-file edits in this repair. Current repaired detail SHA256 is `6a0fad60a876162718a2c5c87fcc5cba3ac3a45c5b566a717d0749955f29a025`.

Both old failing test files still exactly match the earlier refinement source manifest:

- M09SubscriptionScreensTest: `9fe901e9b8f7a59ce84594a3c895067487efbe8395f954a22e185524276f4a56`.
- M09SubscriptionLifecycleFlowTest: `f7ec0763e7ee0d576b7e133b07bf97107e49bb2a852adb2fdd5c0ca11e4c4d18`.

The root's additional read-state coverage tests are outside this exact two-line source review. The next targeted run must exercise the final changed input and retain these original assertions; no final PASS, completed Task6, rendered stress-layout success or UI-C1 acceptance is claimed here.


## Root final typography check

The later change is exactly one existing Text style from titleLarge20sp to bodyLarge16sp, keeping full label, value, tag and native system scaling. Inverse reconstruction matches the preceding reviewed hash6a0fad60...; no calculation or callback changed. Final file SHA256 `2bb72e8f39372fdb1907a699df1a287670def7ae34ed67375e83e49b500216e8`. Six subscription visual/read-state tests plus Lint/assemble passed in task6-duration-refinement; both new MainActivity detail PNGs were directly inspected and the unit stays with30. This root addendum is not ChatGPT independent review.
