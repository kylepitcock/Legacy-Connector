package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Things that were plain objects in 1.20.1 and are {@code Holder}s in 1.21.1 (mob effects, attributes, enchantments, game events), so the old method
 * signatures taking the object keep working. The static fields ({@code MobEffects.POISON}, {@code Attributes.MAX_HEALTH}) are unwrapped by the
 * loader ({@code mc-fields.tsv}); these methods wrap an object back into its registered holder where 1.21.1 wants one.
 */
public final class HolderCompat {
    private HolderCompat() {}

    public static Holder<MobEffect> holder(MobEffect e) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e);
    }

    public static Holder<Attribute> holder(Attribute a) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(a);
    }

    public static Holder<GameEvent> holder(GameEvent g) {
        return BuiltInRegistries.GAME_EVENT.wrapAsHolder(g);
    }

    public static Holder<Enchantment> holder(LegacyEnchantment e) {
        return e.holder();
    }

    // ------------------------------------------------------------------ mob effects

    public static MobEffectInstance newEffect(MobEffect e, int duration) {
        return new MobEffectInstance(holder(e), duration);
    }

    public static MobEffectInstance newEffect(MobEffect e, int duration, int amplifier) {
        return new MobEffectInstance(holder(e), duration, amplifier);
    }

    public static MobEffectInstance newEffect(MobEffect e, int duration, int amplifier, boolean ambient, boolean visible) {
        return new MobEffectInstance(holder(e), duration, amplifier, ambient, visible);
    }

    public static MobEffectInstance newEffect(MobEffect e, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        return new MobEffectInstance(holder(e), duration, amplifier, ambient, visible, showIcon);
    }

    public static MobEffect effectOf(MobEffectInstance i) {
        return i.getEffect().value();
    }

    public static boolean hasEffect(LivingEntity e, MobEffect effect) {
        return e.hasEffect(holder(effect));
    }

    public static MobEffectInstance getEffect(LivingEntity e, MobEffect effect) {
        return e.getEffect(holder(effect));
    }

    public static boolean removeEffect(LivingEntity e, MobEffect effect) {
        return e.removeEffect(holder(effect));
    }

    // ------------------------------------------------------------------ attributes

    /** A 1.20.1 modifier is identified by a UUID and a name; 1.21.1 by a resource location: the UUID keeps it unique. */
    public static ResourceLocation modifierId(UUID uuid) {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "modifier/" + uuid);
    }

    /** The resource location of a 1.20.1 modifier given as a UUID string (anything that is not a valid path character is replaced). */
    public static ResourceLocation modifierId(String uuid) {
        StringBuilder sb = new StringBuilder();
        for (char c : uuid.toLowerCase(java.util.Locale.ROOT).toCharArray()) sb.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' ? c : '_');
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "modifier/" + sb);
    }

    /** 1.20.1 {@code MobEffect.addAttributeModifier(Attribute, String uuid, double, Operation)}: the attribute is a holder and the id a resource location now. */
    public static net.minecraft.world.effect.MobEffect addEffectModifier(net.minecraft.world.effect.MobEffect effect, Attribute attribute, String uuid,
            double amount, AttributeModifier.Operation op) {
        return effect.addAttributeModifier(holder(attribute), modifierId(uuid), amount, op);
    }

    public static AttributeModifier newModifier(UUID id, String name, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(modifierId(id), amount, op);
    }

    public static int stackEnchantmentLevel(ItemStack stack, LegacyEnchantment e) {
        return stack.getEnchantmentLevel(holder(e));
    }

    /** Enchantments of a stack as the 1.20.1 map from enchantment to level. */
    public static java.util.Map<LegacyEnchantment, Integer> getEnchantments(ItemStack stack) {
        java.util.Map<LegacyEnchantment, Integer> out = new java.util.LinkedHashMap<>();
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) out.put(LegacyEnchantments.legacy(e.getKey()), e.getIntValue());
        return out;
    }

    public static void setEnchantments(java.util.Map<LegacyEnchantment, Integer> map, ItemStack stack) {
        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable m = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        map.forEach((e, lvl) -> m.set(holder(e), lvl));
        EnchantmentHelper.setEnchantments(stack, m.toImmutable());
    }

    public static void enchant(ItemStack stack, LegacyEnchantment e, int level) {
        stack.enchant(holder(e), level);
    }

    public static net.minecraft.world.item.enchantment.EnchantmentInstance newEnchantmentInstance(LegacyEnchantment e, int level) {
        return new net.minecraft.world.item.enchantment.EnchantmentInstance(holder(e), level);
    }

    public static AttributeModifier newModifier(String name, double amount, AttributeModifier.Operation op) {
        return newModifier(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name, amount, op);
    }

    public static AttributeModifier newModifier2(UUID id, java.util.function.Supplier<String> name, double amount, AttributeModifier.Operation op) {
        return new AttributeModifier(modifierId(id), amount, op);
    }

    public static double amount(AttributeModifier m) {
        return m.amount();
    }

    public static AttributeModifier.Operation operation(AttributeModifier m) {
        return m.operation();
    }

    public static String modifierName(AttributeModifier m) {
        return m.id().getPath();
    }

    public static AttributeInstance getAttribute(LivingEntity e, Attribute a) {
        return e.getAttribute(holder(a));
    }

    public static double getAttributeValue(LivingEntity e, Attribute a) {
        return e.getAttributeValue(holder(a));
    }

    public static double getAttributeBaseValue(LivingEntity e, Attribute a) {
        return e.getAttributeBaseValue(holder(a));
    }

    public static Attribute attributeOf(AttributeInstance i) {
        return i.getAttribute().value();
    }

    public static AttributeModifier getModifier(AttributeInstance i, UUID id) {
        return i.getModifier(modifierId(id));
    }

    public static boolean hasModifier(AttributeInstance i, AttributeModifier m) {
        return i.hasModifier(m.id());
    }

    public static void removeModifier(AttributeInstance i, UUID id) {
        i.removeModifier(modifierId(id));
    }

    public static void removeModifier(AttributeInstance i, AttributeModifier m) {
        i.removeModifier(m.id());
    }

    public static AttributeSupplier.Builder add(AttributeSupplier.Builder b, Attribute a) {
        return b.add(holder(a));
    }

    public static AttributeSupplier.Builder add(AttributeSupplier.Builder b, Attribute a, double value) {
        return b.add(holder(a), value);
    }

    // ------------------------------------------------------------------ enchantments

    public static int getItemEnchantmentLevel(LegacyEnchantment e, ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(holder(e), stack);
    }

    public static int getEnchantmentLevel(LegacyEnchantment e, LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(holder(e), entity);
    }

    /** The unwrapped {@code Enchantments.X} (a resource key in 1.21.1, the object in 1.20.1). */
    public static LegacyEnchantment enchantment(ResourceKey<Enchantment> key) {
        return LegacyEnchantments.legacy(MigrateContext.provider().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key));
    }

    /** {@code EnchantmentInstance.enchantment} (a holder in 1.21.1). */
    public static LegacyEnchantment legacy(Holder<Enchantment> holder) {
        return LegacyEnchantments.legacy(holder);
    }

    /** {@code BuiltInRegistries.ENCHANTMENT} is a dynamic registry in 1.21.1. */
    public static net.minecraft.core.Registry<Enchantment> enchantmentRegistry() {
        try {
            return MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT);
        } catch (IllegalStateException notThereYet) {
            // 1.20.1 had the registry as a static from the start: libraries keep it in a static field while the mod is constructed (SuperMartijn642's Core Lib).
            // The reference is a view that asks the real registry on every call; it works once a world's registries exist.
            return lazyEnchantmentRegistry();
        }
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.core.Registry<Enchantment> lazyEnchantmentRegistry() {
        return (net.minecraft.core.Registry<Enchantment>) java.lang.reflect.Proxy.newProxyInstance(net.minecraft.core.Registry.class.getClassLoader(),
                new Class<?>[] {net.minecraft.core.Registry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("key") && method.getParameterCount() == 0) return Registries.ENCHANTMENT;
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "lazy enchantment registry";
                        };
                    }
                    try {
                        return method.invoke(MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT), args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    // ------------------------------------------------------------------ game events

    public static void gameEvent(net.minecraft.world.level.LevelAccessor level, Entity source, GameEvent event, BlockPos pos) {
        level.gameEvent(source, holder(event), pos);
    }

    public static void gameEvent(net.minecraft.world.level.LevelAccessor level, Entity source, GameEvent event, net.minecraft.world.phys.Vec3 pos) {
        level.gameEvent(source, holder(event), pos);
    }

    public static void gameEvent(net.minecraft.world.level.LevelAccessor level, GameEvent event, net.minecraft.world.phys.Vec3 pos, GameEvent.Context ctx) {
        level.gameEvent(holder(event), pos, ctx);
    }

    public static void gameEvent(net.minecraft.world.level.LevelAccessor level, GameEvent event, BlockPos pos, GameEvent.Context ctx) {
        level.gameEvent(holder(event), pos, ctx);
    }

    public static void gameEvent(Entity source, GameEvent event) {
        source.gameEvent(holder(event));
    }

    public static void gameEvent(Entity source, GameEvent event, Entity cause) {
        source.gameEvent(holder(event), cause);
    }
}
