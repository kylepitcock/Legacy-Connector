package org.kylepitcock.legacyconnector.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The central reset hook: every registered action runs on a stop or logout, and one failing action does not stop the others. */
class WorldStateResetTest {
    @Test
    void serverStopRunsServerAndBothKindsOnly() {
        AtomicInteger server = new AtomicInteger();
        AtomicInteger client = new AtomicInteger();
        AtomicInteger both = new AtomicInteger();
        WorldStateReset.onServerStopped(server::incrementAndGet);
        WorldStateReset.onClientLogout(client::incrementAndGet);
        WorldStateReset.onWorldLeft(both::incrementAndGet);
        WorldStateReset.serverStopped();
        assertEquals(1, server.get());
        assertEquals(0, client.get());
        assertEquals(1, both.get());
        WorldStateReset.clientLoggedOut();
        assertEquals(1, server.get());
        assertEquals(1, client.get());
        assertEquals(2, both.get());
    }

    @Test
    void aFailingActionDoesNotSkipTheRest() {
        AtomicInteger after = new AtomicInteger();
        WorldStateReset.onServerStopped(() -> {
            throw new IllegalStateException("boom");
        });
        WorldStateReset.onServerStopped(after::incrementAndGet);
        WorldStateReset.serverStopped();
        assertEquals(1, after.get());
    }
}
