package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Dirt: metadata 0 dirt, 1 coarse dirt (drops as dirt), 2 podzol. */
public class BlockDirt extends Block {
    public static final String[] field_150009_a = {"default", "default", "podzol"};

    public BlockDirt(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDirt() {
        super(Material.ground);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    public int damageDropped(int meta) {
        return meta == 1 ? 0 : meta;
    }

    @Override
    public void getSubBlocks(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab, List list) {
        list.add(new ItemStack(this, 1, 0));
        list.add(new ItemStack(this, 1, 2));
    }

    /** {@code func_149643_k}: the pick-block damage (coarse dirt picks as itself). */
    @Override
    public int getDamageValue(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }
}
