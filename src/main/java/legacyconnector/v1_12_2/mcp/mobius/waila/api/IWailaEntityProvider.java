package legacyconnector.v1_12_2.mcp.mobius.waila.api;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

public interface IWailaEntityProvider {
    default Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    default List<String> getWailaHead(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    default List<String> getWailaBody(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    default List<String> getWailaTail(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    default NBTTagCompound getNBTData(EntityPlayerMP player, Entity ent, NBTTagCompound tag, World world) {
        return tag;
    }
}
