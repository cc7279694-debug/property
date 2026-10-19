import java.util.HashSet;
import java.util.Set;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/** Actual unchanged/new Stage methods on host files; never an Android result. */
public final class StageRecoveryRunner {
    public static void main(String[] names) throws Exception {
        Class<?> type = Class.forName("com.cooper.lithic.data.media.jpegstage.JpegStageV2Test");
        Set<String> seen = new HashSet<>();
        int total=0, failures=0, ignored=0, assumptions=0;
        for (String name : names) {
            if (!name.startsWith("recovery_") || !seen.add(name)) throw new IllegalArgumentException("Invalid selected identity");
            Result result = new JUnitCore().run(Request.method(type, name));
            total+=result.getRunCount(); failures+=result.getFailureCount(); ignored+=result.getIgnoreCount(); assumptions+=result.getAssumptionFailureCount();
            System.out.println((result.wasSuccessful() ? "PASS " : "FAIL ")+name);
            for (Failure failure : result.getFailures()) System.out.print(failure.getTrace());
        }
        System.out.println("RECOVERY_STAGE_TESTS="+total+" FAILURES="+failures+" IGNORED="+ignored+" ASSUMPTIONS="+assumptions);
        System.exit(total==names.length && failures==0 && ignored==0 && assumptions==0 ? 0 : 1);
    }
}
