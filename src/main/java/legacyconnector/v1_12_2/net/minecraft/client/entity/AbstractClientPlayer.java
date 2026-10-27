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
}
