package legacyconnector.v1_12_2.net.minecraftforge.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Property {
    public enum Type {
        STRING, INTEGER, BOOLEAN, DOUBLE, COLOR, MOD_ID;

        private static final Type[] VALUES = {STRING, INTEGER, BOOLEAN, DOUBLE, COLOR, MOD_ID};

        public static Type tryParse(char id) {
            for (Type t : VALUES) if (t.getID() == id) return t;
            return STRING;
        }

        public char getID() {
            return name().charAt(0);
        }
    }

    private String name;
    private String value;
    private String defaultValue;
    public String comment;
    private String[] values;
    private String[] defaultValues;
    private String[] validValues;
    private String langKey;
    private String minValue;
    private String maxValue;
    private boolean requiresWorldRestart;
    private boolean showInGui = true;
    private boolean requiresMcRestart;
    private final boolean isList;
    private boolean isListLengthFixed;
    private int maxListLength = -1;
    private final Type type;
    private boolean changed;
    private boolean wasRead;

    public Property(String name, String value, Type type) {
        this(name, value, type, false, new String[0], name);
    }

    public Property(String name, String value, Type type, boolean read) {
        this(name, value, type, read, new String[0], name);
    }

    public Property(String name, String value, Type type, String[] validValues) {
        this(name, value, type, false, validValues, name);
    }

    public Property(String name, String value, Type type, String langKey) {
        this(name, value, type, false, new String[0], langKey);
    }

    Property(String name, String value, Type type, boolean read, String[] validValues, String langKey) {
        setName(name);
        this.value = value;
        this.values = new String[0];
        this.type = type;
        this.wasRead = read;
        this.isList = false;
        this.defaultValue = value;
        this.defaultValues = new String[0];
        this.validValues = validValues;
        this.langKey = langKey;
        this.minValue = String.valueOf(Integer.MIN_VALUE);
        this.maxValue = String.valueOf(Integer.MAX_VALUE);
        this.comment = "";
    }

    public Property(String name, String[] values, Type type) {
        this(name, values, type, false);
    }

    public Property(String name, String[] values, Type type, boolean read) {
        this(name, values, type, read, new String[0], name);
    }

    Property(String name, String[] values, Type type, boolean read, String[] validValues, String langKey) {
        setName(name);
        this.type = type;
        this.value = "";
        this.values = Arrays.copyOf(values, values.length);
        this.wasRead = read;
        this.isList = true;
        this.defaultValue = "";
        this.defaultValues = Arrays.copyOf(values, values.length);
        this.validValues = validValues;
        this.langKey = langKey;
        this.minValue = String.valueOf(Integer.MIN_VALUE);
        this.maxValue = String.valueOf(Integer.MAX_VALUE);
        this.comment = "";
    }

    public boolean isDefault() {
        return isList ? Arrays.equals(values, defaultValues) : value.equals(defaultValue);
    }

    public Property setToDefault() {
        value = defaultValue;
        values = Arrays.copyOf(defaultValues, defaultValues.length);
        changed = true;
        return this;
    }

    public String getDefault() {
        return defaultValue;
    }

    public String[] getDefaults() {
        return Arrays.copyOf(defaultValues, defaultValues.length);
    }

    public Property setRequiresWorldRestart(boolean v) {
        requiresWorldRestart = v;
        return this;
    }

    public boolean requiresWorldRestart() {
        return requiresWorldRestart;
    }

    public Property setShowInGui(boolean v) {
        showInGui = v;
        return this;
    }

    public boolean showInGui() {
        return showInGui;
    }

    public Property setRequiresMcRestart(boolean v) {
        requiresMcRestart = v;
        return this;
    }

    public boolean requiresMcRestart() {
        return requiresMcRestart;
    }

    public Property setMaxListLength(int max) {
        maxListLength = max;
        return this;
    }

    public int getMaxListLength() {
        return maxListLength;
    }

    public Property setIsListLengthFixed(boolean fixed) {
        isListLengthFixed = fixed;
        return this;
    }

    public boolean isListLengthFixed() {
        return isListLengthFixed;
    }

    public Property setLanguageKey(String key) {
        langKey = key;
        return this;
    }

    public String getLanguageKey() {
        return langKey;
    }

    public Property setDefaultValue(String v) {
        defaultValue = v;
        return this;
    }

    public Property setDefaultValues(String[] v) {
        defaultValue = "";
        defaultValues = Arrays.copyOf(v, v.length);
        return this;
    }

    public Property setDefaultValue(int v) {
        return setDefaultValue(Integer.toString(v));
    }

    public Property setDefaultValue(boolean v) {
        return setDefaultValue(Boolean.toString(v));
    }

    public Property setDefaultValue(double v) {
        return setDefaultValue(Double.toString(v));
    }

    public Property setMinValue(int v) {
        minValue = Integer.toString(v);
        return this;
    }

    public Property setMaxValue(int v) {
        maxValue = Integer.toString(v);
        return this;
    }

    public Property setMinValue(double v) {
        minValue = Double.toString(v);
        return this;
    }

    public Property setMaxValue(double v) {
        maxValue = Double.toString(v);
        return this;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public Property setValidValues(String[] v) {
        validValues = v;
        return this;
    }

    public String[] getValidValues() {
        return validValues;
    }

    public String getString() {
        return value;
    }

    public int getInt() {
        return getInt(parseInt(defaultValue, 0));
    }

    public int getInt(int fallback) {
        return parseInt(value, fallback);
    }

    public boolean isIntValue() {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean getBoolean(boolean fallback) {
        return isBooleanValue() ? Boolean.parseBoolean(value) : fallback;
    }

    public boolean getBoolean() {
        return getBoolean(Boolean.parseBoolean(defaultValue));
    }

    public boolean isBooleanValue() {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    public boolean isDoubleValue() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public double getDouble(double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double getDouble() {
        return getDouble(defaultValue.isEmpty() ? 0 : Double.parseDouble(defaultValue));
    }

    public String[] getStringList() {
        return values;
    }

    public int[] getIntList() {
        List<Integer> out = new ArrayList<>();
        for (String s : values) {
            try {
                out.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean isIntList() {
        for (String s : values) {
            try {
                Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public boolean[] getBooleanList() {
        boolean[] out = new boolean[values.length];
        for (int i = 0; i < values.length; i++) out[i] = Boolean.parseBoolean(values[i].trim());
        return out;
    }

    public boolean isBooleanList() {
        for (String s : values) if (!"true".equalsIgnoreCase(s) && !"false".equalsIgnoreCase(s)) return false;
        return true;
    }

    public double[] getDoubleList() {
        List<Double> out = new ArrayList<>();
        for (String s : values) {
            try {
                out.add(Double.parseDouble(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public boolean isDoubleList() {
        for (String s : values) {
            try {
                Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean wasRead() {
        return wasRead;
    }

    public Type getType() {
        return type;
    }

    public boolean isList() {
        return isList;
    }

    public boolean hasChanged() {
        return changed;
    }

    void resetChangedState() {
        changed = false;
    }

    public void setValue(String v) {
        value = v;
        changed = true;
    }

    public void set(String v) {
        setValue(v);
    }

    public void set(String[] v) {
        values = Arrays.copyOf(v, v.length);
        changed = true;
    }

    public void set(int v) {
        setValue(Integer.toString(v));
    }

    public void set(boolean v) {
        setValue(Boolean.toString(v));
    }

    public void set(double v) {
        setValue(Double.toString(v));
    }

    public void set(int[] v) {
        String[] s = new String[v.length];
        for (int i = 0; i < v.length; i++) s[i] = Integer.toString(v[i]);
        set(s);
    }

    public void set(boolean[] v) {
        String[] s = new String[v.length];
        for (int i = 0; i < v.length; i++) s[i] = Boolean.toString(v[i]);
        set(s);
    }

    public void set(double[] v) {
        String[] s = new String[v.length];
        for (int i = 0; i < v.length; i++) s[i] = Double.toString(v[i]);
        set(s);
    }

    void setValueRaw(String v) {
        value = v;
    }

    void setValuesRaw(String[] v) {
        values = v;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    public String getComment() {
        return comment == null ? "" : comment;
    }

    public Property setComment(String comment) {
        this.comment = comment;
        return this;
    }

    private Object mcc$entryClass;
    private java.util.regex.Pattern mcc$validation;

    public Property setConfigEntryClass(Class<?> clazz) {
        this.mcc$entryClass = clazz;
        return this;
    }

    public Class<?> getConfigEntryClass() {
        return (Class<?>) mcc$entryClass;
    }

    public Property setValidationPattern(java.util.regex.Pattern validationPattern) {
        this.mcc$validation = validationPattern;
        return this;
    }

    public java.util.regex.Pattern getValidationPattern() {
        return mcc$validation;
    }
}
