package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** Netherrack: rock with the netherrack map colour. */
public class BlockNetherrack extends Block {
    public BlockNetherrack(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockNetherrack() {
        super(Material.rock);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.netherrackColor;
    }
}
