package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.chat.Component;

/** Original player feedback stays addressed to its native actor and retains translation-key semantics. */
public final class PlayerMessagesB173 {
    private PlayerMessagesB173(){}
    static Component component(String message){
        if(message==null)return Component.empty();
        String localized=ContentB173.translate(message);
        if(!localized.equals(message))return Component.literal(localized);
        String key=switch(message){
            case "tile.bed.occupied"->"block.minecraft.bed.occupied";
            case "tile.bed.noSleep"->"block.minecraft.bed.no_sleep";
            default->message;
        };
        // An unknown translation key naturally displays its original text, including ordinary chat sentences.
        return Component.translatable(key);
    }
    public static void send(PlayerEntity actor,String message){
        if(message!=null&&actor.mcc$modern instanceof net.minecraft.world.entity.player.Player player)
            player.displayClientMessage(component(message),false);
    }
}
