package legacyconnector.v1_12_2.net.minecraft.entity.boss.dragon.phase;

import legacyconnector.v1_12_2.net.minecraft.entity.MultiPartEntityPart;
import legacyconnector.v1_12_2.net.minecraft.entity.boss.EntityDragon;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** The ender dragon's phase manager: a view of the modern dragon's (a dragon without one stays in its holding pattern). */
public class PhaseManager {
    private final EntityDragon dragon;

    public PhaseManager(EntityDragon dragon) {
        this.dragon = dragon;
    }

    private net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager mcc$modern() {
        return dragon != null && dragon.mcc$entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon d ? d.getPhaseManager() : null;
    }

    public void setPhase(PhaseList<?> phase) {
        var m = mcc$modern();
        if (m != null && phase != null) m.setPhase(phase.mcc$modern());
    }

    public IPhase getCurrentPhase() {
        var m = mcc$modern();
        return m == null ? null : view(m.getCurrentPhase());
    }

    @SuppressWarnings("unchecked")
    public <T extends IPhase> T getPhase(PhaseList<T> phase) {
        var m = mcc$modern();
        return m == null || phase == null ? null : (T) view(m.getPhase(phase.mcc$modern()));
    }

    private IPhase view(net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance p) {
        return p == null ? null : new ModernPhase(p, dragon);
    }

    /** A legacy phase over a modern phase instance. */
    private record ModernPhase(net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance modern, EntityDragon dragon) implements IPhase {
        @Override
        public boolean getIsStationary() {
            return modern.isSitting();
        }

        @Override
        public void doClientRenderEffects() {
            modern.doClientTick();
        }

        @Override
        public void doLocalUpdate() {
            modern.doServerTick();
        }

        @Override
        public void onCrystalDestroyed(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityEnderCrystal crystal,
                legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, DamageSource source, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
            if (!(crystal != null && crystal.mcc$entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal c) || pos == null) return;
            var level = c.level();
            modern.onCrystalDestroyed(c, pos.mcc$modern(), source == null ? level.damageSources().generic() : source.mcc$modern(level),
                    player == null ? null : player.mcc$player());
        }

        @Override
        public void initPhase() {
            modern.begin();
        }

        @Override
        public void removeAreaEffect() {
            modern.end();
        }

        @Override
        public float getMaxRiseOrFall() {
            return modern.getFlySpeed();
        }

        @Override
        public float getYawFactor() {
            return modern.getTurnSpeed();
        }

        @Override
        public PhaseList<? extends IPhase> getType() {
            return PhaseList.mcc$of(modern.getPhase());
        }

        @Override
        public Vec3d getTargetLocation() {
            var v = modern.getFlyTargetLocation();
            return v == null ? null : new Vec3d(v.x, v.y, v.z);
        }

        @Override
        public float getAdjustedDamage(MultiPartEntityPart part, DamageSource source, float damage) {
            var level = dragon == null || dragon.mcc$entity == null ? null : dragon.mcc$entity.level();
            return source == null || level == null ? damage : modern.onHurt(source.mcc$modern(level), damage);
        }
    }
}
