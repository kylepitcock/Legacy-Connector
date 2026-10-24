package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import java.util.List;
import java.util.regex.Pattern;

/** A configuration entry or category shown by {@link GuiConfig}. */
public interface IConfigElement<T> {
    boolean isProperty();

    String getName();

    String getQualifiedName();

    String getLanguageKey();

    String getComment();

    List<IConfigElement> getChildElements();

    GuiConfigEntries.ConfigGuiType getType();

    boolean isList();

    boolean isListLengthFixed();

    int getMaxListLength();

    boolean isDefault();

    void setToDefault();

    boolean requiresWorldRestart();

    boolean showInGui();

    boolean requiresMcRestart();

    Object get();

    Object[] getList();

    Object getDefault();

    Object[] getDefaults();

    void set(T value);

    void set(T[] values);

    String[] getValidValues();

    T getMinValue();

    T getMaxValue();

    Pattern getValidationPattern();
}
