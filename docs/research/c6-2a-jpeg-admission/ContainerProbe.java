package c6audit;

import java.nio.file.Files;
import java.nio.file.Path;
import com.cooper.lithic.data.backup.BackupJpegInspection;
import kotlin.Unit;

/** Existing registered-container inspector only. No pixel/entropy decoder and no Android entry point. */
public final class ContainerProbe {
    public static void main(String[] args) throws Exception {
        for (String name : args) {
            byte[] bytes = Files.readAllBytes(Path.of(name));
            if (bytes.length >= 2 * 1024 * 1024) throw new IllegalArgumentException("Audit fixture limit");
            kotlin.Pair<Integer, Integer> size = BackupJpegInspection.INSTANCE.inspect(bytes, () -> Unit.INSTANCE);
            System.out.println(Path.of(name).getFileName() + "\tCONTAINER_ACCEPT\t" + size.getFirst() + "\t" + size.getSecond());
        }
    }
}
