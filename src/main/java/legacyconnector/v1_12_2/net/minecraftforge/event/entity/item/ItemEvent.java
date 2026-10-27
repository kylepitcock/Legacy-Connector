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

/** An event about a dropped item. */
public class ItemEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityEvent {
    private final legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entityItem;

    public ItemEvent(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem item) {
        super(item);
        this.entityItem = item;
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem getEntityItem() {
        return entityItem;
    }
}
