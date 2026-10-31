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
        if (modern == null) return version == LegacyVersion.V1_7_10 ? null : Stacks1122.fromModern(ItemStack.EMPTY);
        return switch (version) {
            case V1_7_10 -> legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(modern);
            case V1_12_2 -> Stacks1122.fromModern(modern);
        };
    }

    public static ItemStack itemFromLegacy(Object legacy) {
        if (legacy == null) return ItemStack.EMPTY;
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemStack s) return legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(s);
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s) return Stacks1122.toModern(s);
        throw new IllegalArgumentException("Not a legacy ItemStack: " + legacy.getClass().getName());
    }

    // ------------------------------------------------------------------ blocks

    public static Optional<LegacyVersion> versionOf(BlockState state) {
        if (state.getBlock() instanceof LegacyBlockAdapter) return Optional.of(LegacyVersion.V1_7_10);
        if (state.getBlock() instanceof Legacy1122Block) return Optional.of(LegacyVersion.V1_12_2);
        return Optional.empty();
    }

    public static Optional<Object> blockToLegacy(BlockState state, LegacyVersion version) {
        return Optional.ofNullable(switch (version) {
            case V1_7_10 -> BlockBridge.legacyBlock(state);
            case V1_12_2 -> StatesBridge1122.legacyBlock(state);
        });
    }

    public static OptionalInt metaToLegacy(BlockState state, LegacyVersion version) {
        switch (version) {
            case V1_7_10:
                return OptionalInt.of(BlockBridge.legacyMeta(state));
            case V1_12_2:
            default:
                IBlockState legacy = StatesBridge1122.toLegacy(state);
                return legacy == null ? OptionalInt.empty() : OptionalInt.of(legacy.getBlock().getMetaFromState(legacy));
        }
    }

    public static Optional<Object> stateToLegacy(BlockState state) {
        return Optional.ofNullable(StatesBridge1122.toLegacy(state));
    }

    public static Optional<BlockState> blockFromLegacy(Object legacyBlock, int meta) {
        if (legacyBlock instanceof legacyconnector.v1_7_10.net.minecraft.block.Block b) return Optional.ofNullable(BlockBridge.toModern(b, meta));
        if (legacyBlock instanceof legacyconnector.v1_12_2.net.minecraft.block.Block b) {
            return Optional.ofNullable(StatesBridge1122.toModern(b.getStateFromMeta(meta)));
        }
        throw new IllegalArgumentException("Not a legacy Block: " + legacyBlock);
    }

    public static Optional<BlockState> stateFromLegacy(Object legacyState) {
        if (legacyState instanceof IBlockState s) return Optional.ofNullable(StatesBridge1122.toModern(s));
        throw new IllegalArgumentException("Not a 1.12.2 IBlockState: " + legacyState);
    }

    // ------------------------------------------------------------------ tiles and worlds

    public static Optional<Object> tileOf(BlockEntity be) {
        if (be instanceof LegacyBlockEntity l) return Optional.ofNullable(l.legacy());
        if (be instanceof Legacy1122BlockEntity l) return Optional.ofNullable(l.legacy());
        return Optional.empty();
    }

    public static Optional<LegacyVersion> tileVersion(BlockEntity be) {
        if (be instanceof LegacyBlockEntity) return Optional.of(LegacyVersion.V1_7_10);
        if (be instanceof Legacy1122BlockEntity) return Optional.of(LegacyVersion.V1_12_2);
        return Optional.empty();
    }

    public static Object worldToLegacy(Level level, LegacyVersion version) {
        return switch (version) {
            case V1_7_10 -> legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            case V1_12_2 -> WorldBridge1122.wrap(level);
        };
    }
}
