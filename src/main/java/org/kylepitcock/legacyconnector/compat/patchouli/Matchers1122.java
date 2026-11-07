package org.kylepitcock.legacyconnector.compat.patchouli;

import java.util.function.Predicate;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import vazkii.patchouli.api.TriPredicate;

/** 1.12 state matchers over the modern ones and back. */
final class Matchers1122 {
    private Matchers1122() {}

    /** A 1.12 matcher that is a view of a modern one. */
    static final class View implements legacyconnector.v1_12_2.vazkii.patchouli.api.IStateMatcher {
        final vazkii.patchouli.api.IStateMatcher modern;

        View(vazkii.patchouli.api.IStateMatcher modern) {
            this.modern = modern;
        }

        @Override
        public IBlockState getDisplayedState() {
            return StatesBridge1122.toLegacy(modern.getDisplayedState(0L));
        }

        @Override
        public Predicate<IBlockState> getStatePredicate() {
            TriPredicate<BlockGetter, BlockPos, BlockState> p = modern.getStatePredicate();
            return s -> p.test(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, StatesBridge1122.toModern(s));
        }
    }

    /** The modern matcher for a matcher a 1.12 mod implemented itself or got from the API. */
    static vazkii.patchouli.api.IStateMatcher modern(legacyconnector.v1_12_2.vazkii.patchouli.api.IStateMatcher legacy) {
        if (legacy == null) return null;
        if (legacy instanceof View v) return v.modern;
        return new vazkii.patchouli.api.IStateMatcher() {
            @Override
            public BlockState getDisplayedState(long ticks) {
                return StatesBridge1122.toModern(legacy.getDisplayedState());
            }

            @Override
            public TriPredicate<BlockGetter, BlockPos, BlockState> getStatePredicate() {
                Predicate<IBlockState> p = legacy.getStatePredicate();
                return (getter, pos, state) -> {
                    IBlockState s = StatesBridge1122.toLegacy(state);
                    return s != null && p.test(s);
                };
            }
        };
    }

    static legacyconnector.v1_12_2.vazkii.patchouli.api.IStateMatcher view(vazkii.patchouli.api.IStateMatcher modern) {
        return modern == null ? null : new View(modern);
    }
}
