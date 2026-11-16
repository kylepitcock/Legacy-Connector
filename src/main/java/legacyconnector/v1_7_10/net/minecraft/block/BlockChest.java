package legacyconnector.v1_7_10.net.minecraft.block;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.block.BlockChest}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class BlockChest extends legacyconnector.v1_7_10.net.minecraft.block.BlockContainer {
    public BlockChest(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockChest(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    protected BlockChest(int p0) {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.wood);
    }

    /**
     * {@code func_149954_e} (MCP {@code initMetadata}): sets the orientation metadata (2 north, 3 south, 4 west, 5 east) of a chest from its
     * neighbours: a chest next to another chest of this block turns the way that one faces, otherwise it turns away from an opaque neighbour.
     * Server side only. A vanilla chest's facing is a modern block state set when it is placed, so the vanilla block has nothing to do here.
     */
    public void initMetadata(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        if (world.isRemote || mcc$vanilla != null) return;
        var north = world.getBlock(x, y, z - 1);
        var south = world.getBlock(x, y, z + 1);
        var west = world.getBlock(x - 1, y, z);
        var east = world.getBlock(x + 1, y, z);
        int facing;
        if (north != this && south != this) {
            if (west != this && east != this) {
                facing = 3;
                if (north.isOpaqueCube() && !south.isOpaqueCube()) facing = 3;
                if (south.isOpaqueCube() && !north.isOpaqueCube()) facing = 2;
                if (west.isOpaqueCube() && !east.isOpaqueCube()) facing = 5;
                if (east.isOpaqueCube() && !west.isOpaqueCube()) facing = 4;
            } else {
                int dx = west == this ? -1 : 1;
                var northBeside = world.getBlock(x + dx, y, z - 1);
                var southBeside = world.getBlock(x + dx, y, z + 1);
                facing = 3;
                if (world.getBlockMetadata(x + dx, y, z) == 2) facing = 2;
                if ((north.isOpaqueCube() || northBeside.isOpaqueCube()) && !south.isOpaqueCube() && !southBeside.isOpaqueCube()) facing = 3;
                if ((south.isOpaqueCube() || southBeside.isOpaqueCube()) && !north.isOpaqueCube() && !northBeside.isOpaqueCube()) facing = 2;
            }
        } else {
            int dz = north == this ? -1 : 1;
            var westBeside = world.getBlock(x - 1, y, z + dz);
            var eastBeside = world.getBlock(x + 1, y, z + dz);
            facing = 5;
            if (world.getBlockMetadata(x, y, z + dz) == 4) facing = 4;
            if ((west.isOpaqueCube() || westBeside.isOpaqueCube()) && !east.isOpaqueCube() && !eastBeside.isOpaqueCube()) facing = 5;
            if ((east.isOpaqueCube() || eastBeside.isOpaqueCube()) && !west.isOpaqueCube() && !westBeside.isOpaqueCube()) facing = 4;
        }
        world.setBlockMetadataWithNotify(x, y, z, facing, 3);
    }

    /** Whether a sitting cat (ocelot) lies on top of the chest, which blocks opening it. */
    public static boolean isOcelotBlockingChest(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        for (Object o : world.getEntitiesWithinAABB(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot.class,
                legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y + 1, z, x + 1, y + 2, z + 1))) {
            if (o instanceof legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot cat && cat.isSitting()) return true;
        }
        return false;
    }

    /**
     * {@code func_149951_m}: the inventory a player opening the chest sees: the whole double chest, or null when the chest is blocked
     * (a solid block above it). A vanilla chest answers with the modern chest's combined container.
     */
    public legacyconnector.v1_7_10.net.minecraft.inventory.IInventory getInventory(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        var level = world.mcc$liveLevel();
        if (mcc$vanilla != null && level != null) {
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock chest)) return null;
            var container = net.minecraft.world.level.block.ChestBlock.getContainer(chest, state, level, pos, false);
            return container == null ? null : new org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInventoryTile(container);
        }
        if (!(world.getTileEntity(x, y, z) instanceof legacyconnector.v1_7_10.net.minecraft.inventory.IInventory own)) return null;
        var down = legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.DOWN;
        if (world.isSideSolid(x, y + 1, z, down)) return null;
        int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : sides) {
            if (world.getBlock(x + d[0], y, z + d[1]) == this && world.isSideSolid(x + d[0], y + 1, z + d[1], down)) return null;
        }
        legacyconnector.v1_7_10.net.minecraft.inventory.IInventory inventory = own;
        for (int[] d : sides) {
            if (world.getBlock(x + d[0], y, z + d[1]) != this
                    || !(world.getTileEntity(x + d[0], y, z + d[1]) instanceof legacyconnector.v1_7_10.net.minecraft.inventory.IInventory other)) continue;
            boolean first = d[0] < 0 || d[1] < 0;
            inventory = first ? new legacyconnector.v1_7_10.net.minecraft.inventory.InventoryLargeChest("container.chestDouble", other, inventory)
                    : new legacyconnector.v1_7_10.net.minecraft.inventory.InventoryLargeChest("container.chestDouble", inventory, other);
        }
        return inventory;
    }
}
