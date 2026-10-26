package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityXPOrb;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player touches an experience orb; cancel it to leave the orb. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class PlayerPickupXpEvent extends PlayerEvent {
    public final EntityXPOrb orb;

    public PlayerPickupXpEvent(EntityPlayer player, EntityXPOrb orb) {
        super(player);
        this.orb = orb;
    }
}
