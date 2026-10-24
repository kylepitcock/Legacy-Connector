package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-dimension physics that legacy mods applied by patching the engine (Galacticraft's coremod rewrote entity movement and the
 * sound manager). The modern engine cannot be patched that way, so the dimension provider is asked directly, through the
 * {@code IGalacticraftWorldProvider} methods: gravity (the modern gravity attribute), fall damage and, on the client, how much
 * world sound is attenuated (vacuum). Providers without these methods change nothing.
 */
public final class DimensionPhysics1710 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Physics");
    /** 1.7.10 gravity in blocks per tick squared; the modern default of the gravity attribute is the same 0.08. */
    private static final double NORMAL_GRAVITY = 0.08D;
    private static final ClassValue<Methods> METHODS = new ClassValue<>() {
        @Override
        protected Methods computeValue(Class<?> type) {
            return new Methods(find(type, "getGravity"), find(type, "getFallDamageModifier"), find(type, "getSoundVolReductionAmount"));
        }
    };

    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private record Methods(Method gravity, Method fall, Method sound) {}

    private DimensionPhysics1710() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener((EntityJoinLevelEvent e) -> {
            if (e.getEntity() instanceof LivingEntity living) applyGravity(living, e.getLevel());
        });
        gameBus.addListener((LivingFallEvent e) -> {
            Float modifier = number(e.getEntity().level(), Methods::fall);
            if (modifier != null && modifier >= 0) e.setDamageMultiplier(e.getDamageMultiplier() * modifier);
        });
    }

    private static void applyGravity(LivingEntity living, Level level) {
        var attribute = living.getAttribute(Attributes.GRAVITY);
        if (attribute == null) return;
        Float reduction = number(level, Methods::gravity);
        // 1.7.10 providers (Galacticraft) return how much weaker gravity is than the normal 0.08.
        double wanted = reduction == null ? NORMAL_GRAVITY : Math.max(0.002D, Math.min(0.2D, NORMAL_GRAVITY - reduction));
        if (Math.abs(attribute.getBaseValue() - wanted) > 1.0E-6) attribute.setBaseValue(wanted);
        if (reduction != null && LOGGED.add(level.dimension().location().toString())) LOG.info("Gravity in {} is {} (provider reduction {})", level.dimension().location(), wanted, reduction);
    }

    /** One line for logs: what the dimension's provider says about gravity, fall damage and sound. */
    public static String describe(Level level) {
        return "gravity reduction " + number(level, Methods::gravity) + ", fall " + number(level, Methods::fall) + ", sound " + number(level, Methods::sound);
    }

    /** The factor world sounds are divided by in this level (1 = unchanged); client side. */
    public static float soundAttenuation(Level level) {
        Float amount = number(level, Methods::sound);
        return amount == null || amount < 1.0F ? 1.0F : amount;
    }

    private static Float number(Level level, java.util.function.Function<Methods, Method> pick) {
        if (level == null) return null;
        try {
            Object provider = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$get$provider(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
            if (provider == null) return null;
            Method m = pick.apply(METHODS.get(provider.getClass()));
            if (m == null) return null;
            Object value = m.invoke(provider);
            return value instanceof Number n ? n.floatValue() : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Method find(Class<?> type, String name) {
        try {
            Method m = type.getMethod(name);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
