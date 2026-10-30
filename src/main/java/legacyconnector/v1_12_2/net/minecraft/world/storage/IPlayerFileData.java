package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** 1.12.2 storage of the players' saved data ({@code playerdata/<uuid>.dat}); the world's {@link SaveHandler} is one. */
public interface IPlayerFileData {
    /** Saves the player's data now. */
    void writePlayerData(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player);

    /** Reads the player's saved data into the player and returns it (null when the player has none). */
    legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound readPlayerData(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player);

    /** The UUIDs of the players with saved data. */
    String[] getAvailablePlayerDat();
}
