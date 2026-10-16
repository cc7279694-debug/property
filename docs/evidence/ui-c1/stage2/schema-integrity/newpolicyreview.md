# Frozen schema raw-byte audit policy repair

2026-10-16. Scope is `build/ui-c1-stage2/final_static_audit.py` plus this review only. No app/test/schema/attribute/document history, Git state, build, emulator or device input was changed by this subtask. **The repaired audit was not run.** Root must first finish and preserve the ongoing full-suite run, restore the exact Git-blob schema bytes, apply the specifically authorized attribute stanza, commit/freeze the new input, and then rerun all required full commands and this audit.

## What changed in the script

The former exact-Task6 `.gitattributes` restriction is replaced by one exact allowed append to the reviewed `e9bb44f` attribute content:

```text

# Preserve frozen Room schema export bytes across Windows checkouts.
app/schemas/** -text
```

The append is precisely `\n# Preserve frozen Room schema export bytes across Windows checkouts.\napp/schemas/** -text\n`. Attribute-file comparison normalizes CRLF only; any other line, changed original rule, extra comment, duplicate rule or broader exception fails. Effective `git check-attr text` must also report `unset` for both exports, so a nested rule cannot silently reenable text conversion.

New mandatory checks read raw bytes for exactly:

- `app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json`
- `app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json`

Each file's **actual worktree bytes must equal the exact captured HEAD Git-blob bytes**, and that HEAD blob must equal the original `f000639c62fce71be0a9240425c8e27b1a5ed974` blob. No newline normalization is applied to these comparisons. Schema JSON version must remain1/2 respectively. Each result records baseline/HEAD/worktree SHA256, raw byte size and equality flags in structured JSON; no schema contents or device logs are exposed.

V1 additionally must match the unchanged frozen fixture expectation:

`351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`

The script verifies that both baseline and current `V1DatabaseFixture.EXPORT_SHA256` contain that exact constant. Existing full-old-test/helper protection remains; no fixture constant, method identity, assertion or test expectation is relaxed. Schema v2 is tied to its original exact baseline blob rather than a new guessed hash.

`.gitattributes` is also included in the audit's start/end input fingerprint, alongside existing schema/source/config entries, to catch an in-audit attribute-file change.

## Preserved safeguards and evidence meaning

All original701 identity checks, actual current-method enumeration, whole-old-test equality, exceptional UIComponents full-token reconstruction, immutable data/domain/VM/media/config/history allowlist, fixed Nav callback/guard checks and input-race checks remain intact. Final method total is still computed from source; it is not hardcoded to718 or any projected total.

Previous Git-diff-based static PASS records are historical evidence of their narrower checks. They did not compare actual schema asset bytes against Git blobs and must not be rewritten or represented as proof that CRLF assets matched the fixture. Failed full-suite records and pre-fix static results must remain retained. Root reported22 frozen-v1 export failures plus the3 known JPEG failures during the ongoing full run; this subtask has not independently re-counted that run and does not claim a final result before it ends.

A raw-byte restoration and exact `-text` rule preserve the original schema; they do not authorize a schema change, migration, fixture edit, weakened assertion or JPEG fix. This static policy does not itself verify bytes packaged in the next test APK. Root must separately bind the final input and inspect the rebuilt APK's schema assets, then report the actual results of the newly executed full JVM/Android/lint/build commands.

Status: **policy repaired; execution NOT_RUN**, awaiting root's authorized restoration and new freeze. This review supersedes only the previous audit draft's attribute-policy description; it does not overwrite any historical result.
