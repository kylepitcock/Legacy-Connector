package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.IMob;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 entity entries as modern entity types. Each entry a mod registers ({@code EntityRegistry.registerModEntity}, an
 * {@code EntityEntryBuilder}, or an {@code EntityEntry} in {@code RegistryEvent.Register<EntityEntry>}) gets an {@link EntityType}
 * whose factory creates a host ({@link EntityHost1122}, {@link MobHost1122} or {@link MonsterHost1122}, chosen from the legacy class
 * hierarchy) that constructs the legacy object through the entry's factory or its {@code (World)} constructor.
 *
 * <p>The host is bound to the legacy object before the legacy constructor runs (legacy constructors call entityInit, setSize,
 * applyEntityAttributes and add AI tasks). Legacy objects mods construct themselves ({@code new EntityFoo(world)}) are adopted: a host
 * is created for them on the spot.</p>
 */
public final class Hosts1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Entities");

    public enum Kind {
        ENTITY, MOB, MONSTER
    }

    /** {@code translationKey}: the legacy entity name key ("entity.thaumcraft.Firebat.name"). */
    public record Info(Class<?> legacyClass, ResourceLocation id, EntityType<?> type, Kind kind, String translationKey, String fallbackName,
            java.util.function.Function<World, ? extends Entity> factory) {}

    private static final Map<Class<?>, Info> BY_CLASS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, Info> BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<Kind, EntityType<?>> FALLBACK = new ConcurrentHashMap<>();
    private static final ThreadLocal<Host1122> BINDING = new ThreadLocal<>();
    private static final Set<Class<?>> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> NBT_FAILED = ConcurrentHashMap.newKeySet();

    private Hosts1122() {}

    static void nbtFailure(net.minecraft.world.entity.Entity host, String what, Throwable e) {
        if (NBT_FAILED.add(host.getClass())) {
            LOG.warn("A 1.12.2 entity threw while {} its NBT ({}); its saved data may be incomplete: {}", what, host.getType().getDescriptionId(), e.toString());
        }
    }

    // ------------------------------------------------------------------ registration

    /** An entity entry was registered: makes its modern type (mod entries only; vanilla entries are views). */
    public static synchronized Info register(EntityEntry entry) {
        if (entry == null || entry.getRegistryName() == null) return null;
        String ns = entry.getRegistryName().getNamespace();
        if ("minecraft".equals(ns)) return null;
        Class<?> cls = entry.getEntityClass();
        if (cls == null) return null;
        Info existing = BY_CLASS.get(cls);
        if (existing != null) return existing;
        if (!Entity.class.isAssignableFrom(cls) || EntityPlayer.class.isAssignableFrom(cls)) {
            LOG.warn("Not registering {} ({}): not a 1.12.2 entity class", entry.getRegistryName(), cls.getName());
            return null;
        }
        registerFallbacks();
        Kind kind = kindOf(cls);
        String path = entry.getRegistryName().getPath();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Ids.modernModId(ns), Ids.normalizePath(path).replace('/', '_'));
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            id = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_" + Integer.toHexString(cls.getName().hashCode()));
        }
        EntityType.Builder<?> builder = builder(kind, categoryOf(cls))
                .clientTrackingRange(Math.max(4, (entry.trackingRange + 15) / 16))
                .updateInterval(Math.max(1, entry.updateFrequency))
                .setShouldReceiveVelocityUpdates(entry.sendsVelocityUpdates);
        if (kindOf(cls) != Kind.ENTITY && isFireImmune(cls)) builder.fireImmune();
        EntityType<?> type = builder.build(id.toString());
        String name = entry.getName() == null ? path : entry.getName();
        // FML prefixed the translation key with the mod id: "entity.<modid>.<name>.name". Mods building their entries with
        // EntityEntryBuilder name them "<modid>.<name>" themselves (Scape and Run: Parasites' "srparasites.ada_longarms"), and 1.12
        // used that name as it is: prefixing it again gave "entity.srparasites.srparasites.ada_longarms.name", which no lang file has
        String key = name.startsWith(ns + ".") ? "entity." + name + ".name" : "entity." + ns + "." + name + ".name";
        Info info = new Info(cls, id, type, kind, key, name, entry.mcc$factory);
        // indexed before the registry callbacks so the type's factory can already resolve its class
        BY_CLASS.put(cls, info);
        BY_TYPE.put(type, info);
        LegacyRegistrar.register(BuiltInRegistries.ENTITY_TYPE, id, type);
        return info;
    }

    private static boolean isFireImmune(Class<?> cls) {
        return false;
    }

    /** Untracked types for 1.12.2 entity classes no mod registered (not saved with the world). */
    public static synchronized void registerFallbacks() {
        if (!FALLBACK.isEmpty()) return;
        for (Kind kind : Kind.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy1122_" + kind.name().toLowerCase(java.util.Locale.ROOT));
            EntityType<?> type = builder(kind, kind == Kind.MONSTER ? MobCategory.MONSTER : MobCategory.MISC)
                    .noSave().clientTrackingRange(8).updateInterval(kind == Kind.ENTITY ? 10 : 3).build(id.toString());
            FALLBACK.put(kind, type);
            LegacyRegistrar.register(BuiltInRegistries.ENTITY_TYPE, id, type);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityType.Builder<?> builder(Kind kind, MobCategory category) {
        return switch (kind) {
            case ENTITY -> EntityType.Builder.<EntityHost1122>of((t, l) -> new EntityHost1122((EntityType) t, l, null), category).sized(0.6F, 1.8F);
            case MOB -> EntityType.Builder.<MobHost1122>of((t, l) -> new MobHost1122((EntityType) t, l, null), category).sized(0.6F, 1.8F);
            case MONSTER -> EntityType.Builder.<MonsterHost1122>of((t, l) -> new MonsterHost1122((EntityType) t, l, null), category).sized(0.6F, 1.8F);
        };
    }

    public static Kind kindOf(Class<?> cls) {
        if (!EntityLivingBase.class.isAssignableFrom(cls)) return Kind.ENTITY;
        return IMob.class.isAssignableFrom(cls) ? Kind.MONSTER : Kind.MOB;
    }

    /** Spawn category from the legacy hierarchy (by simple name: several of these classes are only synthesized). */
    static MobCategory categoryOf(Class<?> cls) {
        if (IMob.class.isAssignableFrom(cls)) return MobCategory.MONSTER;
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            switch (c.getSimpleName()) {
                case "EntityWaterMob", "EntitySquid" -> {
                    return MobCategory.WATER_CREATURE;
                }
                case "EntityAmbientCreature", "EntityBat" -> {
                    return MobCategory.AMBIENT;
                }
                case "EntityAnimal", "EntityAgeable", "EntityTameable" -> {
                    return MobCategory.CREATURE;
                }
                default -> {
                }
            }
        }
        return MobCategory.MISC;
    }

    /** Display name of a legacy entity type: the mod's own translation of its name. */
    public static net.minecraft.network.chat.Component displayName(EntityType<?> type) {
        Info i = BY_TYPE.get(type);
        if (i == null) return null;
        String translated = legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(i.translationKey());
        return net.minecraft.network.chat.Component.literal(translated.equals(i.translationKey()) ? i.fallbackName() : translated);
    }

    public static Info info(EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    /** The registration of exactly this class (null for unregistered subclasses of a registered class). */
    public static Info info(Class<?> cls) {
        return BY_CLASS.get(cls);
    }

    public static Collection<Info> all() {
        return List.copyOf(BY_CLASS.values());
    }

    public static Collection<EntityType<?>> allTypes() {
        List<EntityType<?>> out = new java.util.ArrayList<>(FALLBACK.values());
        for (Info i : BY_CLASS.values()) out.add(i.type());
        return out;
    }

    // ------------------------------------------------------------------ construction and binding

    /** Called by the legacy Entity constructor: the host being constructed for it, if any (read once). */
    public static Host1122 takeBinding() {
        Host1122 h = BINDING.get();
        if (h != null) BINDING.remove();
        return h;
    }

    /** Constructs the legacy object for a host through its entry's factory or its {@code (World)} constructor. */
    static Entity construct(Class<?> cls, Host1122 host, Level level) {
        Host1122 previous = BINDING.get();
        BINDING.set(host);
        try {
            World world = WorldBridge1122.wrap(level);
            Info info = BY_CLASS.get(cls);
            if (info != null && info.factory() != null) {
                Entity made = info.factory().apply(world);
                if (made != null && made.getClass() == cls) return made;
                if (made != null) {
                    // a factory making another class: that object took the binding; use it only when it is bound to this host
                    return made.mcc$entity == host ? made : null;
                }
            }
            Constructor<?> c = cls.getDeclaredConstructor(World.class);
            c.setAccessible(true);
            return (Entity) c.newInstance(world);
        } catch (InvocationTargetException e) {
            failed(cls, "construction", e.getCause());
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed(cls, "construction", e);
            return null;
        } finally {
            if (previous != null) BINDING.set(previous);
            else BINDING.remove();
        }
    }

    /** A host for a legacy object a mod constructed itself; null when the world is not a real level. */
    public static Host1122 adopt(Entity legacy, World world) {
        Level level = world == null ? null : world.mcc$level();
        if (level == null || isParticle(legacy.getClass())) return null;
        Info info = info(legacy.getClass());
        Kind kind = info != null ? info.kind() : kindOf(legacy.getClass());
        EntityType<?> type = info != null ? info.type() : FALLBACK.get(kind);
        if (type == null) return null;
        return create(type, kind, level, legacy);
    }

    /** Legacy particles are client-side effects, never world entities (checked by name: a client class). */
    private static boolean isParticle(Class<?> cls) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getName().endsWith(".net.minecraft.client.particle.Particle")) return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Host1122 create(EntityType<?> type, Kind kind, Level level, Entity legacy) {
        return switch (kind) {
            case ENTITY -> new EntityHost1122((EntityType) type, level, legacy);
            case MOB -> new MobHost1122((EntityType) type, level, legacy);
            case MONSTER -> new MonsterHost1122((EntityType) type, level, legacy);
        };
    }

    /** Legacy class for a host's type (null for the fallback types: the class comes with the spawn data). */
    static Class<?> classFor(EntityType<?> type) {
        Info i = BY_TYPE.get(type);
        return i == null ? null : i.legacyClass();
    }

    static boolean isFallback(EntityType<?> type) {
        return FALLBACK.containsValue(type);
    }

    static Class<?> loadLegacyClass(String name) {
        for (ClassLoader loader : new ClassLoader[] {Thread.currentThread().getContextClassLoader(), Entity.class.getClassLoader()}) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                return Entity.class.isAssignableFrom(c) ? c : null;
            } catch (ClassNotFoundException | LinkageError e) {
                // next loader
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ overrides

    private static final Map<Class<?>, Map<String, Boolean>> OVERRIDES = new ConcurrentHashMap<>();

    /**
     * Whether a mod class (not the shim's own legacy classes) declares the method somewhere between itself and the shim: hosts only
     * hand a hook to the legacy object when a mod changed it, and keep the vanilla behaviour otherwise.
     */
    public static boolean overrides(Class<?> cls, String name, Class<?>... params) {
        String key = params.length == 0 ? name : name + java.util.Arrays.toString(params);
        return OVERRIDES.computeIfAbsent(cls, c -> new ConcurrentHashMap<>()).computeIfAbsent(key, k -> {
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                String cn = c.getName();
                if (cn.startsWith("legacyconnector.") || cn.startsWith("legacyconnector_synth.") || cn.startsWith("org.kylepitcock.")) return false;
                try {
                    c.getDeclaredMethod(name, params);
                    return true;
                } catch (NoSuchMethodException | LinkageError e) {
                    // look further up
                }
            }
            return false;
        });
    }

    // ------------------------------------------------------------------ failures

    public static boolean failed(Class<?> cls, String what, Throwable t) {
        if (FAILED.add(cls)) {
            LOG.error("1.12.2 entity {} failed during {}", cls.getName(), what, t);
            return true;
        }
        return false;
    }

    public static boolean hasFailed(Class<?> cls) {
        return FAILED.contains(cls);
    }

    private static final Set<String> LOGGED_TICK_FAILURES = ConcurrentHashMap.newKeySet();

    /** Consecutive failing server ticks after which an entity is removed (it would otherwise be stuck broken). */
    static final int MAX_FAILED_TICKS = 200;

    /**
     * A legacy entity threw while ticking. Logged once per class, side and exception type; never removes anything on the client, and
     * on the server only removes an entity that keeps failing.
     *
     * @return true when the entity should be removed
     */
    static boolean tickFailed(net.minecraft.world.entity.Entity host, Class<?> cls, Throwable t, int consecutive) {
        boolean client = host.level().isClientSide();
        if (LOGGED_TICK_FAILURES.add(cls.getName() + (client ? "@client:" : "@server:") + t.getClass().getName())) {
            LOG.error("1.12.2 entity {} failed while ticking on the {} (logged once)", cls.getName(), client ? "client" : "server", t);
        }
        if (!client && consecutive >= MAX_FAILED_TICKS) {
            LOG.warn("Removing {} at {}: it failed {} ticks in a row", cls.getName(), host.blockPosition().toShortString(), consecutive);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ attributes

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        AttributeSupplier supplier = Monster.createMonsterAttributes()
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
                .add(Attributes.JUMP_STRENGTH)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_KNOCKBACK)
                .add(Attributes.LUCK)
                .add(Attributes.ARMOR_TOUGHNESS)
                .build();
        for (EntityType<?> type : allTypes()) {
            Info i = BY_TYPE.get(type);
            Kind kind = i != null ? i.kind() : FALLBACK.entrySet().stream().filter(e -> e.getValue() == type).map(Map.Entry::getKey).findFirst().orElse(Kind.ENTITY);
            if (kind == Kind.ENTITY) continue;
            @SuppressWarnings("unchecked")
            EntityType<? extends net.minecraft.world.entity.LivingEntity> living = (EntityType<? extends net.minecraft.world.entity.LivingEntity>) type;
            event.put(living, supplier);
        }
    }
}
