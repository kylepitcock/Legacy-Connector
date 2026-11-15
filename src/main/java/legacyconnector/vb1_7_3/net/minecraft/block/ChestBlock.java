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
     * Beta 1.7.3's {@code BlockChest.getBlockTexture}: no facing is kept; the front faces away from opaque neighbours (south when nothing
     * decides). A double chest draws one long front and back across its halves (41/42 front, 57/58 back), its ends plain sides.
     */
    @Override public int getTextureId(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z, int side) {
        if (side == 0 || side == 1) return 25;
        int self = view.getBlockId(x, y, z);
        int north = view.getBlockId(x, y, z - 1), south = view.getBlockId(x, y, z + 1);
        int west = view.getBlockId(x - 1, y, z), east = view.getBlockId(x + 1, y, z);
        if (north == self || south == self) {
            if (side == 2 || side == 3) return 26;
            int half = north == self ? -1 : 0;
            int oz = north == self ? z - 1 : z + 1;
            int westOther = view.getBlockId(x - 1, y, oz), eastOther = view.getBlockId(x + 1, y, oz);
            if (side == 4) half = -1 - half;
            int front = 5;
            if ((opaque(west) || opaque(westOther)) && !opaque(east) && !opaque(eastOther)) front = 5;
            if ((opaque(east) || opaque(eastOther)) && !opaque(west) && !opaque(westOther)) front = 4;
            return (side != front ? 58 : 42) + half;
        }
        if (west == self || east == self) {
            if (side == 4 || side == 5) return 26;
            int half = west == self ? -1 : 0;
            int ox = west == self ? x - 1 : x + 1;
            int northOther = view.getBlockId(ox, y, z - 1), southOther = view.getBlockId(ox, y, z + 1);
            if (side == 3) half = -1 - half;
            int front = 3;
            if ((opaque(north) || opaque(northOther)) && !opaque(south) && !opaque(southOther)) front = 3;
            if ((opaque(south) || opaque(southOther)) && !opaque(north) && !opaque(northOther)) front = 2;
            return (side != front ? 58 : 42) + half;
        }
        int front = 3;
        if (opaque(north) && !opaque(south)) front = 3;
        if (opaque(south) && !opaque(north)) front = 2;
        if (opaque(west) && !opaque(east)) front = 5;
        if (opaque(east) && !opaque(west)) front = 4;
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
