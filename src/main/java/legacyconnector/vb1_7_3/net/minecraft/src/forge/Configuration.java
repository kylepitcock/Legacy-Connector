package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/**
 * Forge 1.0's config file for Beta 1.7.3 mods: three sections ({@code block}, {@code item}, {@code general}) of {@code key=value} lines
 * with {@code #} comments. {@link #getOrCreateBlockIdProperty} gives a new key its default id only when that block slot is free, else
 * the highest free one: Forge's own answer to id clashes between mods, kept as it was.
 */
public class Configuration {
    public static final int GENERAL_PROPERTY = 0;
    public static final int BLOCK_PROPERTY = 1;
    public static final int ITEM_PROPERTY = 2;

    private boolean[] configBlocks;
    File file;
    public TreeMap<String, Property> blockProperties = new TreeMap<>();
    public TreeMap<String, Property> itemProperties = new TreeMap<>();
    public TreeMap<String, Property> generalProperties = new TreeMap<>();

    public Configuration(File file) {
        this.file = file;
    }

    public Property getOrCreateBlockIdProperty(String key, int defaultId) {
        if (configBlocks == null) {
            configBlocks = new boolean[Block.BLOCKS.length];
            for (int i = 0; i < configBlocks.length; i++) configBlocks[i] = Block.BLOCKS[i] != null;
        }
        if (blockProperties.containsKey(key)) {
            Property p = getOrCreateIntProperty(key, BLOCK_PROPERTY, defaultId);
            try {
                configBlocks[Integer.parseInt(p.value.trim())] = true;
            } catch (RuntimeException e) {
                // a broken value: the mod reads it and fails as it did
            }
            return p;
        }
        Property p = new Property();
        p.name = key;
        blockProperties.put(key, p);
        // (ids another mod holds in an @MLProp count as taken: that mod may create its block only later, in ModsLoaded)
        if (defaultId >= 0 && defaultId < configBlocks.length && Block.BLOCKS[defaultId] == null && !configBlocks[defaultId]
                && !org.kylepitcock.legacyconnector.vb1_7_3.ContentB173.reservedByOther(defaultId)) {
            p.value = Integer.toString(defaultId);
            configBlocks[defaultId] = true;
            return p;
        }
        for (int j = configBlocks.length - 1; j >= 0; j--) {
            if (Block.BLOCKS[j] == null && !configBlocks[j] && !org.kylepitcock.legacyconnector.vb1_7_3.ContentB173.reservedByOther(j)) {
                p.value = Integer.toString(j);
                configBlocks[j] = true;
                return p;
            }
        }
        throw new RuntimeException("No more block ids available for " + key);
    }

    public Property getOrCreateIntProperty(String key, int kind, int defaultValue) {
        Property p = getOrCreateProperty(key, kind, Integer.toString(defaultValue));
        try {
            Integer.parseInt(p.value.trim());
        } catch (RuntimeException e) {
            p.value = Integer.toString(defaultValue);
        }
        return p;
    }

    public Property getOrCreateBooleanProperty(String key, int kind, boolean defaultValue) {
        Property p = getOrCreateProperty(key, kind, Boolean.toString(defaultValue));
        String v = p.value == null ? "" : p.value.trim().toLowerCase(java.util.Locale.ROOT);
        if (!v.equals("true") && !v.equals("false")) p.value = Boolean.toString(defaultValue);
        return p;
    }

    public Property getOrCreateProperty(String key, int kind, String defaultValue) {
        Map<String, Property> section = section(kind);
        Property p = section.get(key);
        if (p != null) {
            if (p.value == null) p.value = defaultValue;
            return p;
        }
        if (defaultValue == null) return null;
        p = new Property();
        p.name = key;
        p.value = defaultValue;
        section.put(key, p);
        return p;
    }

    private Map<String, Property> section(int kind) {
        return switch (kind) {
            case BLOCK_PROPERTY -> blockProperties;
            case ITEM_PROPERTY -> itemProperties;
            default -> generalProperties;
        };
    }

    public void load() {
        if (file == null || !file.isFile()) return;
        try (BufferedReader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<String, Property> current = null;
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                if (l.endsWith("{")) {
                    String name = l.substring(0, l.length() - 1).trim();
                    current = name.equals("block") ? blockProperties : name.equals("item") ? itemProperties : generalProperties;
                    continue;
                }
                if (l.equals("}")) {
                    current = null;
                    continue;
                }
                int eq = l.indexOf('=');
                if (eq <= 0) continue;
                Property p = new Property();
                p.name = l.substring(0, eq).trim();
                p.value = l.substring(eq + 1).trim();
                (current != null ? current : generalProperties).put(p.name, p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + file, e);
        }
    }

    public void save() {
        if (file == null) return;
        try {
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null) parent.mkdirs();
            try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                w.write("# Configuration file\n# Generated on " + new java.util.Date() + "\n\n");
                w.write("###########\n# Block configuration\n###########\n\nblock {\n");
                writeProperties(w, blockProperties.values());
                w.write("}\n\n###########\n# Item configuration\n###########\n\nitem {\n");
                writeProperties(w, itemProperties.values());
                w.write("}\n\n###########\n# General configuration\n###########\n\ngeneral {\n");
                writeProperties(w, generalProperties.values());
                w.write("}\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + file, e);
        }
    }

    private void writeProperties(BufferedWriter w, Collection<Property> props) throws IOException {
        for (Property p : props) {
            if (p.comment != null) w.write("   # " + p.comment + "\n");
            w.write("   " + p.name + "=" + p.value + "\n");
        }
    }
}
