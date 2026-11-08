package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1's collection of the providers attached to one object: asks them in order, saves the serializable ones by name. */
public final class CapabilityDispatcher implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
    private final List<ICapabilityProvider> caps = new ArrayList<>();
    private final List<ICapabilitySerializable<Tag>> writers = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public CapabilityDispatcher(Map<ResourceLocation, ICapabilityProvider> list, List<Runnable> listeners, ICapabilityProvider parent) {
        if (parent != null) caps.add(parent);
        list.forEach((key, value) -> {
            caps.add(value);
            if (value instanceof ICapabilitySerializable<?> s) {
                writers.add((ICapabilitySerializable<Tag>) s);
                names.add(key.toString());
            }
        });
        this.listeners.addAll(listeners);
    }

    public CapabilityDispatcher(Map<ResourceLocation, ICapabilityProvider> list, List<Runnable> listeners) {
        this(list, listeners, null);
    }

    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        for (ICapabilityProvider c : caps) {
            LazyOptional<T> ret = c.getCapability(cap, side);
            if (ret != null && ret.isPresent()) return ret;
        }
        return LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (int x = 0; x < writers.size(); x++) nbt.put(names.get(x), writers.get(x).serializeNBT());
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        for (int x = 0; x < writers.size(); x++) {
            String name = names.get(x);
            if (nbt.contains(name)) writers.get(x).deserializeNBT(nbt.get(name));
        }
    }

    public boolean areCompatible(CapabilityDispatcher other) {
        return true;
    }

    public void invalidate() {
        listeners.forEach(Runnable::run);
    }
}
