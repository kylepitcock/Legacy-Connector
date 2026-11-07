package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;

/**
 * Forge 1.20.1's capability provider. NeoForge 1.21.1's block entities, entities and stacks have no {@code getCapability}; the translator adds this
 * interface to every legacy class that declares one and routes calls through {@code Caps1201}.
 */
public interface ICapabilityProvider {
    <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side);

    default <T> LazyOptional<T> getCapability(Capability<T> cap) {
        return getCapability(cap, null);
    }
}
