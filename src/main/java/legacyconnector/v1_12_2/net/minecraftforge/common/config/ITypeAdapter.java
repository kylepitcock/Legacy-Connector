package legacyconnector.v1_12_2.net.minecraftforge.common.config;

/** Forge's per-type conversion between config properties and field values; mods read the property type (First Aid, by reflection). */
public interface ITypeAdapter {
    Property.Type getType();
}
