package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n;

/** A free-standing element for mods that build their config screen without a Configuration file (Forge 1.7.10). */
public class DummyConfigElement<T> implements IConfigElement<T> {
    protected boolean isProperty = true;
    protected boolean isList = false;
    protected ConfigGuiType type;
    protected String name;
    protected String langKey;
    protected Object value;
    protected Object defaultValue;
    protected Object[] values;
    protected Object[] defaultValues;
    protected String[] validValues;
    protected Pattern validStringPattern;
    protected T minValue;
    protected T maxValue;
    protected boolean requiresWorldRestart = false;
    protected boolean requiresMcRestart = false;
    protected boolean isListFixedLength = false;
    protected int maxListLength = -1;
    protected List<IConfigElement> childElements;
    protected Class<? extends GuiConfigEntries.IConfigEntry> configEntryClass;
    protected Class<? extends GuiEditArrayEntries.IArrayEntry> arrayEntryClass;

    /** A category made of other elements. */
    public static class DummyCategoryElement<T> extends DummyConfigElement<T> {
        public DummyCategoryElement(String name, String langKey, List<IConfigElement> childElements) {
            this(name, langKey, childElements, null);
        }

        public DummyCategoryElement(String name, String langKey, Class<? extends GuiConfigEntries.IConfigEntry> customListEntryClass) {
            this(name, langKey, new ArrayList<>(), customListEntryClass);
        }

        public DummyCategoryElement(String name, String langKey, List<IConfigElement> childElements,
                Class<? extends GuiConfigEntries.IConfigEntry> customListEntryClass) {
            super(name, null, ConfigGuiType.CONFIG_CATEGORY, langKey);
            this.childElements = childElements;
            this.configEntryClass = customListEntryClass;
            this.isProperty = false;
        }
    }

    /** A list of values, edited in its own screen. */
    public static class DummyListElement<T> extends DummyConfigElement<T> {
        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, boolean isListFixedLength, int maxListLength,
                Pattern validStringPattern, T minValue, T maxValue) {
            super(name, null, type, langKey, minValue, maxValue);
            this.defaultValues = defaultValues;
            this.values = defaultValues;
            this.isListFixedLength = isListFixedLength;
            this.maxListLength = maxListLength;
            this.validStringPattern = validStringPattern;
            this.isList = true;
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey) {
            this(name, defaultValues, type, langKey, false, -1, null, null, null);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, boolean isListFixedLength) {
            this(name, defaultValues, type, langKey, isListFixedLength, -1, null, null, null);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, int maxListLength) {
            this(name, defaultValues, type, langKey, false, maxListLength, null, null, null);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, T minValue, T maxValue) {
            this(name, defaultValues, type, langKey, false, -1, null, minValue, maxValue);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, boolean isListFixedLength, T minValue, T maxValue) {
            this(name, defaultValues, type, langKey, isListFixedLength, -1, null, minValue, maxValue);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, int maxListLength, T minValue, T maxValue) {
            this(name, defaultValues, type, langKey, false, maxListLength, null, minValue, maxValue);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, boolean isListFixedLength, int maxListLength,
                T minValue, T maxValue) {
            this(name, defaultValues, type, langKey, isListFixedLength, maxListLength, null, minValue, maxValue);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, Pattern validStringPattern) {
            this(name, defaultValues, type, langKey, false, -1, validStringPattern, null, null);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, boolean isListFixedLength, Pattern validStringPattern) {
            this(name, defaultValues, type, langKey, isListFixedLength, -1, validStringPattern, null, null);
        }

        public DummyListElement(String name, T[] defaultValues, ConfigGuiType type, String langKey, int maxListLength, Pattern validStringPattern) {
            this(name, defaultValues, type, langKey, false, maxListLength, validStringPattern, null, null);
        }

        public DummyListElement setCustomEditListEntryClass(Class<? extends GuiEditArrayEntries.IArrayEntry> clazz) {
            this.arrayEntryClass = clazz;
            return this;
        }

