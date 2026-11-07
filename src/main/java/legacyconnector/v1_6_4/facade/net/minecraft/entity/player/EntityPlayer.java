package legacyconnector.v1_6_4.facade.net.minecraft.entity.player;

/** Facade of the 1.7.10 shim's {@code EntityPlayer} for 1.4.7 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntityPlayer {
    private EntityPlayer() {}

    /** 1.4.7 {@code getCurrentPlayerStrVsBlock(block, metadata)}: the mining speed of the player's held item against the block, with the metadata. */
    public static float getCurrentPlayerStrVsBlock(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer self,
            legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata) {
        return self.mcc$breakSpeed(block, metadata);
    }
}
