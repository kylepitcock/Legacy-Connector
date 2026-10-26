package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** The second leaves block (acacia 0, dark oak 1); its saplings are the second half of the sapling types (damage + 4). */
public class BlockNewLeaf extends BlockLeaves {
    public static final String[][] field_150132_N = {{"leaves_acacia", "leaves_big_oak"}, {"leaves_acacia_opaque", "leaves_big_oak_opaque"}};
    public static final String[] field_150133_O = {"acacia", "big_oak"};
    protected IIcon[][] field_150129_M = new IIcon[2][];

    public BlockNewLeaf(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockNewLeaf() {
        super();
    }

    /** {@code func_150124_c}: dark oak leaves drop apples like oak leaves. */
    protected void func_150124_c(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int meta, int chance) {
        if ((meta & 3) == 1 && world.rand.nextInt(chance) == 0) {
            dropBlockAsItem(world, x, y, z, new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.apple, 1, 0));
        }
    }

    @Override
    public int damageDropped(int meta) {
        return super.damageDropped(meta) + 4;
    }

    @Override
    public int getDamageValue(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) & 3;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        IIcon[] icons = field_150129_M[field_150127_b];
        int type = (meta & 3) == 1 ? 1 : 0;
        if (icons != null && icons[type] != null) return icons[type];
        return mcc$vanillaIcon(field_150132_N[field_150127_b][type]);
    }

    @Override
    public void getSubBlocks(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        for (int i = 0; i < field_150132_N.length; ++i) {
            field_150129_M[i] = new IIcon[field_150132_N[i].length];
            for (int j = 0; j < field_150132_N[i].length; ++j) field_150129_M[i][j] = register.registerIcon(field_150132_N[i][j]);
        }
    }

    @Override
    public String[] func_150125_e() {
        return field_150133_O;
    }
}
