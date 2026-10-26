package legacyconnector.v1_7_10.net.minecraftforge.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.ConfigGuiType;
import legacyconnector.v1_7_10.cpw.mods.fml.client.config.IConfigElement;

/** Adapts a {@link ConfigCategory} or a {@link Property} to the elements a config screen lists. */
public class ConfigElement<T> implements IConfigElement<T> {
    private final Property prop;
    private final ConfigCategory category;
    private final boolean isProperty;

    public ConfigElement(ConfigCategory category) {
        this.category = category;
        this.prop = null;
        this.isProperty = false;
    }

    public ConfigElement(Property prop) {
        this.prop = prop;
        this.category = null;
        this.isProperty = true;
    }

    /** The element of a property, typed by its value type (Boolean, Double, Integer, else String: one class after erasure). */
    public static ConfigElement<?> getTypedElement(Property prop) {
        return new ConfigElement<>(prop);
    }

    @Override
    public List<IConfigElement> getChildElements() {
        if (isProperty) return null;
        List<IConfigElement> elements = new ArrayList<>();
        for (ConfigCategory child : category.getChildren()) {
            if (child.showInGui()) elements.add(new ConfigElement(child));
        }
        for (Property p : category.getOrderedValues()) {
            if (p.showInGui()) elements.add(new ConfigElement(p));
        }
        return elements;
    }

    @Override public boolean isProperty() { return isProperty; }
    @Override public String getName() { return isProperty ? prop.getName() : category.getName(); }
    @Override public String getQualifiedName() { return isProperty ? prop.getName() : category.getQualifiedName(); }
    @Override public String getLanguageKey() { return isProperty ? prop.getLanguageKey() : category.getLanguagekey(); }
    @Override public String getComment() { String c = isProperty ? prop.comment : category.getComment(); return c == null ? "" : c; }
    @Override public boolean isList() { return isProperty && prop.isList(); }
    @Override public boolean isListLengthFixed() { return isProperty && prop.isListLengthFixed(); }
    @Override public int getMaxListLength() { return isProperty ? prop.getMaxListLength() : -1; }
    @Override public boolean isDefault() { return !isProperty || prop.isDefault(); }
    @Override public void setToDefault() { if (isProperty) prop.setToDefault(); }
    @Override public boolean requiresWorldRestart() { return isProperty ? prop.requiresWorldRestart() : category.requiresWorldRestart(); }
    @Override public boolean showInGui() { return isProperty ? prop.showInGui() : category.showInGui(); }
    @Override public boolean requiresMcRestart() { return isProperty ? prop.requiresMcRestart() : category.requiresMcRestart(); }
    @Override public String[] getValidValues() { return isProperty ? prop.getValidValues() : new String[0]; }

    @Override
    public ConfigGuiType getType() {
        if (!isProperty) return ConfigGuiType.CONFIG_CATEGORY;
        return switch (prop.getType()) {
            case BOOLEAN -> ConfigGuiType.BOOLEAN;
            case INTEGER -> ConfigGuiType.INTEGER;
            case DOUBLE -> ConfigGuiType.DOUBLE;
            case COLOR -> ConfigGuiType.COLOR;
            case MOD_ID -> ConfigGuiType.MOD_ID;
            default -> ConfigGuiType.STRING;
        };
    }

    @Override
    public Object get() {
        if (!isProperty) return null;
        return switch (prop.getType()) {
            case BOOLEAN -> prop.getBoolean();
            case INTEGER -> prop.getInt();
            case DOUBLE -> prop.getDouble();
            default -> prop.getString();
        };
    }

    @Override
    public Object[] getList() {
        if (!isProperty) return null;
        return prop.getStringList();
    }

    @Override public Object getDefault() { return isProperty ? prop.getDefault() : null; }
    @Override public Object[] getDefaults() { return isProperty ? prop.getDefaults() : null; }

    @Override
    @SuppressWarnings("unchecked")
    public void set(T value) {
        if (!isProperty) return;
        if (value instanceof Boolean b) prop.set(b);
        else if (value instanceof Integer i) prop.set(i);
        else if (value instanceof Number n) prop.set(n.doubleValue());
        else prop.set(String.valueOf(value));
    }

    @Override
    public void set(T[] values) {
        if (!isProperty) return;
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) strings[i] = String.valueOf(values[i]);
        prop.set(strings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getMinValue() {
        if (!isProperty || prop.getMinValue() == null) return null;
        return (T) (prop.getType() == Property.Type.INTEGER ? (Object) Integer.valueOf(prop.getMinValue()) : (Object) Double.valueOf(prop.getMinValue()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getMaxValue() {
        if (!isProperty || prop.getMaxValue() == null) return null;
        return (T) (prop.getType() == Property.Type.INTEGER ? (Object) Integer.valueOf(prop.getMaxValue()) : (Object) Double.valueOf(prop.getMaxValue()));
    }

    @Override public Pattern getValidationPattern() { return null; }
}
