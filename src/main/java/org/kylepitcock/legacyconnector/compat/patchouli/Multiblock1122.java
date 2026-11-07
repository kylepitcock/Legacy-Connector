package org.kylepitcock.legacyconnector.compat.patchouli;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IMultiblock;

/**
 * A 1.12 multiblock that is a view of a modern Patchouli multiblock, built on first use: a 1.12 mod makes its multiblocks in preInit,
 * before its blocks exist as modern blocks. Changes made before that (offset, symmetry, id) are replayed on the built one.
 */
public final class Multiblock1122 implements IMultiblock {
    private final Supplier<vazkii.patchouli.api.IMultiblock> factory;
    private final List<Consumer<vazkii.patchouli.api.IMultiblock>> pending = new ArrayList<>();
    private vazkii.patchouli.api.IMultiblock built;
    private boolean symmetrical;

    Multiblock1122(Supplier<vazkii.patchouli.api.IMultiblock> factory) {
        this.factory = factory;
    }

    static IMultiblock view(Supplier<vazkii.patchouli.api.IMultiblock> factory) {
        return new Multiblock1122(factory);
    }

    /** The modern multiblock (built now when it was not yet). */
    synchronized vazkii.patchouli.api.IMultiblock modern() {
        if (built == null) {
            built = factory.get();
            for (var change : pending) change.accept(built);
            pending.clear();
        }
        return built;
    }

    private synchronized void change(Consumer<vazkii.patchouli.api.IMultiblock> change) {
        if (built != null) change.accept(built);
        else pending.add(change);
    }

    static net.minecraft.world.level.block.Rotation rotation(Rotation r) {
        return switch (r == null ? Rotation.NONE : r) {
            case NONE -> net.minecraft.world.level.block.Rotation.NONE;
            case CLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
        };
    }

    @Override
    public IMultiblock offset(int x, int y, int z) {
        change(m -> m.offset(x, y, z));
        return this;
    }

    @Override
    public IMultiblock offsetView(int x, int y, int z) {
        change(m -> m.offsetView(x, y, z));
        return this;
    }

    @Override
    public IMultiblock setSymmetrical(boolean symmetrical) {
        this.symmetrical = symmetrical;
        change(m -> m.setSymmetrical(symmetrical));
        return this;
    }

    @Override
    public IMultiblock setResourceLocation(ResourceLocation res) {
        change(m -> m.setId(res.mcc$modern()));
        return this;
    }

    @Override
    public boolean isSymmetrical() {
        return symmetrical;
    }

    @Override
    public void place(World world, BlockPos pos, Rotation rotation) {
        modern().place(world.mcc$level(), pos.mcc$modern(), rotation(rotation));
    }

    @Override
    public void forEach(World world, BlockPos pos, Rotation rotation, char c, Consumer<BlockPos> action) {
        forEachMatcher(world, pos, rotation, c, (p, rel, x, y, z, ch, matcher) -> {
            action.accept(p);
            return true;
        });
    }

    @Override
    public boolean forEachMatcher(World world, BlockPos pos, Rotation rotation, char c, MatcherAcceptor action) {
        var result = modern().simulate(world.mcc$level(), pos.mcc$modern(), rotation(rotation), false);
        net.minecraft.core.BlockPos anchor = result.getFirst();
        for (var r : result.getSecond()) {
            if (r.getCharacter() == null || r.getCharacter() != c) continue;
            net.minecraft.core.BlockPos at = r.getWorldPosition();
            net.minecraft.core.BlockPos rel = at.subtract(anchor);
            var matcher = r.getStateMatcher();
            if (!action.accepts(BlockPos.mcc$of(at), BlockPos.mcc$of(rel), rel.getX(), rel.getY(), rel.getZ(), c, Matchers1122.view(() -> matcher))) return false;
        }
        return true;
    }

    @Override
    public boolean validate(World world, BlockPos pos) {
        return modern().validate(world.mcc$level(), pos.mcc$modern()) != null;
    }

    @Override
    public boolean test(World world, BlockPos start, int x, int y, int z, Rotation rotation) {
        return modern().test(world.mcc$level(), start.mcc$modern(), x, y, z, rotation(rotation));
    }
}
