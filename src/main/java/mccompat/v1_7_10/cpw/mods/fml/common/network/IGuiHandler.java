package mccompat.v1_7_10.cpw.mods.fml.common.network;

import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.world.World;

public interface IGuiHandler {
    Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z);

    Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z);
}
