package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta 1.7.3 {@code BlockChest}: a full wooden cube from terrain.png (top 25, side 26, front 27). Mods extend it for their own chests. */
public class ChestBlock extends BlockWithEntity {
    public ChestBlock(int id) {
        super(id, Material.WOOD);
        this.textureId = 26;
    }

    @Override public int getTexture(int side) { return side == 0 || side == 1 ? 25 : side == 3 ? 27 : 26; }
    /**
     * Beta 1.7.3 kept no facing for a chest: the front (27) is chosen from the four neighbours as placed, facing away from an opaque block
     * (south when nothing decides). Double chests' halves (41/42, 57/58) are not among the bundled sprites, so each half shows its own front.
     */
    @Override public int getTextureId(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z, int side) {
        if (side == 0 || side == 1) return 25;
        int self = view.getBlockId(x, y, z);
        // a double chest decides its front from both halves' outer neighbours, along the axis across the pair
        boolean pairX = view.getBlockId(x - 1, y, z) == self || view.getBlockId(x + 1, y, z) == self;
        boolean pairZ = view.getBlockId(x, y, z - 1) == self || view.getBlockId(x, y, z + 1) == self;
        if (pairX) {
            int ox = view.getBlockId(x - 1, y, z) == self ? x - 1 : x + 1;
            boolean north = opaque(view.getBlockId(x, y, z - 1)) || opaque(view.getBlockId(ox, y, z - 1));
            boolean south = opaque(view.getBlockId(x, y, z + 1)) || opaque(view.getBlockId(ox, y, z + 1));
            int front = south && !north ? 2 : 3;
            return side == front ? 27 : 26;
        }
        if (pairZ) {
            int oz = view.getBlockId(x, y, z - 1) == self ? z - 1 : z + 1;
            boolean west = opaque(view.getBlockId(x - 1, y, z)) || opaque(view.getBlockId(x - 1, y, oz));
            boolean east = opaque(view.getBlockId(x + 1, y, z)) || opaque(view.getBlockId(x + 1, y, oz));
            int front = west && !east ? 5 : 4;
            return side == front ? 27 : 26;
        }
        boolean north = opaque(view.getBlockId(x, y, z - 1)), south = opaque(view.getBlockId(x, y, z + 1));
        boolean west = opaque(view.getBlockId(x - 1, y, z)), east = opaque(view.getBlockId(x + 1, y, z));
        int front = 3;
        if (north && !south) front = 3;
        if (south && !north) front = 2;
        if (west && !east) front = 5;
        if (east && !west) front = 4;
        return side == front ? 27 : 26;
    }
    private static boolean opaque(int id) { return id > 0 && id < BLOCKS_OPAQUE.length && BLOCKS_OPAQUE[id]; }

    @Override public BlockEntity createBlockEntity() { return new ChestBlockEntity(); }

    @Override public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
        if (world.isRemote) return true;
        if (world.getBlockEntity(x, y, z) instanceof Inventory inventory) player.openChestScreen(inventory);
        return true;
    }
}
