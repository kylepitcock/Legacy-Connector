package legacyconnector.v1_12_2.net.minecraftforge.common.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The legacy Forge .cfg format, implemented from scratch with the same syntax so existing config files keep
 * working and remain hand-editable:
 * <pre>
 * category {
 *     # comment
 *     I:someInt=5
 *     S:someList &lt;
 *         a
 *      &gt;
 * }
 * </pre>
 */
public class Configuration {
    public static final String CATEGORY_GENERAL = "general";
    public static final String ALLOWED_CHARS = "._-";
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String CATEGORY_SPLITTER = ".";
    public static final String NEW_LINE = System.lineSeparator();
    public static final String COMMENT_SEPARATOR = "##########################################################################################################";
    private static final Pattern CONFIG_START = Pattern.compile("START: \"([^\\\"]+)\"");
    private static final Pattern CONFIG_END = Pattern.compile("END: \"([^\\\"]+)\"");

    private final Map<String, ConfigCategory> categories = new TreeMap<>();
    private final Map<String, Configuration> children = new TreeMap<>();
    private final File file;
    private boolean caseSensitiveCustomCategories;
    public String defaultEncoding = DEFAULT_ENCODING;
    private String fileName;
    public boolean isChild;
    private boolean changed;
    private String definedConfigVersion;
    private String loadedConfigVersion;

    public Configuration() {
        this.file = null;
    }

    public Configuration(File file) {
        this(file, null);
    }

    public Configuration(File file, String configVersion) {
        this(file, configVersion, false);
    }

    public Configuration(File file, boolean caseSensitiveCustomCategories) {
        this(file, null, caseSensitiveCustomCategories);
    }

    public Configuration(File file, String configVersion, boolean caseSensitiveCustomCategories) {
        this.file = file;
        this.definedConfigVersion = configVersion;
        this.caseSensitiveCustomCategories = caseSensitiveCustomCategories;
        if (file != null) {
            this.fileName = file.getAbsolutePath();
            try {
                load();
            } catch (Throwable e) {
                File backup = new File(file.getAbsolutePath() + "_" + System.currentTimeMillis() + ".errored");
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Config").error("Error loading {}, renaming to {}", file, backup.getName(), e);
                file.renameTo(backup);
                load();
            }
        }
    }

    @Override
    public String toString() {
        return file == null ? "Configuration" : file.getAbsolutePath();
    }

    public String getDefinedConfigVersion() {
        return definedConfigVersion;
    }

    public String getLoadedConfigVersion() {
        return loadedConfigVersion;
    }

    // ------------------------------------------------------------------ typed getters (legacy signatures)

