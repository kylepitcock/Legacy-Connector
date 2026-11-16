package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IMinecartHandler {
    void onMinecartUpdate(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, int x, int y, int z);

    void onMinecartEntityCollision(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity);

    boolean onMinecartInteract(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart cart, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, boolean doInteraction);
}
