package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The pack folder and the active packs as legacy code lists them (BiblioCraft reads the folder); packs themselves are modern ones. */
public class ResourcePackRepository {
    public static final ResourcePackRepository INSTANCE = new ResourcePackRepository();

    public File getDirResourcepacks() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("resourcepacks").toFile();
    }

    /** The active resource packs; legacy code that wants their files has no way to read them here. */
    public List getRepositoryEntries() {
        return new ArrayList();
    }

    public List getRepositoryEntriesAll() {
        return new ArrayList();
    }

    public void updateRepositoryEntriesAll() {
    }
}
