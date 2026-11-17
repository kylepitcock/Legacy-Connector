package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/**
 * A huge mushroom block (1.7.10 {@code BlockHugeMushroom}): type 0 is brown, 1 red; metadata picks which faces show the cap skin
 * (1-9 the cap, 10 the stem sides, 14 all skin, 15 all stem). Drops 0 to 2 small mushrooms.
 */
public class BlockHugeMushroom extends Block {
    private static final String[] TEXTURES = {"mushroom_block_skin_brown", "mushroom_block_skin_red"};
    private final int field_149793_a;
    private IIcon[] field_149794_M;
    private IIcon field_149791_b;
    private IIcon field_149792_N;

    public BlockHugeMushroom(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_149793_a = info.legacyName().contains("red") ? 1 : 0;
    }

    public BlockHugeMushroom(Material material, int type) {
        super(material);
        this.field_149793_a = type;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_149794_M == null) return super.getIcon(side, meta);
        IIcon skin = field_149794_M[field_149793_a];
        if (meta == 10 && side > 1) return field_149791_b;
        if (meta >= 1 && meta <= 9 && side == 1) return skin;
        if (meta >= 1 && meta <= 3 && side == 2) return skin;
        if (meta >= 7 && meta <= 9 && side == 3) return skin;
        if ((meta == 1 || meta == 4 || meta == 7) && side == 4) return skin;
        if ((meta == 3 || meta == 6 || meta == 9) && side == 5) return skin;
        if (meta == 14) return skin;
        return meta == 15 ? field_149791_b : field_149792_N;
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$vanilla != null ? super.quantityDropped(random) : Math.max(0, random.nextInt(10) - 7);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        if (mcc$vanilla != null) return super.getItemDropped(meta, random, fortune);
        return Item.getItemFromBlock(field_149793_a == 0 ? Blocks.brown_mushroom : Blocks.red_mushroom);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        String base = getTextureName();
        field_149794_M = new IIcon[TEXTURES.length];
        for (int i = 0; i < TEXTURES.length; ++i) field_149794_M[i] = register.registerIcon(base + "_skin_" + (i == 0 ? "brown" : "red"));
        field_149792_N = register.registerIcon(base + "_inside");
        field_149791_b = register.registerIcon(base + "_skin_stem");
    }
}
