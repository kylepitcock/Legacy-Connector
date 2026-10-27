package legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Changes (or drops, by returning null) each block of a template as it is placed. */
public interface ITemplateProcessor {
    Template.BlockInfo processBlock(World worldIn, BlockPos pos, Template.BlockInfo blockInfoIn);
}
