package legacyconnector.v1_20_1.net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.Event;

/** Forge 1.20.1's despawn check event (replaced by Mob#checkDespawn hooks); a stand-in that is not fired. */
public class MobSpawnEvent$AllowDespawn extends Event {
    private final Mob entity;
    private final ServerLevelAccessor level;

    public MobSpawnEvent$AllowDespawn(Mob mob, ServerLevelAccessor level) {
        this.entity = mob;
        this.level = level;
    }

    public Mob getEntity() {
        return entity;
    }

    public ServerLevelAccessor getLevel() {
        return level;
    }
}
