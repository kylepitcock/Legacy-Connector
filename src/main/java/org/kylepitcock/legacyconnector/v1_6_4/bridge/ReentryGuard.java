package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls in progress on this thread, by receiver, kind and argument: a redirect that answers through a method a 1.4.7 mod's override may call again
 * (Mystcraft's {@code getIcon} calls {@code getBlockTextureFromSide}, which the shim answers with {@code getIcon}) asks {@link #enter} first and, when it
 * is refused, answers from a plain field instead of recursing.
 */
final class ReentryGuard {
    private final ThreadLocal<List<Object[]>> open = ThreadLocal.withInitial(ArrayList::new);

    /** @return false when the same call (receiver by identity, kind, argument) is already in progress on this thread; true when it was added (call {@link #leave} after) */
    boolean enter(Object owner, int kind, int arg) {
        List<Object[]> list = open.get();
        for (Object[] k : list) if (k[0] == owner && (int) k[1] == kind && (int) k[2] == arg) return false;
        list.add(new Object[] {owner, kind, arg});
        return true;
    }

    void leave() {
        List<Object[]> list = open.get();
        list.remove(list.size() - 1);
    }
}
