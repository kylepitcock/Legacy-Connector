package legacyconnector.v1_6_4.cpw.mods.fml.common;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** FML 1.6's item pickup callback; registered with {@code GameRegistry.registerPickupHandler} (see {@code Crafting164}). */
public interface IPickupNotifier {
    void notifyPickup(EntityItem item, EntityPlayer player);
}
