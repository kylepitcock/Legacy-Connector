package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2 anvil item: one item per damage stage (intact, slightly and very damaged), its metadata shifted into the block's. */
public class ItemAnvilBlock extends ItemMultiTexture {
    public ItemAnvilBlock(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        super(block, block, new String[] {"intact", "slightlyDamaged", "veryDamaged"});
    }

    @Override
    public int getMetadata(int damage) {
        return damage << 2;
    }
}
