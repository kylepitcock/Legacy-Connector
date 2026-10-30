package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** A player acting on an entity. */
public class TargetContext extends PlayerContext {
    private final Entity target;

    public TargetContext(EntityPlayer ep, Entity entity) {
        super(ep);
        this.target = entity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ContextKey<T> key) {
        return key.equals(ContextKeys.TARGET) ? (T) target : super.get(key);
    }

    @Override
    protected boolean covers(ContextKey<?> key) {
        return key.equals(ContextKeys.TARGET);
    }

    @Override
    public boolean has(ContextKey<?> key) {
        return target != null && covers(key) || super.has(key);
    }
}
