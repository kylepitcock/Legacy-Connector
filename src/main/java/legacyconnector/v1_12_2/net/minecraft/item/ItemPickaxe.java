package legacyconnector.v1_12_2.net.minecraft.item;

import com.google.common.collect.Sets;

public class ItemPickaxe extends ItemTool {
    private static final java.util.Set<legacyconnector.v1_12_2.net.minecraft.block.Block> EFFECTIVE_ON = Sets.newHashSet();

    public ItemPickaxe(ToolMaterial material) {
        super(1.0F, -2.8F, material, EFFECTIVE_ON);
    }

    /** Vanilla: a pickaxe harvests the blocks it mines when its material's level reaches the block's (stone 1, iron 2, diamond 3). */
    @Override
    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        try {
            return mcc$effective("pickaxe", state) && (toolMaterial == null ? 0 : toolMaterial.getHarvestLevel()) >= mcc$requiredLevel(state);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
