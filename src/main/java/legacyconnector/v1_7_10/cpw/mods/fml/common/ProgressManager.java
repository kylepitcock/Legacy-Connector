package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FML's loading progress bars. The modern loading screen shows its own progress, so these only count and log the steps;
 * mods drive them in their loading code (Fisk's Heroes loads its hero renderers through one, and a null bar from the old
 * stub stopped that loading).
 */
public final class ProgressManager {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Progress");
    private static final Deque<ProgressBar> BARS = new ArrayDeque<>();

    private ProgressManager() {}

    public static synchronized ProgressBar push(String title, int steps) {
        return push(title, steps, false);
    }

    public static synchronized ProgressBar push(String title, int steps, boolean timeEachStep) {
        ProgressBar bar = new ProgressBar(title, steps);
        BARS.push(bar);
        return bar;
    }

    public static synchronized void pop(ProgressBar bar) {
        if (bar.step != bar.steps) LOG.debug("Progress bar {} ended at step {} of {}", bar.title, bar.step, bar.steps);
        BARS.remove(bar);
    }

    public static synchronized java.util.Iterator<ProgressBar> barIterator() {
        return new java.util.ArrayList<>(BARS).iterator();
    }

    public static class ProgressBar {
        private final String title;
        private final int steps;
        private volatile int step;
        private volatile String message = "";

        ProgressBar(String title, int steps) {
            this.title = title;
            this.steps = steps;
        }

        public void step(Class<?> classToName, String... extra) {
            step(classToName.getSimpleName() + (extra.length > 0 ? " " + String.join(" ", extra) : ""));
        }

        public void step(String message) {
            if (step < steps) step++;
            this.message = message == null ? "" : message;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public int getStep() {
            return step;
        }

        public int getSteps() {
            return steps;
        }
    }
}