    public Property get(String category, String key, int defaultValue) {
        return get(category, key, defaultValue, null, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Property get(String category, String key, int defaultValue, String comment) {
        return get(category, key, defaultValue, comment, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Property get(String category, String key, int defaultValue, String comment, int min, int max) {
        Property prop = get(category, key, Integer.toString(defaultValue), comment, Property.Type.INTEGER);
        prop.setDefaultValue(Integer.toString(defaultValue));
        prop.setMinValue(min);
        prop.setMaxValue(max);
        if (!prop.isIntValue()) prop.setValue(Integer.toString(defaultValue));
        return prop;
    }

    public Property get(String category, String key, int[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, int[] defaultValues, String comment) {
        return get(category, key, defaultValues, comment, Integer.MIN_VALUE, Integer.MAX_VALUE, false, -1);
    }

    public Property get(String category, String key, int[] defaultValues, String comment, int min, int max) {
        return get(category, key, defaultValues, comment, min, max, false, -1);
    }

    public Property get(String category, String key, int[] defaultValues, String comment, int min, int max, boolean fixed, int maxLength) {
        String[] values = new String[defaultValues.length];
        for (int i = 0; i < defaultValues.length; i++) values[i] = Integer.toString(defaultValues[i]);
        Property prop = get(category, key, values, comment, Property.Type.INTEGER);
        prop.setDefaultValues(values);
        prop.setMinValue(min);
        prop.setMaxValue(max);
        prop.setIsListLengthFixed(fixed);
        prop.setMaxListLength(maxLength);
        if (!prop.isIntList()) prop.set(values);
        return prop;
    }

    public Property get(String category, String key, boolean defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, boolean defaultValue, String comment) {
        Property prop = get(category, key, Boolean.toString(defaultValue), comment, Property.Type.BOOLEAN);
        prop.setDefaultValue(Boolean.toString(defaultValue));
        if (!prop.isBooleanValue()) prop.setValue(Boolean.toString(defaultValue));
        return prop;
    }

    public Property get(String category, String key, boolean[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, boolean[] defaultValues, String comment) {
        String[] values = new String[defaultValues.length];
        for (int i = 0; i < defaultValues.length; i++) values[i] = Boolean.toString(defaultValues[i]);
        Property prop = get(category, key, values, comment, Property.Type.BOOLEAN);
        prop.setDefaultValues(values);
        if (!prop.isBooleanList()) prop.set(values);
        return prop;
    }

    public Property get(String category, String key, double defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, double defaultValue, String comment) {
        return get(category, key, defaultValue, comment, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public Property get(String category, String key, double defaultValue, String comment, double min, double max) {
        Property prop = get(category, key, Double.toString(defaultValue), comment, Property.Type.DOUBLE);
        prop.setDefaultValue(Double.toString(defaultValue));
        prop.setMinValue(min);
        prop.setMaxValue(max);
        if (!prop.isDoubleValue()) prop.setValue(Double.toString(defaultValue));
        return prop;
    }

    public Property get(String category, String key, double[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, double[] defaultValues, String comment) {
        String[] values = new String[defaultValues.length];
        for (int i = 0; i < defaultValues.length; i++) values[i] = Double.toString(defaultValues[i]);
        Property prop = get(category, key, values, comment, Property.Type.DOUBLE);
        prop.setDefaultValues(values);
        if (!prop.isDoubleList()) prop.set(values);
        return prop;
    }

    public Property get(String category, String key, String defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, String defaultValue, String comment) {
        return get(category, key, defaultValue, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, String defaultValue, String comment, String[] validValues) {
        Property prop = get(category, key, defaultValue, comment, Property.Type.STRING);
        prop.setValidValues(validValues);
        return prop;
    }

    public Property get(String category, String key, String[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, String[] defaultValues, String comment) {
        return get(category, key, defaultValues, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, String defaultValue, String comment, Property.Type type) {
        ConfigCategory cat = getCategory(category);
        if (cat.containsKey(key)) {
            Property prop = cat.get(key);
            if (prop.getType() == null) {
                prop = new Property(prop.getName(), prop.getString(), type);
                cat.put(key, prop);
            }
            prop.setDefaultValue(defaultValue);
            prop.comment = comment;
            return prop;
        } else if (defaultValue != null) {
            Property prop = new Property(key, defaultValue, type);
            prop.setValue(defaultValue);
            prop.comment = comment;
            cat.put(key, prop);
            return prop;
        }
        return null;
    }

    public Property get(String category, String key, String defaultValue, String comment, java.util.regex.Pattern validationPattern) {
        Property prop = get(category, key, defaultValue, comment, Property.Type.STRING);
        return prop;
    }

    public Property get(String category, String key, String[] defaultValues, String comment, boolean isListLengthFixed, int maxListLength, java.util.regex.Pattern validationPattern) {
        Property prop = get(category, key, defaultValues, comment, Property.Type.STRING);
        return prop;
    }

    public Property get(String category, String key, String[] defaultValues, String comment, java.util.regex.Pattern validationPattern) {
        return get(category, key, defaultValues, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, String[] defaultValues, String comment, Property.Type type) {
        ConfigCategory cat = getCategory(category);
        if (cat.containsKey(key)) {
            Property prop = cat.get(key);
            if (prop.getType() == null) {
                prop = new Property(prop.getName(), prop.getStringList(), type);
                cat.put(key, prop);
            }
            prop.setDefaultValues(defaultValues);
            prop.comment = comment;
            return prop;
        } else if (defaultValues != null) {
            Property prop = new Property(key, defaultValues, type);
            prop.setDefaultValues(defaultValues);
            prop.comment = comment;
            cat.put(key, prop);
            return prop;
        }
        return null;
    }

    // ------------------------------------------------------------------ convenience getters (Forge 1.12.2)

    public String getString(String name, String category, String defaultValue, String comment) {
        return getString(name, category, defaultValue, comment, name, null);
    }

    public String getString(String name, String category, String defaultValue, String comment, String langKey) {
        return getString(name, category, defaultValue, comment, langKey, null);
    }

    public String getString(String name, String category, String defaultValue, String comment, Pattern pattern) {
        return getString(name, category, defaultValue, comment, name, pattern);
    }

    public String getString(String name, String category, String defaultValue, String comment, String langKey, Pattern pattern) {
        Property prop = get(category, name, defaultValue);
        prop.setLanguageKey(langKey);
        prop.comment = comment + " [default: " + defaultValue + "]";
        return prop.getString();
    }

    public String getString(String name, String category, String defaultValue, String comment, String[] validValues) {
        return getString(name, category, defaultValue, comment, validValues, name);
    }

    /** A string limited to {@code validValues}, shown in the config screen with the matching {@code validValuesDisplay} names. */
    public String getString(String name, String category, String defaultValue, String comment, String[] validValues, String[] validValuesDisplay) {
        Property prop = get(category, name, defaultValue);
        prop.setValidValues(validValues);
        prop.setValidValuesDisplay(validValuesDisplay);
        prop.setLanguageKey(name);
        prop.comment = comment + " [default: " + defaultValue + "]";
        return prop.getString();
    }

    public String getString(String name, String category, String defaultValue, String comment, String[] validValues, String langKey) {
        Property prop = get(category, name, defaultValue);
        prop.setValidValues(validValues);
        prop.setLanguageKey(langKey);
        prop.comment = comment + " [default: " + defaultValue + "]";
        return prop.getString();
    }

    public String[] getStringList(String name, String category, String[] defaultValues, String comment) {
        return getStringList(name, category, defaultValues, comment, null, name);
    }

    public String[] getStringList(String name, String category, String[] defaultValue, String comment, String[] validValues) {
        return getStringList(name, category, defaultValue, comment, validValues, name);
    }

    public String[] getStringList(String name, String category, String[] defaultValue, String comment, String[] validValues, String langKey) {
        Property prop = get(category, name, defaultValue);
        prop.setLanguageKey(langKey);
        prop.setValidValues(validValues);
        prop.comment = comment + " [default: " + Arrays.toString(defaultValue) + "]";
        return prop.getStringList();
    }

    public boolean getBoolean(String name, String category, boolean defaultValue, String comment) {
        return getBoolean(name, category, defaultValue, comment, name);
    }

    public boolean getBoolean(String name, String category, boolean defaultValue, String comment, String langKey) {
        Property prop = get(category, name, defaultValue);
        prop.setLanguageKey(langKey);
        prop.comment = comment + " [default: " + defaultValue + "]";
        return prop.getBoolean(defaultValue);
    }

    public int getInt(String name, String category, int defaultValue, int minValue, int maxValue, String comment) {
        return getInt(name, category, defaultValue, minValue, maxValue, comment, name);
    }

    public int getInt(String name, String category, int defaultValue, int minValue, int maxValue, String comment, String langKey) {
        Property prop = get(category, name, defaultValue);
        prop.setLanguageKey(langKey);
        prop.comment = comment + " [range: " + minValue + " ~ " + maxValue + ", default: " + defaultValue + "]";
        prop.setMinValue(minValue);
        prop.setMaxValue(maxValue);
        int v = prop.getInt(defaultValue);
        return v < minValue ? minValue : Math.min(v, maxValue);
    }

    public float getFloat(String name, String category, float defaultValue, float minValue, float maxValue, String comment) {
        return getFloat(name, category, defaultValue, minValue, maxValue, comment, name);
    }

    public float getFloat(String name, String category, float defaultValue, float minValue, float maxValue, String comment, String langKey) {
        Property prop = get(category, name, Float.toString(defaultValue), name);
        prop.setLanguageKey(langKey);
        prop.comment = comment + " [range: " + minValue + " ~ " + maxValue + ", default: " + defaultValue + "]";
        prop.setMinValue(minValue);
        prop.setMaxValue(maxValue);
        try {
            float v = Float.parseFloat(prop.getString());
            return Math.max(minValue, Math.min(maxValue, v));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ------------------------------------------------------------------ categories

    public boolean hasCategory(String category) {
        return categories.get(normalize(category)) != null;
    }

    public boolean hasKey(String category, String key) {
        ConfigCategory cat = categories.get(normalize(category));
        return cat != null && cat.containsKey(key);
    }

    public boolean renameProperty(String category, String oldName, String newName) {
        if (!hasCategory(category)) return false;
        ConfigCategory cat = getCategory(category);
        if (!cat.containsKey(oldName) || cat.containsKey(newName)) return false;
        Property prop = cat.remove(oldName);
        prop.setName(newName);
        cat.put(newName, prop);
        return true;
    }

    public boolean moveProperty(String oldCategory, String propName, String newCategory) {
        if (!hasCategory(oldCategory)) return false;
        ConfigCategory from = getCategory(oldCategory);
        ConfigCategory to = getCategory(newCategory);
        if (!from.containsKey(propName) || to.containsKey(propName)) return false;
        to.put(propName, from.remove(propName));
        return true;
    }

    public Set<String> getCategoryNames() {
        return new LinkedHashSet<>(categories.keySet());
    }

    public ConfigCategory getCategory(String category) {
        category = normalize(category);
        ConfigCategory ret = categories.get(category);
        if (ret == null) {
            if (category.contains(CATEGORY_SPLITTER)) {
                String[] hierarchy = category.split("\\" + CATEGORY_SPLITTER);
                ConfigCategory parent = categories.get(hierarchy[0]);
                if (parent == null) {
                    parent = new ConfigCategory(hierarchy[0]);
                    categories.put(parent.getQualifiedName(), parent);
                    changed = true;
                }
                for (int i = 1; i < hierarchy.length; i++) {
                    String name = parent.getQualifiedName() + CATEGORY_SPLITTER + hierarchy[i];
                    ConfigCategory child = categories.get(name);
                    if (child == null) {
                        child = new ConfigCategory(hierarchy[i], parent);
                        categories.put(name, child);
                        changed = true;
                    }
                    ret = child;
                    parent = child;
                }
            } else {
                ret = new ConfigCategory(category);
                categories.put(category, ret);
                changed = true;
            }
        }
        return ret;
    }

    public void removeCategory(ConfigCategory category) {
        for (ConfigCategory child : new ArrayList<>(category.getChildren())) removeCategory(child);
        if (categories.containsKey(category.getQualifiedName())) {
            categories.remove(category.getQualifiedName());
            if (category.parent != null) category.parent.removeChild(category);
            changed = true;
        }
    }

    public void addCustomCategoryComment(String category, String comment) {
        setCategoryComment(category, comment);
    }

    public Configuration setCategoryComment(String category, String comment) {
        getCategory(category).setComment(comment);
        return this;
    }

    public Configuration setCategoryLanguageKey(String category, String langKey) {
        getCategory(category).setLanguageKey(langKey);
        return this;
    }

    public Configuration setCategoryRequiresMcRestart(String category, boolean v) {
        getCategory(category).setRequiresMcRestart(v);
        return this;
    }

    public Configuration setCategoryRequiresWorldRestart(String category, boolean v) {
        getCategory(category).setRequiresWorldRestart(v);
        return this;
    }

    public Configuration setCategoryPropertyOrder(String category, List<String> order) {
        getCategory(category).setPropertyOrder(order);
        return this;
    }

    private String normalize(String category) {
        return caseSensitiveCustomCategories ? category : category.toLowerCase(java.util.Locale.ENGLISH);
    }

    public boolean hasChanged() {
        if (changed) return true;
        for (ConfigCategory c : categories.values()) if (c.hasChanged()) return true;
        for (Configuration c : children.values()) if (c.hasChanged()) return true;
        return false;
    }

    private void resetChangedState() {
        changed = false;
        for (ConfigCategory c : categories.values()) c.resetChangedState();
        for (Configuration c : children.values()) c.resetChangedState();
    }

    // ------------------------------------------------------------------ IO

    public void load() {
        if (file == null || isChild) return;
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            if (!file.exists()) {
                if (!file.createNewFile()) return;
            }
            if (!file.canRead()) return;
            categories.clear();
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            parse(lines);
        } catch (IOException e) {
            throw new RuntimeException("Could not read config " + file, e);
        }
        resetChangedState();
    }

    private void parse(List<String> lines) {
        ConfigCategory current = null;
        Property.Type type = null;
        String listName = null;
        List<String> list = null;
        int lineNum = 0;
        for (String raw : lines) {
            lineNum++;
            String line = raw;
            if (line.startsWith("﻿")) line = line.substring(1);
            String trimmed = line.trim();
            if (list != null) {
                if (trimmed.equals(">")) {
                    Property prop = new Property(listName, list.toArray(new String[0]), type, true);
                    current.put(listName, prop);
                    list = null;
                    listName = null;
                } else if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                if (trimmed.startsWith("# configuration version:") || trimmed.startsWith("~CONFIG_VERSION:")) {
                    loadedConfigVersion = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                }
                continue;
            }
            if (trimmed.startsWith("~CONFIG_VERSION:")) {
                loadedConfigVersion = trimmed.substring(16).trim();
                continue;
            }
            if (trimmed.endsWith("{")) {
                String name = unquote(trimmed.substring(0, trimmed.length() - 1).trim());
                String qualified = current == null ? name : current.getQualifiedName() + CATEGORY_SPLITTER + name;
                ConfigCategory cat = categories.get(qualified);
                if (cat == null) {
                    cat = new ConfigCategory(name, current);
                    categories.put(qualified, cat);
                }
                current = cat;
                continue;
            }
            if (trimmed.equals("}")) {
                if (current == null) throw new RuntimeException("Config file corrupt, attempted to close too many categories '" + fileName + ":" + lineNum + "'");
                current = current.parent;
                continue;
            }
            int eq = trimmed.indexOf('=');
            int lt = trimmed.lastIndexOf('<');
            if (current == null) continue;
            if (eq > 0 && (lt < 0 || eq < lt)) {
                String head = trimmed.substring(0, eq);
                String value = trimmed.substring(eq + 1);
                type = typeOf(head);
                String name = unquote(nameOf(head));
                current.put(name, new Property(name, value, type, true));
            } else if (lt > 0 && trimmed.endsWith("<")) {
                String head = trimmed.substring(0, lt).trim();
                type = typeOf(head);
                listName = unquote(nameOf(head));
                list = new ArrayList<>();
            }
        }
        if (list != null) throw new RuntimeException("Config file corrupt, list '" + listName + "' was not closed in " + fileName);
    }

    private static Property.Type typeOf(String head) {
        return head.length() > 2 && head.charAt(1) == ':' ? Property.Type.tryParse(head.charAt(0)) : Property.Type.STRING;
    }

    private static String nameOf(String head) {
        return head.length() > 2 && head.charAt(1) == ':' ? head.substring(2) : head;
    }

    private static String unquote(String s) {
        return s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
    }

    public void save() {
        if (isChild || file == null) return;
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                w.write("# Configuration file" + NEW_LINE + NEW_LINE);
                if (definedConfigVersion != null) w.write("~CONFIG_VERSION: " + definedConfigVersion + NEW_LINE + NEW_LINE);
                for (ConfigCategory cat : categories.values()) {
                    if (!cat.isChild()) {
                        writeCategory(w, cat, 0);
                        w.write(NEW_LINE);
                    }
                }
            }
            resetChangedState();
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Config").error("Error while saving config {}", fileName, e);
        }
    }

    private void writeCategory(BufferedWriter w, ConfigCategory cat, int indent) throws IOException {
        String pad0 = "    ".repeat(indent);
        String pad1 = "    ".repeat(indent + 1);
        if (cat.getComment() != null && !cat.getComment().isEmpty()) {
            w.write(pad0 + COMMENT_SEPARATOR + NEW_LINE);
            w.write(pad0 + "# " + cat.getName() + NEW_LINE);
            w.write(pad0 + "#--------------------------------------------------------------------------------------------------------#" + NEW_LINE);
            for (String l : cat.getComment().split("\r?\n")) w.write(pad0 + "# " + l + NEW_LINE);
            w.write(pad0 + COMMENT_SEPARATOR + NEW_LINE + NEW_LINE);
        }
        w.write(pad0 + quote(cat.getName()) + " {" + NEW_LINE);
        boolean first = true;
        for (Property p : cat.getOrderedValues()) {
            if (!first) w.write(NEW_LINE);
            first = false;
            if (p.comment != null && !p.comment.isEmpty()) {
                for (String l : p.comment.split("\r?\n")) w.write(pad1 + "# " + l + NEW_LINE);
            }
            String head = p.getType().getID() + ":" + quote(p.getName());
            if (p.isList()) {
                w.write(pad1 + head + " <" + NEW_LINE);
                for (String v : p.getStringList()) w.write(pad1 + "    " + v + NEW_LINE);
                w.write(pad1 + " >" + NEW_LINE);
            } else {
                w.write(pad1 + head + "=" + p.getString() + NEW_LINE);
            }
        }
        for (ConfigCategory child : cat.getChildren()) {
            w.write(NEW_LINE);
            writeCategory(w, child, indent + 1);
        }
        w.write(pad0 + "}" + NEW_LINE + NEW_LINE);
    }

    private static String quote(String name) {
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && ALLOWED_CHARS.indexOf(c) == -1) return "\"" + name + "\"";
        }
        return name;
    }

    public File getConfigFile() {
        return file;
    }

    public static void enableGlobalConfig() {
    }
}
