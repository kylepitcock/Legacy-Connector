package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Called by the stand-ins the loader's soft link ({@code SoftLink1201}) puts where a Forge 1.20.1 mod references a Minecraft 1.21.1 or NeoForge
 * symbol that does not exist: the call, class use or similar throws {@link UnsupportedOperationException} naming the symbol. The first execution of
 * each symbol is logged once; the log is capped so a hot path cannot flood it.
 */
public final class SoftLink1201Runtime {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/SoftLink");
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger LOGGED = new AtomicInteger();
    private static final int MAX_LOGGED = 200;

    private SoftLink1201Runtime() {}

    /** A stand-in that keeps the stack of the original instruction: always throws. */
    public static void fail(String symbol) {
        throw unresolved(symbol);
    }

    /**
     * A stand-in that does not throw: used for fluent methods (the receiver is returned) and for void/primitive methods called while the mod
     * constructs itself or registers content, where a throw would take the whole mod and the game down. Logged once per symbol.
     */
    public static void note(String symbol) {
        if (SEEN.add(symbol)) {
            int n = LOGGED.incrementAndGet();
            if (n <= MAX_LOGGED) LOG.warn("Soft-linked symbol ignored (first time, no effect): {}{}", symbol.replace('/', '.'),
                    n == MAX_LOGGED ? " (further symbols are no longer logged)" : "");
        }
    }

    public static UnsupportedOperationException unresolved(String symbol) {
        UnsupportedOperationException e = new UnsupportedOperationException("Legacy Connector: " + symbol.replace('/', '.')
                + " does not exist in Minecraft 1.21.1 / NeoForge (the mod was built for 1.20.1); the feature that needs it is unavailable");
        if (SEEN.add(symbol)) {
            int n = LOGGED.incrementAndGet();
            if (n <= MAX_LOGGED) LOG.warn("Soft-linked symbol executed (first time): {}{}", symbol.replace('/', '.'),
                    n == MAX_LOGGED ? " (further symbols are no longer logged)" : "");
        }
        return e;
    }

    /** Number of distinct stubbed symbols executed so far. */
    public static int executedSymbols() {
        return SEEN.size();
    }
}
