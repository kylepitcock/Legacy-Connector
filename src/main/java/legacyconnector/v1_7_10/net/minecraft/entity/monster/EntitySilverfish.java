package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.EntityDamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 silverfish: chases players within 8 blocks and bites them; when hurt it wakes the silverfish hidden in stone around it
 * after {@link #allySummonCooldown} ticks. A plain silverfish is the modern one; mods' (Nether Ores' hellfish) run here.
 */
public class EntitySilverfish extends EntityMob {
    public int allySummonCooldown;

    /** View of a modern silverfish. */
    public EntitySilverfish(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntitySilverfish(World world) {
        super(world);
        setSize(0.3F, 0.7F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(8.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.6000000238418579D);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0D);
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected Entity findPlayerToAttack() {
        return mcc$world() == null ? null : mcc$world().getClosestVulnerablePlayerToEntity(this, 8.0D);
    }

    @Override
    protected String getLivingSound() {
        return "mob.silverfish.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.silverfish.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.silverfish.kill";
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable()) return false;
        if (allySummonCooldown <= 0 && (source instanceof EntityDamageSource || source == DamageSource.magic)) allySummonCooldown = 20;
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected void attackEntity(Entity target, float distance) {
        if (attackTime <= 0 && distance < 1.2F && target.mcc$entity != null && mcc$entity != null
                && target.mcc$entity.getBoundingBox().maxY > mcc$entity.getBoundingBox().minY
                && target.mcc$entity.getBoundingBox().minY < mcc$entity.getBoundingBox().maxY) {
            attackTime = 20;
            attackEntityAsMob(target);
        }
    }

    /** Counts the summon cooldown down; at the end every infested block within 5 (2 up/down) breaks open into a silverfish. */
    @Override
    protected void updateEntityActionState() {
        super.updateEntityActionState();
        if (mcc$world() == null || mcc$world().isRemote || allySummonCooldown <= 0 || --allySummonCooldown != 0) return;
        if (!(mcc$entity != null && mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var center = mcc$entity.blockPosition();
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(center.offset(-5, -2, -5), center.offset(5, 2, 5))) {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.InfestedBlock infested)) continue;
            level.setBlock(pos, infested.hostStateByInfested(state), 3);
            var fish = net.minecraft.world.entity.EntityType.SILVERFISH.create(level);
            if (fish == null) continue;
            fish.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(fish);
            fish.spawnAnim();
        }
    }

    @Override
    protected boolean isValidLightLevel() {
        return true;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute.ARTHROPOD;
    }
}
