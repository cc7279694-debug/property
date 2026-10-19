import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/** Executes actual cached JUnit against freshly compiled, unchanged production Java components. */
public final class HostSafetyRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("Fixed group and class names required");
        String group = args[0];
        if (!group.equals("EXISTING_COMPONENT_REGRESSION") && !group.equals("REPRODUCES_OPEN_BLOCKER")) {
            throw new IllegalArgumentException("Unknown evidence group");
        }
        List<Class<?>> classes = new ArrayList<>();
        Set<String> declared = new HashSet<>();
        for (int i = 1; i < args.length; i++) {
            Class<?> type = Class.forName(args[i]);
            classes.add(type);
            for (Method method : type.getDeclaredMethods()) if (method.isAnnotationPresent(Test.class)) {
                if (!declared.add(type.getName() + "#" + method.getName())) throw new IllegalStateException("Duplicate method");
            }
        }
        List<String> identities = new ArrayList<>(declared);
        Collections.sort(identities);
        for (String identity : identities) System.out.println("DECLARED " + identity);
        Set<String> executed = new HashSet<>();
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener() {
            @Override public void testStarted(org.junit.runner.Description description) {
                String identity = description.getClassName() + "#" + description.getMethodName();
                if (!executed.add(identity)) throw new AssertionError("Repeated execution " + identity);
                System.out.println("EXECUTED " + identity);
            }
            @Override public void testFailure(Failure failure) { System.out.print(failure.getTrace()); }
        });
        Result result = junit.run(classes.toArray(new Class<?>[0]));
        boolean matched = declared.equals(executed);
        System.out.println("SUMMARY group=" + group + " declared=" + declared.size() + " executed=" + result.getRunCount()
                + " failures=" + result.getFailureCount() + " ignored=" + result.getIgnoreCount()
                + " assumptionFailures=" + result.getAssumptionFailureCount() + " identityMatch=" + matched);
        if (!result.wasSuccessful() || !matched || result.getIgnoreCount() != 0 || result.getAssumptionFailureCount() != 0) System.exit(1);
    }
}
