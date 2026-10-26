package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.DynamicTexture;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyResourcePacks;

/**
 * The resource packs folder and its packs as legacy code lists them: the entries are the modern game's packs from the
 * folder (the active ones in priority order, lowest first), each readable as a legacy pack. A mod that sets the active
 * list itself ({@link #func_148527_a}) sees its list from then on, and the packs of its own entries are served to the game.
 */
public class ResourcePackRepository {
    public static final ResourcePackRepository INSTANCE = new ResourcePackRepository();

    private List repositoryEntriesAll = new ArrayList();
    private List repositoryEntries = new ArrayList();
    /** True once a mod set the active entries. */
    private boolean mcc$entriesSet;
    /** Entries of modern packs by pack id, so a pack keeps its entry. */
    private final Map<String, Entry> mcc$byId = new java.util.concurrent.ConcurrentHashMap<>();

    public File getDirResourcepacks() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("resourcepacks").toFile();
    }

    private static net.minecraft.server.packs.repository.PackRepository mcc$modern() {
        return net.minecraft.client.Minecraft.getInstance().getResourcePackRepository();
    }

    /** Packs from the folder (and the server's pack): what 1.7.10 listed; built-in and mod packs are not entries. */
    private static boolean mcc$listed(Pack pack) {
        return pack.getId().startsWith("file/");
    }

    private Entry mcc$entry(Pack pack) {
        return mcc$byId.computeIfAbsent(pack.getId(), id -> new Entry(pack));
    }

    private List mcc$entries(Collection<Pack> packs) {
        List out = new ArrayList();
        for (Pack pack : packs) if (mcc$listed(pack)) out.add(mcc$entry(pack));
        return out;
    }

    /** Rescans the folder. */
    public void updateRepositoryEntriesAll() {
        try {
            mcc$modern().reload();
        } catch (RuntimeException | LinkageError e) {
            // not on a client
        }
    }

    public List getRepositoryEntriesAll() {
        try {
            repositoryEntriesAll = mcc$entries(mcc$modern().getAvailablePacks());
        } catch (RuntimeException | LinkageError e) {
            // not on a client: none
        }
        return new ArrayList(repositoryEntriesAll);
    }

    /** The active packs, lowest priority first. */
    public List getRepositoryEntries() {
        if (!mcc$entriesSet) {
            try {
                repositoryEntries = mcc$entries(mcc$modern().getSelectedPacks());
            } catch (RuntimeException | LinkageError e) {
                // not on a client: none
            }
        }
        return new ArrayList(repositoryEntries);
    }

    /** Sets the active entries (mods put their own packs in); takes effect on the next resource reload. */
    public void func_148527_a(List entries) {
        repositoryEntries = new ArrayList(entries);
        mcc$entriesSet = true;
    }

    /** The packs of active entries a mod added that are not the modern game's own (not a 1.7.10 method). */
    public List<IResourcePack> mcc$modEntryPacks() {
        List<IResourcePack> out = new ArrayList<>();
        if (!mcc$entriesSet) return out;
        for (Object o : repositoryEntries.toArray()) {
            if (o instanceof Entry e && e.mcc$modern == null) {
                IResourcePack pack = e.getResourcePack();
                if (pack != null) out.add(pack);
            }
        }
        return out;
    }

    /** The pack the server sent, or null. */
    public IResourcePack getResourcePackInstance() {
        try {
            for (Pack pack : mcc$modern().getSelectedPacks()) {
                if (pack.getId().startsWith("server/") || pack.getPackSource() == net.minecraft.server.packs.repository.PackSource.SERVER) {
                    return mcc$entry(pack).getResourcePack();
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // not on a client
        }
        return null;
    }

    /** Asks for the server's pack (the modern game downloads server packs itself). */
    public void obtainResourcePack(String url) {
    }

    /** Drops the server's pack (the modern game does it when leaving the server). */
    public void func_148529_f() {
    }

    /** A pack of the folder: its file, the pack read from it and its icon. */
    public class Entry {
        private final File resourcePackFile;
        private IResourcePack reResourcePack;
        private BufferedImage texturePackIcon;
        private ResourceLocation locationTexturePackIcon;
        private final Pack mcc$modern;

        public Entry(File file) {
            this.resourcePackFile = file;
            this.mcc$modern = null;
        }

        private Entry(Pack pack) {
            String id = pack.getId();
            this.resourcePackFile = id.startsWith("file/") ? new File(getDirResourcepacks(), id.substring("file/".length())) : null;
            this.mcc$modern = pack;
        }

        public void updateResourcePack() throws IOException {
            if (mcc$modern != null) {
                reResourcePack = new LegacyResourcePacks.ModernPack(mcc$modern);
            } else {
                reResourcePack = resourcePackFile.isDirectory() ? new FolderResourcePack(resourcePackFile) : new FileResourcePack(resourcePackFile);
            }
            try {
                texturePackIcon = reResourcePack.getPackImage();
            } catch (IOException | RuntimeException e) {
                texturePackIcon = null;
            }
        }

        public void bindTexturePackIcon(TextureManager textures) {
            if (reResourcePack == null) getResourcePack();
            if (locationTexturePackIcon == null) {
                locationTexturePackIcon = texturePackIcon == null ? new ResourceLocation("textures/misc/unknown_pack.png")
                        : textures.getDynamicTextureLocation("texturepackicon", new DynamicTexture(texturePackIcon));
            }
            textures.bindTexture(locationTexturePackIcon);
        }

        public void closeResourcePack() {
            if (reResourcePack instanceof Closeable c) {
                try {
                    c.close();
                } catch (IOException ignored) {
                    // already closed
                }
            }
        }

        public IResourcePack getResourcePack() {
            if (reResourcePack == null) {
                try {
                    updateResourcePack();
                } catch (IOException | RuntimeException e) {
                    return null;
                }
            }
            return reResourcePack;
        }

        public String getResourcePackName() {
            if (mcc$modern != null) {
                String id = mcc$modern.getId();
                return id.startsWith("file/") ? id.substring("file/".length()) : id;
            }
            return resourcePackFile.getName();
        }

        /** The description from the pack's pack.mcmeta. */
        public String getTexturePackDescription() {
            if (mcc$modern != null) return mcc$modern.getDescription().getString();
            if (getResourcePack() instanceof AbstractResourcePack pack) {
                try (InputStream in = pack.getInputStreamByName("pack.mcmeta")) {
                    var root = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    var description = root.getAsJsonObject("pack").get("description");
                    return description.isJsonPrimitive() ? description.getAsString() : net.minecraft.network.chat.Component.Serializer.fromJson(description,
                            net.minecraft.core.RegistryAccess.EMPTY).getString();
                } catch (IOException | RuntimeException e) {
                    // no description
                }
            }
            return "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry e)) return false;
            if (mcc$modern != null || e.mcc$modern != null) return mcc$modern != null && e.mcc$modern != null && mcc$modern.getId().equals(e.mcc$modern.getId());
            return resourcePackFile.equals(e.resourcePackFile);
        }

        @Override
        public int hashCode() {
            return mcc$modern != null ? mcc$modern.getId().hashCode() : resourcePackFile.hashCode();
        }

        @Override
        public String toString() {
            File file = resourcePackFile;
            if (file == null) return getResourcePackName();
            return String.format("%s:%s:%d", file.getName(), file.isDirectory() ? "folder" : "zip", file.lastModified());
        }
    }
}
