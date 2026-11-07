package org.kylepitcock.legacyconnector.compat.patchouli;

import java.util.function.Consumer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IMultiblock;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IStateMatcher;

/** A 1.12 multiblock that is a view of a modern Patchouli multiblock. */
public final class Multiblock1122 implements IMultiblock {
    final vazkii.patchouli.api.IMultiblock modern;

    Multiblock1122(vazkii.patchouli.api.IMultiblock modern) {
        this.modern = modern;
    }

    static IMultiblock view(vazkii.patchouli.api.IMultiblock modern) {
        return modern == null ? null : new Multiblock1122(modern);
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
        modern.offset(x, y, z);
        return this;
    }

    @Override
    public IMultiblock offsetView(int x, int y, int z) {
        modern.offsetView(x, y, z);
        return this;
    }

    @Override
    public IMultiblock setSymmetrical(boolean symmetrical) {
        modern.setSymmetrical(symmetrical);
        return this;
    }

    @Override
    public IMultiblock setResourceLocation(ResourceLocation res) {
        modern.setId(res.mcc$modern());
        return this;
    }

    @Override
    public boolean isSymmetrical() {
        return modern.isSymmetrical();
    }

    @Override
    public void place(World world, BlockPos pos, Rotation rotation) {
        modern.place(world.mcc$level(), pos.mcc$modern(), rotation(rotation));
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
        var result = modern.simulate(world.mcc$level(), pos.mcc$modern(), rotation(rotation), false);
        net.minecraft.core.BlockPos anchor = result.getFirst();
        for (var r : result.getSecond()) {
            if (r.getCharacter() == null || r.getCharacter() != c) continue;
            net.minecraft.core.BlockPos at = r.getWorldPosition();
            net.minecraft.core.BlockPos rel = at.subtract(anchor);
            if (!action.accepts(BlockPos.mcc$of(at), BlockPos.mcc$of(rel), rel.getX(), rel.getY(), rel.getZ(), c, Matchers1122.view(r.getStateMatcher()))) return false;
        }
        return true;
    }

    @Override
    public boolean validate(World world, BlockPos pos) {
        return modern.validate(world.mcc$level(), pos.mcc$modern()) != null;
    }

    @Override
    public boolean test(World world, BlockPos start, int x, int y, int z, Rotation rotation) {
        return modern.test(world.mcc$level(), start.mcc$modern(), x, y, z, rotation(rotation));
    }
}
