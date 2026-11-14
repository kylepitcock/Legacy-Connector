package org.kylepitcock.legacyconnector.api.internal;

import java.util.Optional;
import java.util.OptionalInt;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/**
 * The conversions behind {@code LegacyItems}/{@code LegacyBlocks}/{@code LegacyWorlds}: the one place of the API package that
 * touches shim types. Shim fields exist only as virtual accessors, so this class calls methods only.
 */
public final class Convert {
    private Convert() {}

    // ------------------------------------------------------------------ items

    public static Object itemToLegacy(ItemStack modern, LegacyVersion version) {
        return switch (version) {
            case V1_7_10, V1_4_7, V1_5_2, V1_6_4 -> modern == null ? null : legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(modern);
            case V1_12_2 -> standIn1122(modern == null ? ItemStack.EMPTY : modern);
            case V1_20_1 -> modern == null ? ItemStack.EMPTY : modern; // the legacy stack IS the modern one
            case VB1_7_3 -> modern == null ? null : org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toLegacy(modern); // null for empty, like Beta
        };
    }

    /** The 1.12.2 stack of a modern one; an item the legacy version never had gets a stable stand-in item (registered nowhere). */
    static legacyconnector.v1_12_2.net.minecraft.item.ItemStack standIn1122(ItemStack modern) {
        var legacy = Stacks1122.fromModern(modern);
        if (!legacy.isEmpty() || modern.isEmpty()) return legacy;
        var item = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.standIn(modern.getItem());
        var out = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, modern.getCount(), 0);
        var data = modern.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) out.setTagCompound(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(data.copyTag()));
        return out;
    }

    public static ItemStack itemFromLegacy(Object legacy) {
        if (legacy == null) return ItemStack.EMPTY;
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemStack s) return legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(s);
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s) return Stacks1122.toModern(s);
        if (legacy instanceof legacyconnector.vb1_7_3.net.minecraft.item.ItemStack s) return org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toModern(s);
        if (legacy instanceof ItemStack s) return s; // 1.20.1: already modern
        throw new IllegalArgumentException("Not a legacy ItemStack: " + legacy.getClass().getName());
    }

    // ------------------------------------------------------------------ blocks

    public static Optional<LegacyVersion> versionOf(BlockState state) {
        // the owning mod's version first (tells 1.4.7/1.5.2/1.6.4 blocks from 1.7.10 ones and finds 1.20.1 blocks), then the block class
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        var byMod = InPlaceMods.versionOfNamespace(key.getNamespace());
        if (byMod.isPresent()) return byMod;
        if (state.getBlock() instanceof LegacyBlockAdapter) return Optional.of(LegacyVersion.V1_7_10);
        if (state.getBlock() instanceof Legacy1122Block) return Optional.of(LegacyVersion.V1_12_2);
        if (state.getBlock() instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block) return Optional.of(LegacyVersion.VB1_7_3);
        return Optional.empty();
    }

    public static Optional<Object> blockToLegacy(BlockState state, LegacyVersion version) {
        return Optional.ofNullable(switch (version) {
            case V1_7_10, V1_4_7, V1_5_2, V1_6_4 -> BlockBridge.legacyBlock(state);
            case V1_12_2 -> StatesBridge1122.legacyBlock(state);
            case V1_20_1 -> state.getBlock(); // the legacy block IS the modern one
            case VB1_7_3 -> legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[Math.max(0, Math.min(255, org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173.legacyId(state)))];
        });
    }

    public static OptionalInt metaToLegacy(BlockState state, LegacyVersion version) {
        switch (version) {
            case V1_7_10:
            case V1_4_7:
            case V1_5_2:
            case V1_6_4:
                return OptionalInt.of(BlockBridge.legacyMeta(state));
            case V1_20_1:
                return OptionalInt.empty(); // no metadata after the flattening
            case VB1_7_3:
                return OptionalInt.of(org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173.legacyMeta(state));
            case V1_12_2:
            default:
                IBlockState legacy = StatesBridge1122.toLegacy(state);
                return legacy == null ? OptionalInt.empty() : OptionalInt.of(legacy.getBlock().getMetaFromState(legacy));
        }
    }

    public static Optional<Object> stateToLegacy(BlockState state) {
        if (versionOf(state).orElse(null) == LegacyVersion.V1_20_1) return Optional.of(state); // the legacy state IS the modern one
        return Optional.ofNullable(StatesBridge1122.toLegacy(state));
    }

    public static Optional<BlockState> blockFromLegacy(Object legacyBlock, int meta) {
        if (legacyBlock instanceof legacyconnector.v1_7_10.net.minecraft.block.Block b) return Optional.ofNullable(BlockBridge.toModern(b, meta));
        if (legacyBlock instanceof legacyconnector.vb1_7_3.net.minecraft.block.Block b) return Optional.ofNullable(org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173.modernState(b.id, meta));
        if (legacyBlock instanceof legacyconnector.v1_12_2.net.minecraft.block.Block b) {
            return Optional.ofNullable(StatesBridge1122.toModern(b.getStateFromMeta(meta)));
        }
        if (legacyBlock instanceof net.minecraft.world.level.block.Block b) return Optional.of(b.defaultBlockState()); // 1.20.1: meta is ignored
        throw new IllegalArgumentException("Not a legacy Block: " + legacyBlock);
    }

    public static Optional<BlockState> stateFromLegacy(Object legacyState) {
        if (legacyState instanceof IBlockState s) return Optional.ofNullable(StatesBridge1122.toModern(s));
        if (legacyState instanceof BlockState s) return Optional.of(s); // 1.20.1
        throw new IllegalArgumentException("Not a legacy block state: " + legacyState);
    }

    // ------------------------------------------------------------------ tiles and worlds

    public static Optional<Object> tileOf(BlockEntity be) {
        if (be instanceof LegacyBlockEntity l) return Optional.ofNullable(l.legacy());
        if (be instanceof Legacy1122BlockEntity l) return Optional.ofNullable(l.legacy());
        if (be instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity l) return Optional.ofNullable(l.legacy());
        if (isInPlace(be)) return Optional.of(be); // 1.20.1: the mod's own block entity
        return Optional.empty();
    }

    public static Optional<LegacyVersion> tileVersion(BlockEntity be) {
        if (be instanceof LegacyBlockEntity) {
            // a hosted 1.4.7/1.5.2/1.6.4 tile: the version of the mod owning the block
            var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
            var byMod = InPlaceMods.versionOfNamespace(key.getNamespace());
            return Optional.of(byMod.isPresent() ? byMod.get() : LegacyVersion.V1_7_10);
        }
        if (be instanceof Legacy1122BlockEntity) return Optional.of(LegacyVersion.V1_12_2);
        if (be instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity) return Optional.of(LegacyVersion.VB1_7_3);
        if (isInPlace(be)) return Optional.of(LegacyVersion.V1_20_1);
        return Optional.empty();
    }

    public static Object worldToLegacy(Level level, LegacyVersion version) {
        return switch (version) {
            case V1_7_10, V1_4_7, V1_5_2, V1_6_4 -> legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            case V1_12_2 -> WorldBridge1122.wrap(level);
            case V1_20_1 -> level; // the legacy level IS the modern one
            case VB1_7_3 -> legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level);
        };
    }

    /** True for a block entity of a translated Forge 1.20.1 mod (its type is registered under the mod's namespace). */
    public static boolean isInPlace(BlockEntity be) {
        if (be == null || be instanceof LegacyBlockEntity || be instanceof Legacy1122BlockEntity
                || be instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity) return false;
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        return key != null && InPlaceMods.isInPlaceNamespace(key.getNamespace());
    }

    // ------------------------------------------------------------------ numeric ids (1.4.7, 1.5.2, 1.6.4)

    /** Runs {@code action} with the numeric id space of {@code version} selected. */
    private static <T> T inIdSpace(LegacyVersion version, java.util.function.Supplier<T> action) {
        if (!version.usesNumericIds()) throw new IllegalArgumentException(version + " has no numeric ids");
        String previous = legacyconnector.v1_6_4.net.minecraft.util.Ids.enter(version.id(), null);
        try {
            return action.get();
        } finally {
            legacyconnector.v1_6_4.net.minecraft.util.Ids.leave(previous);
        }
    }

    public static OptionalInt numericBlockId(Object legacyBlock, LegacyVersion version) {
        if (version.isBeta()) {
            return legacyBlock instanceof legacyconnector.vb1_7_3.net.minecraft.block.Block b && b.id > 0 ? OptionalInt.of(b.id) : OptionalInt.empty();
        }
        if (!(legacyBlock instanceof legacyconnector.v1_7_10.net.minecraft.block.Block b)) return OptionalInt.empty();
        int id = inIdSpace(version, () -> legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(b));
        return id > 0 ? OptionalInt.of(id) : OptionalInt.empty();
    }

    public static OptionalInt numericItemId(Object legacyItemOrStack, LegacyVersion version) {
        if (version.isBeta()) {
            Object o = legacyItemOrStack instanceof legacyconnector.vb1_7_3.net.minecraft.item.ItemStack s
                    ? (s.itemId >= 0 && s.itemId < legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS.length ? legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[s.itemId] : null)
                    : legacyItemOrStack;
            return o instanceof legacyconnector.vb1_7_3.net.minecraft.item.Item i && i.id > 0 ? OptionalInt.of(i.id) : OptionalInt.empty();
        }
        Object o = legacyItemOrStack instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemStack s ? s.getItem() : legacyItemOrStack;
        if (!(o instanceof legacyconnector.v1_7_10.net.minecraft.item.Item item)) return OptionalInt.empty();
        int id = inIdSpace(version, () -> legacyconnector.v1_6_4.net.minecraft.util.Ids.itemId(item));
        return id > 0 ? OptionalInt.of(id) : OptionalInt.empty();
    }

    public static Optional<Object> legacyBlockById(int id, LegacyVersion version) {
        if (version.isBeta()) {
            var blocks = legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS;
            return id > 0 && id < blocks.length ? Optional.ofNullable(blocks[id]) : Optional.empty();
        }
        return Optional.ofNullable(inIdSpace(version, () -> (Object) legacyconnector.v1_6_4.net.minecraft.util.Ids.block(id)));
    }

    public static Optional<Object> legacyItemById(int id, LegacyVersion version) {
        if (version.isBeta()) {
            var items = legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS;
            return id > 0 && id < items.length ? Optional.ofNullable(items[id]) : Optional.empty();
        }
        return Optional.ofNullable(inIdSpace(version, () -> (Object) legacyconnector.v1_6_4.net.minecraft.util.Ids.item(id)));
    }
}
