package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;

/**
 * ShockAhPI's player extension, which SAPI attached to every player: a mod portal block's collision sets {@code portal} (the
 * {@code DimensionBase} number the portal leads to, Twilight Forest's) and calls {@code setInPortal}. {@code portal} is the player's own
 * portal target ({@link ClientPlayerEntity#portal}), so the layer's portal timer takes the player there.
 */
public class PlayerBaseSAPI extends PlayerBase {
    public PlayerBaseSAPI(ClientPlayerEntity player) { super(player); }

    public static int mcc$get$portal(PlayerBaseSAPI base) { return base.player.portal; }

    public static void mcc$set$portal(PlayerBaseSAPI base, int dimension) { base.player.portal = dimension; }
}
