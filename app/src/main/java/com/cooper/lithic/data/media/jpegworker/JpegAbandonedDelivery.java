package com.cooper.lithic.data.media.jpegworker;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** App-lifetime handoff for an undelivered result. It confers no success, receipt or cleanup authority. */
public final class JpegAbandonedDelivery<T> {
    public enum State { IDLE, SCHEDULED, RUNNING, HANDLED, FAILED }
    private final Executor executor;
    private final Consumer<T> handler;
    private final Consumer<Throwable> observer;
    private final AtomicReference<State> state=new AtomicReference<>(State.IDLE);
    private final AtomicReference<Throwable> failure=new AtomicReference<>(), observerFailure=new AtomicReference<>();
    public JpegAbandonedDelivery(Executor executor,Consumer<T> handler,Consumer<Throwable> observer) {
        this.executor=Objects.requireNonNull(executor);this.handler=Objects.requireNonNull(handler);this.observer=Objects.requireNonNull(observer);
    }
    public boolean abandon(T value) {
        if(!state.compareAndSet(State.IDLE,State.SCHEDULED))return false;
        try {executor.execute(()->{
            if(!state.compareAndSet(State.SCHEDULED,State.RUNNING))return;
            try {handler.accept(value);state.set(State.HANDLED);}
            catch(Throwable error) {recordFailure(error);rethrowFatal(error);}
        });} catch(Throwable error) {recordFailure(error);rethrowFatal(error);}
        return true;
    }
    public State state() {return state.get();}
    public Throwable failure() {return failure.get();}
    public Throwable observerFailure() {return observerFailure.get();}
    private void recordFailure(Throwable error) {
        state.set(State.FAILED);
        if(failure.compareAndSet(null,error))try {observer.accept(error);}
        catch(Throwable observerError) {observerFailure.compareAndSet(null,observerError);rethrowFatal(observerError);}
    }
    private static void rethrowFatal(Throwable error) {
        if(error instanceof VirtualMachineError)throw (VirtualMachineError)error;
        if(error instanceof ThreadDeath)throw (ThreadDeath)error;
    }
}
