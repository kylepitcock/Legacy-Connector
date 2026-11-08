package legacyconnector.v1_7_10.net.minecraft.client.entity;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.ImageBufferDownload;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.ThreadDownloadImageData;
import legacyconnector.v1_7_10.net.minecraft.client.resources.SkinManager;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractClientPlayer extends EntityPlayer implements SkinManager.SkinAvailableCallback {
    public static final ResourceLocation locationStevePng = new ResourceLocation("textures/entity/steve.png");
    private ResourceLocation locationSkin;
    private ResourceLocation locationCape;
    /** 1.7.10 asked for the profile's skin and cape when the player was made; here on first use of either. */
    private boolean mcc$skinsRequested;

    protected AbstractClientPlayer(Player modern) {
        super(modern);
    }

    /** Legacy mods created stand-in players (mannequins drawn by tile entity renderers): a modern remote player is the host. */
    public AbstractClientPlayer(legacyconnector.v1_7_10.net.minecraft.world.World world, com.mojang.authlib.GameProfile profile) {
        this(Remote.create(profile));
        // the modern player's view is this object, and 1.7.10's Entity constructor fired EntityConstructing (Fisk's Heroes
        // attaches its player data there and read null for its book's preview player)
        org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.bind(this, mcc$entity);
        mcc$fireConstructing();
    }

    private static final class Remote {
        static Player create(com.mojang.authlib.GameProfile profile) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return menuStandIn(profile);
            return new net.minecraft.client.player.RemotePlayer(level, profile);
        }

        /**
         * No world is loaded (a legacy main menu drawing the player's head and name): a modern player cannot be built without a
         * level, so a hollow remote player that only knows its profile stands in (skin and name lookups work, nothing ticks it).
         */
        private static Player menuStandIn(com.mojang.authlib.GameProfile profile) {
            try {
                var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                var unsafe = (sun.misc.Unsafe) f.get(null);
                var player = (net.minecraft.client.player.RemotePlayer) unsafe.allocateInstance(net.minecraft.client.player.RemotePlayer.class);
                for (Class<?> c = player.getClass(); c != null; c = c.getSuperclass()) {
                    for (var field : c.getDeclaredFields()) {
                        if (field.getType() == com.mojang.authlib.GameProfile.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            unsafe.putObject(player, unsafe.objectFieldOffset(field), profile);
                        }
                    }
                }
                return player;
            } catch (ReflectiveOperationException | RuntimeException e) {
                throw new IllegalStateException("cannot make a menu stand-in player", e);
            }
        }
    }

    private void mcc$requestSkins() {
        if (mcc$skinsRequested) return;
        mcc$skinsRequested = true;
        try {
            var profile = mcc$player().getGameProfile();
            if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                SkinManager.mcc$instance().func_152790_a(profile, this, true);
            }
        } catch (RuntimeException | LinkageError e) {
            // no profile: the default skin
        }
    }

    /** Whether the player's cape has been loaded. */
    public boolean hasCape() {
        mcc$requestSkins();
        return locationCape != null;
    }

    /** Whether the player's own skin has been loaded (else the default skin is used). */
    public boolean hasSkin() {
        mcc$requestSkins();
        return locationSkin != null;
    }

    public ResourceLocation getLocationSkin() {
        mcc$requestSkins();
        return locationSkin == null ? locationStevePng : locationSkin;
    }

    /** 1.6.4: the downloading texture of the player's cape; null while there is none (or it is a plain resource). */
    public ThreadDownloadImageData getTextureCape() {
        ResourceLocation cape = getLocationCape();
        if (cape == null) return null;
        Object texture = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getTextureManager().getTexture(cape);
        return texture instanceof ThreadDownloadImageData data ? data : null;
    }

    public ResourceLocation getLocationCape() {
        mcc$requestSkins();
        return locationCape;
    }

    /**
     * The skin texture of a player by name, registered under {@code location} and downloaded in the background (the default
     * skin until then). 1.7.10 fetched skins.minecraft.net/MinecraftSkins/&lt;name&gt;.png, which is gone: the name is looked
     * up as a profile and its skin downloaded from the texture server.
     */
    public static ThreadDownloadImageData getDownloadImageSkin(ResourceLocation location, String username) {
        var textures = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getTextureManager();
        Object existing = textures.getTexture(location);
        if (existing instanceof ThreadDownloadImageData data) return data;
        ThreadDownloadImageData data = new ByName(legacyconnector.v1_7_10.net.minecraft.util.StringUtils.stripControlCodes(username));
        textures.loadTexture(location, data);
        return data;
    }

    public static ResourceLocation getLocationSkin(String username) {
        return new ResourceLocation("skins/" + legacyconnector.v1_7_10.net.minecraft.util.StringUtils.stripControlCodes(username));
    }

    @Override
    public void onSkinAvailable(MinecraftProfileTexture.Type type, ResourceLocation location) {
        switch (type) {
            case SKIN -> locationSkin = location;
            case CAPE -> locationCape = location;
            default -> {
            }
        }
    }

    /** The direction the player lies in bed, in degrees (0 when not in a bed). */
    public float getBedOrientationInDegrees() {
        var player = mcc$player();
        var dir = player.getBedOrientation();
        if (dir == null || !player.isSleeping()) return 0.0F;
        // 1.7.10: bed direction 0 (south) 90, 1 (west) 0, 2 (north) 270, 3 (east) 180
        return switch (dir) {
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            default -> 0.0F;
        };
    }

    /** A skin downloaded by player name: the name is resolved to a profile, whose skin URL is downloaded and cut to 64x32. */
    private static final class ByName extends ThreadDownloadImageData {
        private final String name;

        ByName(String name) {
            super(null, null, locationStevePng, new ImageBufferDownload());
            this.name = name;
        }

        @Override
        protected void func_152433_a() {
            imageThread = new Thread(this::mcc$fetch, "Texture Downloader #" + threadDownloadCounter.incrementAndGet());
            imageThread.setDaemon(true);
            imageThread.start();
        }

        private void mcc$fetch() {
            try {
                var profile = net.minecraft.world.level.block.entity.SkullBlockEntity.fetchGameProfile(name)
                        .get(30, java.util.concurrent.TimeUnit.SECONDS).orElse(null);
                if (profile == null) return;
                var skin = net.minecraft.client.Minecraft.getInstance().getMinecraftSessionService().getTextures(profile).skin();
                if (skin == null) return;
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) java.net.URI.create(skin.getUrl()).toURL()
                        .openConnection(net.minecraft.client.Minecraft.getInstance().getProxy());
                try {
                    connection.setDoInput(true);
                    connection.connect();
                    if (connection.getResponseCode() / 100 != 2) return;
                    java.awt.image.BufferedImage image;
                    try (java.io.InputStream in = connection.getInputStream()) {
                        image = javax.imageio.ImageIO.read(in);
                    }
                    if (image != null) setBufferedImage(imageBuffer.parseUserSkin(image));
                } finally {
                    connection.disconnect();
                }
            } catch (Exception | LinkageError e) {
                logger.warn("Couldn't download the skin of {}: {}", name, e.toString());
            }
        }
    }
}
