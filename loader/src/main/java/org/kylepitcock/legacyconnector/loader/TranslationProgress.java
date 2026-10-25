package org.kylepitcock.legacyconnector.loader;

import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;

/**
 * A progress bar in FML's early loading window ("Translating legacy mods 3/12") while legacy jars are translated, so that a
 * first start with a big pack does not look frozen. Only jars that are not cached count; with a warm cache there is no bar.
 * Any failure to show it is ignored: it is cosmetic.
 */
final class TranslationProgress implements AutoCloseable {
    private ProgressMeter bar;
    private final int total;
    private int done;

    TranslationProgress(int total) {
        this.total = total;
        if (total <= 0) return;
        try {
            bar = StartupNotificationManager.addProgressBar("Translating legacy mods", total);
            bar.label(label("Preparing"));
        } catch (RuntimeException | LinkageError e) {
            bar = null;
        }
    }

    /** The jar being worked on next. */
    void starting(String fileName) {
        set(label(fileName));
    }

    /** One jar finished (translated or failed). */
    void finished() {
        done++;
        if (bar == null) return;
        try {
            bar.increment();
        } catch (RuntimeException | LinkageError e) {
            bar = null;
        }
    }

    /** A step that is not one jar (scanning all jars before the first translation, building the shared API). */
    void step(String what) {
        set(label(what));
    }

    private String label(String what) {
        return "Translating legacy mods (" + Math.min(done + 1, total) + "/" + total + "): " + what;
    }

    private void set(String text) {
        if (bar == null) return;
        try {
            bar.label(text);
        } catch (RuntimeException | LinkageError e) {
            bar = null;
        }
    }

    @Override
    public void close() {
        if (bar == null) return;
        try {
            bar.complete();
            StartupNotificationManager.popBar(bar);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic
        }
        bar = null;
    }
}
