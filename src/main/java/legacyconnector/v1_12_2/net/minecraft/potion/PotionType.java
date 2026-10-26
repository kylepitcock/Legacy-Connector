package legacyconnector.v1_12_2.net.minecraft.potion;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespacedDefaultedByKey;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/**
 * A brewable potion (water, awkward, long swiftness, ...): a list of effects. The vanilla types carry the effects of the modern potion of
 * the same name; mod types are registered through {@code RegistryEvent.Register<PotionType>}.
 */
public class PotionType extends IForgeRegistryEntry.Impl<PotionType> {
    @SuppressWarnings("unchecked")
    public static final RegistryNamespacedDefaultedByKey<ResourceLocation, PotionType> REGISTRY =
            (RegistryNamespacedDefaultedByKey<ResourceLocation, PotionType>) (RegistryNamespacedDefaultedByKey<?, ?>) Registries1122.view(Registries1122.POTION_TYPES, "empty");
    private static final Map<String, PotionType> VANILLA = new ConcurrentHashMap<>();

    private final String baseName;
    /** Null for a vanilla type until first read: its effects are those of the modern potion of the same name. */
    private List<PotionEffect> effects;

    public PotionType(PotionEffect... effects) {
        this(null, effects);
    }

    public PotionType(String baseName, PotionEffect... effects) {
        this.baseName = baseName;
        this.effects = ImmutableList.copyOf(effects);
    }

    /** The legacy object of a vanilla potion type ("water", "long_swiftness"), the same one PotionTypes and the registry give. */
    public static PotionType mcc$vanilla(String name) {
        return VANILLA.computeIfAbsent(name, n -> {
            PotionType t = new PotionType((String) null);
            t.effects = null;
            t.setRegistryName("minecraft", n);
            return t;
        });
    }

    /** The modern potion of this type, or null (the empty type, mod types that have no modern potion). */
    public net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> mcc$modern() {
        ResourceLocation name = getRegistryName();
        if (name == null) return null;
        var holder = net.minecraft.core.registries.BuiltInRegistries.POTION.getHolder(name.mcc$modern());
        return holder.isPresent() ? holder.get() : null;
    }

    /** The legacy type of a modern potion: the vanilla type of that name or the registered mod type. */
    public static PotionType mcc$fromModern(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        var key = potion.unwrapKey();
        if (key.isEmpty()) return null;
        var id = key.get().location();
        PotionType registered = Registries1122.POTION_TYPES.getValue(new ResourceLocation(id.getNamespace(), id.getPath())) instanceof PotionType t ? t : null;
        if (registered != null) return registered;
        return "minecraft".equals(id.getNamespace()) ? mcc$vanilla(id.getPath()) : null;
    }

    public static PotionType getPotionTypeForName(String name) {
        return REGISTRY.getObject(new ResourceLocation(name));
    }

    public String getNamePrefixed(String prefix) {
        if (baseName != null) return prefix + baseName;
        ResourceLocation name = REGISTRY.getNameForObject(this);
        if (name == null) name = getRegistryName();
        return prefix + (name == null ? "empty" : name.getPath());
    }

    public synchronized List<PotionEffect> getEffects() {
        if (effects == null) {
            ImmutableList.Builder<PotionEffect> out = ImmutableList.builder();
            var modern = mcc$modern();
            if (modern != null) {
                for (var e : modern.value().getEffects()) {
                    PotionEffect legacy = PotionEffect.mcc$fromModern(e);
                    if (legacy != null) out.add(legacy);
                }
            }
            effects = out.build();
        }
        return effects;
    }

    public static void registerPotionTypes() {
        // the vanilla types are entries of the registry already
    }

    protected static void registerPotionType(String name, PotionType type) {
        if (type.getRegistryName() == null) type.setRegistryName(new ResourceLocation(name));
        Registries1122.POTION_TYPES.register(type);
    }

    public boolean hasInstantEffect() {
        for (PotionEffect e : getEffects()) if (e.getPotion() != null && e.getPotion().isInstant()) return true;
        return false;
    }
}
