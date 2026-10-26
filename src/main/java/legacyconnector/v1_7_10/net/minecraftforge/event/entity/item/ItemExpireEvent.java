package legacyconnector.v1_7_10.net.minecraftforge.event.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;

/** An item on the ground is about to despawn; cancel it and set {@link #extraLife} (ticks) to keep it longer. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class ItemExpireEvent extends ItemEvent {
    public int extraLife;

    public ItemExpireEvent(EntityItem entityItem, int extraLife) {
        super(entityItem);
        this.extraLife = extraLife;
    }
}
