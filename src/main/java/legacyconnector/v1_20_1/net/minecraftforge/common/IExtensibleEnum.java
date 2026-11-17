package legacyconnector.v1_20_1.net.minecraftforge.common;

/**
 * Forge 1.20.1's marker for enums that Forge's launch-time transformer let other mods add constants to (the enum declares a static {@code create(...)} the
 * transformer fills in). NeoForge 1.21.1 extends enums through a data file instead, so the marker has no effect here: the interface exists so the mod's own enum
 * links, constants a mod adds to its OWN enum in code keep working, constants other mods try to add are not created.
 */
public interface IExtensibleEnum {
}
