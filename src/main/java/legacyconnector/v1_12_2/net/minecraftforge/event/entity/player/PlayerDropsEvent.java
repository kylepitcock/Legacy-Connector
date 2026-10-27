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

/** A dying player's drops (cancel to drop nothing). */
@Cancelable
public class PlayerDropsEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDropsEvent {
    private final EntityPlayer entityPlayer;

    public PlayerDropsEvent(EntityPlayer entity, DamageSource source, java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem> drops, boolean recentlyHit) {
        super(entity, source, drops, 0, recentlyHit);
        this.entityPlayer = entity;
    }

    public EntityPlayer getEntityPlayer() {
        return entityPlayer;
    }
}
