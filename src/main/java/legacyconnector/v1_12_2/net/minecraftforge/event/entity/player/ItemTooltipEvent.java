package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Client: the tooltip lines of a stack (the list can be changed). */
public class ItemTooltipEvent extends PlayerEvent {
    private final ItemStack itemStack;
    private final java.util.List<String> toolTip;
    /** The modern tooltip flag (advanced tooltips with F3+H). */
    public final boolean mcc$advanced;

    public ItemTooltipEvent(ItemStack itemStack, EntityPlayer player, java.util.List<String> toolTip, boolean advanced) {
        super(player);
        this.itemStack = itemStack;
        this.toolTip = toolTip;
        this.mcc$advanced = advanced;
    }

    /** Whether advanced tooltips (F3+H) are on. */
    public legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag getFlags() {
        return legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag.mcc$of(mcc$advanced);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public java.util.List<String> getToolTip() {
        return toolTip;
    }
}
