package legacyconnector.v1_12_2.net.minecraft.client.resources;

import com.google.common.util.concurrent.ListenableFuture;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The resource packs the player can choose ({@code Minecraft.getResourcePackRepository()}), a view over the game's pack repository: the
 * entries are the game's packs, the active ones its selected packs, and choosing entries selects them and reloads the resources.
 */
public class ResourcePackRepository {
    public final IResourcePack rprDefaultResourcePack;
    public final MetadataSerializer rprMetadataSerializer;
    private final File dirResourcepacks;

    public ResourcePackRepository(File dirResourcepacks, File dirServerResourcepacks, IResourcePack defaultPack, MetadataSerializer serializer,
            legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings settings) {
        this.dirResourcepacks = dirResourcepacks;
        this.rprDefaultResourcePack = defaultPack;
        this.rprMetadataSerializer = serializer;
    }

    private static net.minecraft.server.packs.repository.PackRepository modern() {
        return net.minecraft.client.Minecraft.getInstance().getResourcePackRepository();
    }

    public File getDirResourcepacks() {
        return dirResourcepacks;
    }

    /** Finds packs added to the folder since the last scan. */
    public void updateRepositoryEntriesAll() {
        modern().reload();
    }

    /** Every pack the player can choose. */
    public List<Entry> getRepositoryEntriesAll() {
        List<Entry> out = new ArrayList<>();
        for (var pack : modern().getAvailablePacks()) out.add(new Entry(pack));
        return out;
    }

    /** The active packs, lowest priority first. */
    public List<Entry> getRepositoryEntries() {
        List<Entry> out = new ArrayList<>();
        for (var pack : modern().getSelectedPacks()) out.add(new Entry(pack));
        return out;
    }

    /** Makes these the active packs (the game's required packs stay) and reloads the resources. */
    public void setRepositories(List<Entry> entries) {
        List<String> ids = new ArrayList<>();
        for (var pack : modern().getSelectedPacks()) if (pack.isRequired()) ids.add(pack.getId());
        for (Entry e : entries) if (!ids.contains(e.pack.getId())) ids.add(e.pack.getId());
        modern().setSelected(ids);
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.options.updateResourcePacks(modern());
    }

    /** The server's resource pack is handled by the game when it asks; mods cannot set one. */
    public IResourcePack getServerResourcePack() {
        return null;
    }

    public ListenableFuture<Object> downloadResourcePack(String url, String hash) {
        return com.google.common.util.concurrent.Futures.immediateFailedFuture(new UnsupportedOperationException("server packs are downloaded by the game"));
    }

    public ListenableFuture<Object> setServerResourcePack(File file) {
        return com.google.common.util.concurrent.Futures.immediateFailedFuture(new UnsupportedOperationException("server packs are applied by the game"));
    }

    /** Drops the server's pack: the game's own pack manager does it when leaving the server. */
    public void clearResourcePack() {
        try {
            net.minecraft.client.Minecraft.getInstance().getDownloadedPackSource().popAll();
        } catch (RuntimeException | LinkageError e) {
            // nothing to drop
        }
    }

    /** One pack the player can choose. */
    public class Entry {
        final net.minecraft.server.packs.repository.Pack pack;
        private ModernPack resources;

        Entry(net.minecraft.server.packs.repository.Pack pack) {
            this.pack = pack;
        }

        /** Opens the pack (read its metadata and icon). */
        public void updateResourcePack() {
            getResourcePack();
        }

        public void bindTexturePackIcon(legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager textureManager) {
            textureManager.bindTexture(new ResourceLocation("textures/misc/unknown_pack.png"));
        }

        public void closeResourcePack() {
            if (resources != null) {
                resources.close();
                resources = null;
            }
        }

        public synchronized IResourcePack getResourcePack() {
            if (resources == null) resources = new ModernPack(pack);
            return resources;
        }

        public String getResourcePackName() {
            String id = pack.getId();
            return id.startsWith("file/") ? id.substring("file/".length()) : id;
        }

        public String getTexturePackDescription() {
            return org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.legacyText(pack.getDescription());
        }

        /** 1.12's pack format of the pack: 3 when it fits the game, otherwise older or newer. */
        public int getPackFormat() {
            return switch (pack.getCompatibility()) {
                case TOO_OLD -> 2;
                case TOO_NEW -> 4;
                default -> 3;
            };
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Entry e && e.pack.getId().equals(pack.getId());
        }

        @Override
        public int hashCode() {
            return pack.getId().hashCode();
        }

        @Override
        public String toString() {
            return getResourcePackName();
        }
    }

    /** A pack of the game (a resource pack the player chose, the vanilla assets) as a legacy pack. */
    static final class ModernPack implements IResourcePack {
        private final net.minecraft.server.packs.repository.Pack pack;
        private net.minecraft.server.packs.PackResources resources;

        ModernPack(net.minecraft.server.packs.repository.Pack pack) {
            this.pack = pack;
        }

        private synchronized net.minecraft.server.packs.PackResources resources() {
            if (resources == null) resources = pack.open();
            return resources;
        }

        private static net.minecraft.resources.ResourceLocation modern(ResourceLocation location) {
            return org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(location.getNamespace(), location.getPath());
        }

        @Override
        public InputStream getInputStream(ResourceLocation location) throws IOException {
            var supplier = resources().getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, modern(location));
            if (supplier == null) throw new FileNotFoundException(location.toString());
            return supplier.get();
        }

        @Override
        public boolean resourceExists(ResourceLocation location) {
            try {
                return resources().getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, modern(location)) != null;
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public Set<String> getResourceDomains() {
            return new HashSet<>(resources().getNamespaces(net.minecraft.server.packs.PackType.CLIENT_RESOURCES));
        }

        @Override
        public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) throws IOException {
            var root = resources().getRootResource("pack.mcmeta");
            if (root == null) return null;
            try (InputStream in = root.get()) {
                @SuppressWarnings("unchecked")
                T out = (T) MetadataSerializer.mcc$parse(serializer, in.readAllBytes(), section);
                return out;
            }
        }

        @Override
        public BufferedImage getPackImage() throws IOException {
            var icon = resources().getRootResource("pack.png");
            if (icon == null) return null;
            try (InputStream in = icon.get()) {
                return javax.imageio.ImageIO.read(in);
            }
        }

        @Override
        public String getPackName() {
            return pack.getId();
        }

        synchronized void close() {
            if (resources != null) {
                resources.close();
                resources = null;
            }
        }
    }
}
