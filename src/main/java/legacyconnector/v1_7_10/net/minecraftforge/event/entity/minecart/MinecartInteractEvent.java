package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player right-clicks a minecart; cancelling stops the default (boarding). */
@Cancelable
public class MinecartInteractEvent extends MinecartEvent {
    public final EntityPlayer player;

    public MinecartInteractEvent(EntityMinecart minecart, EntityPlayer player) {
        super(minecart);
        this.player = player;
    }
}
