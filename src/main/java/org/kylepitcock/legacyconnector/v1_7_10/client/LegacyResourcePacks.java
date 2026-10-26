package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResource;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourcePack;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSection;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSerializer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

/**
 * Resource packs that legacy mods add to the game at run time: packs put into {@code Minecraft.defaultResourcePacks}
 * (by reflection, then {@code refreshResources}), packs handed to {@code SimpleReloadableResourceManager.reloadResourcePack}
 * or to a domain's {@code FallbackResourceManager}, and repository entries a mod made active. Legacy lookups through the
 * game's resource manager find them first (the newest wins, as in 1.7.10), and {@code LegacyModPacks} serves them to the
 * modern game (textures, sounds). A mod's own jar pack is skipped: its assets are in the game already, translated.
 */
public final class LegacyResourcePacks {
    private LegacyResourcePacks() {}

    /** Packs given to reloadResourcePack or addResourcePack, oldest first. */
    private static final List<IResourcePack> ADDED = new CopyOnWriteArrayList<>();

    public static void add(IResourcePack pack) {
        if (pack != null && !ADDED.contains(pack)) ADDED.add(pack);
    }

    /** Every pack mods added, lowest priority first. */
    public static List<IResourcePack> injected() {
        List<IResourcePack> out = new ArrayList<>();
        try {
            for (Object o : legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().mcc$defaultResourcePacks().toArray()) add(out, o);
            for (Object o : legacyconnector.v1_7_10.net.minecraft.client.resources.ResourcePackRepository.INSTANCE.mcc$modEntryPacks()) add(out, o);
        } catch (RuntimeException | LinkageError e) {
            // not on a client yet
        }
        for (IResourcePack p : ADDED) add(out, p);
        return out;
    }

    private static void add(List<IResourcePack> out, Object o) {
        if (!(o instanceof IResourcePack pack) || o instanceof legacyconnector.v1_7_10.cpw.mods.fml.client.FMLContainerHolder
                || o instanceof ModernPack || out.contains(pack)) return;
        out.add(pack);
    }

    /** The newest added pack that has the resource, as a resource; null when none has it. */
    public static IResource find(ResourceLocation location) {
        List<IResourcePack> packs = injected();
        for (int i = packs.size() - 1; i >= 0; i--) {
            IResourcePack pack = packs.get(i);
            try {
                if (pack.resourceExists(location)) return resource(pack, location);
            } catch (IOException | RuntimeException | LinkageError e) {
                // a broken pack serves nothing
            }
        }
        return null;
    }

    /** The domains of all added packs. */
    public static Set<String> domains() {
        Set<String> out = new HashSet<>();
        for (IResourcePack pack : injected()) {
            try {
                Set<?> domains = pack.getResourceDomains();
                if (domains != null) for (Object d : domains) if (d instanceof String s) out.add(s);
            } catch (RuntimeException | LinkageError ignored) {
                // a broken pack serves nothing
            }
        }
        return out;
    }

    /** A resource of a pack, read at once (legacy IResource streams do not throw). */
    public static IResource resource(IResourcePack pack, ResourceLocation location) throws IOException {
        byte[] bytes;
        try (InputStream in = pack.getInputStream(location)) {
            if (in == null) throw new FileNotFoundException(location.toString());
            bytes = in.readAllBytes();
        }
        byte[] meta = null;
        ResourceLocation metaLocation = new ResourceLocation(location.getResourceDomain(), location.getResourcePath() + ".mcmeta");
        try {
            if (pack.resourceExists(metaLocation)) {
                try (InputStream in = pack.getInputStream(metaLocation)) {
                    if (in != null) meta = in.readAllBytes();
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // no metadata
        }
        final byte[] metadata = meta;
        String name = pack.getPackName();
        return new IResource() {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public boolean hasMetadata() {
                return metadata != null;
            }

            @Override
            public IMetadataSection getMetadata(String section) {
                return metadata == null ? null : IMetadataSerializer.mcc$parse(metadata, section);
            }

            @Override
            public String getResourcePackName() {
                return name;
            }
        };
    }

    /** A modern pack (a resource pack the player chose, the server's pack) as a legacy pack. */
    public static final class ModernPack implements IResourcePack, Closeable {
        private final Pack pack;
        private PackResources resources;

        public ModernPack(Pack pack) {
            this.pack = pack;
        }

        public Pack mcc$pack() {
            return pack;
        }

        private synchronized PackResources resources() {
            if (resources == null) resources = pack.open();
            return resources;
        }

        private static net.minecraft.resources.ResourceLocation modern(ResourceLocation location) {
            return TextureLocations.modern(location.getResourceDomain(), location.getResourcePath());
        }

        @Override
        public InputStream getInputStream(ResourceLocation location) throws IOException {
            var supplier = resources().getResource(PackType.CLIENT_RESOURCES, modern(location));
            if (supplier == null) throw new FileNotFoundException(location.toString());
            return supplier.get();
        }

        @Override
        public boolean resourceExists(ResourceLocation location) {
            try {
                return resources().getResource(PackType.CLIENT_RESOURCES, modern(location)) != null;
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public Set getResourceDomains() {
            return new HashSet<>(resources().getNamespaces(PackType.CLIENT_RESOURCES));
        }

        @Override
        public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) {
            return null;
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
            String id = pack.getId();
            return id.startsWith("file/") ? id.substring("file/".length()) : id;
        }

        @Override
        public synchronized void close() {
            if (resources != null) {
                resources.close();
                resources = null;
            }
        }
    }
}
