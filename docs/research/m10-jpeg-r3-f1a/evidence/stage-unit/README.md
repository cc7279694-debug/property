# Stage unit evidence scope

RED-StageAdmission.java.txt is an executable extraction of the old guard-only policy, not unmodified Android StageStore. It compiled and ran14 methods with13 assertion failures. RED-StageIdentity.java.txt is the initial tests-first comparison seam, with10/12 failing assertions. Both snapshots are retained; compile/test exits, raw logs and input hashes are adjacent.

GREEN ran the real StageAdmission and StageIdentity used by StageStore plus the unchanged17-method SessionGate regression:43 PASS. Final combined Gradle ran these same classes and all F1A tests:60 unique PASS, not43+60 distinct tests. Pure comparisons are actually used for owner/pin/receipt/revoke/terminal and OS stat values; the synthetic backend validates sequencing only. Android JSON/file/socket/FD/fsync behavior remains NOT_RUN.

The context StageStore hash in GREEN-inputs predates the final integer-boxing/receipt-comparison adjustment and was not a JVM-compiled Android adapter. Final build-runs/20261017T024420.362749Z/inputs.json binds that adapter to the actual research APK. Pure helper and test hashes remain unchanged.

To repeat GREEN, run the research run_f1a_jvm.py with a fresh phase and StageAdmissionTest StageIdentityTest SessionGateTest. To repeat RED, compile the retained snapshots as their class names in a temporary host tree with the same test classes using existing JDK17/JUnit4.13.2/Hamcrest1.3. Do not edit the committed source or rewrite evidence. No device command is required or permitted by F1-A.
