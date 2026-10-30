package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/** The mod container of Minecraft itself (id "minecraft", version 1.12.2). */
public class MinecraftDummyContainer extends DummyModContainer {
    public MinecraftDummyContainer(String actualMCVersion) {
        super(mcc$metadata(actualMCVersion));
    }

    private static ModMetadata mcc$metadata(String version) {
        ModMetadata md = new ModMetadata();
        md.modId = "minecraft";
        md.name = "Minecraft";
        md.version = version;
        return md;
    }

    /** Version ranges of Minecraft that mods declared they accept ("[1.12,1.12.2]" ...): this one is 1.12.2. */
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.versioning.VersionRange getStaticVersionRange() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.versioning.VersionRange.createFromVersion("1.12.2");
    }
}
