package legacyconnector.v1_12_2.net.minecraftforge.common.config;

/** Forge's accessor for one {@code @Config} field; only the type adapter is modelled (mods inspect it to learn the property type). */
public interface IFieldWrapper {
    /** The adapter for the field's value type, or null for maps and sub-categories. */
    ITypeAdapter getTypeAdapter();
}
