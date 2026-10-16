# UI-C1 Task7 independent local source review

Reviewed 2026-10-16, read-only, against **e9bb44f4c77a2cacd16c1f4910e6512f56c6327c** in `E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP`. HEAD remained that commit. This report is the only file written by this review. No production/test source edits, Gradle, ADB, emulator operation, Git mutation, or test execution was performed.

## Result

**No concrete source blocker found in the reviewed Task7 change set.** This is a local Codex review of source, not ChatGPT acceptance, runtime verification, or a UI-C1/M10 acceptance decision. The remaining runtime and evidence qualifications below must stay visible in Task7/Task8 reporting.

Read `AGENTS.md`, the complete `docs/design/ui-c1/source-stage2/USER_INSTRUCTIONS.md`, current state/spec/plan, actual form inventory, and the bounded old-save diagnostic. Inspected the complete production diff against e9bb44f, new `FormPresentation.kt`, both new test classes, relevant real navigation/VM/repository/media implementations, and all original `UIComponentsTest` assertions around the changed test.

## Protected behavior and production review

- All reviewed production changes remain in Compose presentation files. The baseline diff is empty for the three ViewModels, data/Room/schema/migrations/repositories/owner/coordinator/media protocols, domain/model/time, Manifest/backup rules, build/Gradle configuration, and the approved home/project-list/project-detail files.
- `FormHeader` calls the supplied original `onBack` directly; it does not own navigation or state. The icon button remains 48dp in both dimensions, receives the original tag and an accessible back description, and the title retains heading semantics with unrestricted wrapping. Existing test drivers use the original back tags, so the text-to-icon change does not invalidate their selectors.
- Form card wrappers are nonclickable presentation containers. They do not merge descendants or add a second submit action. Raw String values, original date arguments/errors, note fields, direction/cycle selected semantics, callback arguments, enabled predicates, action tags, and conditional field branches remain present. The shared wrappers introduce neither a remembered business state nor an effect that resets a SavedState envelope.
- Project create/edit, money entry/correction, end/correct-end, and delete retain their original save/retry/committed/conflict/initial-read branch order. The actual-recovery checkbox keeps the parent toggleable callback with child `onCheckedChange = null`.
- Subscription create/renew/correct-payment/reenable retain explicit real-payment labels and all four distinct raw dates. Payment actions keep `verifiedCannotWrite -> COMMITTED -> conflict -> needsInitialRead -> normal/UNCONFIRMED`; state operations keep `verifiedCannotWrite || lastPayment -> COMMITTED/conflict/needsInitialRead -> confirmation`. DELETE_RENEWAL's extra error color affects display only.
- CoverPicker retains target-checked URI delivery, the exact lock/save predicates, prepared/remove/unknown/committed/conflict branches, and original media callbacks. Wrapping `CoverDisplay` in a card changes available preview width only; it does not change stored bytes, ownership, or cache identity.
- Navigation diff is confined to scrollable warning text and min48dp buttons. The complete discard recheck/consume/discard/navigate/cleanup callback, pending routing logic, target IDs, and close behavior remain unchanged.
- `git diff --check e9bb44f` completed with no reported whitespace issue. This is not a compile/build claim.

## New tests and evidence audit

### `UIC1FormVisualTest`

The four methods mount actual production form composables in a business-free isolated Compose host. The fixture defaults permit editing where intended; the COMMITTED renewal fixture stays read-only and only its read callback is invoked. Field tags match real controls, and the long text checks obtain actual `TextLayoutResult` values after scrolling rather than assuming source presence proves fit. Header back/submit targets have explicit size assertions.

**Qualification:** these tests use `LocalDensity(fontScale = 2f)` and supplied UI-state errors/callback counters. Their PASS would establish presentation/raw-value propagation and callback reachability, not actual system font configuration, real IME presence, VM validation, duplicate-write protection, recovery, or persistence. They must not replace the existing real VM/flow/guard tests or Task8 real-IME/system-font evidence. Only four form/theme/state combinations are covered; the other modes and dialogs need the already planned coverage.

