package legacyconnector.v1_12_2.vazkii.patchouli.api;

import java.util.function.Predicate;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** What a multiblock position shows and accepts (Patchouli 1.12 API). */
public interface IStateMatcher {
    IBlockState getDisplayedState();

    Predicate<IBlockState> getStatePredicate();
}
