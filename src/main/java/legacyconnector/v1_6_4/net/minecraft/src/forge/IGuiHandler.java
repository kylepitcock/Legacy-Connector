package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x's GUI handler: one call for both sides (the Container on the server, the GuiScreen on the client). */
public interface IGuiHandler {
    Object getGuiElement(int id, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z);
}
