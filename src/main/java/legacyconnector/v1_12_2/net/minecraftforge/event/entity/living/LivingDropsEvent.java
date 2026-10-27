package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

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

/** A dying entity's drops (cancel to drop nothing; the list can be changed). */
@Cancelable
public class LivingDropsEvent extends LivingEvent {
    private final DamageSource source;
    private final java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem> drops;
    private final int lootingLevel;
    private final boolean recentlyHit;

    public LivingDropsEvent(EntityLivingBase entity, DamageSource source, java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem> drops, int lootingLevel, boolean recentlyHit) {
        super(entity);
        this.source = source;
        this.drops = drops;
        this.lootingLevel = lootingLevel;
        this.recentlyHit = recentlyHit;
    }

    public DamageSource getSource() {
        return source;
    }

    public java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem> getDrops() {
        return drops;
    }

    public int getLootingLevel() {
        return lootingLevel;
    }

    public boolean isRecentlyHit() {
        return recentlyHit;
    }
}
