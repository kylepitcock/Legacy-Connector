package mccompat.v1_7_10.mcp.mobius.waila.api;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.world.World;

public interface IWailaEntityAccessor {
    World getWorld();

    EntityPlayer getPlayer();

    Entity getEntity();

    NBTTagCompound getNBTData();

    int getNBTInteger(NBTTagCompound tag, String key);

    double getPartialFrame();
}
