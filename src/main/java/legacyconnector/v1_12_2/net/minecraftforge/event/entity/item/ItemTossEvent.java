package legacyconnector.v1_12_2.net.minecraftforge.event.entity.item;

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

/** A player throws an item out (cancel to keep it from being spawned). */
@Cancelable
public class ItemTossEvent extends ItemEvent {
    private final EntityPlayer player;

    public ItemTossEvent(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem item, EntityPlayer player) {
        super(item);
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
