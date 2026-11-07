package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.compat.jdk.ResourceCompat;
import org.kylepitcock.legacyconnector.core.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy texture references that name a classpath file rather than an asset: {@code new ResourceLocation("/Reika/RotaryCraft/
 * Textures/GUI/dropgui-nei.png")} (domain {@code minecraft}, path with a leading slash and upper case), as Reika's NEI handlers
 * return from {@code getGuiTexture()}. 1.7.10 accepted any string as a location (its resource manager simply found nothing at
 * {@code assets/minecraft//Reika/...}); the modern game rejects the empty path segment in every resource pack, once per pack and
 * per frame. Such a path is read from the mods' class path instead (the loader keeps files outside {@code assets/} at their
 * original names) and registered once as a dynamic texture under {@code legacyconnector:classpath/<normalized path>}; a file that
 * does not exist gets the missing texture under that name, so the lookup is never repeated.
 */
public final class ClasspathTextures {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Textures");
    private static final Map<String, ResourceLocation> LOCATIONS = new ConcurrentHashMap<>();
    /** Locations loaded (or found missing) already; the value is never retried. */
    private static final Set<ResourceLocation> DONE = ConcurrentHashMap.newKeySet();
    /** Locations waiting for the render thread. */
    private static final Set<ResourceLocation> QUEUED = ConcurrentHashMap.newKeySet();

    private ClasspathTextures() {}

    /** True for a legacy path naming a classpath file (leading slash) rather than an asset of a domain. */
    public static boolean isClasspath(String path) {
        return path != null && path.startsWith("/");
    }

    /** The modern location of a classpath texture path; PNG files are loaded (once, on the render thread). */
    public static ResourceLocation location(String path) {
        ResourceLocation rl = LOCATIONS.computeIfAbsent(path, ClasspathTextures::name);
        if (path.regionMatches(true, path.length() - 4, ".png", 0, 4)) ensure(rl, path);
        return rl;
    }

    private static ResourceLocation name(String path) {
        ArrayDeque<String> parts = new ArrayDeque<>();
        for (String part : Ids.normalizePath(path).split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                parts.pollLast();
                continue;
            }
            parts.addLast(part);
        }
        String joined = String.join("/", parts);
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "classpath/" + (joined.isEmpty() ? "unnamed" : joined));
    }

    private static void ensure(ResourceLocation rl, String path) {
        if (DONE.contains(rl)) return;
        if (!RenderSystem.isOnRenderThread()) {
            if (QUEUED.add(rl)) RenderSystem.recordRenderCall(() -> {
                QUEUED.remove(rl);
                ensure(rl, path);
            });
            return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        var manager = mc.getTextureManager();
        if (manager == null || !DONE.add(rl)) return;
        // a legacy texture object registered under this name (TextureManager#loadTexture) stays
        if (manager.getTexture(rl, null) != null) return;
        NativeImage image = read(path);
        if (image == null) {
            LOG.warn("A legacy texture names the class path file {}, which no mod contains", path);
            manager.register(rl, MissingTextureAtlasSprite.getTexture());
            return;
        }
        manager.register(rl, new DynamicTexture(image));
    }

    /** The class path file {@code path} names (a texture pack's {@code getResourceAsStream}), or null. */
    public static InputStream open(String path) {
        String name = path;
        while (name.startsWith("/")) name = name.substring(1);
        ClassLoader[] loaders = {Thread.currentThread().getContextClassLoader(), ClasspathTextures.class.getClassLoader()};
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            InputStream in = ResourceCompat.getResourceAsStream(loader, name);
            if (in != null) return in;
        }
        return null;
    }

    private static NativeImage read(String path) {
        String name = path;
        while (name.startsWith("/")) name = name.substring(1);
        ClassLoader[] loaders = {Thread.currentThread().getContextClassLoader(), ClasspathTextures.class.getClassLoader()};
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try (InputStream in = ResourceCompat.getResourceAsStream(loader, name)) {
                if (in != null) return NativeImage.read(in);
            } catch (Exception e) {
                LOG.warn("Reading the legacy texture {} failed: {}", path, e.toString());
                return null;
            }
        }
        return null;
    }
}
