package legacyconnector.v1_7_10.cpw.mods.fml.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.net.minecraft.client.resources.FolderResourcePack;

/** A mod's folder (a development environment) as a pack. */
public class FMLFolderResourcePack extends FolderResourcePack implements FMLContainerHolder {
    private final ModContainer container;

    public FMLFolderResourcePack(ModContainer container) {
        super(container.getSource());
        this.container = container;
    }

    @Override
    public String getPackName() {
        return "FMLFileResourcePack:" + container.getName();
    }

    @Override
    protected boolean hasResourceName(String name) {
        return super.hasResourceName(name) || "pack.mcmeta".equals(name);
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        try {
            return super.getInputStreamByName(name);
        } catch (IOException e) {
            if ("pack.mcmeta".equals(name)) {
                String meta = "{\"pack\":{\"description\":\"dummy FML pack for " + container.getName() + "\",\"pack_format\":1}}";
                return new ByteArrayInputStream(meta.getBytes(StandardCharsets.UTF_8));
            }
            throw e;
        }
    }

    @Override
    public ModContainer getFMLContainer() {
        return container;
    }
}
