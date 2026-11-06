package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityMobSpawner;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10's monster spawner block: a cage that is not opaque, drops nothing and hosts a {@link TileEntityMobSpawner}. Mods subclass it
 * for their own spawners (and set the mob from {@code onBlockAdded}).
 */
public class BlockMobSpawner extends BlockContainer {
    public BlockMobSpawner(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockMobSpawner() {
        super(Material.rock);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityMobSpawner();
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return null;
    }
}
