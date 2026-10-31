package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * GL display lists on top of the emulated fixed-function state. While a list is being compiled, the emulated GL calls and
 * Tessellator draws are kept as actions instead of being run (GL_COMPILE) or as well as being run (GL_COMPILE_AND_EXECUTE);
 * {@code glCallList} replays them against whatever the matrix, colour and texture are at that moment, as the driver did.
 * Legacy renderers compile their geometry once and call the list every frame (OpenBlocks' flags, Mystcraft's books ...).
 *
 * <p>A finished list is kept compiled: its actions in an array, its Tessellator draws as {@link LegacyVertices.Snapshot}s that
 * keep the primitives they assemble into (quads with their face normals, worked out on the first call), so a call per frame
 * only writes vertices. Lists are found by id in a paged table (no boxing per call); ids outside it use a map.</p>
 */
public final class DisplayLists {
    public static final int GL_COMPILE = 4864;
    public static final int GL_COMPILE_AND_EXECUTE = 4865;
    private static final int MAX_DEPTH = 8;
    private static final Runnable[] EMPTY = new Runnable[0];

    private static final AtomicInteger NEXT = new AtomicInteger(1);
    /** Ids 0 .. PAGES * PAGE - 1 (glGenLists hands them out from 1 up), by page; pages are made on first use. */
    private static final int PAGE_BITS = 10, PAGE = 1 << PAGE_BITS, PAGES = 4096;
    private static final AtomicReferenceArray<AtomicReferenceArray<Runnable[]>> TABLE = new AtomicReferenceArray<>(PAGES);
    /** Lists with ids outside the table (a mod choosing its own ids). */
    private static final Map<Integer, Runnable[]> OTHER = new ConcurrentHashMap<>();
    private static final ThreadLocal<Recording> RECORDING = new ThreadLocal<>();
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private record Recording(int id, List<Runnable> actions, boolean execute) {}

    private DisplayLists() {}

    private static boolean inTable(int id) {
        return id >= 0 && id < PAGES * PAGE;
    }

    private static Runnable[] get(int id) {
        if (!inTable(id)) return OTHER.get(id);
        AtomicReferenceArray<Runnable[]> page = TABLE.get(id >>> PAGE_BITS);
        return page == null ? null : page.get(id & (PAGE - 1));
    }

    private static void put(int id, Runnable[] actions) {
        if (!inTable(id)) {
            if (actions == null) OTHER.remove(id);
            else OTHER.put(id, actions);
            return;
        }
        int p = id >>> PAGE_BITS;
        AtomicReferenceArray<Runnable[]> page = TABLE.get(p);
        if (page == null) {
            if (actions == null) return;
            TABLE.compareAndSet(p, null, new AtomicReferenceArray<>(PAGE));
            page = TABLE.get(p);
        }
        page.set(id & (PAGE - 1), actions);
    }

    public static int generate(int range) {
        return NEXT.getAndAdd(Math.max(1, range));
    }

    public static void delete(int list, int range) {
        for (int i = 0; i < Math.max(1, range); i++) put(list + i, null);
    }

    /**
     * Lists being compiled on any thread: while none is, {@link #recording} answers without a thread-local lookup, and the emulated
     * GL calls check it before building the action they would record (a lambda per call otherwise).
     */
    private static final AtomicInteger COMPILING = new AtomicInteger();

    public static void begin(int list, int mode) {
        if (RECORDING.get() == null) COMPILING.incrementAndGet();
        RECORDING.set(new Recording(list, new ArrayList<>(), mode == GL_COMPILE_AND_EXECUTE));
    }

    public static void end() {
        Recording r = RECORDING.get();
        if (r == null) return;
        RECORDING.remove();
        COMPILING.decrementAndGet();
        put(r.id, r.actions.toArray(EMPTY));
    }

    /** {@code glIsList}: a list that was compiled (or is being compiled) under this id and not deleted since. */
    public static boolean exists(int list) {
        if (get(list) != null) return true;
        Recording r = RECORDING.get();
        return r != null && r.id == list;
    }

    public static boolean recording() {
        return COMPILING.get() != 0 && RECORDING.get() != null;
    }

    /**
     * Keeps {@code action} in the list being compiled.
     *
     * @return true if the action must not run now (compile only); false when nothing is compiling or the list also executes
     */
    public static boolean record(Runnable action) {
        Recording r = RECORDING.get();
        if (r == null) return false;
        r.actions.add(action);
        return !r.execute;
    }

    /** {@code glCallList}: runs a compiled list (lists calling lists are followed to a limited depth). */
    public static void call(int list) {
        Runnable[] actions = get(list);
        if (actions == null || actions.length == 0) return;
        int[] depth = DEPTH.get();
        if (depth[0] >= MAX_DEPTH) return;
        depth[0]++;
        try {
            for (Runnable a : actions) a.run();
        } finally {
            depth[0]--;
        }
    }
}
