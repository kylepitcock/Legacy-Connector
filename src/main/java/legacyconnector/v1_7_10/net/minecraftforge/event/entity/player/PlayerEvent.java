package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingEvent;

public class PlayerEvent extends LivingEvent {
    public final EntityPlayer entityPlayer;

    public PlayerEvent(EntityPlayer player) {
        super(player);
        this.entityPlayer = player;
    }
}
