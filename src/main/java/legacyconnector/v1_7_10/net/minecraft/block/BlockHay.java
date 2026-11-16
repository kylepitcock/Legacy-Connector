package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** A hay bale (1.7.10 {@code BlockHay}): a pillar of grass material, hardness 0.5, with its own top and side textures. */
public class BlockHay extends BlockRotatedPillar {
    public BlockHay(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockHay() {
        super(Material.grass);
        setHardness(0.5F);
        setStepSound(soundTypeGrass);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    protected IIcon getSideIcon(int variant) {
        return blockIcon;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        field_150164_N = register.registerIcon(getTextureName() + "_top");
        blockIcon = register.registerIcon(getTextureName() + "_side");
    }

    /** Hay keeps no variants: the axis bits are not part of the dropped item. */
    @Override
    public int damageDropped(int meta) {
        return 0;
    }
}
