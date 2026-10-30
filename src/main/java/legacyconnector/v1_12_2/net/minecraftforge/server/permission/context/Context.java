package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A context without player or world: only the values put into it. */
public class Context implements IContext {
    private Map<ContextKey<?>, Object> map;

    @Override
    public World getWorld() {
        return null;
    }

    @Override
    public EntityPlayer getPlayer() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ContextKey<T> key) {
        return map == null || map.isEmpty() ? null : (T) map.get(key);
    }

    @Override
    public boolean has(ContextKey<?> key) {
        return map != null && !map.isEmpty() && map.containsKey(key);
    }

    /** Whether the key is one this kind of context fills itself (it may not be set). */
    protected boolean covers(ContextKey<?> key) {
        return false;
    }

    public <T> Context set(ContextKey<T> key, T obj) {
        if (covers(key)) return this;
        if (map == null) map = new HashMap<>();
        map.put(key, obj);
        return this;
    }
}
