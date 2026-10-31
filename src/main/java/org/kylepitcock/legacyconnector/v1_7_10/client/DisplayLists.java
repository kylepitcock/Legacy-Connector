package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GL display lists on top of the emulated fixed-function state. While a list is being compiled, the emulated GL calls and
 * Tessellator draws are kept as actions instead of being run (GL_COMPILE) or as well as being run (GL_COMPILE_AND_EXECUTE);
 * {@code glCallList} replays them against whatever the matrix, colour and texture are at that moment, as the driver did.
 * Legacy renderers compile their geometry once and call the list every frame (OpenBlocks' flags, Mystcraft's books ...).
 */
public final class DisplayLists {
    public static final int GL_COMPILE = 4864;
    public static final int GL_COMPILE_AND_EXECUTE = 4865;
    private static final int MAX_DEPTH = 8;

    private static final AtomicInteger NEXT = new AtomicInteger(1);
    private static final Map<Integer, List<Runnable>> LISTS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Recording> RECORDING = new ThreadLocal<>();
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private record Recording(int id, List<Runnable> actions, boolean execute) {}

    private DisplayLists() {}

    public static int generate(int range) {
        return NEXT.getAndAdd(Math.max(1, range));
    }

    public static void delete(int list, int range) {
        for (int i = 0; i < Math.max(1, range); i++) LISTS.remove(list + i);
    }

    public static void begin(int list, int mode) {
        RECORDING.set(new Recording(list, new ArrayList<>(), mode == GL_COMPILE_AND_EXECUTE));
    }

    public static void end() {
        Recording r = RECORDING.get();
        if (r == null) return;
        RECORDING.remove();
        LISTS.put(r.id, r.actions);
    }

    /** {@code glIsList}: a list that was compiled (or is being compiled) under this id and not deleted since. */
    public static boolean exists(int list) {
        Recording r = RECORDING.get();
        return LISTS.containsKey(list) || r != null && r.id == list;
    }

    public static boolean recording() {
        return RECORDING.get() != null;
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
        List<Runnable> actions = LISTS.getOrDefault(list, Collections.emptyList());
        if (actions.isEmpty()) return;
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
