# Frozen v1 fixture diagnosis (read-only)

Confirmed: the frozen v1 export was converted from LF to CRLF in the Windows worktree, then copied unchanged into the Android test APK. The fixture correctly rejects these different raw bytes before opening or creating a SQLite database. This is an environmental test-resource byte problem; it does not demonstrate a production migration failure.

HEAD: `45e91163b07bcfe41927310ef37dabe7f003cdfd`; branch: `feat/ui-c1-visual-redesign`. Collected UTC: `2026-10-16T01:23:51.245337+00:00`. The full Android command is still running, so this document does not state final suite counts.

## Exact evidence

- `V1DatabaseFixture.kt:16` fixes expected SHA-256 to `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`.
- `V1DatabaseFixture.kt:34` reads `com.cooper.lithic.data.local.LithicDatabase/1.json` from the instrumentation context assets; line 35 checks its raw SHA-256. SQLite file creation begins after this check.
- Original M04, M08 reviewed `a716bc5ad9e3ab49cbe858f2c966fecf93e09fbb`, M09 freeze `57de897917473f255d43dfb306a8433b366c00eb`, and current HEAD all resolve v1 to identical Git blob `0158d4a176d1a78c379909d5892b0104da26c4ff`: **10,840 bytes, SHA-256 `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`, 347 bare LF, 0 CRLF, no final newline/BOM**.
- Current `app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json`: **11,187 bytes, SHA-256 `a765bcde9a519ff68f1513b5a58d08c1702c2b6e6e8b798d561c4ef4cfe5eb6a`, 347 CRLF, 0 bare LF/CR, no final newline/BOM**.
- In-memory replacement of exactly `CRLF -> LF` makes the entire worktree file byte-for-byte equal to every historical original. The exact size difference is 347 inserted CR bytes. Parsed JSON objects are also equal; version remains 1 and Room identity remains `bdbb5a3f035020f10628f46ddfa4551d`.
- `git ls-files --eol` reports `i/lf w/crlf attr/` for v1; there is no matching attribute. Effective `core.autocrlf=true` originates in `C:/Program Files/Git/etc/gitconfig`.
- `app/build.gradle.kts:40` copies `$projectDir/schemas` into androidTest assets. No schema v1 asset is present in the production APK.

| Actual current artifact | Size | SHA-256 | v1 asset |
| --- | ---: | --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | 12,982,546 | `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46` | Absent |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | 2,919,086 | `46756beab59e0c069f260136a5d71a32f24ca4d01e1e65ef98e7acce48800ba1` | `assets/com.cooper.lithic.data.local.LithicDatabase/1.json`; identical to CRLF worktree |

- Frozen full-run ZIP `build/ui-c1-stage2/stage2-final-full-source.zip` SHA-256 `b7cc2f655761ecb49d61bf0db578bd7ec0931589ef350efc51d8ec71134dced2` also contains the same 11,187-byte CRLF v1 input. All **187** current source inputs still match the frozen summary at diagnosis time.

## Failure boundary

- The live log snapshot contains **22** exact `The frozen v1 export changed` failures at `V1DatabaseFixture.kt:35`, plus **3** unrelated known JPEG failures. The check fails before fixture creation; these are not 22 independently diagnosed migration regressions.
- Live progress is recorded in the JSON report. The full run is unfinished. The interim summary currently contains only 86 completed JVM tests and must not be reported as a full Android/suite PASS.

## Minimal proposed repair after current full run finishes

1. Preserve all original failed-run logs, XML, input snapshot and APK fingerprints.
2. Restore only the v1 worktree file from the exact unchanged HEAD blob bytes. Do not edit JSON content, SQL, version, identity, expected hash, fixture code or assertions.
3. Add only the exact-path attribute `app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json -text` to prevent future checkout conversion. Do not run repository-wide normalization or touch v2.
4. Verify raw SHA-256 against the existing constant and unchanged Git blob ID. This restores the originally frozen export bytes; no Room schema/migration content modification is needed.
5. Create a new frozen input snapshot and rerun all three authorized commands unfiltered. Inspect the new APK asset bytes. Keep the separate known JPEG failures explicit.

## Scope and limitations

This subtask ran only read commands and in-memory comparisons. No Gradle, ADB, source/APK modification, Git mutation, database operation or device action was performed. Only this Markdown file and its JSON companion were written under the permitted ignored build directory. No claim is made that a byte restoration has been applied or that the 22 tests have passed after repair.

Git status observed: `M docs/CURRENT_STATE.md` (pre-existing parent state-document change).

The JSON companion records all hashes, byte/newline counts, refs, asset paths, input validation and the 25 observed failure identities.
