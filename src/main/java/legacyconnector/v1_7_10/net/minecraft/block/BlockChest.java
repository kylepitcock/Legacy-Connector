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
