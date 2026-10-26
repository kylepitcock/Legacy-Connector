package legacyconnector.v1_7_10.net.minecraft.world.storage;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** Reads and writes players' saved data ({@code playerdata/<uuid>.dat}). */
public interface IPlayerFileData {
    void writePlayerData(EntityPlayer player);

    NBTTagCompound readPlayerData(EntityPlayer player);

    String[] getAvailablePlayerDat();
}
