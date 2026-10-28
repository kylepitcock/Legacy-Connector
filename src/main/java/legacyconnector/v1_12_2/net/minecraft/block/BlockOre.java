package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 ores: drop themselves, or for the vanilla gem ores their gem with fortune and experience. Mods' ores override getItemDropped
 * and get the same fortune and experience rules when they drop something other than themselves.
 */
public class BlockOre extends Block {
    public BlockOre() {
        this(Material.ROCK.getMaterialMapColor());
    }

    public BlockOre(MapColor color) {
        super(Material.ROCK, color);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (mcc$named(this, "coal_ore")) return Items.COAL;
        if (mcc$named(this, "diamond_ore")) return Items.DIAMOND;
        if (mcc$named(this, "lapis_ore")) return Items.DYE;
        if (mcc$named(this, "emerald_ore")) return Items.EMERALD;
        if (mcc$named(this, "quartz_ore")) return Items.QUARTZ;
        return Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$named(this, "lapis_ore") ? 4 + random.nextInt(5) : 1;
    }

    /** Fortune: more of a dropped item that is not the ore itself. */
    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune > 0 && Item.getItemFromBlock(this) != getItemDropped(getDefaultState(), random, fortune)) {
            int i = random.nextInt(fortune + 2) - 1;
            if (i < 0) i = 0;
            return quantityDropped(random) * (i + 1);
        }
        return quantityDropped(random);
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        Random rand = world instanceof World w ? w.rand : new Random();
        if (getItemDropped(state, rand, fortune) == Item.getItemFromBlock(this)) return 0;
        if (mcc$named(this, "coal_ore")) return MathHelper.getInt(rand, 0, 2);
        if (mcc$named(this, "diamond_ore", "emerald_ore")) return MathHelper.getInt(rand, 3, 7);
        if (mcc$named(this, "lapis_ore", "quartz_ore")) return MathHelper.getInt(rand, 2, 5);
        return 0;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return mcc$named(this, "lapis_ore") ? 4 : 0;
    }
}
