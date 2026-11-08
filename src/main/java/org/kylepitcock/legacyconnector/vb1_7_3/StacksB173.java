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
                } else if (legacy.hasSubtypes()) {
                    var tag = new net.minecraft.nbt.CompoundTag();
                    tag.putInt("b173_meta", damage);
                    stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                }
            } else if (stack.isDamageableItem()) {
                stack.setDamageValue(Math.min(damage, stack.getMaxDamage()));
            }
        }
        return stack;
    }

    private static volatile java.util.Map<net.minecraft.world.item.Item, int[]> reverse;

    /**
     * The legacy stack of a modern one: a mod's item or block item (metadata from its {@code block_state}), or the b1.7.3 id of a vanilla
     * item (metadata variants such as wool and dye colours). Null for an empty stack or an item b1.7.3 did not have.
     */
    public static legacyconnector.vb1_7_3.net.minecraft.item.ItemStack toLegacy(ItemStack modern) {
        if (modern == null || modern.isEmpty()) return null;
        int[] hit = reverse().get(modern.getItem());
        if (hit == null) return null;
        int damage = hit[1];
        var state = modern.get(DataComponents.BLOCK_STATE);
        if (state != null && modern.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof LegacyB173Block) {
            Integer meta = state.get(LegacyB173Block.META);
            if (meta != null) damage = meta;
        } else if (modern.isDamageableItem()) {
            damage = modern.getDamageValue();
        } else if (modern.getItem() instanceof LegacyB173Item && modern.has(DataComponents.CUSTOM_DATA)) {
            damage = modern.get(DataComponents.CUSTOM_DATA).copyTag().getInt("b173_meta");
        }
        return new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(hit[0], modern.getCount(), damage);
    }

    private static java.util.Map<net.minecraft.world.item.Item, int[]> reverse() {
        var r = reverse;
        if (r != null) return r;
        synchronized (StacksB173.class) {
            if (reverse != null) return reverse;
            r = new java.util.HashMap<>();
            for (Item legacy : ContentB173.items()) {
                if (legacy.mcc$owner != null && legacy.mcc$modern != null) r.putIfAbsent(legacy.mcc$modern, new int[] {legacy.id, 0});
            }
            for (Block block : ContentB173.blocks()) {
                if (block.mcc$owner != null && block.mcc$modern != null) {
                    var item = block.mcc$modern.asItem();
                    if (item != net.minecraft.world.item.Items.AIR) r.putIfAbsent(item, new int[] {block.id, 0});
                }
            }
            // Native flattened items can alias both an old block and its real item (wheat crop / wheat, sign / sign item).
            // Preserve the original item identity before considering block-item aliases.
            addVanillaReverse(r,256,Math.min(Item.ITEMS.length,2300));
            addVanillaReverse(r,1,Math.min(Item.ITEMS.length,256));
            return reverse = r;
        }
    }

    private static void addVanillaReverse(java.util.Map<net.minecraft.world.item.Item,int[]> r,int start,int end){
            for (int id = start; id < end; id++) {
                if (!VanillaB173.isVanillaId(id)) continue;
                int variants = switch (id) {
                    case 35, 351 -> 16;
                    case 17, 18, 6, 31 -> 3;
                    case 44 -> 4;
                    case 263 -> 2;
                    default -> 1;
                };
                for (int meta = 0; meta < variants; meta++) {
                    var item = VanillaB173.modernItem(id, meta);
                    if (item != null) r.putIfAbsent(item, new int[] {id, meta});
                }
            }
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
