package legacyconnector.v1_12_2.net.minecraft.block.state.pattern;

import com.google.common.base.Predicate;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** Matches the states of one block (ore generation's "replace stone", block patterns). */
public class BlockMatcher implements Predicate<IBlockState> {
    private final Block block;

    private BlockMatcher(Block block) {
        this.block = block;
    }

    public static BlockMatcher forBlock(Block block) {
        return new BlockMatcher(block);
    }

    @Override
    public boolean apply(IBlockState state) {
        return state != null && state.getBlock() == block;
    }
}
