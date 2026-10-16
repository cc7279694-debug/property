import java.nio.file.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;
import com.android.repository.impl.meta.TrimStringAdapter;
class LicenseCanonical {
 public static void main(String[] args) throws Exception {
  var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
  factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  var doc=factory.newDocumentBuilder().parse(args[0]);
  var nodes=doc.getElementsByTagName("license");
  for(int i=0;i<nodes.getLength();i++){
   var e=(org.w3c.dom.Element)nodes.item(i);
   if(!e.getAttribute("id").equals("android-sdk-license"))continue;
   String normalized=new TrimStringAdapter().unmarshal(e.getTextContent());
   String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(normalized.getBytes(StandardCharsets.UTF_8)));
   System.out.println("SDK_ACTUAL_TRIM_STRING_ADAPTER_LICENSE_SHA1="+hash);
  }
 }
}
