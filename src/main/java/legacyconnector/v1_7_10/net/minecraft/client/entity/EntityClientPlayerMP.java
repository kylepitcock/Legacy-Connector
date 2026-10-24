package legacyconnector.v1_7_10.net.minecraft.client.entity;

import net.minecraft.world.entity.player.Player;

/** The local player as 1.7.10 code sees it ({@code Minecraft.getMinecraft().thePlayer}). */
public class EntityClientPlayerMP extends EntityPlayerSP {
    public EntityClientPlayerMP(Player modern) {
        super(modern);
    }

    public void sendChatMessage(String message) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null) return;
        if (message.startsWith("/")) connection.sendCommand(message.substring(1));
        else connection.sendChat(message);
    }

    public float getSwingProgress(float partial) {
        return mcc$player().getAttackAnim(partial);
    }
}
