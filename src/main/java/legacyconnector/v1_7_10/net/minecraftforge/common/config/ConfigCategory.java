package legacyconnector.v1_7_10.net.minecraftforge.common.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ConfigCategory implements Map<String, Property> {
    private final String name;
    private String comment;
    private String languagekey;
    private final List<ConfigCategory> children = new ArrayList<>();
    private final Map<String, Property> properties = new TreeMap<>();
    public final ConfigCategory parent;
    private boolean changed;
    private boolean requiresWorldRestart;
    private boolean showInGui = true;
    private boolean requiresMcRestart;
    private List<String> propertyOrder;

    public ConfigCategory(String name) {
        this(name, null);
    }

    public ConfigCategory(String name, ConfigCategory parent) {
        this.name = name;
        this.parent = parent;
        if (parent != null) parent.children.add(this);
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return parent == null ? name : parent.getQualifiedName() + Configuration.CATEGORY_SPLITTER + name;
    }

    public ConfigCategory getFirstParent() {
        return parent == null ? this : parent.getFirstParent();
    }

    public boolean isChild() {
        return parent != null;
    }

    public Map<String, Property> getValues() {
        return properties;
    }

    public List<Property> getOrderedValues() {
        List<Property> out = new ArrayList<>();
        if (propertyOrder != null) {
            for (String k : propertyOrder) if (properties.containsKey(k)) out.add(properties.get(k));
        } else {
            out.addAll(properties.values());
        }
        return out;
    }

    public ConfigCategory setPropertyOrder(List<String> order) {
        this.propertyOrder = order;
        return this;
    }

    public List<String> getPropertyOrder() {
        return propertyOrder == null ? new ArrayList<>(properties.keySet()) : propertyOrder;
    }

    public ConfigCategory setLanguageKey(String key) {
        languagekey = key;
        return this;
    }

    public String getLanguagekey() {
        return languagekey == null ? getQualifiedName() : languagekey;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public ConfigCategory setRequiresWorldRestart(boolean v) {
        requiresWorldRestart = v;
        return this;
    }

    public boolean requiresWorldRestart() {
        return requiresWorldRestart;
    }

    public ConfigCategory setShowInGui(boolean v) {
        showInGui = v;
        return this;
    }

    public boolean showInGui() {
        return showInGui;
    }

    public ConfigCategory setRequiresMcRestart(boolean v) {
        requiresMcRestart = v;
        return this;
    }

    public boolean requiresMcRestart() {
        return requiresMcRestart;
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    public Property get(String key) {
        return properties.get(key);
    }

    /** The sub-categories, in creation order (Forge returns an immutable copy). */
    public java.util.Set<ConfigCategory> getChildren() {
        return com.google.common.collect.ImmutableSet.copyOf(children);
    }

    public void removeChild(ConfigCategory child) {
        if (children.remove(child)) changed = true;
    }

    public boolean hasChanged() {
        if (changed) return true;
        for (Property p : properties.values()) if (p.hasChanged()) return true;
        for (ConfigCategory c : children) if (c.hasChanged()) return true;
        return false;
    }

    void resetChangedState() {
        changed = false;
        for (Property p : properties.values()) p.resetChangedState();
        for (ConfigCategory c : children) c.resetChangedState();
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    @Override
    public Property get(Object key) {
        return properties.get(key);
    }

    @Override
    public Property put(String key, Property value) {
        changed = true;
        if (propertyOrder != null && !propertyOrder.contains(key)) propertyOrder.add(key);
        return properties.put(key, value);
    }

    @Override
    public Property remove(Object key) {
        changed = true;
        return properties.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Property> m) {
        changed = true;
        properties.putAll(m);
    }

    @Override
    public void clear() {
        changed = true;
        properties.clear();
    }

    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }

    @Override
    public Collection<Property> values() {
        return properties.values();
    }

    @Override
    public Set<Entry<String, Property>> entrySet() {
        return properties.entrySet();
    }

    /** The entry the config screen shows for this category (null: the default button opening a sub-screen). */
    private Class<? extends legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.IConfigEntry> configEntryClass;

    public ConfigCategory setConfigEntryClass(Class<? extends legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.IConfigEntry> clazz) {
        this.configEntryClass = clazz;
        return this;
    }

    public Class<? extends legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.IConfigEntry> getConfigEntryClass() {
        return configEntryClass;
    }
}
