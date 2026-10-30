package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** 1.12.2 multipart condition ({@code when} of a blockstate part): which block states a part applies to. */
public interface ICondition {
    ICondition TRUE = blockState -> Predicates.alwaysTrue();
    ICondition FALSE = blockState -> Predicates.alwaysFalse();

    Predicate<IBlockState> getPredicate(BlockStateContainer blockState);
}
