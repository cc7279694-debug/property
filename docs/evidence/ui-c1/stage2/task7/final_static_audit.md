# Final static audit draft — ready for root freeze

Created independently from current full Stage2 USER_INSTRUCTIONS, Git baseline `f000639c62fce71be0a9240425c8e27b1a5ed974`, the original baseline-method manifest, current UIComponents diagnostic changes, actual navigation, and the Task7 ownership brief. Only this document and `final_static_audit.py` were written by this subtask. **The audit script has not been executed.** Run only after root explicitly freezes the final input; no claimed PASS or final count exists yet.

Example invocation after the root records its full frozen HEAD (use its actual SHA):

```powershell
python .\build\ui-c1-stage2\final_static_audit.py --freeze-head <actual-full-frozen-head> --output build/ui-c1-stage2/final-static-audit.json
```

Use the configured project Python runtime. The script uses only Python's standard library and read-only Git commands. It never builds, runs Android/Gradle/tests, touches the emulator, changes Git/index/branches, or reads raw device/ledger/log content. It writes only the requested structured JSON result and prints result/failure names. Do not count its PASS as application tests or final acceptance.

## What it proves

- Derives baseline method identities directly from Git and requires exactly the user's **701 original identities**. Cross-checks the existing baseline-method manifest if present. The final count comes from actual tracked **and untracked** current Kotlin test files, with no hardcoded final total. Reports every new method identity and per-kind counts.
- Keeps all baseline test and fixture/helper source files except UIComponentsTest exactly unchanged after CRLF normalization. This is stronger than counting asserts and includes M10 JPEG tests/fixtures and all original business test helpers.
- For UIComponentsTest, reconstructs the **entire baseline token stream** by removing only the one added boundary test, the exactly fingerprinted read-only diagnostic helper, the two exact before/after diagnostic calls, and `.safeDrawingPadding()` plus its import in LargeTextHost. Original error/raw/callback/10-second assertions, the SyntheticFormHost business-free callbacks, constants, annotations and all other original code must match in exact sequence. The diagnostic helper is bound to its reviewed canonical text hash; changing it requires an explicit review/update, not a silent exception.
- Rejects introduced ignore/assumption markers. This does not weaken any inherited opt-in checks.
- Restricts production changes to the explicit Task6/Task7 display files and the one new `core/design/FormPresentation.kt`. Every other production source, resources, data/domain/core model/time, schema, VM/envelopes, media protocol, dependency/configuration, app identity and backup resources remain protected by the baseline change allowlist. Any deletion fails.
- Preserves all existing historical/checkpoint/evidence files, formal acceptance matrix, original planning/130 cases/16 boundary groups through the baseline allowlist. Only current approved docs, new Stage2 evidence, new Stage2 verification reports/checkpoints, and new source-stage2 instruction data are accepted. Existing history is not parsed or copied into output.
- Checks the reviewed Task6 `.gitattributes` form against `e9bb44f`; new attributes cannot silently alter protected evidence handling.
- Proves the navigation prefix, all guard/click/dismiss callback token bodies, recovery route selection and subscription-operation helpers match baseline; the only pre-dialog wiring exception is exactly three existing `state.mediaRepository` injections. Dialogs may receive min48dp/scrollable presentation wrappers; warning/tag strings must remain.
- Captures source/config input hashes and checks they remain identical during audit. The root's frozen HEAD must match both start and end. It fails closed on unsupported/ambiguous Kotlin method shapes rather than inventing identities.

## Limits / root follow-up

The lightweight lexical parser supports the repository's current one-test-class/block-body convention; it is not a Kotlin compiler. Full old-file protection plus exact UI reconstruction proves retained old assertions more reliably than regex assertion counts. New tests, all allowed display files and the semantics of new helpers still require normal source review and actual execution.

No static script can prove actual 701-method execution, OEM/API26/real IME/LMK behavior, database/media integrity, screenshot provenance or an unchanged installed APK. Final actual test XML/method union, build commands/exit codes, frozen input/APK binding, original ledger/private-cover/settings checks, permission/signature checks and screenshots remain separate. Known M10 JPEG failures and any unresolved form risk must remain explicit.

The script expects the authorized safeDrawing host repair to have landed before final freeze. It will fail if that repair or a required original token differs. It also intentionally rejects any extra navigation or old-test edit; explain and independently review a legitimate scope adjustment before altering the audit policy.

Baseline inventory was inspected, not freshly executed here. All audit outcomes are **NOT_RUN** until root runs the draft on frozen final input. No source/test/evidence history or Git state was modified by this subtask.
