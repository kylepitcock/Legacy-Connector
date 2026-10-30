package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import legacyconnector.v1_12_2.net.minecraft.client.resources.FolderResourcePack;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer;

/**
 * FML's resource pack over a mod folder (a mod in development or an unpacked mod): the folder's assets, named after the mod, with the
 * mod's logo as the pack image and a default {@code pack.mcmeta} when it has none.
 */
public class FMLFolderResourcePack extends FolderResourcePack implements FMLContainerHolder {
    private final ModContainer container;

    public FMLFolderResourcePack(ModContainer container) {
        super(container.getSource());
        this.container = container;
    }

    /** The pack.mcmeta FML made up for a mod without one. */
    static InputStream mcc$defaultMeta(ModContainer container) {
        String json = "{\n \"pack\": {\"description\": \"dummy FML pack for " + container.getName().replace("\\", "\\\\").replace("\"", "\\\"")
                + "\",\n \"pack_format\": 3\n}\n}";
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean hasResourceName(String name) {
        return super.hasResourceName(name) || "pack.mcmeta".equals(name);
    }

    @Override
    public String getPackName() {
        return "FMLFileResourcePack:" + container.getName();
    }

    @Override
    protected InputStream getInputStreamByName(String resourceName) throws IOException {
        try {
            return super.getInputStreamByName(resourceName);
        } catch (IOException ioe) {
            if ("pack.mcmeta".equals(resourceName)) return mcc$defaultMeta(container);
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
