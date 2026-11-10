package legacyconnector.vb1_7_3.net.minecraft.client;

/** The original controller's HUD and reach queries, resolved for the current callback actor. */
public class InteractionManager {
    protected final Minecraft minecraft;

    public InteractionManager(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public boolean canBeRendered() {
        var player = Minecraft.mcc$get$player(minecraft);
        return player == null || !(player.mcc$modern instanceof net.minecraft.world.entity.player.Player nativePlayer)
                || !nativePlayer.isCreative() && !nativePlayer.isSpectator();
    }

    public float getReachDistance() {
        var player = Minecraft.mcc$get$player(minecraft);
        return player != null && player.mcc$modern instanceof net.minecraft.world.entity.player.Player nativePlayer
                ? (float) nativePlayer.blockInteractionRange() : 4.0F;
    }
}
