import com.cooper.lithic.research.StrictJpegCandidate;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

/** Linux-host JNI execution only; no Android or business commit authority. */
public final class JniHostTest {
    private static int calls;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static long[] result(int fd, long budget, int expected) {
        long[] value = StrictJpegCandidate.validateReadonlyFd(fd, budget);
        calls++;
        check(value != null && value.length == 16, "JNI result shape");
        check(value[0] == expected, "classification: " + value[0]);
        check(value[3] == 0 && value[4] == 0 && value[5] == 0, "allocation release");
        check(value[6] == 1 && value[7] == 1, "owned FD and decoder release");
        check(value[14] == 0 || value[15] == 1, "create/destroy responsibility");
        if (expected == 0) check(value[8] == value[9] && value[10] == 1, "complete decode");
        return value;
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("load-error")) {
            try {
                System.load(Path.of(args[1]).toAbsolutePath().toString());
                throw new AssertionError("Missing library loaded");
            } catch (UnsatisfiedLinkError expected) {
                System.out.println("{\"loadFailurePreserved\":true,\"Android\":\"NOT_RUN\"}");
            }
            return;
        }
        check(args.length == 4, "library, healthy, damaged, 40MP progressive input required");
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        Field field = FileDescriptor.class.getDeclaredField("fd");
        field.setAccessible(true);
        for (int index = 1; index <= 3; index++) {
            try (FileInputStream stream = new FileInputStream(args[index])) {
                stream.getChannel().position(7);
                int fd = field.getInt(stream.getFD());
                result(fd, 512L * 1024 * 1024, index == 2 ? 10 : 0);
                check(stream.getFD().valid() && stream.getChannel().position() == 7, "caller FD/offset changed");
                if (index == 1) {
                    result(fd, 1, 20); // Real native context allocation denial.
                    result(fd, 0, 20); // Bridge argument gate.
                    result(fd, -1, 20);
                    check(stream.getFD().valid() && stream.getChannel().position() == 7, "failure altered caller");
                }
            }
        }
        result(-1, 512L * 1024 * 1024, 40);
        int closed;
        try (FileInputStream stream = new FileInputStream(args[1])) {
            closed = field.getInt(stream.getFD());
        }
        result(closed, 512L * 1024 * 1024, 40);
        System.out.println("{\"jniCalls\":" + calls + ",\"callerOffsetPreserved\":true,\"Android\":\"NOT_RUN\"}");
    }
}
