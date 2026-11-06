package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_6_4.cpw.mods.fml.common.IScheduledTickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.TickType;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;



/**
 * The registered 1.6.4 tick handlers and the rules of calling them (no game types, so it is unit tested): a handler gets the ticks it lists in
 * {@code ticks()} on the side it was registered for, {@code tickStart} before and {@code tickEnd} after; a scheduled handler only on the game ticks its
 * {@code nextTickSpacing()} allows (counted per side in {@link #beginGameTick}).
 */
public final class TickDispatch {
    private static final System.Logger LOG = System.getLogger("LegacyConnector/Ticks164");
    private static final int MAX_ERRORS_LOGGED = 5;

    private static final class Entry {
        final ITickHandler handler;
        final Side side;
        final IScheduledTickHandler scheduled;
        int countdown;
        boolean active = true;
        int errors;
        long fired;
        String firstError;

        Entry(ITickHandler handler, Side side) {
            this.handler = handler;
            this.side = side;
            this.scheduled = handler instanceof IScheduledTickHandler s ? s : null;
        }
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void add(ITickHandler handler, Side side) {
        entries.add(new Entry(handler, side));
    }

    /** Whether some handler of {@code side} wants {@code type} ({@code null} type: any scheduled handler, which needs the game tick to count). */
    public boolean wants(Side side, TickType type) {
        for (Entry e : entries) {
            if (e.side != side) continue;
            if (type == null ? e.scheduled != null : listed(e.handler, type)) return true;
        }
        return false;
    }

    /** A game tick of {@code side} starts: scheduled handlers of that side are due or not for everything that ticks during it. */
    public void beginGameTick(Side side) {
        for (Entry e : entries) {
            if (e.side != side || e.scheduled == null) continue;
            if (--e.countdown <= 0) {
                e.active = true;
                int spacing;
                try {
                    spacing = e.scheduled.nextTickSpacing();
                } catch (RuntimeException | LinkageError ex) {
                    spacing = 1;
                }
                e.countdown = Math.max(1, spacing);
            } else {
                e.active = false;
            }
        }
    }

    /** Calls the handlers of {@code side} that list {@code type}. */
    public void fire(Side side, TickType type, boolean end, Object... data) {
        for (Entry e : entries) {
            if (e.side != side || !e.active || !listed(e.handler, type)) continue;
            EnumSet<TickType> set = EnumSet.of(type);
            e.fired++;
            try {
                if (end) e.handler.tickEnd(set, data);
                else e.handler.tickStart(set, data);
            } catch (RuntimeException | LinkageError ex) {
                if (e.firstError == null) e.firstError = ex.toString() + (ex.getStackTrace().length > 0 ? " at " + ex.getStackTrace()[0] : "");
                if (e.errors++ < MAX_ERRORS_LOGGED) {
                    LOG.log(System.Logger.Level.ERROR, "The 1.6.4 tick handler " + label(e.handler) + " failed on " + type + " (" + (end ? "end" : "start") + ")", ex);
                }
            }
        }
    }

    private static boolean listed(ITickHandler handler, TickType type) {
        try {
            EnumSet<TickType> ticks = handler.ticks();
            return ticks != null && ticks.contains(type);
        } catch (RuntimeException | LinkageError ex) {
            return false;
        }
    }

    private static String label(ITickHandler handler) {
        try {
            return handler.getLabel() + " (" + handler.getClass().getName() + ")";
        } catch (RuntimeException | LinkageError ex) {
            return handler.getClass().getName();
        }
    }

    /** One line per handler: label, side and how often it was called (the self test report). */
    public List<String> summary() {
        List<String> out = new java.util.ArrayList<>();
        for (Entry e : entries) out.add(label(e.handler) + " [" + e.side + "] fired " + e.fired + "x, " + e.errors + " error(s)" + (e.firstError == null ? "" : ", first: " + e.firstError));
        return out;
    }

    public int size() {
        return entries.size();
    }
}
