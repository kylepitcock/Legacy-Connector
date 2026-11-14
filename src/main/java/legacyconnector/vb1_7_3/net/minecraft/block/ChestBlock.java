package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta {@code BlockChest}: drawn by the chest tile entity renderer (render type 22), not as a cube. Mods extend it for their own chests. */
public class ChestBlock extends BlockWithEntity {
    public ChestBlock(int id) {
        super(id, Material.WOOD);
        this.textureId = 26;
    }

    @Override public int getRenderType() { return 22; }
    @Override public boolean isOpaque() { return false; }
    @Override public boolean isFullCube() { return false; }

    @Override public int getTexture(int side) { return side == 1 || side == 0 ? 25 : side == 3 ? 27 : 26; }

    @Override public BlockEntity createBlockEntity() { return new ChestBlockEntity(); }

    @Override public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
        if (world.isRemote) return true;
        if (world.getBlockEntity(x, y, z) instanceof Inventory inventory) player.openChestScreen(inventory);
        return true;
    }
}
