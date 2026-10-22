package legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** FML's network entry points that mods call directly (GUI opening). */
public class FMLNetworkHandler {
    public static void openGui(EntityPlayer entityPlayer, Object mod, int modGuiId, World world, int x, int y, int z) {
        if (entityPlayer != null) entityPlayer.openGui(mod, modGuiId, world, x, y, z);
    }
}
