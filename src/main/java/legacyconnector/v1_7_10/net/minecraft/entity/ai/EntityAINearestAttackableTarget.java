package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import java.util.Comparator;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/** Targets the nearest suitable entity of a legacy class within follow range (every {@code targetChance} ticks). */
public class EntityAINearestAttackableTarget extends EntityAITarget {
    private final Class<?> targetClass;
    private final int targetChance;
    private final Object selector;
    private EntityLivingBase targetEntity;

    public EntityAINearestAttackableTarget(EntityCreature owner, Class<?> targetClass, int chance, boolean checkSight) {
        this(owner, targetClass, chance, checkSight, false);
    }

    public EntityAINearestAttackableTarget(EntityCreature owner, Class<?> targetClass, int chance, boolean checkSight, boolean nearbyOnly) {
        this(owner, targetClass, chance, checkSight, nearbyOnly, null);
    }

    public EntityAINearestAttackableTarget(EntityCreature owner, Class<?> targetClass, int chance, boolean checkSight, boolean nearbyOnly, Object selector) {
        super(owner, checkSight, nearbyOnly);
        this.targetClass = targetClass;
        this.targetChance = chance;
        this.selector = selector;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (targetChance > 0 && taskOwner.getRNG().nextInt(targetChance) != 0) return false;
        if (!(taskOwner.mcc$entity instanceof LivingEntity self)) return false;
        double range = getTargetDistance();
        var box = self.getBoundingBox().inflate(range, 4.0, range);
        targetEntity = self.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != self).stream()
                .sorted(Comparator.comparingDouble(self::distanceToSqr))
                .map(e -> EntityBridge.wrap(e))
                .filter(e -> targetClass == null || targetClass.isInstance(e))
                .filter(e -> e instanceof EntityLivingBase)
                .map(e -> (EntityLivingBase) e)
                .filter(e -> selects(e) && isSuitableTarget(e, false))
                .findFirst().orElse(null);
        return targetEntity != null;
    }

    private boolean selects(Entity e) {
        if (selector == null) return true;
        try {
            var m = selector.getClass().getMethod("isEntityApplicable", Entity.class);
            return (Boolean) m.invoke(selector, e);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return true;
        }
    }

    @Override
    public void startExecuting() {
        taskOwner.setAttackTarget(targetEntity);
        super.startExecuting();
    }
}
