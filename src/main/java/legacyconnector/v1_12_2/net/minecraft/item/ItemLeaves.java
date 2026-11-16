package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.BlockLeaves;

/** The item of a leaves block: one subtype per wood type, always the non-decaying (player placed) state. */
public class ItemLeaves extends ItemBlock {
    private final BlockLeaves leaves;

    public ItemLeaves(BlockLeaves block) {
        super(block);
        this.leaves = block;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage | 4;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + leaves.getWoodType(stack.getMetadata()).getUnlocalizedName();
    }
}
