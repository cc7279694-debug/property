# M10-R3 isolated worker probe

Research only; source baseline `2912601899f0282ecc6aa32d5cf26a7b44c24c63`. Independent Gradle root under `host/`, never included in the real app settings. Packages `com.cooper.lithic.m10r3.probe` and different-UID `com.cooper.lithic.m10r3.outsider`; no requested permissions, no JPEG/native/Room libraries.

Goal: real API36 Binder/FD/worker lifecycle and synthetic stage-v2 recovery evidence. The external driver targets only `emulator-5596`, checks its AVD identity, never clears data, replaces the Lithic APK, or reads private ledger/cover content. Original-app checks use package/APK and private-file metadata only; metadata equality is not a byte-level ledger integrity proof.

Research refinement: worker holds an exclusive abstract `LocalServerSocket` named by the owned random session lease from reserve until self-retirement. A restarted Host must acquire and retain that exact lease while durably revoking the old attempt and cleaning only verified inodes. Lease availability is cooperative-consumer evidence, not independent process-death proof or an OS sandbox. The initial FileLock sketch was rejected before device execution because closing another FD for the same inode can release a POSIX process lock. No host code kills a caller-supplied or persisted PID.

Build uses a byte-identical, timestamped ASCII source projection under `E:/CODEX/.codex/research-build/m10-r3/` because the first direct Unicode-path AIDL build failed with `MalformedInputException`. The source manifests, projection identity and all build outcomes are archived; APKs/caches stay ignored. This changes no production paths, global encoding or toolchain.

Acceptance: request identities fail closed; no active-consumer cleanup, no late synthetic success after revoke, no double grant; host/worker death and new-host recovery converge using internal evidence rather than an external PID assertion. Any listed safety violation stops further device experiments. Failures/unknowns are retained. Production gates (API26 actual recovery, native all-thread failure/OOM, allocator and normalization) remain unproved.

Verification uses JVM gate tests plus external ADB-supervised Android scenarios with raw event logs. R1 120-second UNKNOWN and M10 three FAIL remain unchanged. At completion stop for ChatGPT review; do not start another research stage or M11.
