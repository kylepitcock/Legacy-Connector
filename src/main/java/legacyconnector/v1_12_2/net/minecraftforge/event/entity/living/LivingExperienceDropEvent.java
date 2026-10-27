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

/** The experience a dying entity drops (cancel for none). */
@Cancelable
public class LivingExperienceDropEvent extends LivingEvent {
    private final EntityPlayer attackingPlayer;
    private final int originalExperience;

    public LivingExperienceDropEvent(EntityLivingBase entity, EntityPlayer attackingPlayer, int originalExperience) {
        super(entity);
        this.attackingPlayer = attackingPlayer;
        this.originalExperience = originalExperience;
    }

    public EntityPlayer getAttackingPlayer() {
        return attackingPlayer;
    }

    public int getOriginalExperience() {
        return originalExperience;
    }

    private int droppedExperience = -1;

    public int getDroppedExperience() {
        return droppedExperience < 0 ? getOriginalExperience() : droppedExperience;
    }

    public void setDroppedExperience(int droppedExperience) {
        this.droppedExperience = droppedExperience;
    }
}
