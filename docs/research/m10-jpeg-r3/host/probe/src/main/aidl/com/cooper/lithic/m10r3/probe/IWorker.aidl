package com.cooper.lithic.m10r3.probe;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.cooper.lithic.m10r3.probe.IHost;
import com.cooper.lithic.m10r3.probe.SessionEnvelope;
interface IWorker {
    boolean reserve(in SessionEnvelope request, IHost host);
    boolean start(in SessionEnvelope request, in ParcelFileDescriptor source, in ParcelFileDescriptor output);
    boolean cancel(String nonce, String attempt);
    boolean retire(String nonce, String attempt);
    Bundle inspect();
}
