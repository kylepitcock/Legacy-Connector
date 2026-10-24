package legacyconnector.v1_7_10.net.minecraftforge.event.entity.item;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player threw an item out of their inventory. */
@Cancelable
public class ItemTossEvent extends ItemEvent {
    public final EntityPlayer player;

    public ItemTossEvent(EntityItem item, EntityPlayer player) {
        super(item);
        this.player = player;
    }
}