### `UIC1Stage2EvidenceTest`

- Fixture writes use a checked unique child of `noBackupFilesDir`, one managed generation/coordinator, and production project/subscription/media commands. All fixture UUIDs are valid; positive payment amounts, current/prepaid coverage, pause, and expired-subscription stop facts satisfy the inspected production predicates. No active-ledger root/pointer is substituted.
- The real navigation tags, subscription cover prefix, payment-history row/delete tags, state-back route, history entries, and project-history cover prefix match current production source. The light navigation sequence returns to Home before leaving navigation; the next theme remount starts from Home while retaining only the supplied models. No direct VM form reset bypass is added.
- Capture accounting is consistent: 5 common captures per theme plus 4 extra light captures = 14. Normal/history/operation captures default to `ISOLATED_NAVIGATION_HOST`; injected stale/read-error/missing-cover cases are explicitly `ISOLATED_PRESENTATION_HOST` with distinct scenarios. The host Activity is recorded. None of these 14 images is a MainActivity screenshot.
- The missing-cover UUID exists only in an unpersisted detail copy; injected read errors do not claim actual disk corruption. Before/after fixture project/subscription snapshots, known cover bytes and record zone are compared, and the actual pointer is checked. VM cancellation/job joining precedes generation close and cleanup of the unique fixture root.

**Qualifications before delivery:**

1. The JSON records image SHA, host, API, pixel size, density/dp width, system resource font scale, explicit composition theme, generation and fixture origin. It does **not** embed final APK SHA, test APK SHA, full frozen input fingerprint or device identifier. Task8's external gallery/manifest must join those exact artifacts to each image. The 14 images supplement, and do not fulfill, the separate 16 final MainActivity images.
2. The test's standalone error screens receive `safeDrawingPadding` and do not include the app's bottom navigation. Their explicit source classification is essential when comparing layouts.
3. Evidence filenames are fixed inside `ui-c1-stage2-visual`. Root must archive a completed run before another execution so earlier raw outputs are retained. This review did not create, replace or delete any image.
4. The evidence flow does not confirm the delete action; it verifies opening/cancelling and zero fixture changes. It must not be counted as a deletion business journey.
5. Async history completion is indirectly awaited by the cover/UI driver and subsequent scroll actions; no deterministic source/tag failure was found. If a run fails during history scrolling, inspect actual loading/error state before classifying it as a product regression. No delay or timeout extension is recommended on this source-only evidence.

## Old synthetic-save timeout

The original invalid-input, error-fit, raw amount/date, name, exactly-one save, back, and 10,000ms waits remain unchanged. The added helper records observations; it does not retry, substitute a semantic click, swallow assertions, or extend the wait. The added independent test checks the full Save target above the measured navigation inset. `LargeTextHost.safeDrawingPadding()` is confined to the test host and aligns it with the existing production Scaffold inset policy.

The bounded diagnostic report correctly separates its observed callback 0->1 sample and the separately reproduced unsafe-target geometry from the old intermittent callback timeout. Diagnostic collection changes timing. **INTERMITTENT / ROOT_CAUSE_UNKNOWN remains the correct timeout status**; neither the inset fix nor any later PASS closes that historical risk. No fresh runtime result is asserted by this review.

## Runtime checks still required

Actual compilation/full tests, form/three-VM guard/rotation/date-picker/category-picker/cover-viewer checks, real IME, 360dp plus actual 200% system font, theme switching, final screenshots, and ledger/device before/after checks remain the root's work. In particular the pending-warning recovery buttons at `LithicNavigation.kt:410` remain an ordinary non-scrolling Column, as in the baseline: multiple simultaneous recovery destinations at large font should be exercised; static source alone cannot establish their reachability. This is an unverified existing layout risk, not a demonstrated new source regression.

Known JPEG failures remain outside this change and are not reclassified. Full TalkBack, physical/OEM/API26/Release/LMK/power-loss coverage remains NOT_RUN unless separately executed.

