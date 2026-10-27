package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.entity.EntityQueries1122;

/** Targets the nearest suitable entity of a class within follow range (one chance in {@code targetChance} each check). */
public class EntityAINearestAttackableTarget<T extends EntityLivingBase> extends EntityAITarget {
    protected final Class<T> targetClass;
    private final int targetChance;
    protected final Sorter sorter;
    protected final com.google.common.base.Predicate<? super T> targetEntitySelector;
    protected T targetEntity;

    public EntityAINearestAttackableTarget(EntityCreature creature, Class<T> classTarget, boolean checkSight) {
        this(creature, classTarget, checkSight, false);
    }

    public EntityAINearestAttackableTarget(EntityCreature creature, Class<T> classTarget, boolean checkSight, boolean onlyNearby) {
        this(creature, classTarget, 10, checkSight, onlyNearby, null);
    }

    public EntityAINearestAttackableTarget(EntityCreature creature, Class<T> classTarget, int chance, boolean checkSight, boolean onlyNearby,
            com.google.common.base.Predicate<? super T> targetSelector) {
        super(creature, checkSight, onlyNearby);
        this.targetClass = classTarget;
        this.targetChance = chance;
        this.sorter = new Sorter(creature);
        setMutexBits(1);
        this.targetEntitySelector = targetSelector;
    }

    @Override
    public boolean shouldExecute() {
        if (targetChance > 0 && taskOwner.getRNG().nextInt(targetChance) != 0) return false;
        var level = taskOwner.mcc$entity == null ? null : taskOwner.mcc$entity.level();
        if (level == null) return false;
        java.util.function.Predicate<T> test = e -> (targetEntitySelector == null || targetEntitySelector.apply(e)) && isSuitableTarget(e, false);
        if (targetClass == EntityPlayer.class || targetClass == legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP.class) {
            double d = getTargetDistance();
            @SuppressWarnings("unchecked")
            T p = (T) EntityQueries1122.nearestPlayer(level, taskOwner.mcc$x(), taskOwner.mcc$y() + taskOwner.getEyeHeight(), taskOwner.mcc$z(), d,
                    e -> targetClass.isInstance(e) && test.test(targetClass.cast(e)));
            targetEntity = p;
            return targetEntity != null;
        }
        java.util.List<T> list = EntityQueries1122.within(level, getTargetableArea(getTargetDistance()), targetClass, taskOwner, test);
        if (list.isEmpty()) return false;
        list.sort(sorter);
        targetEntity = list.get(0);
        return true;
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getTargetableArea(double targetDistance) {
        return taskOwner.getEntityBoundingBox().grow(targetDistance, 4.0D, targetDistance);
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(targetEntity);
        super.startExecuting();
    }

    /** Orders entities by their distance to {@code entity}, nearest first. */
    public static class Sorter implements java.util.Comparator<Entity> {
        private final Entity entity;

        public Sorter(Entity entity) {
            this.entity = entity;
        }

        @Override
        public int compare(Entity a, Entity b) {
            return Double.compare(entity.getDistanceSq(a), entity.getDistanceSq(b));
        }
    }
}
