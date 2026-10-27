package legacyconnector.v1_12_2.net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.util.EnumMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A player in the tab list: profile, game mode, latency and skin textures, viewed from the game's own entry. The texture map
 * ({@code playerTextures}) follows the downloaded skin, cape and elytra; textures a mod puts in it (custom capes) stay until the player's
 * own texture of that kind changes.
 */
public class NetworkPlayerInfo {
    private static final Map<net.minecraft.client.multiplayer.PlayerInfo, NetworkPlayerInfo> VIEWS = new java.util.WeakHashMap<>();

    private final GameProfile gameProfile;
    private net.minecraft.client.multiplayer.PlayerInfo modern;
    private legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent displayName;
    private final Map<MinecraftProfileTexture.Type, ResourceLocation> textures = new EnumMap<>(MinecraftProfileTexture.Type.class);
    private final Map<MinecraftProfileTexture.Type, ResourceLocation> lastModern = new EnumMap<>(MinecraftProfileTexture.Type.class);
    /** Phantom: the profile textures by kind (redirected to {@link #mcc$get$playerTextures}). */
    @Deprecated
    public Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures;

    public NetworkPlayerInfo(GameProfile profile) {
        this.gameProfile = profile;
    }

    private NetworkPlayerInfo(net.minecraft.client.multiplayer.PlayerInfo modern) {
        this.gameProfile = modern.getProfile();
        this.modern = modern;
    }

    /** The view of the game's entry (one per entry). */
    public static NetworkPlayerInfo mcc$of(net.minecraft.client.multiplayer.PlayerInfo modern) {
        if (modern == null) return null;
        synchronized (VIEWS) {
            return VIEWS.computeIfAbsent(modern, NetworkPlayerInfo::new);
        }
    }

    /** The game's entry: the connection's, or (for an entry a mod made) one of our own that loads the skin. */
    net.minecraft.client.multiplayer.PlayerInfo mcc$modern() {
        if (modern == null) {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            var listed = connection == null || gameProfile.getId() == null ? null : connection.getPlayerInfo(gameProfile.getId());
            modern = listed != null ? listed : new net.minecraft.client.multiplayer.PlayerInfo(gameProfile, false);
        }
        return modern;
    }

    private static ResourceLocation legacy(net.minecraft.resources.ResourceLocation id) {
        return id == null ? null : new ResourceLocation(id.getNamespace(), id.getPath());
    }

    private synchronized void refreshTextures() {
        var skin = mcc$modern().getSkin();
        refresh(MinecraftProfileTexture.Type.SKIN, skin.textureUrl() == null ? null : legacy(skin.texture()));
        refresh(MinecraftProfileTexture.Type.CAPE, legacy(skin.capeTexture()));
        refresh(MinecraftProfileTexture.Type.ELYTRA, legacy(skin.elytraTexture()));
    }

    private void refresh(MinecraftProfileTexture.Type type, ResourceLocation now) {
        ResourceLocation before = lastModern.get(type);
        if (java.util.Objects.equals(before, now)) return;
        if (java.util.Objects.equals(textures.get(type), before)) {
            if (now == null) textures.remove(type);
            else textures.put(type, now);
        }
        if (now == null) lastModern.remove(type);
        else lastModern.put(type, now);
    }

    public static Map<MinecraftProfileTexture.Type, ResourceLocation> mcc$get$playerTextures(NetworkPlayerInfo info) {
        info.refreshTextures();
        return info.textures;
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.GameType getGameType() {
        var mode = mcc$modern().getGameMode();
        return mode == null ? legacyconnector.v1_12_2.net.minecraft.world.GameType.NOT_SET : legacyconnector.v1_12_2.net.minecraft.world.GameType.mcc$of(mode);
    }

    public int getResponseTime() {
        return mcc$modern().getLatency();
    }

    public boolean hasLocationSkin() {
        return getLocationSkin() != null;
    }

    /** "slim" or "default" (the arm width of the skin's model). */
    public String getSkinType() {
        return mcc$modern().getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM ? "slim" : "default";
    }

    /** The player's skin (the default skin for its UUID while it downloads or when there is none). */
    public ResourceLocation getLocationSkin() {
        ResourceLocation skin = mcc$get$playerTextures(this).get(MinecraftProfileTexture.Type.SKIN);
        return skin != null ? skin : legacy(mcc$modern().getSkin().texture());
    }

    public ResourceLocation getLocationCape() {
        return mcc$get$playerTextures(this).get(MinecraftProfileTexture.Type.CAPE);
    }

    public ResourceLocation getLocationElytra() {
        return mcc$get$playerTextures(this).get(MinecraftProfileTexture.Type.ELYTRA);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        if (displayName != null) return displayName;
        var name = mcc$modern().getTabListDisplayName();
        return name == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(
                org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.legacyText(name));
    }

    public void setDisplayName(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent name) {
        this.displayName = name;
        mcc$modern().setTabListDisplayName(name == null ? null : name.mcc$modern());
    }
}
