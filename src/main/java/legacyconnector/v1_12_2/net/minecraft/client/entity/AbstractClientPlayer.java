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
}
