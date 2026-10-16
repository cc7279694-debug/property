# Task7 project forms — implementation draft

Status: DRAFT ONLY. Read actual AGENTS.md, current Stage2 user instructions, Task7 inventory, PROJECT/CURRENT_STATE, UI-C1 specification/plan, and all five owned source files. No production source mutation, build, ADB, Git operation, old test edit, or verification execution by this subtask. Source implementation waits for root's explicit GO after recorded RED.

## Contract and ownership

Goal: make the existing project create/edit, money-entry/correction, ending/correction and permanent-delete surfaces match the approved UI-C1 system, retaining original business behavior and reachability.

Only owned source files (under app/src/main/java/com/cooper/lithic/feature/project):

- ProjectEditorScreen.kt
- MoneyEventEditor.kt
- MoneyEventCorrectionScreen.kt
- EndProjectDialog.kt (includes shared LifecycleFormActions)
- DeleteProjectDialog.kt

No ViewModel/model/domain/data/time/media/navigation/schema/config/dependency changes and no new state. Root owns FormFields, ProjectInputs, CoverPicker, navigation and shared helpers; other subtask owns subscription forms. Do not edit their files.

Dependency: root supplies presentation-only `core.design.FormHeader(title, onBack, backTag, backLabel = "返回")`, carrying existing back tag and a `${backTag}-header` tag, wrapping 24sp title/heading and original 48dp back-icon target. Each use passes the original `onBack` directly. Existing root scrolling and imePadding stay.

## Proposed minimal changes

1. **ProjectEditorScreen** — replace duplicated back/title with FormHeader. Keep CREATE/EDIT title expression. Use LithicCard to group existing name/start fields, CREATE-only initial amount/payment-date block, then category/optional note. Keep exact field order within and across groups, every raw value/error and `state.canEdit`. Render `project-form-error` on a padded errorContainer surface while retaining the original Text/tag. Preserve conflict, initial-read, busy, COMMITTED/retry and save predicates/branch order exactly. No new submit action.
2. **MoneyEventEditor** — same header replacement. Give project name an identity surface, group original direction controls and amount/date/description without putting the two long controls in a constrained horizontal row. Preserve direction list ordering, `selected` semantics, raw amount and positive-or-zero helper text, canEdit, message, busy, COMMITTED/read and UNCONFIRMED labels/save binding.
3. **MoneyEventCorrectionScreen** — same header replacement. Group original project identity/explanation and read-only direction/role, then raw correction fields. Keep loading, INITIAL/REGULAR/END_SETTLEMENT expression and all tags. Leave LifecycleFormActions argument expressions intact.
4. **EndProjectDialog / LifecycleFormActions** — same header replacement with original correcting title expression. Group original project identity/explanation, end date/reason and conditional actual-recovery block. Retain every correcting/settlement/recovery condition exactly, parent Row.toggleable and Checkbox(onCheckedChange = null), so one tap still sends one callback. Keep no-future/no-estimate/zero-allowed text and raw fields. Style the shared message with a padded error surface, leave IN_FLIGHT then COMMITTED/conflict/else branch ordering and all retry/save predicates and literal messages unchanged.
5. **DeleteProjectDialog** — retain actual AlertDialog, original title/text, scrollable body, all predicates and guarded dismiss. Apply approved rounded surface/container colors and make irreversible warning visibly error-colored on a padded errorContainer surface. Keep every message and conditional warning. Existing action targets already have min48dp; retain them and wrapping text with no fixed button height. Keep original needsRead branch ahead of permanent confirm and original cancel behavior.

Use only existing MaterialTheme/LithicSpacing/LithicCard. Presentation wrappers must not merge descendants, intercept clicks, or move tags off the original semantic input/action. No re-parsing/formatting/normalizing raw state; no extra remember, callbacks, route changes, or business-state copies.

## Root verification / acceptance handoff

- Before GO: root records new UI tests' real RED on current Task6 source. This subtask does not run tests.
- After implementation: static per-file comparison of literal strings, testTag values, callback bindings, enabled expressions, state branches and toggleable/Checkbox contract against originals; root reviews focused diff.
- Root runs focused existing project screen/entry/lifecycle/navigation/date tests and new Task7 style/reachability tests, followed by authorized actual-device Light/Dark,360dp/font2,IME and raw/error/retry/dirty/pending/COMMITTED checks.
- Fields/errors/back/confirm remain reachable in the root vertical scroll; no fixed card/text heights. Delete body remains scrollable and actions min48dp.
- Root records actual results. This draft and display implementation do not prove old synthetic timeout resolved, final full regression passed, M10 JPEG repaired, or UI-C1 independently accepted.

Current verification status: NOT_RUN (delegated draft-only phase).
