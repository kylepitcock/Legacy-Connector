package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173;

/** Registration and lookup entry points of the Beta PlayerAPI. */
public final class PlayerAPI {
    private PlayerAPI(){}
    public static void RegisterPlayerBase(Class<?> type){PlayerApiB173.register(type);}
    public static PlayerBase getPlayerBase(ClientPlayerEntity player,Class<?> type){return PlayerApiB173.get(player,type);}
}
