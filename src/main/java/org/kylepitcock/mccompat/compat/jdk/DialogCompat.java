package org.kylepitcock.mccompat.compat.jdk;

import java.awt.Component;
import javax.swing.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Legacy Swing dialogs (fatal-error popups, update notices) as log messages: the game runs headless. */
public final class DialogCompat {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Dialogs");

    private DialogCompat() {}

    private static String text(Object message) {
        String s = String.valueOf(message);
        if (message instanceof javax.swing.JEditorPane pane) s = pane.getText();
        return s.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private static void log(String title, Object message) {
        LOG.warn("Legacy mod dialog{}: {}", title == null || title.isEmpty() ? "" : " \"" + title + "\"", text(message));
    }

    public static void showMessageDialog(Component parent, Object message) {
        log(null, message);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int type) {
        log(title, message);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int type, Icon icon) {
        log(title, message);
    }

    /** Confirmations answer "yes/OK" (0), as a user clicking through would. */
    public static int showConfirmDialog(Component parent, Object message) {
        log(null, message);
        return 0;
    }

    public static int showConfirmDialog(Component parent, Object message, String title, int optionType) {
        log(title, message);
        return 0;
    }

    public static int showConfirmDialog(Component parent, Object message, String title, int optionType, int messageType) {
        log(title, message);
        return 0;
    }

    public static int showOptionDialog(Component parent, Object message, String title, int optionType, int messageType, Icon icon, Object[] options,
            Object initial) {
        log(title, message);
        return 0;
    }
}
