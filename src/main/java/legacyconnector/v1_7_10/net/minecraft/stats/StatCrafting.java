package legacyconnector.v1_7_10.net.minecraft.stats;

import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** A statistic counted per item (crafted, used, broken, mined). */
public class StatCrafting extends StatBase {
    private final Item field_150960_a;

    public StatCrafting(String id, IChatComponent name, Item item) {
        super(id, name);
        this.field_150960_a = item;
    }

    public Item func_150959_a() {
        return field_150960_a;
    }
}