        @Override
        public Object getDefault() {
            return Arrays.toString(this.defaultValues);
        }
    }

    @SuppressWarnings("unchecked")
    public DummyConfigElement(String name, T defaultValue, ConfigGuiType type, String langKey, String[] validValues, Pattern validStringPattern,
            T minValue, T maxValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.type = type;
        this.langKey = langKey;
        this.validValues = validValues;
        this.validStringPattern = validStringPattern;
        if (minValue == null) {
            if (type == ConfigGuiType.INTEGER) this.minValue = (T) (Integer) Integer.MIN_VALUE;
            else if (type == ConfigGuiType.DOUBLE) this.minValue = (T) (Double) (-Double.MAX_VALUE);
        } else {
            this.minValue = minValue;
        }
        if (maxValue == null) {
            if (type == ConfigGuiType.INTEGER) this.maxValue = (T) (Integer) Integer.MAX_VALUE;
            else if (type == ConfigGuiType.DOUBLE) this.maxValue = (T) (Double) Double.MAX_VALUE;
        } else {
            this.maxValue = maxValue;
        }
    }

    public DummyConfigElement(String name, T defaultValue, ConfigGuiType type, String langKey, Pattern validStringPattern) {
        this(name, defaultValue, type, langKey, null, validStringPattern, null, null);
    }

    public DummyConfigElement(String name, T defaultValue, ConfigGuiType type, String langKey, String[] validValues) {
        this(name, defaultValue, type, langKey, validValues, null, null, null);
    }

    public DummyConfigElement(String name, T defaultValue, ConfigGuiType type, String langKey) {
        this(name, defaultValue, type, langKey, null, null, null, null);
    }

    public DummyConfigElement(String name, T defaultValue, ConfigGuiType type, String langKey, T minValue, T maxValue) {
        this(name, defaultValue, type, langKey, null, null, minValue, maxValue);
    }

    public DummyConfigElement setCustomListEntryClass(Class<? extends GuiConfigEntries.IConfigEntry> clazz) {
        this.configEntryClass = clazz;
        return this;
    }

    public IConfigElement setConfigEntryClass(Class<? extends GuiConfigEntries.IConfigEntry> clazz) {
        this.configEntryClass = clazz;
        return this;
    }

    public IConfigElement setArrayEntryClass(Class<? extends GuiEditArrayEntries.IArrayEntry> clazz) {
        this.arrayEntryClass = clazz;
        return this;
    }

    @Override public boolean isProperty() { return isProperty; }
    @Override public Class<? extends GuiConfigEntries.IConfigEntry> getConfigEntryClass() { return configEntryClass; }
    @Override public Class<? extends GuiEditArrayEntries.IArrayEntry> getArrayEntryClass() { return arrayEntryClass; }
    @Override public String getName() { return name; }
    @Override public String getQualifiedName() { return name; }
    @Override public String getLanguageKey() { return langKey; }
    @Override public String getComment() { return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(langKey + ".tooltip"); }
    @Override public List<IConfigElement> getChildElements() { return childElements; }
    @Override public ConfigGuiType getType() { return type; }
    @Override public boolean isList() { return isList; }
    @Override public boolean isListLengthFixed() { return isListFixedLength; }
    @Override public int getMaxListLength() { return maxListLength; }

    @Override
    public boolean isDefault() {
        if (!isProperty) return true;
        if (isList) return Arrays.deepEquals(values, defaultValues);
        return value != null ? value.equals(defaultValue) : defaultValue == null;
    }

    @Override public Object getDefault() { return defaultValue; }
    @Override public Object[] getDefaults() { return defaultValues; }

    @Override
    public void setToDefault() {
        if (isList) this.values = Arrays.copyOf(defaultValues, defaultValues.length);
        else this.value = defaultValue;
    }

    @Override public boolean requiresWorldRestart() { return requiresWorldRestart; }

    public DummyConfigElement setRequiresWorldRestart(boolean requiresWorldRestart) {
        this.requiresWorldRestart = requiresWorldRestart;
        return this;
    }

    @Override public boolean showInGui() { return true; }
    @Override public boolean requiresMcRestart() { return requiresMcRestart; }

    /** Forge returns the element as an IConfigElement (mods link to that descriptor). */
    public IConfigElement setRequiresMcRestart(boolean requiresMcRestart) {
        this.requiresMcRestart = this.requiresWorldRestart = requiresMcRestart;
        return this;
    }

    @Override public String[] getValidValues() { return validValues; }
    @Override public Pattern getValidationPattern() { return validStringPattern; }
    @Override public Object get() { return value; }
    @Override public Object[] getList() { return values; }
    @Override public void set(T value) { this.value = value; }
    @Override public void set(T[] aVal) { this.values = aVal; }
    @Override public T getMinValue() { return minValue; }
    @Override public T getMaxValue() { return maxValue; }
}
