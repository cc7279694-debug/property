package com.cooper.lithic.m10r3.probe;

import java.util.*;

/** Pure exact-identity comparisons shared by the Android stage adapter and host tests. */
final class StageIdentity {
    static boolean sameRecord(Map<String,Object> identity,Map<String,Object> record,Set<String> extraKeys) {
        if(identity==null||record==null||extraKeys==null)return false;
        Set<String> expected=new HashSet<>(identity.keySet());expected.addAll(extraKeys);
        if(!expected.equals(record.keySet()))return false;
        for(Map.Entry<String,Object> field:identity.entrySet())
            if(field.getValue()==null||!field.getValue().equals(record.get(field.getKey())))return false;
        return true;
    }
    static boolean sameInode(boolean regular,long expectedDevice,long expectedInode,long device,long inode) {
        return regular&&expectedDevice==device&&expectedInode==inode;
    }
}
