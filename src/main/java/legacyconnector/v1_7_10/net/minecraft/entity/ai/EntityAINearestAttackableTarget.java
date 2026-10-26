package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.command.IEntitySelector;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * Targets the nearest suitable entity of a legacy class within follow range (every {@code targetChance} ticks), optionally
 * filtered by an {@link IEntitySelector}.
 */
public class EntityAINearestAttackableTarget extends EntityAITarget {
    protected Class<?> targetClass;
    protected int targetChance;
    protected IEntitySelector targetEntitySelector;
    protected Sorter theNearestAttackableTargetSorter;
    protected EntityLivingBase targetEntity;

    public EntityAINearestAttackableTarget(EntityCreature owner, Class<?> targetClass, int chance, boolean checkSight) {
        this(owner, targetClass, chance, checkSight, false);
    }

    public EntityAINearestAttackableTarget(EntityCreature owner, Class<?> targetClass, int chance, boolean checkSight, boolean nearbyOnly) {
        this(owner, targetClass, chance, checkSight, nearbyOnly, null);
    }

    public EntityAINearestAttackableTarget(EntityCreature owner, Class<?> targetClass, int chance, boolean checkSight, boolean nearbyOnly,
            IEntitySelector selector) {
        super(owner, checkSight, nearbyOnly);
        this.targetClass = targetClass;
        this.targetChance = chance;
        this.targetEntitySelector = selector;
        this.theNearestAttackableTargetSorter = new Sorter(owner);
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (targetChance > 0 && taskOwner.getRNG().nextInt(targetChance) != 0) return false;
        if (!(taskOwner.mcc$entity instanceof LivingEntity self)) return false;
        double range = getTargetDistance();
        var box = self.getBoundingBox().inflate(range, 4.0, range);
        targetEntity = self.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != self).stream()
                .map(e -> EntityBridge.wrap(e))
                .filter(e -> targetClass == null || targetClass.isInstance(e))
                .filter(e -> e instanceof EntityLivingBase)
                .map(e -> (EntityLivingBase) e)
                .filter(e -> selects(e) && isSuitableTarget(e, false))
                .min(theNearestAttackableTargetSorter).orElse(null);
        return targetEntity != null;
    }

    private boolean selects(Entity e) {
        try {
            return targetEntitySelector == null || targetEntitySelector.isEntityApplicable(e);
        } catch (RuntimeException | LinkageError ex) {
            return false;
        }
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(targetEntity);
        super.startExecuting();
    }

    /** Orders entities by their distance to {@link #theEntity}, nearest first. */
    public static class Sorter implements java.util.Comparator<Entity> {
        public Entity theEntity;

        public Sorter(Entity entity) {
            this.theEntity = entity;
        }

        @Override
        public int compare(Entity a, Entity b) {
            return Double.compare(theEntity.getDistanceSqToEntity(a), theEntity.getDistanceSqToEntity(b));
        }
    }
}
