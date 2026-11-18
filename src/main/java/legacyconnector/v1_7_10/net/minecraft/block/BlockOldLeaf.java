package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** The first leaves block (oak 0, spruce 1, birch 2, jungle 3); oak leaves drop apples. */
public class BlockOldLeaf extends BlockLeaves {
    public static final String[][] field_150130_N = {{"leaves_oak", "leaves_spruce", "leaves_birch", "leaves_jungle"},
            {"leaves_oak_opaque", "leaves_spruce_opaque", "leaves_birch_opaque", "leaves_jungle_opaque"}};
    public static final String[] field_150131_O = {"oak", "spruce", "birch", "jungle"};

    public BlockOldLeaf(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockOldLeaf() {
        super();
    }

    /** Oak leaves drop apples (about one in {@code chance} when they decay or are broken). */
    @Override
    protected void func_150124_c(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int meta, int chance) {
        if ((meta & 3) == 0 && world.rand.nextInt(chance) == 0) {
            dropBlockAsItem(world, x, y, z, new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.apple, 1, 0));
        }
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (mcc$vanilla != null) return super.getIcon(side, meta);
        IIcon[] icons = field_150129_M[field_150127_b];
        int type = meta & 3;
        if (icons != null && type < icons.length && icons[type] != null) return icons[type];
        return mcc$vanillaIcon(field_150130_N[field_150127_b][type]);
    }

    @Override
    public void getSubBlocks(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab, List list) {
        for (int i = 0; i < 4; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        for (int i = 0; i < field_150130_N.length; i++) {
            field_150129_M[i] = new IIcon[field_150130_N[i].length];
            for (int j = 0; j < field_150130_N[i].length; j++) field_150129_M[i][j] = register.registerIcon(field_150130_N[i][j]);
        }
    }

    @Override
    public String[] func_150125_e() {
        return field_150131_O;
    }
}
