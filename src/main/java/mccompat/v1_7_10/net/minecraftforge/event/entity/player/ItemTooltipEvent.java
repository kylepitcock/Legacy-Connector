package mccompat.v1_7_10.net.minecraftforge.event.entity.player;

import java.util.List;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;

public class ItemTooltipEvent extends PlayerEvent {
    public final boolean showAdvancedItemTooltips;
    public final ItemStack itemStack;
    public final List<String> toolTip;

    public ItemTooltipEvent(ItemStack itemStack, EntityPlayer player, List<String> toolTip, boolean advanced) {
        super(player);
        this.itemStack = itemStack;
        this.toolTip = toolTip;
        this.showAdvancedItemTooltips = advanced;
    }
}
