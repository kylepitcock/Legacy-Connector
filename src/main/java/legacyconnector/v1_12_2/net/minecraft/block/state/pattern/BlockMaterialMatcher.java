package legacyconnector.v1_12_2.net.minecraft.block.state.pattern;

import com.google.common.base.Predicate;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** Matches the states of one material (the air around a golem pattern). */
public class BlockMaterialMatcher implements Predicate<IBlockState> {
    private final Material material;

    private BlockMaterialMatcher(Material materialIn) {
        this.material = materialIn;
    }

    public static BlockMaterialMatcher forMaterial(Material materialIn) {
        return new BlockMaterialMatcher(materialIn);
    }

    @Override
    public boolean apply(IBlockState state) {
        return state != null && state.getMaterial() == material;
    }
}
