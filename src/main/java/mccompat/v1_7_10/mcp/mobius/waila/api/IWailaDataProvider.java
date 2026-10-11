package mccompat.v1_7_10.mcp.mobius.waila.api;

import java.util.List;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.world.World;

public interface IWailaDataProvider {
    ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaHead(ItemStack stack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaBody(ItemStack stack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaTail(ItemStack stack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config);

    NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z);
}
