package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;

/** A player is about to earn an achievement; cancel it to withhold it. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class AchievementEvent extends PlayerEvent {
    public final Achievement achievement;

    public AchievementEvent(EntityPlayer player, Achievement achievement) {
        super(player);
        this.achievement = achievement;
    }
}
