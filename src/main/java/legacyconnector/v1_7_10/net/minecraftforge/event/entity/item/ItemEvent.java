package legacyconnector.v1_7_10.net.minecraftforge.event.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityEvent;

public class ItemEvent extends EntityEvent {
    public final EntityItem entityItem;

    public ItemEvent(EntityItem item) {
        super(item);
        this.entityItem = item;
    }
}
