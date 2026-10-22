package legacyconnector.v1_7_10.net.minecraft.client.entity;

import net.minecraft.world.entity.player.Player;

/** The local player as 1.7.10 code sees it ({@code Minecraft.getMinecraft().thePlayer}). */
public class EntityClientPlayerMP extends EntityPlayerSP {
    public EntityClientPlayerMP(Player modern) {
        super(modern);
    }
}
