package mccompat.v1_7_10.net.minecraft.entity.passive;

import mccompat.v1_7_10.net.minecraft.entity.EntityAgeable;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy ocelot. Modern ocelots/cats use the goal system, which legacy AI lists cannot drive yet, so no modern
 * entity is presented as this class; mod code that looks for ocelots simply finds none.
 */
public class EntityOcelot extends EntityTameable {

    public EntityOcelot(mccompat.v1_7_10.net.minecraft.world.World world) {
        super(world);
        // The vanilla task list, in vanilla order: mod subclasses remove and replace entries by index.
        tasks.addTask(1, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(2, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAISit(this));
        tasks.addTask(3, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAITempt(this, 0.6, mccompat.v1_7_10.net.minecraft.init.Items.fish, true));
        tasks.addTask(4, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIAvoidEntity(this,
                mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, 16.0F, 0.8, 1.33));
        tasks.addTask(5, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIFollowOwner(this, 1.0, 10.0F, 5.0F));
        tasks.addTask(6, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIOcelotSit(this, 1.33));
        tasks.addTask(7, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAILeapAtTarget(this, 0.3F));
        tasks.addTask(8, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIOcelotAttack(this));
        tasks.addTask(9, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIMate(this, 0.8));
        tasks.addTask(10, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIWander(this, 0.8));
        tasks.addTask(11, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAIWatchClosest(this,
                mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, 10.0F));
        // Vanilla hunted chickens while untamed; the chicken class is not part of the shim, so nothing matches.
        targetTasks.addTask(1, new mccompat.v1_7_10.net.minecraft.entity.ai.EntityAITargetNonTamed(this, Void.class, 750, false));
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    protected EntityOcelot(LivingEntity modern) {
        super(modern);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return null;
    }
}
