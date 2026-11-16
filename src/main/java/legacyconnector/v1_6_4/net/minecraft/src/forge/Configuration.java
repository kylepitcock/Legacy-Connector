package legacyconnector.v1_6_4.net.minecraft.src.forge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Forge 3.x's configuration file: categories of {@code key=value} lines in {@code category { ... }} blocks, {@code #} comments. A block id the
 * file does not have yet gets its default only when that id is free, else the highest free one (Forge's answer to id clashes between mods).
 */
public class Configuration {
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_BLOCK = "block";
    public static final String CATEGORY_ITEM = "item";

    private boolean[] configBlocks;
    File file;
    public Map<String, Map<String, Property>> categories = new TreeMap<>();
    public TreeMap<String, Property> blockProperties = new TreeMap<>();
    public TreeMap<String, Property> itemProperties = new TreeMap<>();
    public TreeMap<String, Property> generalProperties = new TreeMap<>();

    public Configuration(File file) {
        this.file = file;
        categories.put(CATEGORY_GENERAL, generalProperties);
        categories.put(CATEGORY_BLOCK, blockProperties);
        categories.put(CATEGORY_ITEM, itemProperties);
    }

    public Property getOrCreateBlockIdProperty(String key, int defaultId) {
        if (configBlocks == null) configBlocks = new boolean[256];
        Map<String, Property> properties = categories.get(CATEGORY_BLOCK);
        if (properties.containsKey(key)) {
            Property p = getOrCreateIntProperty(key, CATEGORY_BLOCK, defaultId);
            int id = p.getInt(defaultId);
            if (id >= 0 && id < configBlocks.length) configBlocks[id] = true;
            return p;
        }
        Property p = new Property();
        properties.put(key, p);
        p.name = key;
        legacyconnector.v1_7_10.net.minecraft.block.Block[] blocks = legacyconnector.v1_6_4.net.minecraft.util.Ids.blocksList();
        if (defaultId >= 0 && defaultId < configBlocks.length && blocks[defaultId] == null && !configBlocks[defaultId]) {
            p.value = Integer.toString(defaultId);
            configBlocks[defaultId] = true;
            return p;
        }
        for (int j = configBlocks.length - 1; j >= 0; j--) {
            if (blocks[j] == null && !configBlocks[j]) {
                p.value = Integer.toString(j);
                configBlocks[j] = true;
                return p;
            }
        }
        throw new RuntimeException("No more block ids available for " + key);
    }

    public Property getOrCreateIntProperty(String key, String category, int defaultValue) {
        Property p = getOrCreateProperty(key, category, Integer.toString(defaultValue));
        if (!p.isIntValue()) p.value = Integer.toString(defaultValue);
        return p;
    }

    public Property getOrCreateBooleanProperty(String key, String category, boolean defaultValue) {
        Property p = getOrCreateProperty(key, category, Boolean.toString(defaultValue));
        if (!p.isBooleanValue()) p.value = Boolean.toString(defaultValue);
        return p;
    }

    public Property getOrCreateProperty(String key, String category, String defaultValue) {
        category = category.toLowerCase(java.util.Locale.ROOT);
        Map<String, Property> source = categories.computeIfAbsent(category, c -> new TreeMap<>());
        Property existing = source.get(key);
        if (existing != null) return existing;
        if (defaultValue == null) return null;
        Property p = new Property();
        p.name = key;
        p.value = defaultValue;
        source.put(key, p);
        return p;
    }

    public void load() {
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            if (!file.exists() && !file.createNewFile()) return;
            if (!file.canRead()) return;
            try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.ISO_8859_1)) {
                Map<String, Property> current = null;
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    int nameStart = -1, nameEnd = -1;
                    boolean skip = false;
                    for (int i = 0; i < line.length() && !skip; i++) {
                        char c = line.charAt(i);
                        if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
                            if (nameStart == -1) nameStart = i;
                            nameEnd = i;
                        } else if (Character.isWhitespace(c)) {
                            // ignored
                        } else if (c == '#') {
                            skip = true;
                        } else if (c == '{' && nameStart >= 0) {
                            String scope = line.substring(nameStart, nameEnd + 1);
                            current = categories.computeIfAbsent(scope, s -> new TreeMap<>());
                        } else if (c == '}') {
                            current = null;
                        } else if (c == '=' && nameStart >= 0 && current != null) {
                            Property p = new Property();
                            p.name = line.substring(nameStart, nameEnd + 1);
                            p.value = line.substring(i + 1);
                            current.put(p.name, p);
                            i = line.length();
                        } else {
                            skip = true; // a line the layer does not understand is left alone
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            if (!file.exists() && !file.createNewFile()) return;
            if (!file.canWrite()) return;
            try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.ISO_8859_1)) {
                out.write("# Configuration file\r\n\r\n");
                for (Map.Entry<String, Map<String, Property>> category : categories.entrySet()) {
                    out.write("####################\r\n# " + category.getKey() + " \r\n####################\r\n\r\n");
                    out.write(category.getKey() + " {\r\n");
                    writeProperties(out, category.getValue().values());
                    out.write("}\r\n\r\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeProperties(BufferedWriter out, Collection<Property> props) throws IOException {
        for (Property p : props) {
            if (p.comment != null) out.write("   # " + p.comment + "\r\n");
            out.write("   " + p.name + "=" + p.value + "\r\n");
        }
    }
}
