# UI-C1 Stage2 final local scope review

2026-10-16. This is a bounded local source/Git review, not ChatGPT independent acceptance. The final full regression was still in progress when this review was requested; no final test outcome is presumed here.

## Inputs and actions

- Worktree: `E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP`.
- Baseline: `f000639c62fce71be0a9240425c8e27b1a5ed974`.
- Reviewed HEAD: `6a25965225ace3212d1653d15b45fc56ef87411e`.
- Read the complete `docs/design/ui-c1/source-stage2/USER_INSTRUCTIONS.md`, current governing instructions/state, Task7 old-save diagnosis, current static-audit policy and actual production/test diffs.
- Reviewed the three commits: `e9bb44f` Task6 presentation/wiring, `45e9116` Task7 forms/dialogs, and `6a25965` frozen schema byte protection plus evidence.
- Performed read-only Git/source comparisons. No Gradle, ADB, device action, schema write, Git mutation, product change or formal-document update was performed. This report is the only file written by this review.

## Confirmed scope and retained safety

- Exactly 19 production Kotlin files differ from the baseline. They are navigation display wiring, shared form presentation, existing project/subscription forms and history screens, subscription presentation, review and settings. No stage1 HomeScreen, ProjectListScreen, ProjectDetailScreen, theme or CoverPreviewLoader source changed.
- Actual Git object checks across 66 selected protected files found zero changes: data/domain, ViewModels, schemas, resources, Manifest, build configuration/wrapper, toolchain and formal acceptance register. The broader production allowlist comparison also found no production source/resource change beyond the reviewed display files. No new production resource or runtime dependency was introduced.
- Navigation token comparisons retain the complete pre-dialog guard/navigation sequence after removing exactly three existing `state.mediaRepository` display injections. Dialog click/dismiss callback bodies, pending recovery target selection and the subscription operation/recovery helper tail match the original token sequences. Added scrolling and minimum touch heights do not replace guarded callbacks.
- Manual form diffs preserve controlled raw fields, date/category/direction callbacks, `state.canEdit`, correction/settlement conditions, real-payment confirmation, existing retry/conflict/UNCONFIRMED/COMMITTED branches, fixed input identities and guarded back callbacks. Card/field/header/error/danger presentation was wrapped or restyled rather than routed to new business owners.
- Subscription presentation continues to read the original historical average, current plan/standard daily rate, standard purchased days and elapsed paid days as separate facts. Current-plan selection remains inherited; history paging and actual-payment ordering are not reimplemented. Review exposes its two existing functional history callbacks; settings uses installed package version and retains the explicit incomplete-backup/data-loss warning.

## Original tests and intermittent save risk

- Fresh source enumeration found 701 unique baseline methods and 718 unique current methods: all 701 original identities remain, with 17 additions and zero missing identities.
- All 67 original test/helper Kotlin files other than UIComponentsTest match baseline text after CRLF normalization. This includes original JPEG tests, fixture expectations and business assertions.
- UIComponentsTest reconstructs the complete original token sequence after removing only the reviewed diagnostic helper/calls, one added system-inset test and the LargeTextHost safeDrawing padding/import. Original raw/error/callback assertions and the 10,000 ms wait remain. No introduced ignore/disabled/assumption marker was found.
- The test-host safe-area defect has a bounded diagnostic/RED history. Added observation changes timing, so an observed save PASS is not proof of the historical timeout root cause. Retain `INTERMITTENT / ROOT_CAUSE_UNKNOWN` and the historical external Digital Wellbeing window diagnosis separately. This review does not close that risk.

## Frozen schema bytes and attribute rule

The exact `.gitattributes` contents equal Task6 `e9bb44f` plus the single reviewed append `# Preserve frozen Room schema export bytes across Windows checkouts.` / `app/schemas/** -text`. Both schema exports have effective text attribute `unset`. This disables text conversion; it changes no schema version, table, migration, fixture expectation or test assertion.

Raw bytes were read directly with `git cat-file blob`, not inferred from Git diff or a normalized export:

| File | Baseline = HEAD = worktree SHA-256 | Bytes | JSON version |
|---|---|---:|---:|
| schema 1.json | `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b` | 10840 | 1 |
| schema 2.json | `ceb49b458c4c5baa08066586f209f96842935f5456a0a53a01220f6d958f7131` | 21042 | 2 |

The unchanged V1DatabaseFixture expected export hash is still the exact v1 hash above. Initial review use of `git archive` yielded line-converted export bytes and was rejected as a raw-blob authority; direct cat-file and Git tree-object checks resolved that review-method artifact. No schema change was found. A checkout-only CRLF restoration cannot justify changing a frozen fixture expectation, and no such change occurred.

## Result and limits

No concrete new scope violation, changed original business guard/assertion or protected Git-content change was identified in the reviewed input. HEAD and tracked working tree remained unchanged/clean at the end of read-only checks.

This result is static only. The parser helpers were reused from the reviewed static-audit implementation for identity/token checks; this is complementary manual/byte review, not an independent parser proof or Kotlin semantic-equivalence proof. The review did not execute or re-count final XML, inspect final APK assets/signature/permissions, establish screenshot provenance, inspect device ledger/media/settings or prove actual touch/IME/runtime behavior. Root must bind those results to the final frozen input and final APK separately.

The first full run's schema-byte failures must remain historical evidence; correcting source bytes and `-text` is not evidence that the subsequent full migration tests passed. The three known M10 JPEG failures remain unchanged and must retain their actual final results. If they fail, the overall regression is FAIL. Any additional final failure needs its own classification and evidence. No ACCEPTED/FROZEN, M10 repair, M11, main merge or public APK release is authorized by this review.
