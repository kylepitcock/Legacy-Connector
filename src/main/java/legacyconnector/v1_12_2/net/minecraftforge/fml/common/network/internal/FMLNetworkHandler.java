package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** FML's network internals mods call directly: opening a mod's GUI for a player (as {@code EntityPlayer.openGui}). */
public class FMLNetworkHandler {
    public static final int READ_TIMEOUT = 30;
    public static final int LOGIN_TIMEOUT = 600;

    private FMLNetworkHandler() {
    }

    public static void openGui(EntityPlayer entityPlayer, Object mod, int modGuiId, World world, int x, int y, int z) {
        if (entityPlayer != null) entityPlayer.openGui(mod, modGuiId, world, x, y, z);
    }
}
