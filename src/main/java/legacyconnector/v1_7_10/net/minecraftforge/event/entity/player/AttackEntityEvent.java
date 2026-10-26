package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player attacks an entity; cancel it to stop the attack. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class AttackEntityEvent extends PlayerEvent {
    public final Entity target;

    public AttackEntityEvent(EntityPlayer player, Entity target) {
        super(player);
        this.target = target;
    }
}
