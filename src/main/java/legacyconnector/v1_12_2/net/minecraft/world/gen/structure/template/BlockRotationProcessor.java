package legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's default template processor: drops blocks at random by the placement's integrity. */
public class BlockRotationProcessor implements ITemplateProcessor {
    private final float chance;
    private final Random random;

    public BlockRotationProcessor(BlockPos pos, PlacementSettings settings) {
        this.chance = settings.getIntegrity();
        this.random = settings.getRandom(pos);
    }

    @Override
    public Template.BlockInfo processBlock(World worldIn, BlockPos pos, Template.BlockInfo blockInfoIn) {
        return this.chance < 1.0F && this.random.nextFloat() > this.chance ? null : blockInfoIn;
    }
}
