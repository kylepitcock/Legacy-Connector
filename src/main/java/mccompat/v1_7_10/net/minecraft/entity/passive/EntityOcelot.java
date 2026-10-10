package mccompat.v1_7_10.net.minecraft.entity.passive;

import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy ocelot. Modern ocelots/cats use the goal system, which legacy AI lists cannot drive yet, so no modern
 * entity is presented as this class; mod code that looks for ocelots simply finds none.
 */
public class EntityOcelot extends EntityLivingBase {
    public EntityAITasks tasks = new EntityAITasks();

    protected EntityOcelot(LivingEntity modern) {
        super(modern);
    }
}
