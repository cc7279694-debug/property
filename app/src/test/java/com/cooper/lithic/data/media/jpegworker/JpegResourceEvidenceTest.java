package com.cooper.lithic.data.media.jpegworker;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class JpegResourceEvidenceTest {
    private static final String ID="12345678-1234-1234-1234-123456789abc";
    private static String permit() {
        return "version=1\ngeneration=m10-resource-v2-"+ID+"\nassetId="+ID+"\nattemptId="+ID+
            "\nsessionId="+ID+"\nnonce="+ID+"\nsourceBytes=17163380\nsourceSha="+
            "c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7\n"+
            "fixtureName=large_40mp_baseline_420.jpg\nhostPid=123\nhostUid=10216\nhostStartTicks=456\nbootId="+ID+"\n";
    }
    private static String status() {
        return "Name:\tworker\nPid:\t234\nTgid:\t234\nUid:\t10216\t10216\t10216\t10216\n"+
            "VmRSS:\t1000 kB\nVmHWM:\t1200 kB\nThreads:\t5\n";
    }
    private static String procStat() {
        return "234 (worker with space) S 1 1 1 0 -1 0 0 0 0 0 12 34 0 0 0 0 5 0 456 1 1\n";
    }
    private static long[] nativeResult() {return new long[]{0,0,240265637,0,0,0,1,1,5000,5000,1,1000,900,236212,1,1};}
    private static void rejected(Runnable call) {try {call.run();fail("Expected rejection");}catch(IllegalArgumentException expected){}}

    @Test public void fixed_permit_round_trips_without_weakening_identity() {
        Map<String,String> parsed=JpegResourceEvidence.permit(permit());
        JpegResourceEvidence.match(parsed,new HashMap<>(parsed));
        assertEquals("17163380",parsed.get("sourceBytes"));
    }
    @Test public void every_request_host_and_boot_field_is_matched() {
        Map<String,String> parsed=JpegResourceEvidence.permit(permit());
        for(String key:parsed.keySet()) {Map<String,String> other=new HashMap<>(parsed);other.put(key,"different");rejected(()->JpegResourceEvidence.match(parsed,other));}
    }
    @Test public void duplicate_marker_field_is_rejected() {rejected(()->JpegResourceEvidence.permit(permit()+"nonce="+ID+"\n"));}
    @Test public void unknown_marker_field_is_rejected() {rejected(()->JpegResourceEvidence.permit(permit()+"other=1\n"));}
    @Test public void non_synthetic_generation_is_rejected() {rejected(()->JpegResourceEvidence.permit(permit().replace("m10-resource-v2-","real-user-")));}
    @Test public void missing_marker_identity_is_rejected() {rejected(()->JpegResourceEvidence.permit(permit().replace("nonce="+ID+"\n","")));}
    @Test public void filename_cannot_substitute_incomplete_444_as_healthy() {rejected(()->JpegResourceEvidence.permit(permit().replace("large_40mp_baseline_420.jpg","large_40mp_progressive_444.jpg")));}
    @Test public void size_and_sha_must_match_fixed_fixture() {rejected(()->JpegResourceEvidence.permit(permit().replace("17163380","20406272")));}
    @Test public void permit_size_is_bounded_and_has_no_nul() {rejected(()->JpegResourceEvidence.permit(permit()+"\0"));rejected(()->JpegResourceEvidence.permit("x".repeat(4097)));}
    @Test public void all_three_original_fixture_identities_are_supported() {
        String p=permit();
        JpegResourceEvidence.permit(p.replace("17163380","15458857").replace("large_40mp_baseline_420.jpg","large_40mp_progressive_420.jpg").replace("c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7","2b02f35f08a8cb36d31a0a4f01e8810f554be67619950c8e2b0d0d2be873c5c8"));
        JpegResourceEvidence.permit(p.replace("17163380","27445246").replace("large_40mp_baseline_420.jpg","valid_40mp_progressive_444.jpg").replace("c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7","0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6"));
    }
    @Test public void proc_status_preserves_rss_and_lifetime_hwm_as_distinct_metrics() {
        Map<String,Long> value=JpegResourceEvidence.proc(status(),procStat(),234,10216,456);
        assertEquals(Long.valueOf(1000),value.get("rssKiB"));assertEquals(Long.valueOf(1200),value.get("hwmKiB"));
        assertEquals(Long.valueOf(12),value.get("utimeTicks"));assertEquals(Long.valueOf(34),value.get("stimeTicks"));
    }
    @Test public void wrong_pid_uid_or_startticks_is_rejected() {
        rejected(()->JpegResourceEvidence.proc(status(),procStat(),235,10216,456));
        rejected(()->JpegResourceEvidence.proc(status(),procStat(),234,10217,456));
        rejected(()->JpegResourceEvidence.proc(status(),procStat(),234,10216,457));
    }
    @Test public void conflicting_uid_credentials_are_rejected() {rejected(()->JpegResourceEvidence.proc(status().replace("10216\t10216\t10216\t10216","10216\t10217\t10216\t10216"),procStat(),234,10216,456));}
    @Test public void missing_duplicate_or_overflowed_metrics_are_rejected() {
        rejected(()->JpegResourceEvidence.proc(status().replace("VmHWM:\t1200 kB\n",""),procStat(),234,10216,456));
        rejected(()->JpegResourceEvidence.proc(status()+"VmRSS: 1000 kB\n",procStat(),234,10216,456));
        rejected(()->JpegResourceEvidence.proc(status().replace("1000 kB","999999999999999999999999 kB"),procStat(),234,10216,456));
    }
    @Test public void wrong_units_negative_or_rss_greater_than_hwm_are_rejected() {
        rejected(()->JpegResourceEvidence.proc(status().replace("kB","MB"),procStat(),234,10216,456));
        rejected(()->JpegResourceEvidence.proc(status().replace("1000","-1"),procStat(),234,10216,456));
        rejected(()->JpegResourceEvidence.proc(status().replace("1000","2000"),procStat(),234,10216,456));
    }
    @Test public void proc_stat_truncation_is_not_a_zero_measurement() {rejected(()->JpegResourceEvidence.proc(status(),"234 (worker) S 1 2",234,10216,456));}
    @Test public void native_success_contains_balanced_real_release_and_metrics() {JpegResourceEvidence.nativeResult(nativeResult());}
    @Test public void native_unavailable_metric_remains_unknown() {long[] a=nativeResult();a[13]=-1;rejected(()->JpegResourceEvidence.nativeResult(a));}
    @Test public void native_unbalanced_or_incomplete_success_is_rejected() {
        for(int index:new int[]{3,4,5}) {long[] a=nativeResult();a[index]=1;rejected(()->JpegResourceEvidence.nativeResult(a));}
        for(int index:new int[]{6,7,10,14,15}) {long[] a=nativeResult();a[index]=0;rejected(()->JpegResourceEvidence.nativeResult(a));}
    }
    @Test public void native_array_shape_and_status_are_checked() {rejected(()->JpegResourceEvidence.nativeResult(new long[15]));long[] a=nativeResult();a[0]=99;rejected(()->JpegResourceEvidence.nativeResult(a));}
    @Test public void monotonic_delta_overflow_or_backwards_time_is_rejected() {
        assertEquals(5,JpegResourceEvidence.delta(15,10));
        rejected(()->JpegResourceEvidence.delta(10,15));rejected(()->JpegResourceEvidence.delta(Long.MAX_VALUE,-1));
    }
}
