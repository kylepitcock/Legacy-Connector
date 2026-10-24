package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;

/** The item form of an anvil: the damage value picks how damaged the placed anvil is (stored in the high bits of the metadata). */
public class ItemAnvilBlock extends ItemBlock {
    public ItemAnvilBlock(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage << 2;
    }
}
