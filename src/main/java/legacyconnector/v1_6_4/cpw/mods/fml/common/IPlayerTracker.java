package legacyconnector.v1_6_4.cpw.mods.fml.common;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** FML 1.6's player life cycle callbacks; registered with {@code GameRegistry.registerPlayerTracker}. */
public interface IPlayerTracker {
    void onPlayerLogin(EntityPlayer player);

    void onPlayerLogout(EntityPlayer player);

    void onPlayerChangedDimension(EntityPlayer player);

    void onPlayerRespawn(EntityPlayer player);
}
