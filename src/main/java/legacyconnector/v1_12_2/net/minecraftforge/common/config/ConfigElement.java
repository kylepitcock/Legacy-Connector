package legacyconnector.v1_12_2.net.minecraftforge.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.ConfigGuiType;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiConfigEntries;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiEditArrayEntries;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.IConfigElement;

/**
 * Adapts a {@link ConfigCategory} or a {@link Property} to the elements a config screen lists (Forge 1.7.10): single values are
 * read as the property's string, lists as typed arrays, and written back through the typed setters.
 */
public class ConfigElement<T> implements IConfigElement<T> {
    private Property prop;
    private Property.Type type;
    private boolean isProperty;
    private ConfigCategory ctgy;
    private boolean categoriesFirst = true;

    public ConfigElement(ConfigCategory ctgy) {
        this.ctgy = ctgy;
        this.isProperty = false;
    }

    public ConfigElement(Property prop) {
        this.prop = prop;
        this.type = prop.getType();
        this.isProperty = true;
    }

    public ConfigElement<T> listCategoriesFirst(boolean categoriesFirst) {
        this.categoriesFirst = categoriesFirst;
        return this;
    }

    /**
     * The config screen element of a {@link Config} class (EpicFight reads its custom weapon list through it): the class's category, or for an
     * empty category the file's top-level categories under one element. The class is synced first if the mod has not done it yet.
     */
    public static IConfigElement from(Class<?> configClass) {
        Config annot = configClass.getAnnotation(Config.class);
        if (annot == null) throw new RuntimeException(configClass + " is missing the @Config annotation");
        String name = annot.name().isEmpty() ? annot.modid() : annot.name();
        Configuration config = ConfigManager.getConfiguration(annot.modid(), name);
        if (config == null) {
            ConfigManager.mcc$register(configClass);
            ConfigManager.sync(annot.modid(), annot.type());
            config = ConfigManager.getConfiguration(annot.modid(), name);
        }
        if (config == null) return new legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement<>(name, name, new ArrayList<>());
        if (annot.category().isEmpty()) {
            List<IConfigElement> elements = new ArrayList<>();
            for (String cat : config.getCategoryNames()) {
                ConfigCategory category = config.getCategory(cat);
                if (category.isChild()) continue;
                elements.add(new ConfigElement<>(category));
            }
            return new legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement<>(name, name, elements);
        }
        return new ConfigElement<>(config.getCategory(annot.category()));
    }

    /** The element of a property, typed by its value type (Boolean, Double, Integer, else String: one class after erasure). */
    public static ConfigElement<?> getTypedElement(Property prop) {
        return new ConfigElement<>(prop);
    }

    @Override
    public List<IConfigElement> getChildElements() {
        if (isProperty) return null;
        List<IConfigElement> elements = new ArrayList<>();
        List<IConfigElement> categories = new ArrayList<>();
        for (ConfigCategory child : ctgy.getChildren()) {
            if (child.showInGui()) categories.add(new ConfigElement(child));
        }
        if (categoriesFirst) elements.addAll(categories);
        for (Property p : ctgy.getOrderedValues()) {
            if (p.showInGui()) elements.add(new ConfigElement(p));
        }
        if (!categoriesFirst) elements.addAll(categories);
        return elements;
    }

    public static ConfigGuiType getType(Property prop) {
        return switch (prop.getType()) {
            case BOOLEAN -> ConfigGuiType.BOOLEAN;
            case DOUBLE -> ConfigGuiType.DOUBLE;
            case INTEGER -> ConfigGuiType.INTEGER;
            case COLOR -> ConfigGuiType.COLOR;
            case MOD_ID -> ConfigGuiType.MOD_ID;
            default -> ConfigGuiType.STRING;
        };
    }

    @Override public boolean isProperty() { return isProperty; }

    @Override
    public Class<? extends GuiConfigEntries.IConfigEntry> getConfigEntryClass() {
        Class<?> c = isProperty ? prop.getConfigEntryClass() : ctgy.getConfigEntryClass();
        @SuppressWarnings("unchecked")
        Class<? extends GuiConfigEntries.IConfigEntry> out = (Class<? extends GuiConfigEntries.IConfigEntry>) c;
        return out;
    }

