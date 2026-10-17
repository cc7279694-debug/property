package com.cooper.lithic.m10r3.probe;
import com.cooper.lithic.m10r3.probe.SessionEnvelope;
interface IHost {
    boolean hello(in SessionEnvelope identity);
    boolean grant(in SessionEnvelope request);
    boolean result(in SessionEnvelope result);
}
