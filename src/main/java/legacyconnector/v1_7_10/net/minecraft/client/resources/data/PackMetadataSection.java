package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** The "pack" section of a resource pack's pack.mcmeta: its description and format number. */
public class PackMetadataSection implements IMetadataSection {
    private final IChatComponent packDescription;
    private final int packFormat;

    public PackMetadataSection(IChatComponent description, int format) {
        this.packDescription = description;
        this.packFormat = format;
    }

    public IChatComponent func_152805_a() {
        return packDescription;
    }

    public int getPackFormat() {
        return packFormat;
    }
}
