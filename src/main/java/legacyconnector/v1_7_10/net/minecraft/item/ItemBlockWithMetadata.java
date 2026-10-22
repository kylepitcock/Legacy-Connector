package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

public class ItemBlockWithMetadata extends ItemBlock {
    private final Block field_150950_b;

    public ItemBlockWithMetadata(Block block, Block iconBlock) {
        super(block);
        this.field_150950_b = iconBlock;
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return field_150950_b.getIcon(2, damage);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }
}
