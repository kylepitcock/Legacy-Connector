package org.kylepitcock.mccompat.v1_7_10.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureMap;
import mccompat.v1_7_10.net.minecraft.client.resources.IResource;
import mccompat.v1_7_10.net.minecraft.client.resources.IResourceManager;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import net.neoforged.fml.ModList;
import org.kylepitcock.mccompat.core.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sprites that legacy mods build themselves (a {@code TextureAtlasSprite} with a custom loader registered through
 * {@code TextureMap.setTextureEntry}, as Railcraft's unstitched sprite sheets): when the resource pack is generated
 * their loader runs against the mods' own resources and the resulting pixels are written as ordinary textures that the
 * generated models reference. An animated result becomes a vertical strip with an animation description.
 */
public final class LegacySprites {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Sprites");

    private LegacySprites() {}

    /** Mods' resources by legacy resource location (the generated pack is not readable while it is being built). */
    private static boolean reportedMissing;

    public static final class ModResources implements IResourceManager {
        @Override
        public Set getResourceDomains() {
            return Set.of();
        }

        @Override
        public IResource getResource(ResourceLocation location) throws IOException {
            String ns = Ids.namespace(location.getResourceDomain());
            String path = Ids.normalizePath(location.getResourcePath());
            for (var file : ModList.get().getModFiles()) {
                Path p = file.getFile().findResource("assets", ns, path);
                if (p != null && Files.isRegularFile(p)) {
                    byte[] bytes = Files.readAllBytes(p);
                    String name = file.getFile().getFileName();
                    return new IResource() {
                        @Override
                        public InputStream getInputStream() {
                            return new java.io.ByteArrayInputStream(bytes);
                        }

                        @Override
                        public boolean hasMetadata() {
                            return false;
                        }

                        @Override
                        public String getResourcePackName() {
                            return name;
                        }
                    };
                }
            }
            byte[] fallback = modernResource(ns, path);
            if (fallback != null) {
                byte[] bytes = fallback;
                return new IResource() {
                    @Override
                    public InputStream getInputStream() {
                        return new java.io.ByteArrayInputStream(bytes);
                    }

                    @Override
                    public boolean hasMetadata() {
                        return false;
                    }

                    @Override
                    public String getResourcePackName() {
                        return "minecraft";
                    }
                };
            }
            if (!reportedMissing) {
                reportedMissing = true;
                LOG.warn("Legacy resource {}:{} (assets/{}/{}) not found in {} mod files", location.getResourceDomain(),
                        location.getResourcePath(), ns, path, ModList.get().getModFiles().size());
            }
            throw new java.io.FileNotFoundException(location.getResourceDomain() + ":" + location.getResourcePath());
        }

        /** Assets of the running game (vanilla textures and the like); 1.7.10's glyph widths, which 1.21 no longer ships. */
        private static byte[] modernResource(String ns, String path) {
            try {
                var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ns, path);
                var manager = net.minecraft.client.Minecraft.getInstance().getResourceManager();
                var found = manager.getResource(id);
                if (found.isPresent()) {
                    try (InputStream in = found.get().open()) {
                        return in.readAllBytes();
                    }
                }
            } catch (Exception | LinkageError e) {
                // not available this early, or not a valid location
            }
            if (ns.equals("minecraft") && path.equals("font/glyph_sizes.bin")) {
                byte[] sizes = new byte[65536];
                java.util.Arrays.fill(sizes, (byte) 0x07);
                return sizes;
            }
            return null;
        }

        @Override
        public List getAllResources(ResourceLocation location) throws IOException {
            return List.of(getResource(location));
        }
    }

    public static void bake(TextureMap map, boolean block, Map<String, Supplier<byte[]>> out) {
        ModResources resources = new ModResources();
        String folder = block ? "blocks" : "items";
        int[] built = {0, 0};
        for (var entry : map.mcc$sprites().entrySet()) {
            String name = entry.getKey();
            TextureAtlasSprite sprite = entry.getValue();
            try {
                int colon = name.indexOf(':');
                String domain = colon < 0 ? "minecraft" : name.substring(0, colon);
                String path = colon < 0 ? name : name.substring(colon + 1);
                // Forge hands custom loaders the plain name; they build the texture path themselves.
                ResourceLocation location = new ResourceLocation(domain, path);
                if (!sprite.hasCustomLoader(resources, location)) continue;
                built[1]++;
                // Forge: load() returning false means "loaded, stitch it"; true means the loader skipped the sprite.
                if (sprite.load(resources, location)) continue;
                if (sprite.getFrameCount() == 0) {
                    LOG.warn("Custom loader of sprite {} produced no frames", name);
                    continue;
                }
                int[][] first = sprite.getFrameTextureData(0);
                int w = Math.max(1, sprite.getIconWidth0()), h = Math.max(1, sprite.getIconHeight0());
                int frames = sprite.getFrameCount();
                BufferedImage image = new BufferedImage(w, h * frames, BufferedImage.TYPE_INT_ARGB);
                for (int f = 0; f < frames; f++) {
                    int[] pixels = sprite.getFrameTextureData(f)[0];
                    if (pixels.length >= w * h) image.setRGB(0, f * h, w, h, pixels, 0, w);
                }
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(image, "png", png);
                byte[] bytes = png.toByteArray();
                String id = sprite.getIconName(); // modern id "ns:blocks/name"
                int idColon = id.indexOf(':');
                String file = "assets/" + id.substring(0, idColon) + "/textures/" + id.substring(idColon + 1) + ".png";
                built[0]++;
                out.putIfAbsent(file, () -> bytes);
                if (frames > 1) {
                    byte[] meta = "{\"animation\":{\"frametime\":2}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    out.putIfAbsent(file + ".mcmeta", () -> meta);
                }
            } catch (IOException | RuntimeException | LinkageError e) {
                LOG.warn("Could not build sprite {} ({}): {}", name, folder, e.toString());
            }
        }
        if (built[1] > 0) LOG.info("Built {} of {} custom-loaded {} sprites", built[0], built[1], folder);
    }
}
