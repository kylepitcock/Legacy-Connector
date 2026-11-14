package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta 1.7.3 {@code BlockChest}: a full wooden cube from terrain.png (top 25, side 26, front 27 on the side its metadata faces). Mods extend it for their own chests. */
public class ChestBlock extends BlockWithEntity {
    public ChestBlock(int id) {
        super(id, Material.WOOD);
        this.textureId = 26;
    }

    @Override public int getTexture(int side) { return side == 0 || side == 1 ? 25 : side == 3 ? 27 : 26; }
    @Override public int getTexture(int side, int meta) {
        if (side == 0 || side == 1) return 25;
        return meta >= 2 && meta <= 5 ? (side == meta ? 27 : 26) : getTexture(side);
    }

    @Override public BlockEntity createBlockEntity() { return new ChestBlockEntity(); }

    @Override public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
        if (world.isRemote) return true;
        if (world.getBlockEntity(x, y, z) instanceof Inventory inventory) player.openChestScreen(inventory);
        return true;
    }
}
