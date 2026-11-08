package legacyconnector.vb1_7_3.net.minecraft.client.gui.hud;

/** Original chat calls target the player participating in the current legacy callback. */
public class InGameHud {
    public void addChatMessage(String message){var player=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance());if(player!=null)player.sendMessage(message);}
}
