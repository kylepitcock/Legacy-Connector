package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.BlockItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/** Legacy b1.7.3 stacks (id, count, damage) as modern ones. */
public final class StacksB173 {
    private StacksB173() {}

    public static ItemStack toModern(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack legacy) {
        return legacy == null ? ItemStack.EMPTY : toModern(legacy.itemId, legacy.count, legacy.getDamage());
    }

    /**
     * The modern stack of a legacy id: a mod item or block item, or a vanilla one (metadata picks the flattened variant). A mod block item
     * with metadata places that metadata ({@code block_state} component); a damageable item keeps its damage.
     */
    public static ItemStack toModern(int id, int count, int damage) {
        net.minecraft.world.item.Item item = modernItem(id, damage);
        if (item == null || count <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, Math.min(count, 99));
        if (damage > 0) {
            Item legacy = id >= 0 && id < Item.ITEMS.length ? Item.ITEMS[id] : null;
            if (legacy != null && legacy.mcc$owner != null) {
                if (legacy instanceof BlockItem && item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof LegacyB173Block) {
                    stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LegacyB173Block.META, damage & 15));
                } else if (stack.isDamageableItem()) {
                    stack.setDamageValue(Math.min(damage, stack.getMaxDamage()));
                }
            } else if (stack.isDamageableItem()) {
                stack.setDamageValue(Math.min(damage, stack.getMaxDamage()));
            }
        }
        return stack;
    }

    /** The modern item of a legacy id with that damage/metadata, or null (unknown id, or a mod item not registered). */
    public static net.minecraft.world.item.Item modernItem(int id, int damage) {
        if (id < 0 || id >= Item.ITEMS.length) return null;
        Item legacy = Item.ITEMS[id];
        if (legacy != null && legacy.mcc$owner != null) return legacy.mcc$modern;
        if (id < 256) {
            Block block = Block.BLOCKS[id];
            if (block != null && block.mcc$owner != null) return block.mcc$modern == null ? null : block.mcc$modern.asItem();
        }
        return VanillaB173.modernItem(id, damage);
    }
}
