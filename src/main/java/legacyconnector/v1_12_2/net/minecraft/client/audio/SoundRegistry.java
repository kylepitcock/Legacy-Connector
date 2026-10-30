package legacyconnector.v1_12_2.net.minecraft.client.audio;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.registry.RegistrySimple;

/**
 * 1.12.2's sound event registry (sounds.json of every pack): a live view of the game's sound events, by name. Events a mod adds itself
 * ({@link #add}) are kept beside them.
 */
public class SoundRegistry extends RegistrySimple<ResourceLocation, SoundEventAccessor> {
    private static boolean client() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public void add(SoundEventAccessor accessor) {
        if (accessor != null && accessor.getLocation() != null) registryObjects.put(accessor.getLocation(), accessor);
    }

    public void clearMap() {
        registryObjects.clear();
    }

    @Override
    public SoundEventAccessor getObject(ResourceLocation name) {
        SoundEventAccessor own = registryObjects.get(name);
        if (own != null || name == null || !client()) return own;
        return SoundHandler.mcc$INSTANCE.getAccessor(name);
    }

    @Override
    public Set<ResourceLocation> getKeys() {
        Set<ResourceLocation> keys = new LinkedHashSet<>(registryObjects.keySet());
        if (client()) {
            for (var rl : net.minecraft.client.Minecraft.getInstance().getSoundManager().getAvailableSounds()) {
                keys.add(new ResourceLocation(rl.getNamespace(), rl.getPath()));
            }
        }
        return java.util.Collections.unmodifiableSet(keys);
    }

    @Override
    public boolean containsKey(ResourceLocation key) {
        return getObject(key) != null;
    }

    @Override
    public Iterator<SoundEventAccessor> iterator() {
        java.util.List<SoundEventAccessor> out = new java.util.ArrayList<>();
        for (ResourceLocation key : getKeys()) {
            SoundEventAccessor a = getObject(key);
            if (a != null) out.add(a);
        }
        return out.iterator();
    }
}
