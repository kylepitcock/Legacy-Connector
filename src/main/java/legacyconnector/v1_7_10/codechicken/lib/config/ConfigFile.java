package legacyconnector.v1_7_10.codechicken.lib.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CodeChickenLib's config file: tags with values (name=value), nested with braces ("name { ... }") or dots, comments with '#'.
 * Loaded when constructed, written by {@link #save()}.
 */
public class ConfigFile extends ConfigTagParent {
    public final File file;

    public ConfigFile(File file) {
        this.file = file;
        load();
    }

    public String getNameQualifier() {
        return "";
    }

    private void load() {
        if (file == null || !file.exists()) return;
        try {
            Deque<ConfigTagParent> stack = new ArrayDeque<>();
            stack.push(this);
            StringBuilder comment = new StringBuilder();
            for (String raw : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) {
                    if (comment.length() > 0) comment.append('\n');
                    comment.append(line.substring(1).trim());
                    continue;
                }
                if (line.equals("}")) {
                    if (stack.size() > 1) stack.pop();
                    continue;
                }
                boolean open = line.endsWith("{");
                if (open) line = line.substring(0, line.length() - 1).trim();
                int eq = line.indexOf('=');
                String name = eq < 0 ? line : line.substring(0, eq).trim();
                ConfigTag tag = stack.peek().getTag(name);
                if (eq >= 0) tag.setValue(line.substring(eq + 1).trim());
                if (comment.length() > 0) {
                    tag.comment = comment.toString();
                    comment.setLength(0);
                }
                if (open) stack.push(tag);
            }
        } catch (IOException ignored) {
            // an unreadable file is the same as an empty one: defaults apply and save() recreates it
        }
    }

    public void save() {
        if (file == null) return;
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            StringBuilder out = new StringBuilder();
            if (comment != null) for (String line : comment.split("\n")) out.append("# ").append(line).append('\n');
            write(out, "");
            Files.writeString(file.toPath(), out.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // read-only config directory: the values stay in memory
        }
    }
}
