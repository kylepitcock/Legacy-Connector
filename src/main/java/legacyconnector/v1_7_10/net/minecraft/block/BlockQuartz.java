package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Quartz block (1.7.10 {@code BlockQuartz}): metadata 0 plain, 1 chiseled, 2 pillar up/down, 3 and 4 the pillar turned on its side (placed by the face
 * clicked); both side metadata drop the pillar (2).
 */
public class BlockQuartz extends Block {
    public static final String[] field_150191_a = {"default", "chiseled", "lines", null, null};
    public static final String[] field_150189_b = {"side", "chiseled", "lines", null, null};

    private IIcon[] field_150192_M;
    private IIcon field_150193_N;
    private IIcon field_150194_O;
    private IIcon field_150190_P;
    private IIcon field_150188_Q;

    public BlockQuartz(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockQuartz() {
        super(Material.rock);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150192_M == null) return super.getIcon(side, meta);
        if (meta == 2 || meta == 3 || meta == 4) {
            boolean endFace = meta == 2 && (side == 1 || side == 0) || meta == 3 && (side == 5 || side == 4) || meta == 4 && (side == 2 || side == 3);
            return endFace ? field_150194_O : field_150192_M[meta];
        }
        if (side != 1 && (side != 0 || meta != 1)) {
            if (side == 0) return field_150188_Q;
            if (meta < 0 || meta >= field_150192_M.length) meta = 0;
            return field_150192_M[meta];
        }
        return meta == 1 ? field_150193_N : field_150190_P;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        if (meta == 2) {
            switch (side) {
                case 0, 1 -> meta = 2;
                case 2, 3 -> meta = 4;
                case 4, 5 -> meta = 3;
                default -> {
                }
            }
        }
        return meta;
    }

    @Override
    public int damageDropped(int meta) {
        return meta != 3 && meta != 4 ? meta : 2;
    }

    @Override
    public ItemStack createStackedBlock(int meta) {
        return meta != 3 && meta != 4 ? super.createStackedBlock(meta) : new ItemStack(Item.getItemFromBlock(this), 1, 2);
    }

    @Override
    public int getRenderType() {
        return 39;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
        list.add(new ItemStack(item, 1, 2));
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        field_150192_M = new IIcon[field_150189_b.length];
        for (int i = 0; i < field_150192_M.length; i++) {
            field_150192_M[i] = field_150189_b[i] == null ? field_150192_M[i - 1] : register.registerIcon(getTextureName() + "_" + field_150189_b[i]);
        }
        field_150190_P = register.registerIcon(getTextureName() + "_top");
        field_150193_N = register.registerIcon(getTextureName() + "_chiseled_top");
        field_150194_O = register.registerIcon(getTextureName() + "_lines_top");
        field_150188_Q = register.registerIcon(getTextureName() + "_bottom");
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.quartzColor;
    }
}
