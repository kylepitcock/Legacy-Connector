package legacyconnector.v1_7_10.cpw.mods.fml.common;

import legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.VersionParser;
import legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.VersionRange;

/** Minecraft itself as an entry of the mod list (mods read the game version from it: BuildCraft, Forestry). */
public class MinecraftDummyContainer extends DummyModContainer {
    private final VersionRange staticRange;

    public MinecraftDummyContainer(String actualMCVersion) {
        super(mcc$metadata(actualMCVersion));
        staticRange = VersionParser.parseRange("[" + actualMCVersion + "]");
        mcc$source = new java.io.File("minecraft.jar");
    }

    private static ModMetadata mcc$metadata(String version) {
        ModMetadata md = new ModMetadata();
        md.modId = "Minecraft";
        md.name = "Minecraft";
        md.version = version;
        return md;
    }

    public VersionRange getStaticVersionRange() {
        return staticRange;
    }
}
