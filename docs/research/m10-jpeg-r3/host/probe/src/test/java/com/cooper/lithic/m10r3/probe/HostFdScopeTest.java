package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public final class HostFdScopeTest {
    private static Closeable resource(List<String> closed, String name, IOException failure) {
        return () -> { closed.add(name); if (failure != null) throw failure; };
    }

    @Test public void sourceClosesWhenOutputCreationFails() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        IOException createError = new IOException("output creation failed");
        try (HostFdScope owned = scope) {
            owned.own("source", resource(closed, "source", null)); throw createError;
        } catch (IOException actual) { assertSame(createError, actual); }
        assertEquals(Arrays.asList("source"), closed); assertTrue(scope.allClosed());
    }

    @Test public void bothHostCopiesCloseWhenStartThrows() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        try (HostFdScope owned = scope) {
            owned.own("source", resource(closed, "source", null));
            owned.own("output", resource(closed, "output", null));
            throw new IllegalStateException("start failed");
        } catch (IllegalStateException expected) { assertEquals("start failed", expected.getMessage()); }
        assertEquals(Arrays.asList("output", "source"), closed); assertTrue(scope.allClosed());
    }

    @Test public void oneCloseFailureStillClosesAllAndPreservesEvidence() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        IOException error = new IOException("output close unknown");
        scope.own("source", resource(closed, "source", null)); scope.own("output", resource(closed, "output", error));
        try { scope.close(); fail("close must report failure"); } catch (IOException actual) { assertSame(error, actual); }
        assertEquals(Arrays.asList("output", "source"), closed);
        assertEquals(HostFdScope.State.CLOSED, scope.state("source"));
        assertEquals(HostFdScope.State.CLOSE_UNKNOWN, scope.state("output"));
        assertSame(error, scope.closeError("output")); assertFalse(scope.allClosed());
    }

    @Test public void multipleCloseFailuresRetainFirstAndSuppressed() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        IOException source = new IOException("source"), output = new IOException("output");
        scope.own("source", resource(closed, "source", source)); scope.own("output", resource(closed, "output", output));
        try { scope.close(); fail("both closes failed"); }
        catch (IOException actual) { assertSame(output, actual); assertArrayEquals(new Throwable[]{source}, actual.getSuppressed()); }
        assertEquals(2, closed.size());
    }

    @Test public void operationFailureRemainsPrimaryWhenClosingAlsoFails() throws Exception {
        HostFdScope scope = new HostFdScope(); IOException closeError = new IOException("close unknown");
        IllegalStateException operation = new IllegalStateException("start failed");
        try (HostFdScope owned = scope) {
            owned.own("source", () -> { throw closeError; }); throw operation;
        } catch (IllegalStateException actual) {
            assertSame(operation, actual); assertArrayEquals(new Throwable[]{closeError}, actual.getSuppressed());
        }
    }

    @Test public void knownClosedScopeNeverClosesResourcesTwice() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        scope.own("source", resource(closed, "source", null)); scope.close(); scope.close();
        assertEquals(1, closed.size()); assertTrue(scope.allClosed());
    }

    @Test public void unknownCloseRemainsUnknownAndIsNotRetriedAutomatically() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        IOException error = new IOException("unknown"); scope.own("source", resource(closed, "source", error));
        for (int i = 0; i < 2; i++) {
            try { scope.close(); fail("unknown must remain visible"); } catch (IOException actual) { assertSame(error, actual); }
        }
        assertEquals(1, closed.size()); assertFalse(scope.allClosed());
    }

    @Test public void runtimeCloseFailureDoesNotSkipRemainingResource() throws Exception {
        List<String> closed = new ArrayList<>(); HostFdScope scope = new HostFdScope();
        RuntimeException runtime = new IllegalStateException("native wrapper error");
        scope.own("source", resource(closed, "source", null));
        scope.own("output", () -> { closed.add("output"); throw runtime; });
        try { scope.close(); fail("runtime close must remain visible"); }
        catch (IOException error) { assertSame(runtime, error.getCause()); }
        assertEquals(Arrays.asList("output", "source"), closed); assertFalse(scope.allClosed());
    }

    @Test public void ownershipAfterCloseIsRejectedWithoutClosingUnownedResource() throws Exception {
        HostFdScope scope = new HostFdScope(); scope.close(); List<String> closed = new ArrayList<>();
        try { scope.own("late", resource(closed, "late", null)); fail("closed scope cannot acquire"); }
        catch (IllegalStateException expected) { assertTrue(closed.isEmpty()); }
    }

    @Test public void duplicateLabelsAndDuplicateResourceAreRejected() throws Exception {
        HostFdScope scope = new HostFdScope(); Closeable source = () -> {};
        assertSame(source, scope.own("source", source));
        try { scope.own("source", () -> {}); fail("duplicate label"); } catch (IllegalArgumentException expected) { }
        try { scope.own("second", source); fail("duplicate resource"); } catch (IllegalArgumentException expected) { }
        scope.close();
    }
}
