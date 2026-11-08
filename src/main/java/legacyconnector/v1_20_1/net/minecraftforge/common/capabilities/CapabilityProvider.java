package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import java.util.function.Supplier;
import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;

/**
 * Forge 1.20.1's base of everything that can hold capabilities (entities, block entities, stacks, levels). Legacy classes extend it
 * directly for their own provider logic; the capabilities of attached providers come from the {@link CapabilityDispatcher}.
 */
public abstract class CapabilityProvider<B extends CapabilityProvider<B>> implements ICapabilityProvider {
    private final Class<B> baseClass;
    private CapabilityDispatcher capabilities;
    private boolean valid = true;
    private boolean isLazy;
    private Supplier<ICapabilityProvider> lazyParentSupplier;

    protected CapabilityProvider(Class<B> baseClass) {
        this(baseClass, false);
    }

    protected CapabilityProvider(Class<B> baseClass, boolean isLazy) {
        this.baseClass = baseClass;
        this.isLazy = isLazy;
    }

    protected final void gatherCapabilities() {
        gatherCapabilities((ICapabilityProvider) null);
    }

    protected final void gatherCapabilities(Supplier<ICapabilityProvider> parent) {
        this.lazyParentSupplier = parent;
        if (!isLazy) this.capabilities = new CapabilityDispatcher(java.util.Map.of(), java.util.List.of(), parent == null ? null : parent.get());
    }

    protected final void gatherCapabilities(ICapabilityProvider parent) {
        if (!isLazy) this.capabilities = new CapabilityDispatcher(java.util.Map.of(), java.util.List.of(), parent);
    }

    protected final CapabilityDispatcher getCapabilities() {
        if (isLazy && capabilities == null) {
            isLazy = false;
            gatherCapabilities(lazyParentSupplier);
        }
        return capabilities;
    }

    protected final boolean areCapsCompatible(CapabilityProvider<B> other) {
        return true;
    }

    protected final boolean areCapsCompatible(CapabilityDispatcher other) {
        return true;
    }

    public void invalidateCaps() {
        valid = false;
    }

    public void reviveCaps() {
        valid = true;
    }

    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        CapabilityDispatcher d = getCapabilities();
        return !valid || d == null ? LazyOptional.empty() : d.getCapability(cap, side);
    }
}
