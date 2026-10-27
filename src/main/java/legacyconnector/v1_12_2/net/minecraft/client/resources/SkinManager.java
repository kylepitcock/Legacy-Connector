package legacyconnector.v1_12_2.net.minecraft.client.resources;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Player skins, capes and elytras ({@code Minecraft.getSkinManager()}). A profile texture is downloaded once into the game's skin cache and
 * registered with the game's texture manager under {@code skins/<sha1 of hash>} (where the game keeps its own copy), so legacy renderers
 * bind the same texture the game draws.
 */
public class SkinManager {
    private static SkinManager instance;

    public SkinManager(legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager textureManager, java.io.File skinCacheDirectory,
            com.mojang.authlib.minecraft.MinecraftSessionService sessionService) {
    }

    public static synchronized SkinManager mcc$instance() {
        if (instance == null) instance = new SkinManager(null, null, null);
        return instance;
    }

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    public ResourceLocation loadSkin(MinecraftProfileTexture texture, MinecraftProfileTexture.Type type) {
        return loadSkin(texture, type, null);
    }

    /** Registers the texture (downloading it if needed); the callback hears when the image is there. */
    public ResourceLocation loadSkin(MinecraftProfileTexture texture, MinecraftProfileTexture.Type type, SkinAvailableCallback callback) {
        String sha = com.google.common.hash.Hashing.sha1().hashUnencodedChars(texture.getHash()).toString();
        String folder = switch (type) {
            case SKIN -> "skins";
            case CAPE -> "capes";
            case ELYTRA -> "elytra";
        };
        var modern = net.minecraft.resources.ResourceLocation.withDefaultNamespace(folder + "/" + sha);
        ResourceLocation legacy = new ResourceLocation(modern.getNamespace(), modern.getPath());
        Runnable register = () -> {
            var manager = mc().getTextureManager();
            if (manager.getTexture(modern, null) != null) {
                if (callback != null) callback.skinAvailable(type, legacy, texture);
                return;
            }
            java.nio.file.Path root = mc().gameDirectory.toPath().resolve("assets").resolve("skins");
            java.io.File file = root.resolve(sha.length() > 2 ? sha.substring(0, 2) : "xx").resolve(sha).toFile();
            manager.register(modern, new net.minecraft.client.renderer.texture.HttpTexture(file, texture.getUrl(),
                    net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture(), type == MinecraftProfileTexture.Type.SKIN, () -> {
                        if (callback != null) callback.skinAvailable(type, legacy, texture);
                    }));
        };
        if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) register.run();
        else mc().execute(register);
        return legacy;
    }

    /** Looks up the profile's textures in the background (asking the session server when the profile carries none) and loads them. */
    public void loadProfileTextures(GameProfile profile, SkinAvailableCallback callback, boolean requireSecure) {
        if (profile == null) return;
        net.minecraft.Util.backgroundExecutor().execute(() -> {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = textures(profile, requireSecure, true);
            if (textures.isEmpty()) return;
            mc().execute(() -> textures.forEach((type, texture) -> loadSkin(texture, type, callback)));
        });
    }

    /** The textures the profile carries (no network access), by kind. */
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadSkinFromCache(GameProfile profile) {
        return textures(profile, false, false);
    }

    private static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures(GameProfile profile, boolean requireSecure, boolean fetch) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> out = new HashMap<>();
        if (profile == null) return out;
        try {
            var service = mc().getMinecraftSessionService();
            MinecraftProfileTextures textures = service.getTextures(profile);
            if ((textures == null || textures.skin() == null && textures.cape() == null) && fetch && profile.getId() != null) {
                var result = service.fetchProfile(profile.getId(), requireSecure);
                if (result != null) textures = service.getTextures(result.profile());
            }
            if (textures != null) {
                if (textures.skin() != null) out.put(MinecraftProfileTexture.Type.SKIN, textures.skin());
                if (textures.cape() != null) out.put(MinecraftProfileTexture.Type.CAPE, textures.cape());
                if (textures.elytra() != null) out.put(MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
            }
        } catch (RuntimeException e) {
            // insecure or unavailable textures: none
        }
        return out;
    }

    /** Told when a profile texture finished loading. */
    public interface SkinAvailableCallback {
        void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation location, MinecraftProfileTexture texture);
    }
}
