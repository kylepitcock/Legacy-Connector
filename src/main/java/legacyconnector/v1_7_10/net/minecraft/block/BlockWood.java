package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** Wooden planks: metadata 0-5 is the wood type, each with its own texture ({@code planks_oak} ...). */
public class BlockWood extends Block {
    /** The wood type names, by metadata. */
    public static final String[] field_150096_a = {"oak", "spruce", "birch", "jungle", "acacia", "big_oak"};
    private IIcon[] field_150095_b;

    public BlockWood(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockWood() {
        super(Material.wood);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150095_b == null) return super.getIcon(side, meta);
        if (meta < 0 || meta >= field_150095_b.length) meta = 0;
        return field_150095_b[meta];
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < field_150096_a.length; ++i) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        field_150095_b = new IIcon[field_150096_a.length];
        for (int i = 0; i < field_150095_b.length; ++i) field_150095_b[i] = register.registerIcon(getTextureName() + "_" + field_150096_a[i]);
    }
}
