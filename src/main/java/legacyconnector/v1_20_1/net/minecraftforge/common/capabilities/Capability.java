package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * Forge 1.20.1's capability key. NeoForge 1.21.1 has one key class per target kind ({@code BlockCapability}, {@code EntityCapability},
 * {@code ItemCapability}); a Forge capability that NeoForge also knows (item handler, fluid handler, energy) carries its NeoForge counterparts, any
 * other (a mod's own, from {@code CapabilityManager.get}) is a plain name that only the mods exchange among themselves.
 */
public final class Capability<T> {
    private final String name;
    /** The NeoForge keys for the same interface, or null. */
    public final BlockCapability<T, ?> mcc$block;
    public final EntityCapability<T, ?> mcc$entity;
    public final ItemCapability<T, ?> mcc$item;

    public Capability(String name) {
        this(name, null, null, null);
    }

    public Capability(String name, BlockCapability<T, ?> block, EntityCapability<T, ?> entity, ItemCapability<T, ?> item) {
        this.name = name;
        this.mcc$block = block;
        this.mcc$entity = entity;
        this.mcc$item = item;
    }

    /** Forge's "tell me when this capability is registered": registration is immediate here, so the callback runs at once. */
    public Capability<T> addListener(java.util.function.Consumer<Capability<T>> listener) {
        listener.accept(this);
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isRegistered() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public <R> LazyOptional<R> orEmpty(Capability<R> toCheck, LazyOptional<T> inst) {
        return this == toCheck ? inst.cast() : LazyOptional.empty();
    }

    @Override
    public String toString() {
        return "Capability[" + name + "]";
    }
}
