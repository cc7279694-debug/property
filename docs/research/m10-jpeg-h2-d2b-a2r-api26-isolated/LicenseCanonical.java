// Host-only SDK metadata inspection. This is not Android App source.
import com.android.repository.impl.meta.TrimStringAdapter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

class LicenseCanonical {
    public static void main(String[] args) throws Exception {
        String text;
        if (args.length > 1 && args[1].equals("--xml")) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            var document = factory.newDocumentBuilder().parse(Path.of(args[0]).toFile());
            var licenses = document.getElementsByTagName("license");
            text = null;
            for (int i = 0; i < licenses.getLength(); i++) {
                var license = (Element) licenses.item(i);
                if (license.getAttribute("id").equals("android-sdk-license")) {
                    if (text != null) throw new IllegalArgumentException("Duplicate license");
                    text = license.getTextContent();
                }
            }
            if (text == null) throw new IllegalArgumentException("Missing license");
        } else {
            text = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        }
        String canonical = new TrimStringAdapter().unmarshal(text);
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b & 255));
        System.out.println(hex);
    }
}
