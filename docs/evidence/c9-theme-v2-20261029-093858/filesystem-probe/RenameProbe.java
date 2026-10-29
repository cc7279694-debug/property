import java.nio.file.*;
public class RenameProbe {
 public static void main(String[] args) throws Exception {
  Path dir=Files.createTempDirectory("lithic-c9-rename-");
  Path target=dir.resolve("target"), source=dir.resolve("source");
  try {
   Files.writeString(target,"old"); Files.writeString(source,"new");
   boolean legacy=source.toFile().renameTo(target.toFile());
   System.out.println("os="+System.getProperty("os.name"));
   System.out.println("File.renameTo_existing_target="+legacy);
   System.out.println("target_after_legacy="+Files.readString(target));
   if (!legacy) Files.move(source,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   System.out.println("NIO_atomic_replace_result="+Files.readString(target));
  } finally { Files.deleteIfExists(source); Files.deleteIfExists(target); Files.delete(dir); }
 }
}
