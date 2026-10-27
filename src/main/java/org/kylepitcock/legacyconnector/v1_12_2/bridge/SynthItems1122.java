package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * Members of 1.12.2 vanilla block, item and tile entity classes that only the synthesizer provides (TileEntityFurnace, BlockFire),
 * answered with their real behaviour through {@link Synth}.
 */
public final class SynthItems1122 {
    private SynthItems1122() {}

    /** Furnace burn time: the item's own (Forge's getItemBurnTime) when it gives one, else the modern fuel value. */
    public static int burnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int own;
        try {
            own = stack.getItem().getItemBurnTime(stack);
        } catch (RuntimeException | LinkageError e) {
            own = -1;
        }
        if (own >= 0) return own;
        var modern = Stacks1122.toModern(stack);
        return modern.isEmpty() ? 0 : modern.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
    }

    public static void register() {
        Synth.addStaticMethod("net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime",
                args -> args.length == 1 && args[0] instanceof ItemStack s ? (Object) burnTime(s) : Synth.NONE);
        Synth.addStaticMethod("net.minecraft.tileentity.TileEntityFurnace.isItemFuel",
                args -> args.length == 1 && args[0] instanceof ItemStack s ? (Object) (burnTime(s) > 0) : Synth.NONE);
        // enchanted books: the StoredEnchantments list
        Synth.addStaticMethod("net.minecraft.item.ItemEnchantedBook.getEnchantments", args -> {
            if (args.length != 1 || !(args[0] instanceof ItemStack s)) return Synth.NONE;
            var tag = s.getTagCompound();
            return tag != null ? tag.getTagList("StoredEnchantments", 10) : new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
        });
        Synth.addStaticMethod("net.minecraft.item.ItemEnchantedBook.addEnchantment", args -> {
            if (args.length != 2 || !(args[0] instanceof ItemStack s) || !(args[1] instanceof legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData d)) {
                return Synth.NONE;
            }
            var stored = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantments(s);
            Integer old = stored.get(d.enchantment);
            if (old == null || old < d.enchantmentLevel) stored.put(d.enchantment, d.enchantmentLevel);
            legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.setEnchantments(stored, s);
            return null;
        });
        Synth.addStaticMethod("net.minecraft.item.ItemEnchantedBook.getEnchantedItemStack", args -> args.length == 1
                && args[0] instanceof legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData d
                ? legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.mcc$enchantedBook(d) : Synth.NONE);
        // bone meal: the modern growth rules of the block there
        Synth.StaticMethod bonemeal = args -> {
            if (args.length < 3 || !(args[0] instanceof ItemStack s) || !(args[1] instanceof legacyconnector.v1_12_2.net.minecraft.world.World w)
                    || !(args[2] instanceof legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos)) {
                return Synth.NONE;
            }
            var modern = Stacks1122.toModernLive(s);
            if (modern.isEmpty()) modern = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL);
            boolean grown = net.minecraft.world.item.BoneMealItem.applyBonemeal(modern, w.mcc$level(), pos.mcc$modern(), null);
            if (grown && s.mcc$backing() == null) s.shrink(1);
            return grown;
        };
        Synth.addStaticMethod("net.minecraft.item.ItemDye.applyBonemeal", bonemeal);
        // Blocks.FIRE.setFireInfo(block, encouragement, flammability): the modern fire's flammability table
        Synth.addInstanceMethod("net.minecraft.block.BlockFire.setFireInfo", (self, args) -> {
            if (args.length != 3 || !(args[0] instanceof Block b) || !(b.mcc$modern instanceof net.minecraft.world.level.block.Block modern)) return Synth.NONE;
            ((net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE).setFlammable(modern, Synth.toInt(args[1]), Synth.toInt(args[2]));
            return null;
        });
    }
}
