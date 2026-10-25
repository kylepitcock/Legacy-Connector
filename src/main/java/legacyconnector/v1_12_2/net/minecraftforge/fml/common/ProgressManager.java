package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/** The loading screen progress bars of FML; the layer has its own loading screen, so these only count. */
public class ProgressManager {
    public static ProgressBar push(String title, int steps) {
        return new ProgressBar(title, steps);
    }

    public static ProgressBar push(String title, int steps, boolean timeEachStep) {
        return new ProgressBar(title, steps);
    }

    public static void pop(ProgressBar bar) {
    }

    public static java.util.Iterator<ProgressBar> barIterator() {
        return java.util.Collections.emptyIterator();
    }

    public static class ProgressBar {
        private final String title;
        private final int steps;
        private int step;
        private String message = "";

        ProgressBar(String title, int steps) {
            this.title = title;
            this.steps = steps;
        }

        public void step(String message) {
            this.step++;
            this.message = message;
        }

        public void step(Class<?> classToStep) {
            step(classToStep.getSimpleName());
        }

        public String getTitle() {
            return title;
        }

        public int getSteps() {
            return steps;
        }

        public int getStep() {
            return step;
        }

        public String getMessage() {
            return message;
        }
    }
}
