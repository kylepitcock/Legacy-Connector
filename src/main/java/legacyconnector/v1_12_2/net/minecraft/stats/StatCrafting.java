package legacyconnector.v1_12_2.net.minecraft.stats;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A statistic about an item or block (crafted, used, mined, broken, picked up, dropped). */
public class StatCrafting extends StatBase {
    private final Item item;

    public StatCrafting(String prefix, String suffix, ITextComponent statNameIn, Item itemIn) {
        super(prefix + suffix, statNameIn);
        this.item = itemIn;
    }

    public Item getItem() {
        return item;
    }
}
