package legacyconnector.v1_6_4.facade.net.minecraft.entity.player;

/** Facade of the 1.7.10 shim's {@code EntityPlayer} for 1.4.7 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntityPlayer {
    private EntityPlayer() {}

    /** 1.4.7 {@code getCurrentPlayerStrVsBlock(block, metadata)}: the mining speed of the player's held item against the block, with the metadata. */
    public static float getCurrentPlayerStrVsBlock(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer self,
            legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata) {
        return self.mcc$breakSpeed(block, metadata);
    }

    /** Forge 3.x (1.2.x): {@code player.openGui(mod, id, world, x, y, z)} with the ModLoader mod object; the 1.7.10 shim opens it through the FML GUI registry. */
    public static void openGui(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer self, legacyconnector.v1_6_4.net.minecraft.src.BaseMod mod,
            int id, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        self.openGui(mod, id, world, x, y, z);
    }
}
