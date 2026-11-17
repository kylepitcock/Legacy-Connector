package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.20.1 shape of {@code BlockSource} (an interface: {@code Position} plus these four getters). In 1.21.1 {@code BlockSource} is a record, so a mod class that
 * implemented it is rewritten by the loader (ClassShape1201) to extend the record (access transformer {@code public-f}) and to implement this interface, which
 * carries the mod's own answers; the record's accessors ({@code level()}, {@code pos()}, {@code state()}, {@code blockEntity()}, {@code center()}) are bridged to
 * them, so the game and the translated callers see one consistent source (see {@code MiscCompat.sourcePos} and friends).
 */
public interface LegacyBlockSource {
    double x();

    double y();

    double z();

    BlockPos getPos();

    BlockState getBlockState();

    BlockEntity getEntity();

    ServerLevel getLevel();
}
