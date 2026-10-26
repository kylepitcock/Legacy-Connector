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
            return new net.minecraft.client.player.RemotePlayer(level, profile);
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
