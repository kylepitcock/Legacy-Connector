package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/**
 * The flowers, ported from 1.7.10: type 0 is the dandelion block ({@code yellow_flower}), type 1 the block of the other nine
 * small flowers ({@code red_flower}, metadata 0-8). The static tables name them; {@link #func_149857_e} and
 * {@link #func_149856_f} turn a name back into the block and metadata.
 */
public class BlockFlower extends BlockBush {
    /** Texture names per type and metadata. */
    public static final String[][] field_149860_M = {{"flower_dandelion"},
            {"flower_rose", "flower_blue_orchid", "flower_allium", "flower_houstonia", "flower_tulip_red", "flower_tulip_orange",
                    "flower_tulip_white", "flower_tulip_pink", "flower_oxeye_daisy"}};
    /** Names of the {@code red_flower} kinds, by metadata. */
    public static final String[] field_149859_a = {"poppy", "blueOrchid", "allium", "houstonia", "tulipRed", "tulipOrange", "tulipWhite",
            "tulipPink", "oxeyeDaisy"};
    /** Names of the {@code yellow_flower} kinds, by metadata. */
    public static final String[] field_149858_b = {"dandelion"};
    private IIcon[] field_149861_N;
    /** 0 dandelion block, 1 the other flowers. */
    private int field_149862_O;

    public BlockFlower(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_149862_O = info.legacyName().endsWith("red_flower") ? 1 : 0;
    }

    protected BlockFlower(Material material) {
        super(material);
    }

    protected BlockFlower(int type) {
        this(Material.plants);
        this.field_149862_O = type;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_149861_N == null || field_149861_N.length == 0) return super.getIcon(side, meta);
        if (meta < 0 || meta >= field_149861_N.length) meta = 0;
        return field_149861_N[meta];
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        String[] names = field_149860_M[field_149862_O == 1 ? 1 : 0];
        field_149861_N = new IIcon[names.length];
        for (int i = 0; i < names.length; ++i) field_149861_N[i] = register.registerIcon(names[i]);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        String[] names = field_149860_M[field_149862_O == 1 ? 1 : 0];
        for (int i = 0; i < names.length; ++i) list.add(new ItemStack(item, 1, i));
    }

    /** The flower block that has a kind of that name ({@code "dandelion"}, {@code "tulipRed"}...), or null. */
    public static BlockFlower func_149857_e(String name) {
        for (String s : field_149858_b) if (s.equals(name)) return Blocks.yellow_flower;
        for (String s : field_149859_a) if (s.equals(name)) return Blocks.red_flower;
        return null;
    }

    /** The metadata of the flower kind of that name (0 when unknown). */
    public static int func_149856_f(String name) {
        for (int i = 0; i < field_149858_b.length; ++i) if (field_149858_b[i].equals(name)) return i;
        for (int i = 0; i < field_149859_a.length; ++i) if (field_149859_a[i].equals(name)) return i;
        return 0;
    }
}
