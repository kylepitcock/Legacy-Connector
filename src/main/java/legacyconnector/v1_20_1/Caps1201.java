package legacyconnector.v1_20_1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.ICapabilityProvider;
import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import legacyconnector.v1_20_1.net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BaseCapability;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Forge 1.20.1's {@code getCapability(cap, side)} on block entities, entities and stacks, over NeoForge 1.21.1. The translator redirects every such call
 * here ({@code forge-call-rewrites.tsv}):
 * <ul>
 *   <li>a legacy class that declares {@code getCapability} implements the facade {@link ICapabilityProvider} (added by the translator) and answers itself;</li>
 *   <li>providers attached through {@link AttachCapabilitiesEvent} are asked next;</li>
 *   <li>for the capabilities NeoForge has counterparts of (items, fluids, energy) the NeoForge lookup is the last resort, so other mods' handlers are reached.</li>
 * </ul>
 * In the other direction {@code Forge1201Runtime} registers NeoForge providers for every legacy block entity type that call back into the legacy
 * {@code getCapability}, so NeoForge-side code (hoppers, pipes of other mods) sees legacy containers.
 */
public final class Caps1201 {
    private Caps1201() {
    }

    /** Providers per object, by identity (entities compare equal by id, across the client and server side). */
    private static final Map<Object, List<ICapabilityProvider>> ATTACHED = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    /** {@code obj.getCapability(cap, side)}. */
    public static <T> LazyOptional<T> getCapability(Object recv, Capability<T> cap, Direction side) {
        if (recv instanceof ICapabilityProvider p) return p.getCapability(cap, side);
        return nativeGet(recv, cap, side, true);
    }

    public static <T> LazyOptional<T> getCapability(Object recv, Capability<T> cap) {
        return getCapability(recv, cap, null);
    }

    /** {@code super.getCapability(cap, side)} of a legacy class whose parent is a Minecraft class: what the base object offers by itself. */
    public static <T> LazyOptional<T> superGetCapability(Object recv, Capability<T> cap, Direction side) {
        return nativeGet(recv, cap, side, false);
    }

    public static <T> LazyOptional<T> superGetCapability(Object recv, Capability<T> cap) {
        return superGetCapability(recv, cap, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> LazyOptional<T> nativeGet(Object recv, Capability<T> cap, Direction side, boolean askNeoForge) {
        List<ICapabilityProvider> attached = attached(recv);
        if (attached != null) {
            for (ICapabilityProvider p : attached) {
                LazyOptional<T> lo = p.getCapability(cap, side);
                if (lo.isPresent()) return lo;
            }
        }
        if (!askNeoForge) return LazyOptional.empty();
        Object instance = null;
        if (recv instanceof BlockEntity be && cap.mcc$block != null && be.getLevel() != null) {
            BlockCapability c = cap.mcc$block;
            instance = c.getCapability(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, context(c, side));
        } else if (recv instanceof Entity e && cap.mcc$entity != null) {
            EntityCapability c = cap.mcc$entity;
            instance = c.getCapability(e, context(c, side));
        } else if (recv instanceof ItemStack stack && cap.mcc$item != null) {
            ItemCapability c = cap.mcc$item;
            instance = c.getCapability(stack, context(c, side));
        }
        return LazyOptional.ofNullable((T) instance);
    }

    private static Object context(BaseCapability<?, ?> c, Direction side) {
        return c.contextClass() == Direction.class ? side : null;
    }

    // ---- attaching --------------------------------------------------------------------------------------------------------

    /** The providers of an object, attaching them on the first query (Forge did it when the object was created; stacks and block entities are too many to hook). */
    private static List<ICapabilityProvider> attached(Object recv) {
        if (recv == null || recv instanceof ItemStack stack && stack.isEmpty()) return null;
        List<ICapabilityProvider> list = ATTACHED.get(recv);
        if (list == null) {
            attach(recv);
            list = ATTACHED.get(recv);
        }
        return list;
    }

    /**
     * Fires Forge's {@code AttachCapabilitiesEvent} for an object once and remembers the providers the listeners added. For a stack the item's own
     * {@code initCapabilities} comes first. Handlers that declare their object type (the generic argument of the event) only see matching objects: the
     * translator puts an {@code instanceof} guard at the head of such handlers, {@code ForgeApi1201.busAddGenericListener} filters the rest.
     */
    public static <T> void attach(T object) {
        if (ATTACHED.containsKey(object)) return;
        List<ICapabilityProvider> providers = new ArrayList<>();
        ATTACHED.put(object, providers);   // before the event: a handler asking for a capability of the object must not attach again
        if (object instanceof ItemStack stack && stack.getItem() instanceof InitCapabilities1201 init) {
            ICapabilityProvider p = init.initCapabilities(stack, null);
            if (p != null) providers.add(p);
        }
        AttachCapabilitiesEvent<T> event = new AttachCapabilitiesEvent<>(object);
        try {
            NeoForge.EVENT_BUS.post(event);
        } catch (RuntimeException | LinkageError e) {
            if (!warned) {
                warned = true;
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Forge1201").warn("An AttachCapabilitiesEvent handler failed for {}: {}", object.getClass().getName(), e.toString());
            }
        }
        providers.addAll(event.getCapabilities().values());
    }

    private static boolean warned;

    /** Implemented (by the translator) by every item class that declares Forge's {@code initCapabilities(stack, nbt)}. */
    public interface InitCapabilities1201 {
        ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.CompoundTag nbt);
    }

    /** The provider an item's {@code initCapabilities} made for a stack (for the NeoForge-side bridge), or null. */
    public static ICapabilityProvider itemProvider(ItemStack stack) {
        List<ICapabilityProvider> list = attached(stack);
        if (list == null || list.isEmpty()) return null;
        return new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                for (ICapabilityProvider p : list) {
                    LazyOptional<T> lo = p.getCapability(cap, side);
                    if (lo.isPresent()) return lo;
                }
                return LazyOptional.empty();
            }
        };
    }
}
