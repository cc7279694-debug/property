import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/** Runs only named existing Stage tests. It performs no Android operation. */
public final class StageSafetyRunner {
    public static void main(String[] names) throws Exception {
        Class<?> type = Class.forName("com.cooper.lithic.data.media.jpegstage.JpegStageV2Test");
        Set<String> seen = new HashSet<>();
        int total = 0, failures = 0, ignored = 0;
        for (String name : names) {
            if (!seen.add(name)) throw new IllegalArgumentException("Duplicate test");
            Method method = type.getDeclaredMethod(name);
            if (!method.isAnnotationPresent(Test.class)) throw new IllegalArgumentException("Not a test");
            Result result = new JUnitCore().run(Request.method(type, name));
            total += result.getRunCount(); failures += result.getFailureCount(); ignored += result.getIgnoreCount();
            System.out.println((result.wasSuccessful() ? "PASS " : "FAIL ") + name);
            for (Failure failure : result.getFailures()) System.out.println(failure.getTrace());
            if (!result.wasSuccessful()) break;
        }
        System.out.println("EXISTING_STAGE_TESTS=" + total + " FAILURES=" + failures + " IGNORED=" + ignored);
        System.exit(total == names.length && failures == 0 && ignored == 0 ? 0 : 1);
    }
}
