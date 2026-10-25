package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

/** A mod's GUI factory; the ids it knows are opened through {@code EntityPlayer.openGui}. Server/client elements arrive with the GUI bridge. */
public interface IGuiHandler {
    Object getServerGuiElement(int id, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, int x, int y, int z);

    Object getClientGuiElement(int id, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, int x, int y, int z);
}
