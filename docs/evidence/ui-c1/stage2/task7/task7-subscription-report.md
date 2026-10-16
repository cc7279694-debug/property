# Task7 subscription form implementation notes

Status: SOURCE_READY; frozen for root-owned compilation and verification. Root reported valid-fixture RED: four expected missing-header failures before this implementation. This agent ran no Gradle/ADB/Git commands and makes no GREEN claim.

## Exact source changes

- `SubscriptionEditorScreen.kt`: original separate return/title replaced by root FormHeader with original back action tag and callback. Name/category grouped in a LithicCard; original first-payment heading/explanation retained; optional note grouped in a card. SubscriptionPaymentFields now emits two surface-only cards: amount + vertical cycle controls, then the same four date inputs/explanations. SubscriptionPaymentActions has one card around its unchanged ordered branch logic. The today-null raw disabled field uses root lithicFieldColors and shared 12dp/small shape. Private category dialog uses shared large shape/surface/title/body typography; original min48 options/cancel, subscription-only filtering, saveable open state, enabled predicate, onSelect-then-close, dismiss/cancel and tags remain.
- `RenewalEditorScreen.kt`: original correction/reenable booleans retained; all three exact heading strings passed to FormHeader with original back callback/tag/description. Original service text, loading, kind-specific explanation and fixed-stop warning grouped in a card. Existing shared payment/actions invocations and all argument expressions retained.
- `SubscriptionStateDialog.kt`: original title/stop calculations retained; shared FormHeader receives original onBack/tag/backLabel. Surface-only groups for identity/vendor disclaimer, conditional stop choices, original kind body and original result/confirmation branches. Existing Text nodes/buttons use consistent body/title roles. DELETE_RENEWAL warning and actual confirmation use existing theme error/onError colors; the color expression affects only display, not enable/callback/branch behavior. All confirmation/retry/choice targets remain variable height and at least48dp.

## Preservation review

No original business message was reworded. Original input/action tags remain on their real controls; new header tags are supplied by root's helper. All raw values, error keys, keyboard/date paths, canEdit/early-stop/last-payment/verifiedCannotWrite predicates, selected semantics and callbacks remain. Payment branch order remains verifiedCannotWrite → COMMITTED → conflict → needsInitialRead → normal/UNCONFIRMED. State result branch order remains verifiedCannotWrite-or-lastPayment → COMMITTED/conflict/needsInitialRead → confirmation. No new remember/SideEffect or mirrored form state, no semantics merging around cards, and no new business confirmation.

No other source/test file was edited by this subtask. Three VMs, data/domain/model/time/schema/config/media protocol, navigation identities and Task6/approved stage1 screens were untouched. Unused imports removed only from the three owned forms.

## Verification boundary

Read back all changed form bodies and checked original literal messages, callback expressions, controlled arguments, branch order and enabled expressions against the inspected pre-change source. Compilation, focused forms/guards, real IME, themes, font2/360dp, screenshots, full regression and the prior synthetic timeout investigation belong to root and remain NOT_RUN by this agent. Existing historical JPEG failures and timeout uncertainty are not resolved here.
