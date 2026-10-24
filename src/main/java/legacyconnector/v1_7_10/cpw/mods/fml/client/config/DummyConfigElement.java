package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** A free-standing element for mods that build their config screen without a Configuration file. */
public class DummyConfigElement<T> implements IConfigElement<T> {
    protected final String name;
    protected final String langKey;
    protected final boolean listFixed;
    protected final int maxListLength;
    protected final GuiConfigEntries.ConfigGuiType type;
    protected final Object[] defaults;
    protected final String[] validValues;
    protected Object[] values;
    protected final Object min;
    protected final Object max;
    protected final boolean list;

    public DummyConfigElement(String name, Object defaultValue, GuiConfigEntries.ConfigGuiType type, String langKey) {
        this(name, defaultValue, type, langKey, null, null);
    }

    public DummyConfigElement(String name, Object defaultValue, GuiConfigEntries.ConfigGuiType type, String langKey, Object min, Object max) {
        this(name, defaultValue, type, langKey, null, min, max);
    }

    public DummyConfigElement(String name, Object defaultValue, GuiConfigEntries.ConfigGuiType type, String langKey, String[] validValues) {
        this(name, defaultValue, type, langKey, validValues, null, null);
    }

    private DummyConfigElement(String name, Object defaultValue, GuiConfigEntries.ConfigGuiType type, String langKey, String[] validValues, Object min, Object max) {
        this.name = name;
        this.langKey = langKey;
        this.type = type;
        this.defaults = new Object[] {defaultValue};
        this.values = new Object[] {defaultValue};
        this.validValues = validValues == null ? new String[0] : validValues;
        this.min = min;
        this.max = max;
        this.list = false;
        this.listFixed = true;
        this.maxListLength = -1;
    }

    public DummyConfigElement(String name, Object[] defaultValues, GuiConfigEntries.ConfigGuiType type, String langKey, boolean isListFixedLength, int maxListLength) {
        this.name = name;
        this.langKey = langKey;
        this.type = type;
        this.defaults = defaultValues;
        this.values = defaultValues.clone();
        this.validValues = new String[0];
        this.min = null;
        this.max = null;
        this.list = true;
        this.listFixed = isListFixedLength;
        this.maxListLength = maxListLength;
    }

    @Override public boolean isProperty() { return true; }
    @Override public String getName() { return name; }
    @Override public String getQualifiedName() { return name; }
    @Override public String getLanguageKey() { return langKey; }
    @Override public String getComment() { return ""; }
    @Override public List<IConfigElement> getChildElements() { return null; }
    @Override public GuiConfigEntries.ConfigGuiType getType() { return type; }
    @Override public boolean isList() { return list; }
    @Override public boolean isListLengthFixed() { return listFixed; }
    @Override public int getMaxListLength() { return maxListLength; }
    @Override public boolean isDefault() { return java.util.Arrays.equals(values, defaults); }
    @Override public void setToDefault() { values = defaults.clone(); }
    @Override public boolean requiresWorldRestart() { return false; }
    @Override public boolean showInGui() { return true; }
    @Override public boolean requiresMcRestart() { return false; }
    @Override public Object get() { return values.length > 0 ? values[0] : null; }
    @Override public Object[] getList() { return values; }
    @Override public Object getDefault() { return defaults.length > 0 ? defaults[0] : null; }
    @Override public Object[] getDefaults() { return defaults; }
    @Override public void set(T value) { values = new Object[] {value}; }
    @Override public void set(T[] newValues) { values = newValues.clone(); }
    @Override public String[] getValidValues() { return validValues; }
    @Override @SuppressWarnings("unchecked") public T getMinValue() { return (T) min; }
    @Override @SuppressWarnings("unchecked") public T getMaxValue() { return (T) max; }
    @Override public Pattern getValidationPattern() { return null; }

    /** A category made of other elements. */
    public static class DummyCategoryElement extends DummyConfigElement<Object> {
        private final List<IConfigElement> children;

        public DummyCategoryElement(String name, String langKey, List<IConfigElement> children) {
            super(name, (Object) null, GuiConfigEntries.ConfigGuiType.CONFIG_CATEGORY, langKey);
            this.children = children;
        }

        public DummyCategoryElement(String name, String langKey, Class<?> entryClass) {
            this(name, langKey, new ArrayList<>());
        }

        @Override public boolean isProperty() { return false; }
        @Override public List<IConfigElement> getChildElements() { return children; }
    }
}
