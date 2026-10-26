package legacyconnector.v1_7_10.net.minecraft.client.resources;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.IImageBuffer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.ImageBufferDownload;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.ThreadDownloadImageData;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10's player skin loader ({@code Minecraft.getSkinManager()}): a profile texture (skin or cape) is downloaded once into
 * the skin cache and registered as {@code skins/<hash>}, the 1.7.10 way (skins cut to 64x32 for the legacy player model).
 * The textures of a profile come from the modern session service.
 */
public class SkinManager {
    public static final ResourceLocation DEFAULT_SKIN = new ResourceLocation("textures/entity/steve.png");
    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Legacy skin lookup");
        t.setDaemon(true);
        return t;
    });

    private static SkinManager instance;

    private final TextureManager textureManager;
    private final File skinCacheDir;
    private final MinecraftSessionService sessionService;

    public SkinManager(TextureManager textureManager, File skinCacheDir, MinecraftSessionService sessionService) {
        this.textureManager = textureManager;
        this.skinCacheDir = skinCacheDir;
        this.sessionService = sessionService;
    }

    /** The game's skin manager (the client's texture manager and session service, the skin cache under {@code assets/skins}). */
    public static synchronized SkinManager mcc$instance() {
        if (instance == null) {
            MinecraftSessionService service = null;
            try {
                service = net.minecraft.client.Minecraft.getInstance().getMinecraftSessionService();
            } catch (RuntimeException | LinkageError e) {
                // no client yet: profiles without textures
            }
            instance = new SkinManager(TextureManager.INSTANCE,
                    net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("assets").resolve("skins").toFile(), service);
        }
        return instance;
    }

    public ResourceLocation loadSkin(MinecraftProfileTexture texture, MinecraftProfileTexture.Type type) {
        return loadSkin(texture, type, null);
    }

    /**
     * Registers the texture under {@code skins/<hash>} and starts its download; the callback hears once the image is there
     * (right away when it was loaded before).
     */
    public ResourceLocation loadSkin(MinecraftProfileTexture texture, final MinecraftProfileTexture.Type type, final SkinAvailableCallback callback) {
        final ResourceLocation location = new ResourceLocation("skins/" + texture.getHash());
        if (textureManager.getTexture(location) != null) {
            if (callback != null) callback.onSkinAvailable(type, location);
            return location;
        }
        String hash = texture.getHash();
        File dir = new File(skinCacheDir, hash.length() > 2 ? hash.substring(0, 2) : "xx");
        File file = new File(dir, hash);
        final ImageBufferDownload skinFixer = type == MinecraftProfileTexture.Type.SKIN ? new ImageBufferDownload() : null;
        ThreadDownloadImageData data = new ThreadDownloadImageData(file, texture.getUrl(), DEFAULT_SKIN, new IImageBuffer() {
            @Override
            public BufferedImage parseUserSkin(BufferedImage image) {
                return skinFixer != null ? skinFixer.parseUserSkin(image) : image;
            }

            @Override
            public void func_152634_a() {
                if (skinFixer != null) skinFixer.func_152634_a();
                if (callback != null) callback.onSkinAvailable(type, location);
            }
        });
        textureManager.loadTexture(location, data);
        return location;
    }

    /**
     * Looks up the profile's textures in the background (asking the session server for them when the profile carries none)
     * and loads each on the client thread, telling the callback.
     */
    public void func_152790_a(final GameProfile profile, final SkinAvailableCallback callback, final boolean requireSecure) {
        if (profile == null) return;
        THREAD_POOL.submit(() -> {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = mcc$textures(profile, requireSecure, true);
            if (textures.isEmpty()) return;
            Runnable load = () -> {
                if (textures.containsKey(MinecraftProfileTexture.Type.SKIN)) loadSkin(textures.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN, callback);
                if (textures.containsKey(MinecraftProfileTexture.Type.CAPE)) loadSkin(textures.get(MinecraftProfileTexture.Type.CAPE), MinecraftProfileTexture.Type.CAPE, callback);
            };
            try {
                net.minecraft.client.Minecraft.getInstance().execute(load);
            } catch (RuntimeException | LinkageError e) {
                load.run();
            }
        });
    }

    /** The textures the profile carries (no network access), by type. */
    public Map loadSkinFromCache(GameProfile profile) {
        return mcc$textures(profile, false, false);
    }

    private Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> mcc$textures(GameProfile profile, boolean requireSecure, boolean fetch) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> out = new HashMap<>();
        if (profile == null || sessionService == null) return out;
        try {
            MinecraftProfileTextures textures = sessionService.getTextures(profile);
            if ((textures == null || textures.skin() == null && textures.cape() == null) && fetch && profile.getId() != null) {
                var result = sessionService.fetchProfile(profile.getId(), requireSecure);
                if (result != null) textures = sessionService.getTextures(result.profile());
            }
            if (textures != null) {
                if (textures.skin() != null) out.put(MinecraftProfileTexture.Type.SKIN, textures.skin());
                if (textures.cape() != null) out.put(MinecraftProfileTexture.Type.CAPE, textures.cape());
            }
        } catch (RuntimeException e) {
            // insecure or unavailable textures: none
        }
        return out;
    }

    /** Told when a profile texture finished loading. */
    public interface SkinAvailableCallback {
        void onSkinAvailable(MinecraftProfileTexture.Type type, ResourceLocation location);
    }
}
