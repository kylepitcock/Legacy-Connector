package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface ISleepHandler {
    /** A player tries to sleep at the bed; a non-null status (e.g. NOT_POSSIBLE_HERE) refuses it, null leaves the game's checks. */
    legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.EnumStatus sleepInBedAt(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int x, int y, int z);
}
