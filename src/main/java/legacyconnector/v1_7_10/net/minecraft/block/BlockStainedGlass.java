package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/**
 * Stained glass (1.7.10 {@code BlockStainedGlass}): sixteen dye colours in the metadata, translucent (render pass 1), drops nothing
 * unless harvested with Silk Touch, and keeps its colour as the dropped damage value.
 */
public class BlockStainedGlass extends BlockBreakable {
    /** The dye colour names, indexed by dye damage (black first); the texture of metadata {@code n} is {@code glass_<name of dye ~n & 15>}. */
    private static final String[] DYES = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow",
            "light_blue", "magenta", "orange", "white"};

    public static IIcon[] field_149998_a;

    public BlockStainedGlass(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockStainedGlass(Material material) {
        super("glass", material, false);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    /** Metadata to dye damage (and back): the two are mirrored, as for wool. */
    public static int func_149997_b(int meta) {
        return ~meta & 15;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_149998_a == null) return super.getIcon(side, meta);
        return field_149998_a[meta % field_149998_a.length];
    }

    @Override
    public int damageDropped(int meta) {
        if (mcc$vanilla != null) return super.damageDropped(meta);
        return meta;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        if (mcc$vanilla != null) {
            super.getSubBlocks(item, tab, list);
            return;
        }
        for (int i = 0; i < 16; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        if (mcc$vanilla != null) {
            super.registerBlockIcons(register);
            return;
        }
        field_149998_a = new IIcon[DYES.length];
        for (int i = 0; i < DYES.length; i++) field_149998_a[i] = register.registerIcon("glass_" + DYES[func_149997_b(i)]);
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$vanilla != null ? super.quantityDropped(random) : 0;
    }

    @Override
    public boolean canSilkHarvest() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
