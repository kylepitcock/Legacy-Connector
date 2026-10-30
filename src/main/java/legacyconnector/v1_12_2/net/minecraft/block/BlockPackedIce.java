package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;

/** 1.12.2 packed ice: slippery, opaque, drops nothing without silk touch, never melts. */
public class BlockPackedIce extends Block {
    public BlockPackedIce() {
        super(Material.PACKED_ICE);
        this.slipperiness = 0.98F;
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
