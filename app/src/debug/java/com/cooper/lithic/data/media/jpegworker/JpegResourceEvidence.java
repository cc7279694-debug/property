package com.cooper.lithic.data.media.jpegworker;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;

/** Pure Debug-only evidence checks. No Android, process or filesystem operations. */
public final class JpegResourceEvidence {
    private JpegResourceEvidence() {}
    private static final String UUID="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Set<String> KEYS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("version","generation","assetId","attemptId","sessionId","nonce",
        "sourceBytes","sourceSha","fixtureName","hostPid","hostUid","hostStartTicks","bootId")));
    public static String[] fixture(String name) {
        if("large_40mp_baseline_420.jpg".equals(name))return new String[]{"17163380","c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7"};
        if("large_40mp_progressive_420.jpg".equals(name))return new String[]{"15458857","2b02f35f08a8cb36d31a0a4f01e8810f554be67619950c8e2b0d0d2be873c5c8"};
        if("valid_40mp_progressive_444.jpg".equals(name))return new String[]{"27445246","0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6"};
        throw new IllegalArgumentException("Unknown or incomplete fixture");
    }
    private static void require(boolean ok) {if(!ok)throw new IllegalArgumentException("Resource evidence is incomplete or conflicting");}
    private static long number(String value) {
        require(value!=null&&value.matches("0|[1-9][0-9]*"));
        try{return Long.parseLong(value);}catch(NumberFormatException error){throw new IllegalArgumentException("Counter overflow",error);}
    }
    public static Map<String,String> permit(String raw) {
        require(raw!=null&&raw.length()<=4096&&raw.endsWith("\n")&&!raw.contains("\0")&&!raw.contains("\r"));
        Map<String,String> values=new HashMap<>();
        for(String line:raw.split("\n")) {
            int at=line.indexOf('=');require(at>0&&at==line.lastIndexOf('='));
            String key=line.substring(0,at),value=line.substring(at+1);
            require(KEYS.contains(key)&&!values.containsKey(key)&&!value.isEmpty()&&value.equals(value.trim()));
            values.put(key,value);
        }
        require(values.keySet().equals(KEYS)&&"1".equals(values.get("version"))&&
            values.get("generation").matches("m10-resource-v2-"+UUID));
        for(String key:new String[]{"assetId","attemptId","sessionId","nonce","bootId"})require(values.get(key).matches(UUID));
        for(String key:new String[]{"hostPid","hostUid","hostStartTicks","sourceBytes"})require(number(values.get(key))>0);
        require(number(values.get("hostPid"))<=Integer.MAX_VALUE&&number(values.get("hostUid"))<=Integer.MAX_VALUE);
        String[] fixed=fixture(values.get("fixtureName"));
        require(values.get("sourceBytes").equals(fixed[0])&&values.get("sourceSha").equals(fixed[1]));
        return values;
    }
    public static void match(Map<String,String> permit,Map<String,String> expected) {
        require(permit!=null&&expected!=null&&permit.keySet().equals(KEYS)&&expected.equals(permit));
    }
    private static String unique(Map<String,String> values,String key) {String value=values.get(key);require(value!=null);return value;}
    private static long kib(String value) {
        require(value.matches("[0-9]+[ \\t]+kB"));return number(value.split("[ \\t]+")[0]);
    }
    public static Map<String,Long> proc(String status,String stat,int pid,int uid,long start) {
        require(status!=null&&stat!=null&&status.length()<=32768&&stat.length()<=8192&&pid>0&&uid>0&&start>0);
        Map<String,String> fields=new HashMap<>();
        for(String line:status.split("\n")) {
            int at=line.indexOf(':');require(at>0);String key=line.substring(0,at);
            require(!fields.containsKey(key));fields.put(key,line.substring(at+1).trim());
        }
        require(number(unique(fields,"Pid"))==pid&&number(unique(fields,"Tgid"))==pid);
        String[] uids=unique(fields,"Uid").split("[ \\t]+");require(uids.length==4);
        for(String value:uids)require(number(value)==uid);
        long rss=kib(unique(fields,"VmRSS")),hwm=kib(unique(fields,"VmHWM"));require(hwm>=rss&&hwm>0);
        require(stat.startsWith(pid+" (")&&stat.lastIndexOf(')')>0);
        String[] parts=stat.substring(stat.lastIndexOf(')')+1).trim().split(" +");
        require(parts.length>=20&&number(parts[19])==start);
        Map<String,Long> result=new HashMap<>();
        result.put("rssKiB",rss);result.put("hwmKiB",hwm);result.put("startTicks",start);
        result.put("utimeTicks",number(parts[11]));result.put("stimeTicks",number(parts[12]));
        return result;
    }
    public static void nativeResult(long[] v) {
        require(v!=null&&v.length==16&&(v[0]==0||v[0]==10||v[0]==20||v[0]==40));
        for(int i:new int[]{2,3,4,5,8,9,11,12,13})require(v[i]>=0);
        for(int i:new int[]{6,7,10,14,15})require(v[i]==0||v[i]==1);
        require(v[3]==0&&v[4]==0&&v[5]==0&&v[6]==1&&v[7]==1);
        if(v[0]==0)require(v[10]==1&&v[14]==1&&v[15]==1&&v[8]==v[9]&&v[9]>0);
    }
    public static long delta(long after,long before) {
        require(before>=0&&after>=before);
        try{return Math.subtractExact(after,before);}catch(ArithmeticException error){throw new IllegalArgumentException("Time overflow",error);}
    }
}
