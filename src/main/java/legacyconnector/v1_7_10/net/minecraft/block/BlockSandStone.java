package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** Sandstone (1.7.10 {@code BlockSandStone}): metadata 0 plain, 1 chiseled, 2 smooth; one top and one bottom texture for all three. */
public class BlockSandStone extends Block {
    public static final String[] field_150157_a = {"normal", "carved", "smooth"};
    private IIcon[] field_150158_M;
    private IIcon field_150159_b;
    private IIcon field_150160_N;

    public BlockSandStone(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockSandStone() {
        super(Material.rock);
        setHardness(0.8F);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150158_M == null) return super.getIcon(side, meta);
        if (side == 1 || side == 0 && (meta == 1 || meta == 2)) return field_150159_b;
        if (side == 0) return field_150160_N;
        if (meta < 0 || meta >= field_150158_M.length) meta = 0;
        return field_150158_M[meta];
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
        list.add(new ItemStack(item, 1, 2));
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        field_150158_M = new IIcon[field_150157_a.length];
        for (int i = 0; i < field_150158_M.length; i++) field_150158_M[i] = register.registerIcon(getTextureName() + "_" + field_150157_a[i]);
        field_150159_b = register.registerIcon(getTextureName() + "_top");
        field_150160_N = register.registerIcon(getTextureName() + "_bottom");
    }
}
