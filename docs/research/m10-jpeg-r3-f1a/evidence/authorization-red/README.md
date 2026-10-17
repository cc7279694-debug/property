# Callback authority RED

Executable host-only extraction of the old result callback's missing persistent fence, not an execution of the unchanged Android Activity. The retained source ignores the durable-authority predicate, matching that gap. Current `AuthorizationFenceTest` plus unchanged `SessionGateTest` compiled successfully; 22 ran, 4 failed, 18 passed. GREEN invokes the predicate and the gate mutation inside one shared critical section: the same 22 methods passed.

To reproduce RED, copy `authorization-fence-baseline.java.txt` as `AuthorizationFence.java` into a temporary host compilation tree with current `SessionGate.java` and those two test files; use existing JDK17, JUnit4.13.2 and Hamcrest1.3. The final Gradle build runs the current versions of both test classes. The phase `result.json` records input hashes. No Android system behavior is proven by these tests.
