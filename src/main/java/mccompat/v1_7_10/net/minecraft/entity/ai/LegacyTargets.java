package mccompat.v1_7_10.net.minecraft.entity.ai;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;

/** Legacy target classes (any legacy entity class) as predicates on modern entities. */
final class LegacyTargets {
    private LegacyTargets() {}

    static Predicate<LivingEntity> of(Class<?> legacyClass) {
        if (legacyClass == null) return e -> true;
        return e -> legacyClass.isInstance(EntityBridge.wrap(e));
    }

    /** The modern class to search for: players when the legacy class is a player class, else any living entity. */
    @SuppressWarnings("unchecked")
    static Class<LivingEntity> search(Class<?> legacyClass) {
        if (legacyClass != null && mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer.class.isAssignableFrom(legacyClass)) {
            return (Class<LivingEntity>) (Class<?>) net.minecraft.world.entity.player.Player.class;
        }
        return LivingEntity.class;
    }
}
