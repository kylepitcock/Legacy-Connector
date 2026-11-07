package legacyconnector.v1_6_4.facade.net.minecraft.world;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.block.Block;

/**
 * Facade of the 1.7.10 shim's {@code World} for 1.6.4 mods: the methods that took block ids. A block id of 0 (or an unused one)
 * means air. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class World {
    private World() {}

    private static Block blockOf(int id) {
        Block b = id <= 0 ? null : Ids.block(id);
        return b != null ? b : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
    }

    public static boolean setBlock(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id, int meta, int flags) {
        return self.setBlock(x, y, z, blockOf(id), meta, flags);
    }

    public static boolean setBlock(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        return self.setBlock(x, y, z, blockOf(id), 0, 3);
    }

    public static void notifyBlocksOfNeighborChange(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.notifyBlocksOfNeighborChange(x, y, z, blockOf(id));
    }

    public static void notifyBlockOfNeighborChange(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.notifyBlockOfNeighborChange(x, y, z, blockOf(id));
    }

    public static void notifyBlockChange(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.notifyBlockChange(x, y, z, blockOf(id));
    }

    /** Forge's {@code isBlockSolidOnSide} (1.7.10: isSideSolid). */
    public static boolean isBlockSolidOnSide(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection side) {
        return self.isSideSolid(x, y, z, side);
    }

    /** 1.6.4 {@code destroyBlock(x, y, z, drop)} (1.7.10: func_147480_a / breakBlock). */
    public static boolean destroyBlock(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, boolean drop) {
        return self.breakBlock(x, y, z, drop);
    }

    /** 1.6.4 {@code addBlockEvent(x, y, z, blockId, eventId, param)}. */
    public static void addBlockEvent(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id, int event, int param) {
        self.addBlockEvent(x, y, z, blockOf(id), event, param);
    }

    /** 1.6.4 {@code canPlaceEntityOnSide(blockId, x, y, z, skipCollision, side, entity, stack)}. */
    public static boolean canPlaceEntityOnSide(legacyconnector.v1_7_10.net.minecraft.world.World self, int id, int x, int y, int z, boolean skipCollision, int side,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        return self.canPlaceEntityOnSide(blockOf(id), x, y, z, skipCollision, side, entity, stack);
    }

    /** 1.6.4 {@code updateAllLightTypes(x, y, z)} (returned nothing). */
    public static void updateAllLightTypes(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z) {
        self.updateAllLightTypes(x, y, z);
    }

    /** 1.6.4 {@code updateLightByType(type, x, y, z)} (returned nothing). */
    public static void updateLightByType(legacyconnector.v1_7_10.net.minecraft.world.World self, legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock type, int x, int y, int z) {
        self.updateLightByType(type, x, y, z);
    }

    /** 1.6.4 {@code func_96440_m(x, y, z, blockId)}: comparators next to the block re-read it. */
    public static void func_96440_m(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.func_147453_f(x, y, z, blockOf(id));
    }

    /** 1.6.4 {@code doesBlockHaveSolidTopSurface(x, y, z)} (1.7.10: static, with the world). */
    public static boolean doesBlockHaveSolidTopSurface(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z) {
        return legacyconnector.v1_7_10.net.minecraft.world.World.doesBlockHaveSolidTopSurface(self, x, y, z);
    }

    /** 1.6.4 {@code canSnowAt(x, y, z)}: with the light check, as that version did. */
    public static boolean canSnowAt(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z) {
        return self.canSnowAt(x, y, z, true);
    }

    /** 1.6.4 {@code blockHasTileEntity(x, y, z)}. */
    public static boolean blockHasTileEntity(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z) {
        Block b = self.getBlock(x, y, z);
        return b != null && b.hasTileEntity(self.getBlockMetadata(x, y, z));
    }

    /** 1.6.4 {@code getFirstUncoveredBlock(x, z)}: the id of the topmost block with open air above it, searching up from sea level. */
    public static int getFirstUncoveredBlock(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int z) {
        int y = 63;
        while (!self.isAirBlock(x, y + 1, z) && y < 255) y++;
        return Ids.blockId(self.getBlock(x, y, z));
    }

    /** 1.6.4 {@code toggleRain()}: ends the current weather phase. */
    public static void toggleRain(legacyconnector.v1_7_10.net.minecraft.world.World self) {
        legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo info = self.getWorldInfo();
        info.setRaining(!info.isRaining());
    }

    /** 1.6.4 {@code getWorldLogAgent()}: the logger of the world (the shim has log4j only). */
    public static legacyconnector.v1_6_4.net.minecraft.logging.ILogAgent getWorldLogAgent(legacyconnector.v1_7_10.net.minecraft.world.World self) {
        return legacyconnector.v1_6_4.net.minecraft.logging.LogAgent.world();
    }

    public static void scheduleBlockUpdate(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id, int delay) {
        self.scheduleBlockUpdate(x, y, z, blockOf(id), delay);
    }
}
