package legacyconnector.v1_6_4.facade.net.minecraft.block;

/** Facade of the 1.7.10 shim's {@code BlockFire} for 1.4.7 mods (the fire tables were public statics of the fire block). See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class BlockFire {
    private BlockFire() {}

    /** 1.4.7 {@code BlockFire.abilityToCatchFire}: how likely a block (by id) is to burn; the table {@code Block.blockFlammability} is (mods write entries at load time). */
    public static int[] mcc$get$abilityToCatchFire() {
        return Block.mcc$get$blockFlammability();
    }

    /** 1.4.7 {@code BlockFire.chanceToEncourageFire}: how fast fire spreads onto a block (by id); the table {@code Block.blockFireSpreadSpeed} is. */
    public static int[] mcc$get$chanceToEncourageFire() {
        return Block.mcc$get$blockFireSpreadSpeed();
    }

    /** 1.4.7 {@code canNeighborBurn(world, x, y, z)}: whether any of the six blocks around a position can catch fire. */
    public static boolean canNeighborBurn(legacyconnector.v1_7_10.net.minecraft.block.BlockFire self, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y,
            int z) {
        for (var side : legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.VALID_DIRECTIONS) {
            if (self.canCatchFire(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ, side.getOpposite())) return true;
        }
        return false;
    }
}
