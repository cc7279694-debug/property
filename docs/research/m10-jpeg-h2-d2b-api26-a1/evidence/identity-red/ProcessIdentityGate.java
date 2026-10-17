package com.cooper.lithic.m10r3.probe;
import java.io.*;
import java.util.*;
/** COMPILABLE RED ADAPTER ONLY: reproduces D2B role's known-name bypass and
 * UNKNOWN-main rejection. New APIs are adapters, not claims of old APIs. */
public final class ProcessIdentityGate {
 public enum State {PENDING,READY,FAILED}
 public interface NameReader {String read() throws Exception;}
 public interface NanoClock {long now() throws Exception;}
 private String name="UNKNOWN",failure; private State state=State.PENDING;
 public ProcessIdentityGate(){} ProcessIdentityGate(long timeoutMs){}
 private String known(String n){return "com.cooper.lithic.m10r3.probe".equals(n)?"HOST":"com.cooper.lithic.m10r3.probe:jpeg_worker".equals(n)?"WORKER":"REJECT";}
 public void frameworkHint(String n){name=n;}
 public String beginBackground(String n){name=n;return known(n);}
 public boolean completeBackground(){state=State.READY;return true;}
 public void fail(String why){failure=why;state=State.FAILED;}
 public State state(){return state;}
 public String failureReason(){return failure;}
 public String verifiedRole(){return state==State.READY?known(name):"REJECT";}
 public String role(String n,int api,boolean main,NameReader reader,NanoClock clock){return known(n);}
 public Map<String,Object> snapshot(){return new HashMap<>();}
 public static String readCmdline(InputStream in)throws IOException {byte[] b=new byte[256];int n=in.read(b),end=0;if(n<=0)throw new IOException("empty");while(end<n&&b[end]!=0)end++;if(end==n)throw new IOException("unbounded");return new String(b,0,end,java.nio.charset.StandardCharsets.UTF_8);}
}
