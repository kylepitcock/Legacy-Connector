package legacyconnector.v1_7_10.codechicken.nei.api;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Names the block under the cursor for NEI's highlight tooltip (the item stack and the text lines). */
public interface IHighlightHandler {
    ItemStack identifyHighlight(World world, EntityPlayer player, MovingObjectPosition mop);

    List<String> handleTextData(ItemStack itemStack, World world, EntityPlayer player, MovingObjectPosition mop, List<String> currenttip, ItemInfo.Layout layout);
}
