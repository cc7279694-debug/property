package com.cooper.lithic.m10r3.probe;

import java.io.IOException;

/** Research-only durable authorization decision; Android ownership validation remains in StageStore. */
final class StageAuthorization {
    interface Revocation {
        boolean sameOwnerRevoked() throws Exception;
        void writeSameOwnerRevoke() throws Exception;
    }

    static boolean isAuthorized(boolean current,boolean stateExists,String status,boolean pin,
            boolean terminal,boolean revoked,boolean pending) {
        return current&&stateExists&&pin&&!terminal&&!revoked&&!pending
            &&("DELIVERY_UNKNOWN".equals(status)||"FD_SENT".equals(status));
    }

    static void revokeVerified(Revocation durable) throws Exception {
        if(!durable.sameOwnerRevoked())durable.writeSameOwnerRevoke();
        if(!durable.sameOwnerRevoked())throw new IOException("durable revoke readback missing");
    }

    static void assertNoPending(Iterable<String> childNames)throws IOException {
        if(childNames==null)throw new IOException("stage namespace unreadable");
        for(String name:childNames)
            if(name==null||name.endsWith(".pending"))throw new IOException("pending metadata blocks ordinary run");
    }

    private StageAuthorization() {}
}
