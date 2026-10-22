package legacyconnector.v1_7_10.mcp.mobius.waila.api;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public interface IWailaEntityProvider {
    Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaHead(Entity entity, List<String> tip, IWailaEntityAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaBody(Entity entity, List<String> tip, IWailaEntityAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaTail(Entity entity, List<String> tip, IWailaEntityAccessor accessor, IWailaConfigHandler config);

    NBTTagCompound getNBTData(EntityPlayerMP player, Entity entity, NBTTagCompound tag, World world);
}
