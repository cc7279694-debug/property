package com.cooper.lithic.m10r3.probe;

/** Research callback decision boundary; no Binder call/wait may run inside it. */
final class AuthorizationFence {
    interface Decision { boolean evaluate() throws Exception; }
    static boolean decide(Object lock, Decision durableAuthority, Decision mutation) throws Exception {
        synchronized (lock) { return durableAuthority.evaluate() && mutation.evaluate(); }
    }
}
