package com.cooper.lithic.m10r3.probe;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Owns Host copies only; a successful close makes no claim about Worker copies. */
final class HostFdScope implements AutoCloseable {
    enum State { OPEN, CLOSED, CLOSE_UNKNOWN }
    private static final class Entry {
        final Closeable resource;
        State state = State.OPEN;
        Throwable error;
        Entry(Closeable resource) { this.resource = resource; }
    }
    private final LinkedHashMap<String, Entry> resources = new LinkedHashMap<>();
    private final IdentityHashMap<Closeable, Boolean> owned = new IdentityHashMap<>();
    private boolean closed, closing;
    private IOException closeFailure;

    synchronized <T extends Closeable> T own(String label, T resource) {
        Objects.requireNonNull(label); Objects.requireNonNull(resource);
        if (closed) throw new IllegalStateException("FD scope already closed");
        if (label.isEmpty() || resources.containsKey(label) || owned.containsKey(resource)) {
            throw new IllegalArgumentException("Duplicate or empty FD ownership");
        }
        resources.put(label, new Entry(resource)); owned.put(resource, Boolean.TRUE); return resource;
    }

    @Override public void close() throws IOException {
        final List<Entry> entries;
        synchronized (this) {
            if (closing) throw new IOException("Concurrent FD scope close remains in progress");
            if (closed) { if (closeFailure != null) throw closeFailure; return; }
            closed = true; closing = true;
            entries = new ArrayList<>(resources.values()); Collections.reverse(entries);
        }
        IOException first = null;
        for (Entry entry : entries) {
            Throwable failure = null;
            try { entry.resource.close(); }
            catch (Throwable error) { failure = error; }
            synchronized (this) {
                entry.error = failure;
                entry.state = failure == null ? State.CLOSED : State.CLOSE_UNKNOWN;
            }
            if (failure != null) {
                IOException asIo = failure instanceof IOException ? (IOException) failure
                        : new IOException("Host FD close did not complete normally", failure);
                if (first == null) first = asIo;
                else if (first != asIo) first.addSuppressed(asIo);
            }
        }
        synchronized (this) {
            closeFailure = first; closing = false;
        }
        if (first != null) throw first;
    }
    synchronized State state(String label) {
        Entry entry = resources.get(label); return entry == null ? null : entry.state;
    }
    synchronized Throwable closeError(String label) {
        Entry entry = resources.get(label); return entry == null ? null : entry.error;
    }
    synchronized boolean allClosed() {
        if (!closed || closing) return false;
        for (Entry entry : resources.values()) if (entry.state != State.CLOSED) return false;
        return true;
    }
}
