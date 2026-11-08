package legacyconnector.vb1_7_3.net.minecraft.stat;

import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.vb1_7_3.AdvancementsB173;

/** Advancement queries share the same native progress that original award callbacks modify. */
public class PlayerStats {
    public boolean hasAchievement(Achievement achievement){
        var player=Minecraft.mcc$get$player(Minecraft.mcc$instance());
        if(player!=null&&player.mcc$modern instanceof net.minecraft.server.level.ServerPlayer nativePlayer){var advancement=nativePlayer.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));return advancement!=null&&nativePlayer.getAdvancements().getOrStartProgress(advancement).isDone();}
        return org.kylepitcock.legacyconnector.vb1_7_3.MenusB173.hasAchievement(AdvancementsB173.idOf(achievement));
    }
}
