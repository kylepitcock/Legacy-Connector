package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import legacyconnector.v1_12_2.net.minecraft.client.resources.FileResourcePack;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer;

/**
 * FML's resource pack over a mod jar: the jar's assets, named after the mod, with the mod's logo as the pack image. A jar without a
 * {@code pack.mcmeta} still reads as a pack (FML supplied a default one).
 */
public class FMLFileResourcePack extends FileResourcePack implements FMLContainerHolder {
    private final ModContainer container;

    public FMLFileResourcePack(ModContainer container) {
        super(container.getSource());
        this.container = container;
    }

    @Override
    public String getPackName() {
        return "FMLFileResourcePack:" + container.getName();
    }

    @Override
    public boolean hasResourceName(String name) {
        return super.hasResourceName(name) || "pack.mcmeta".equals(name);
    }

    @Override
    protected InputStream getInputStreamByName(String resourceName) throws IOException {
        try {
            return super.getInputStreamByName(resourceName);
        } catch (IOException ioe) {
            if ("pack.mcmeta".equals(resourceName)) return FMLFolderResourcePack.mcc$defaultMeta(container);
            throw ioe;
        }
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        String logo = container.getMetadata() == null ? null : container.getMetadata().logoFile;
        if (logo != null && !logo.isEmpty()) {
            try (InputStream in = getInputStreamByName(logo.startsWith("/") ? logo.substring(1) : logo)) {
                return javax.imageio.ImageIO.read(in);
            } catch (IOException e) {
                // no logo
            }
        }
        return null;
    }

    @Override
    public ModContainer getFMLContainer() {
        return container;
    }
}
