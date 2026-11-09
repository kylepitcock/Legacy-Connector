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
        if (legacy == null) return ItemStack.EMPTY;
        ItemStack result = toModern(legacy.itemId, legacy.count, legacy.getDamage());
        ItemStack snapshot = legacy.mcc$nativeSnapshot();
        if (result.isEmpty() || snapshot == null) return result;
        result.applyComponents(snapshot.getComponentsPatch());
        // A native snapshot must never restore stale values after an original callback edits Beta's fields.
        if (result.isDamageableItem()) result.setDamageValue(Math.max(0, Math.min(legacy.getDamage(), result.getMaxDamage())));
        Item item = legacy.getItem();
        if (item != null && item.mcc$owner != null) {
            if (item instanceof BlockItem && result.getItem() instanceof net.minecraft.world.item.BlockItem block
                    && block.getBlock() instanceof LegacyB173Block) {
                var state = result.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
                result.set(DataComponents.BLOCK_STATE, state.with(LegacyB173Block.META, legacy.getDamage() & 15));
            } else if (!result.isDamageableItem() && item.hasSubtypes()) {
                var data = result.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                data.putInt("b173_meta", legacy.getDamage());
                result.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
            }
        }
        return result;
    }

    public static boolean sameComponents(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack a,
            legacyconnector.vb1_7_3.net.minecraft.item.ItemStack b) {
        if (a == null || b == null) return a == b;
        return ItemStack.isSameItemSameComponents(toModern(a), toModern(b));
    }

    private static final String NATIVE_TAG = "legacyconnector:native_stack";
    private static net.minecraft.core.HolderLookup.Provider registries() {
        var actor = PlayerApiB173.currentPlayer();
        if (actor != null && actor.mcc$modern instanceof net.minecraft.world.entity.player.Player player) return player.registryAccess();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        var player = MenusB173.localPlayer();
        if (player != null) return player.registryAccess();
        return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }

    public static void writeNative(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,
            legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt) {
        if (stack.mcc$nativeSnapshot() == null) return;
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.putInt("legacy_id", stack.itemId);
        saved.put("stack", toModern(stack).saveOptional(registries()));
        // Beta NBT has no int/long array tags. Store native NBT bytes so component data stays lossless.
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            try (var output = new java.io.DataOutputStream(bytes)) { net.minecraft.nbt.NbtIo.write(saved, output); }
            nbt.putByteArray(NATIVE_TAG, bytes.toByteArray());
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot save native stack components", error); }
    }

    public static void readNative(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,
            legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt) {
        if (!nbt.contains(NATIVE_TAG)) return;
        try {
            byte[] bytes = nbt.getByteArray(NATIVE_TAG);
            if (bytes.length > 2_097_152) throw new IllegalArgumentException("Native stack component tag exceeds limit");
            var saved = net.minecraft.nbt.NbtIo.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes)),
                    net.minecraft.nbt.NbtAccounter.create(2_097_152));
            if (saved.getInt("legacy_id") != stack.itemId) return;
            ItemStack modern = ItemStack.parseOptional(registries(), saved.getCompound("stack"));
            if (modern.isEmpty()) return;
            var origin = toLegacy(modern);
            if (origin != null && origin.itemId == stack.itemId) stack.mcc$nativeSnapshot(modern);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot read native stack components", error); }
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

    /** Old item-entity queries require a real nonempty Beta stack, never a null payload. */
    public static boolean canRepresent(ItemStack modern){return modern!=null&&!modern.isEmpty()&&reverse().containsKey(modern.getItem());}

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
        } else if ((modern.getItem() instanceof LegacyB173Item||modern.getItem() instanceof LegacyB173ArmorItem) && modern.has(DataComponents.CUSTOM_DATA)) {
            damage = modern.get(DataComponents.CUSTOM_DATA).copyTag().getInt("b173_meta");
        }
        var legacy = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(hit[0], modern.getCount(), damage);
        legacy.mcc$nativeSnapshot(modern);
        return legacy;
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
