package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The door item: places both halves of a door block. */
public class ItemDoor extends Item {
    private final Material doorMaterial;

    public ItemDoor(Material material) {
        this.doorMaterial = material;
        this.maxStackSize = 1;
    }

    public ItemDoor(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.doorMaterial = Material.wood;
    }

    /**
     * As 1.7.10: the lower half gets {@code meta} as given (facing, plus 4 when open), the upper half 8, plus 1 for a hinge on the right:
     * right when a door stands only on the left, or when more full blocks flank the right side than the left (a door against a
     * wall opens away from it). The modern state of both halves is put together by the world when the second half is written.
     */
    public static void placeDoorBlock(World world, int x, int y, int z, int meta, Block door) {
        int dx = 0, dz = 0;
        if (meta == 0) dz = 1;
        if (meta == 1) dx = -1;
        if (meta == 2) dz = -1;
        if (meta == 3) dx = 1;
        int left = (world.getBlock(x - dx, y, z - dz).isNormalCube() ? 1 : 0) + (world.getBlock(x - dx, y + 1, z - dz).isNormalCube() ? 1 : 0);
        int right = (world.getBlock(x + dx, y, z + dz).isNormalCube() ? 1 : 0) + (world.getBlock(x + dx, y + 1, z + dz).isNormalCube() ? 1 : 0);
        boolean doorLeft = world.getBlock(x - dx, y, z - dz) == door || world.getBlock(x - dx, y + 1, z - dz) == door;
        boolean doorRight = world.getBlock(x + dx, y, z + dz) == door || world.getBlock(x + dx, y + 1, z + dz) == door;
        boolean hingeRight = doorLeft && !doorRight || right > left;
        world.setBlock(x, y, z, door, meta, 2);
        world.setBlock(x, y + 1, z, door, 8 | (hingeRight ? 1 : 0), 2);
        world.notifyBlocksOfNeighborChange(x, y, z, door);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, door);
    }
}
