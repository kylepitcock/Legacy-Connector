package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;

/** 1.12.2 bone block: a pillar (the axis is the metadata). */
public class BlockBone extends BlockRotatedPillar {
    public BlockBone() {
        super(Material.ROCK, MapColor.SAND);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setHardness(2.0F);
        setSoundType(SoundType.STONE);
    }
}
