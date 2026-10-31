package example.tcmodern;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.kylepitcock.legacyconnector.api.LegacyAccess;
import org.kylepitcock.legacyconnector.api.LegacyAddon;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyCapabilities;
import org.kylepitcock.legacyconnector.api.LegacyContentHooks;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example addon: Thaumcraft 4 (1.7.10) knows nothing about copper, deepslate or bee nests. This registers aspects for a few modern
 * items and blocks through Thaumcraft's own API, and shows a tile entity capability.
 */
public final class ThaumcraftModernAspects implements LegacyAddon {
    private static final Logger LOG = LoggerFactory.getLogger("tcmodern");
    private static final String TC = "thaumcraft";

    /** The addon is only active when Thaumcraft runs; no callback happens otherwise. */
    @Override
    public List<String> requiredLegacyMods() {
        return List.of(TC);
    }

    /**
     * Thaumcraft's own aspect registration runs in its postInit; the registry of modern items is frozen at LOAD_COMPLETE, so every
     * modern item resolves. Aspects registered earlier than Thaumcraft's own would be overwritten.
     */
    @Override
    public void onLegacyLoadComplete() {
        Class<?> api = LegacyApi.classOf(TC, "thaumcraft.api.ThaumcraftApi").orElse(null);
        Class<?> aspectClass = LegacyApi.classOf(TC, "thaumcraft.api.aspects.Aspect").orElse(null);
        Class<?> listClass = LegacyApi.classOf(TC, "thaumcraft.api.aspects.AspectList").orElse(null);
        if (api == null || aspectClass == null || listClass == null) {
            LOG.warn("Thaumcraft's API classes were not found; is this Thaumcraft 4?");
            return;
        }
        int done = 0;
        // modern id -> aspects as {name, amount, name, amount, ...}; names are static fields of thaumcraft.api.aspects.Aspect
        done += item(api, aspectClass, listClass, "copper_ingot", "METAL", 5, "EXCHANGE", 2);
        done += item(api, aspectClass, listClass, "raw_iron", "METAL", 8, "EARTH", 4);
        done += item(api, aspectClass, listClass, "raw_copper", "METAL", 8, "EARTH", 4);
        done += item(api, aspectClass, listClass, "amethyst_shard", "CRYSTAL", 6, "MAGIC", 2);
        done += item(api, aspectClass, listClass, "honeycomb", "LIFE", 2, "ORDER", 2);
        done += block(api, aspectClass, listClass, "deepslate", "EARTH", 5, "DARKNESS", 1);
        done += block(api, aspectClass, listClass, "bee_nest", "LIFE", 6, "FLIGHT", 3, "PLANT", 2);
        LOG.info("Registered Thaumcraft aspects for {} modern items/blocks", done);
    }

    private static int item(Class<?> api, Class<?> aspectClass, Class<?> listClass, String path, Object... aspects) {
        return register(api, aspectClass, listClass, LegacyContentHooks.legacyStack(ResourceLocation.withDefaultNamespace(path), 1, LegacyVersion.V1_7_10), path, aspects);
    }

    private static int block(Class<?> api, Class<?> aspectClass, Class<?> listClass, String path, Object... aspects) {
        return register(api, aspectClass, listClass, LegacyContentHooks.legacyBlockStack(ResourceLocation.withDefaultNamespace(path), 1, LegacyVersion.V1_7_10), path, aspects);
    }

    private static int register(Class<?> api, Class<?> aspectClass, Class<?> listClass, Optional<Object> stack, String name, Object[] aspects) {
        if (stack.isEmpty()) {
            LOG.warn("No legacy stack for minecraft:{}", name);
            return 0;
        }
        try {
            // new AspectList().add(Aspect.METAL, 5)...   (AspectList.add returns the list)
            Object list = LegacyAccess.newInstance(listClass);
            for (int i = 0; i < aspects.length; i += 2) {
                Object aspect = LegacyAccess.getStatic(aspectClass, (String) aspects[i]);
                LegacyAccess.invoke(list, "add", aspect, (Integer) aspects[i + 1]);
            }
            // ThaumcraftApi.registerObjectTag(ItemStack, AspectList)
            LegacyAccess.invokeStatic(api, "registerObjectTag", stack.get(), list);
            return 1;
        } catch (RuntimeException e) {
            LOG.warn("Could not register aspects for minecraft:{}: {}", name, e.toString());
            return 0;
        }
    }

    /**
     * The essentia jar (TileJarFillable) as a read-only fluid handler: the stored essentia count shows as millibuckets of water. A real
     * addon would register an "essentia" fluid; this only demonstrates reading a legacy tile through the API. Hoppers and pipes cannot
     * insert or drain it, but Jade/WTHIT-style probes and capability-aware mods can read it.
     */
    @Override
    public void registerCapabilities(LegacyCapabilities capabilities) {
        capabilities.forTile(TC, "thaumcraft.common.tiles.TileJarFillable", Capabilities.FluidHandler.BLOCK, JarView::new);
    }

    private record JarView(Object tile) implements IFluidHandler {
        JarView(Object tile, Direction side, BlockEntity host) {
            this(tile);
        }

        private int amount() {
            return (Integer) LegacyAccess.getField(tile, "amount");
        }

        private int max() {
            return (Integer) LegacyAccess.getField(tile, "maxAmount");
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return amount() <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, amount());
        }

        @Override
        public int getTankCapacity(int tank) {
            return max();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
