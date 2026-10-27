package legacyconnector.v1_12_2.net.minecraft.client.entity;

/** A player on the client (the local player and the others). */
public abstract class AbstractClientPlayer extends legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer {
    protected AbstractClientPlayer(net.minecraft.world.entity.player.Player modern) {
        super(modern);
    }

    public AbstractClientPlayer(legacyconnector.v1_12_2.net.minecraft.world.World world, com.mojang.authlib.GameProfile profile) {
        super(world, profile);
    }

    @Override
    public boolean isSpectator() {
        return mcc$player().isSpectator();
    }

    @Override
    public boolean isCreative() {
        return mcc$player().isCreative();
    }

    public boolean hasSkin() {
        return true;
    }

    /** "slim" for the thin-armed skin model, "default" otherwise (the key of RenderManager.getSkinMap()). */
    public String getSkinType() {
        return mcc$player() instanceof net.minecraft.client.player.AbstractClientPlayer p && p.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM
                ? "slim" : "default";
    }

    /** The skin texture (the modern skin manager's, a downloaded or default skin). */
    public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getLocationSkin() {
        net.minecraft.resources.ResourceLocation skin = mcc$player() instanceof net.minecraft.client.player.AbstractClientPlayer p
                ? p.getSkin().texture() : net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
        return new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(skin.getNamespace(), skin.getPath());
    }

    // ------------------------------------------------------------------ area R2b: tab list entry, cape, elytra, model parts

    /** The player's tab list entry (null when the server does not list them). */
    protected legacyconnector.v1_12_2.net.minecraft.client.network.NetworkPlayerInfo getPlayerInfo() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(mcc$player().getUUID());
        return legacyconnector.v1_12_2.net.minecraft.client.network.NetworkPlayerInfo.mcc$of(info);
    }

    public boolean hasPlayerInfo() {
        return getPlayerInfo() != null;
    }

    public boolean isPlayerInfoSet() {
        return hasPlayerInfo();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getLocationCape() {
        var info = getPlayerInfo();
        return info == null ? null : info.getLocationCape();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getLocationElytra() {
        var info = getPlayerInfo();
        return info == null ? null : info.getLocationElytra();
    }

    /** Whether the player shows this skin layer. */
    public boolean isWearing(legacyconnector.v1_12_2.net.minecraft.entity.player.EnumPlayerModelParts part) {
        return part != null && mcc$player().isModelPartShown(part.mcc$modern());
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide getPrimaryHand() {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.mcc$of(mcc$player().getMainArm());
    }

    /** The field of view factor (sprinting, flying, bows, speed effects). */
    public float getFovModifier() {
        return mcc$player() instanceof net.minecraft.client.player.AbstractClientPlayer p ? p.getFieldOfViewModifier() : 1.0F;
    }
}