## Reviewed exact file-byte SHA-256 values

These are working-file bytes, so LF/CRLF normalization can make them differ from Git blob hashes. Re-review is needed if an implementation/test input below changes after this review.

| File, relative to repository | SHA-256 |
|---|---|
| `app/src/main/java/com/cooper/lithic/core/design/FormPresentation.kt` | `87d3b48ad8b8459140ecb9c3d6731cc958bb94a1ac76b7853eae3c298ff27c49` |
| `app/src/main/java/com/cooper/lithic/core/design/FormFields.kt` | `82d1eac100d872dcac1891de14f35cdaf882e4e92a95bc9c9b903c8f5f3be106` |
| `app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt` | `824c8edf9c92075278b13e58f8626b7657058cfd2b4d1e40d4885757eb9959f8` |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectEditorScreen.kt` | `fe88aa120ea6a408a1cc5c08ddf220823543bb2cef293760ea7c27cbf832cb17` |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectInputs.kt` | `92862e48cd58932c919f0b3c029cced3d9aa22af373e265acfccd2d69731f633` |
| `app/src/main/java/com/cooper/lithic/feature/project/MoneyEventEditor.kt` | `49f6582439a00196fd969bb34e3ede0c43c5c6034d5fe1589b549b8b8fbfc20c` |
| `app/src/main/java/com/cooper/lithic/feature/project/MoneyEventCorrectionScreen.kt` | `60ce82c7ba9e7e9e7458a37a3ec76870d9e7af731a195fbb1a0f7d1f2aa041e9` |
| `app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt` | `53e7b38b6fcc49c0b6745bad0befce17dc31aca7ecf2d63057b59ee1067f54ab` |
| `app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt` | `7588d639e44f1356b31cdb3fa2a0bcc0000c2906643d018a8ec3a45c8035524a` |
| `app/src/main/java/com/cooper/lithic/feature/project/CoverPicker.kt` | `ad454528f3574937bfc2d7a579f8cdf9bd00de9efeeeb3bcccd5644492772b82` |
| `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt` | `5101c7580b249e2f73a96b85a23e3c9135ff9ac40797e55a557644434f3a64d7` |
| `app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt` | `4a2e5dbde8a0786106a5a9e84cfdc461c701aa361b927f8278d49f01d6dd9312` |
| `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt` | `82367adc32fb713ae17b376db80b7e926603ca1e89bbcf4e88c388c2717f3b71` |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIComponentsTest.kt` | `ba78b6ac7eb00b2408925e4ec0222a201c89dc9298ca9746e39432b7e35d12f4` |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1FormVisualTest.kt` | `71c16444be8dece952707883d2e501837d0565b0a1597e19f66f3bda8faa5720` |
| `app/src/androidTest/java/com/cooper/lithic/ui/UIC1Stage2EvidenceTest.kt` | `f646bce4da149c83c45967c0a42e89dd52dd3fee3c3e210a042beaf5060569a4` |
| `app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt` | `b4da126ecc9b4c5836352717fd2a57031e5c84ed9902bcb79bdd9c685e815fc4` |
| `app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt` | `ebcea567f05c9324e2bcbffc27a54058b9a69e917b59d375e7fec93ca627c922` |
| `app/src/main/java/com/cooper/lithic/feature/media/CoverViewModel.kt` | `f85a6cd62ffe8447790f4c3aee42f3fe4f5a5d20533b5e87a3fa4c4ee9683ab0` |


## Root follow-up after runtime

The new short project title fits one line at the tested360dp/font2; removed only that new mandatory-wrap flag while retaining long renewal title wrapping, fit/error/raw/callback checks and every original701 assertion. New empty-room producer was read in full and executed in the7-PASS recheck; it records actual focused host, system vs explicit rendered theme, installed targetAPK SHA and real empty repository checks, with only its own UUID root cleanup. No production source changed after the local19-file review. Final static protection passes with718 actual methods. This is root follow-up, not an additional ChatGPT acceptance.