    @Override
    public Class<? extends GuiEditArrayEntries.IArrayEntry> getArrayEntryClass() {
        @SuppressWarnings("unchecked")
        Class<? extends GuiEditArrayEntries.IArrayEntry> out = isProperty ? (Class<? extends GuiEditArrayEntries.IArrayEntry>) prop.getArrayEntryClass() : null;
        return out;
    }

    @Override public String getName() { return isProperty ? prop.getName() : ctgy.getName(); }
    @Override public String getQualifiedName() { return isProperty ? prop.getName() : ctgy.getQualifiedName(); }
    @Override public String getLanguageKey() { return isProperty ? prop.getLanguageKey() : ctgy.getLanguagekey(); }
    @Override public String getComment() { return isProperty ? prop.comment : ctgy.getComment(); }
    @Override public ConfigGuiType getType() { return isProperty ? getType(prop) : ConfigGuiType.CONFIG_CATEGORY; }
    @Override public boolean isList() { return isProperty && prop.isList(); }
    @Override public boolean isListLengthFixed() { return isProperty && prop.isListLengthFixed(); }
    @Override public int getMaxListLength() { return isProperty ? prop.getMaxListLength() : -1; }
    @Override public boolean isDefault() { return !isProperty || prop.isDefault(); }
    @Override public void setToDefault() { if (isProperty) prop.setToDefault(); }
    @Override public boolean requiresWorldRestart() { return isProperty ? prop.requiresWorldRestart() : ctgy.requiresWorldRestart(); }
    @Override public boolean showInGui() { return isProperty ? prop.showInGui() : ctgy.showInGui(); }
    @Override public boolean requiresMcRestart() { return isProperty ? prop.requiresMcRestart() : ctgy.requiresMcRestart(); }
    @Override public String[] getValidValues() { return isProperty ? prop.getValidValues() : null; }
    @Override public Pattern getValidationPattern() { return isProperty ? prop.getValidationPattern() : null; }
    @Override public Object get() { return isProperty ? prop.getString() : null; }

    @Override
    public Object[] getList() {
        if (!isProperty) return null;
        return typed(prop.getStringList());
    }

    @Override public Object getDefault() { return isProperty ? prop.getDefault() : null; }

    @Override
    public Object[] getDefaults() {
        if (!isProperty) return null;
        return typed(prop.getDefaults());
    }

    /** A list's strings as the values the list editor works with (Boolean/Integer/Double for typed lists). */
    private Object[] typed(String[] strings) {
        if (strings == null) return null;
        switch (type) {
            case BOOLEAN -> {
                Boolean[] out = new Boolean[strings.length];
                for (int i = 0; i < strings.length; i++) out[i] = Boolean.valueOf(strings[i].trim());
                return out;
            }
            case INTEGER -> {
                Integer[] out = new Integer[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    try {
                        out[i] = Integer.valueOf(strings[i].trim());
                    } catch (NumberFormatException e) {
                        out[i] = 0;
                    }
                }
                return out;
            }
            case DOUBLE -> {
                Double[] out = new Double[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    try {
                        out[i] = Double.valueOf(strings[i].trim());
                    } catch (NumberFormatException e) {
                        out[i] = 0.0D;
                    }
                }
                return out;
            }
            default -> {
                return strings;
            }
        }
    }

    @Override
    public void set(T value) {
        if (!isProperty || value == null) return;
        String s = value.toString();
        try {
            switch (type) {
                case BOOLEAN -> prop.set(Boolean.parseBoolean(s));
                case INTEGER -> prop.set(Integer.parseInt(s.trim()));
                case DOUBLE -> prop.set(Double.parseDouble(s.trim()));
                default -> prop.set(s);
            }
        } catch (NumberFormatException e) {
            prop.set(s);
        }
    }

    @Override
    public void set(T[] values) {
        if (!isProperty || values == null) return;
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) strings[i] = String.valueOf(values[i]);
        prop.set(strings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getMinValue() {
        return isProperty ? (T) prop.getMinValue() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getMaxValue() {
        return isProperty ? (T) prop.getMaxValue() : null;
    }
}
