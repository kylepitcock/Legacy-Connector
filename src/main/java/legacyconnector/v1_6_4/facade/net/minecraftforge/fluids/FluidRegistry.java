package legacyconnector.v1_6_4.facade.net.minecraftforge.fluids;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Map;

/** Facade of the 1.7.10 shim's {@code FluidRegistry} for 1.6.4 mods: the static {@code fluidIDs}. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class FluidRegistry {
    private FluidRegistry() {}

    /**
     * 1.6.4 {@code FluidRegistry.fluidIDs} (name to id; Forge 1.6.4 kept a {@code BiMap<String, Integer>}): a copy of the registry's table at the time of the
     * read, in the same form. Writes to the copy do not reach the registry (ids are assigned by {@code registerFluid}).
     */
    public static BiMap<String, Integer> mcc$get$fluidIDs() {
        BiMap<String, Integer> out = HashBiMap.create();
        for (Map.Entry<String, Integer> e : legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry.getRegisteredFluidIDs().entrySet()) out.put(e.getKey(), e.getValue());
        return out;
    }
}
