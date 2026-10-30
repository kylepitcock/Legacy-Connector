package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** 1.12.2 sponge: dry or wet (metadata 1). Soaking up water is the modern sponge's. */
public class BlockSponge extends Block {
    public static final PropertyBool WET = PropertyBool.create("wet");

    protected BlockSponge() {
        super(Material.SPONGE);
        setDefaultState(blockState.getBaseState().withProperty(WET, false));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".dry.name");
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(WET) ? 1 : 0;
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
        items.add(new ItemStack(this, 1, 1));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(WET, (meta & 1) == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(WET) ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, WET);
    }
}
