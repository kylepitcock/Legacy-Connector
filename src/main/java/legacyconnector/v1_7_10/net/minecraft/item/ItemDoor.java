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

    /** Places a door at the position: the lower half carries the facing, the upper half is metadata 8. */
    public static void placeDoorBlock(World world, int x, int y, int z, int facing, Block door) {
        world.setBlock(x, y, z, door, facing & 3, 2);
        world.setBlock(x, y + 1, z, door, 8, 2);
        world.notifyBlocksOfNeighborChange(x, y, z, door);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, door);
    }
}
