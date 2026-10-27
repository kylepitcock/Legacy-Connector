package legacyconnector.v1_12_2.net.minecraft.client.resources.data;

/** The "pack" section of pack.mcmeta: the description and the pack format (3 for 1.12). */
public class PackMetadataSection implements IMetadataSection {
    private final legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent packDescription;
    private final int packFormat;

    public PackMetadataSection(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent description, int format) {
        this.packDescription = description;
        this.packFormat = format;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getPackDescription() {
        return packDescription;
    }

    public int getPackFormat() {
        return packFormat;
    }
}
