package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import java.util.List;
import java.util.regex.Pattern;

/** A configuration entry or category shown by {@link GuiConfig}. */
public interface IConfigElement<T> {
    boolean isProperty();

    /** The entry class the config screen uses for this element (constructed with (GuiConfig, GuiConfigEntries, IConfigElement)); null for the default. */
    Class<? extends GuiConfigEntries.IConfigEntry> getConfigEntryClass();

    /** The entry class of a list's items in the list editor (constructed with (GuiEditArray, GuiEditArrayEntries, IConfigElement, Object)). */
    Class<? extends GuiEditArrayEntries.IArrayEntry> getArrayEntryClass();

    String getName();

    String getQualifiedName();

    String getLanguageKey();

    String getComment();

    List<IConfigElement> getChildElements();

    ConfigGuiType getType();

    boolean isList();

    boolean isListLengthFixed();

    int getMaxListLength();

    boolean isDefault();

    Object getDefault();

    Object[] getDefaults();

    void setToDefault();

    boolean requiresWorldRestart();

    boolean showInGui();

    boolean requiresMcRestart();

    Object get();

    Object[] getList();

    void set(T value);

    void set(T[] values);

    String[] getValidValues();

    T getMinValue();

    T getMaxValue();

    Pattern getValidationPattern();
}
