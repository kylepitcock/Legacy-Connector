package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replays the hooks legacy coremods put into Minecraft 1.7.10's own classes. Those classes are gone, so the transformers cannot run; the loader instead lists
 * what they would have added ({@code CoremodHookScan}: each added static call with its <i>site</i>, the place in 1.7.10's code). A {@link Site} here is such
 * a place offered on the modern game: it names the 1.7.10 site keys it stands for and the values available there, and the code at the modern place (a mixin,
 * or the shim class itself) hands the values over. Nothing here knows a mod: any coremod that hooked a site is called, with the arguments its hook method
 * declares.
 *
 * <p>A hook that changes a value without receiving it (1.7.10 coremods then combined the two with a helper of their own next to the hook, e.g.
 * {@code ASMHelper.multiply(float, float)} or {@code and(boolean, boolean)}) has its result combined with the value through that helper; without one its
 * result replaces the value.
 *
 * <p>Arguments are bound by type: each parameter of a hook takes the first value the site offers that fits and is not taken yet (1.7.10 hooks receive the
 * receiver, the method's arguments and the value being changed in an order of the author's choosing, e.g. {@code getHealRate(EntityPlayer, int)} and
 * {@code getEntityGravity(double, Entity)}). A hook that asks for something a site does not offer is left out and reported, as are hooks at places not offered
 * yet; the report is one log line at start-up ({@code -Dlegacyconnector.coremodHooks.report=full} lists every hook).
 */
public final class CoremodSites {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Coremods");
    private static final String SHIM = "legacyconnector.v1_7_10.";
    private static final String SYNTH = "legacyconnector_synth.v1_7_10.";

    /** A discovered hook call: where, and the static method. */
    private record Listed(String site, String owner, String name, String desc) {}

    /** Site key (without the {@code @n} of repeats) to the hooks there, as the loader listed them. */
    private static final Map<String, List<Listed>> LISTED = load();
    private static final List<Site> SITES = new ArrayList<>();
    private static volatile boolean ready;

    // ---- the catalog: 1.7.10 places that exist on the modern game. Values are legacy views (EntityBridge.wrap).
    private static final Class<?> ENTITY = legacyconnector.v1_7_10.net.minecraft.entity.Entity.class;
    private static final Class<?> LIVING = legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase.class;
    private static final Class<?> PLAYER = legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class;

    /** EntityLivingBase.moveEntityWithHeading, first thing: true cancels the vanilla movement. Values: entity, strafe, forward. */
    public static final Site TRAVEL = site("living entity movement", new Class<?>[] {LIVING, float.class, float.class},
            "net/minecraft/entity/EntityLivingBase.func_70612_e(FF)V#head-cancel");

    /** The gravity constant of an entity's movement step. Values: gravity, entity. */
    public static final Site GRAVITY = site("entity gravity", new Class<?>[] {double.class, ENTITY},
            "net/minecraft/entity/EntityLivingBase.func_70612_e(FF)V#after:ldc 0.08",
            "net/minecraft/entity/item/EntityItem.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/item/EntityFallingBlock.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/item/EntityMinecart.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/item/EntityTNTPrimed.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/projectile/EntityArrow.func_70071_h_()V#after:f2d",
            "net/minecraft/entity/projectile/EntityThrowable.func_70071_h_()V#after:invokevirtual net/minecraft/entity/projectile/EntityThrowable.func_70185_h()F");

    /** FoodStats.onUpdate: the ticks between two natural heals (80). Values: player, ticks. */
    public static final Site HEAL_RATE = site("natural healing rate", new Class<?>[] {PLAYER, int.class},
            "net/minecraft/util/FoodStats.func_75118_a(Lnet/minecraft/entity/player/EntityPlayer;)V#const:80");

    /** MinecraftServer.run: the milliseconds of one tick (50). Value: milliseconds. */
    public static final Site TICK_MILLIS = site("server tick length", new Class<?>[] {long.class},
            "net/minecraft/server/MinecraftServer.run()V#const:50");

    /** World.updateWeather, first thing: true skips the weather step. Value: world. */
    public static final Site WEATHER = site("weather step", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.world.World.class},
            "net/minecraft/world/World.func_72835_b()V#head-cancel");

    /** EntityLivingBase.getArmSwingAnimationEnd: the ticks of an arm swing. Values: ticks, entity. */
    public static final Site SWING_TICKS = site("arm swing length", new Class<?>[] {int.class, LIVING},
            "net/minecraft/entity/EntityLivingBase.func_82166_i()I#tail");

    /** Entity.onEntityUpdate: whether a sprinting entity kicks up particles. Values: sprinting, entity. */
    public static final Site SPRINT_PARTICLES = site("sprint particles", new Class<?>[] {boolean.class, ENTITY},
            "net/minecraft/entity/Entity.func_70030_z()V#after:invokevirtual net/minecraft/entity/Entity.func_70051_ag()Z");

    /** World.removeEntity, last thing. Values: world, entity. */
    public static final Site ENTITY_REMOVED = site("entity removed from the world", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.world.World.class, ENTITY},
            "net/minecraft/world/World.func_72900_e(Lnet/minecraft/entity/Entity;)V#tail");

    /** EntityPlayer.attackTargetEntityWithCurrentItem: stands in for {@code target.attackEntityFrom(source, amount)}. Values: target, source, amount, attacker. */
    public static final Site PLAYER_ATTACK = site("player attack damage",
            new Class<?>[] {ENTITY, legacyconnector.v1_7_10.net.minecraft.util.DamageSource.class, float.class, PLAYER},
            "net/minecraft/entity/player/EntityPlayer.func_71059_n(Lnet/minecraft/entity/Entity;)V#replace:func_70097_a(Lnet/minecraft/util/DamageSource;F)Z");

    /** The model sites: client classes, so apart from the rest. Values: model, entity, the six animation numbers. */
    public static final class Client {
        private static final Class<?>[] MODEL = {legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped.class, ENTITY, float.class, float.class, float.class,
                float.class, float.class, float.class};
        private static final String BIPED = "net/minecraft/client/model/ModelBiped.";
        /** ModelBiped.render, first and last thing. */
        public static final Site RENDER_PRE = site("biped model render (start)", MODEL, BIPED + "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V#after:aload 0",
                BIPED + "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V#head");
        public static final Site RENDER_POST = site("biped model render (end)", MODEL, BIPED + "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V#tail");
        /** ModelBiped.setRotationAngles, first and last thing. */
        public static final Site ANGLES_PRE = site("biped model pose (start)", MODEL, BIPED + "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V#after:aload 0",
                BIPED + "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V#head");
        public static final Site ANGLES_POST = site("biped model pose (end)", MODEL, BIPED + "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V#tail");

        private Client() {}
    }

    // ---- what the modern places call (mixins): the values as legacy views

    /** {@link #TRAVEL}: whether a hook took the movement step over (the travel vector is strafe, up, forward). */
    public static boolean travel(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.phys.Vec3 travel) {
        return TRAVEL.active() && TRAVEL.cancels(EntityBridge.wrap(entity), (float) travel.x, (float) travel.z);
    }

    /** {@link #GRAVITY}. */
    public static double gravity(net.minecraft.world.entity.Entity entity, double gravity) {
        if (gravity == 0.0 || !GRAVITY.active()) return gravity;
        return ((Number) GRAVITY.modify(0, gravity, EntityBridge.wrap(entity))).doubleValue();
    }

    /** {@link #HEAL_RATE}. */
    public static int healRate(net.minecraft.world.entity.player.Player player, int ticks) {
        if (player.level().isClientSide || !HEAL_RATE.active()) return ticks;
        return ((Number) HEAL_RATE.modify(1, EntityBridge.wrap(player), ticks)).intValue();
    }

    /** {@link #WEATHER}: whether a hook skips this weather step. */
    public static boolean skipsWeather(net.minecraft.world.level.Level level) {
        return WEATHER.active() && WEATHER.cancels(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
    }

    /** {@link #SWING_TICKS}. */
    public static int swingTicks(net.minecraft.world.entity.LivingEntity entity, int ticks) {
        if (!SWING_TICKS.active()) return ticks;
        return ((Number) SWING_TICKS.modify(0, ticks, EntityBridge.wrap(entity))).intValue();
    }

    /** {@link #SPRINT_PARTICLES}. */
    public static boolean sprintParticles(net.minecraft.world.entity.Entity entity, boolean sprinting) {
        if (!SPRINT_PARTICLES.active()) return sprinting;
        return Boolean.TRUE.equals(SPRINT_PARTICLES.modify(0, sprinting, EntityBridge.wrap(entity)));
    }

    /** {@link #PLAYER_ATTACK}: the hook's answer, or null when there is none (the game then damages the target itself). */
    public static Boolean playerAttack(net.minecraft.world.entity.player.Player attacker, net.minecraft.world.entity.Entity target,
            net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!PLAYER_ATTACK.active()) return null;
        return PLAYER_ATTACK.replace(null, EntityBridge.wrap(target), EventBridge1710.legacySource(source), amount, EntityBridge.wrap(attacker)) instanceof Boolean b ? b : null;
    }

    private CoremodSites() {}

    /** One place of 1.7.10's code on the modern game. */
    public static final class Site {
        private final String label;
        private final Class<?>[] offered;
        private final List<Listed> listed = new ArrayList<>();
        private volatile Bound[] hooks;
        private int failures;

        private Site(String label, Class<?>[] offered, String[] keys) {
            this.label = label;
            this.offered = offered;
            Map<String, Listed> once = new LinkedHashMap<>(); // several 1.7.10 places can be one modern place: each hook method once
            for (String key : keys) {
                for (Listed l : LISTED.getOrDefault(key, List.of())) once.putIfAbsent(l.owner + "." + l.name + l.desc, l);
            }
            listed.addAll(once.values());
        }

        /** Whether the loader listed a hook here (known from the start; the hooks may not be callable yet). */
        public boolean listed() {
            return !listed.isEmpty();
        }

        /** Whether there is a hook to call: check before building the values. */
        public boolean active() {
            if (listed.isEmpty()) return false;
            Bound[] h = hooks;
            if (h == null) {
                if (!ready()) return false;
                h = resolve();
            }
            return h.length > 0;
        }

        /** Calls every hook; true when one answered true (a head hook that cancels the method). */
        public boolean cancels(Object... values) {
            boolean cancel = false;
            for (Bound b : bound()) {
                if (!b.combiner && Boolean.TRUE.equals(call(b, values))) cancel = true;
            }
            return cancel;
        }

        /** Calls every hook, ignoring results. */
        public void fire(Object... values) {
            for (Bound b : bound()) if (!b.combiner) call(b, values);
        }

        /** Passes {@code values[index]} through every hook (each result replaces it) and returns it. */
        public Object modify(int index, Object... values) {
            Bound[] all = bound();
            for (Bound b : all) {
                if (b.combiner) continue;
                Object r = call(b, values);
                if (r == null) continue;
                Bound combine = takes(b, index) ? null : combiner(all, b);
                if (combine != null) {
                    Object joined = call(combine, new Object[] {values[index], r});
                    if (joined != null) values[index] = joined;
                } else {
                    values[index] = r;
                }
            }
            return values[index];
        }

        /** The result of the first hook (one that stands in for a call), or {@code fallback} when it failed. */
        public Object replace(Object fallback, Object... values) {
            for (Bound b : bound()) {
                if (b.combiner) continue;
                Object r = call(b, values);
                return r != null ? r : fallback;
            }
            return fallback;
        }

        private Bound[] bound() {
            Bound[] h = hooks;
            return h != null ? h : active() ? hooks : NONE;
        }

        private Object call(Bound b, Object[] values) {
            try {
                Object[] args = new Object[b.from.length];
                for (int i = 0; i < args.length; i++) {
                    Object v = values[b.from[i]];
                    if (b.narrowed[i] != null && v != null && !b.narrowed[i].isInstance(v)) return null; // the hook is for a subclass
                    args[i] = v;
                }
                return b.handle.invoke(args);
            } catch (Throwable t) {
                if (failures++ < 3) LOG.warn("A coremod hook failed ({}, {})", label, b.name, t);
                return null;
            }
        }

        private synchronized Bound[] resolve() {
            if (hooks != null) return hooks;
            List<Bound> out = new ArrayList<>();
            for (Listed l : listed) {
                Class<?> joins = combines(l.desc);
                Bound b = joins != null ? bind(l, new Class<?>[] {joins, joins}, true) : bind(l, offered, false);
                if (b != null) out.add(b);
            }
            return hooks = out.toArray(NONE);
        }
    }

    private record Bound(String name, MethodHandle handle, int[] from, Class<?>[] narrowed, boolean combiner, ClassLoader mod) {}

    private static boolean takes(Bound b, int index) {
        for (int from : b.from) if (from == index) return true;
        return false;
    }

    /** The helper of the same mod that joins a value with a hook's result. */
    private static Bound combiner(Bound[] all, Bound hook) {
        for (Bound b : all) if (b.combiner && b.mod == hook.mod) return b;
        return null;
    }

    /** The type a helper like {@code multiply(FF)F} joins, or null when the descriptor is not of that form. */
    private static Class<?> combines(String desc) {
        return switch (desc) {
            case "(ZZ)Z" -> boolean.class;
            case "(II)I" -> int.class;
            case "(FF)F" -> float.class;
            case "(DD)D" -> double.class;
            case "(JJ)J" -> long.class;
            default -> null;
        };
    }

    private static final Bound[] NONE = new Bound[0];

    private static Site site(String label, Class<?>[] offered, String... keys) {
        Site s = new Site(label, offered, keys);
        SITES.add(s);
        return s;
    }

    /** Whether the loader listed any hook at all (cheap; false in a pack without coremods that patch Minecraft). */
    public static boolean any() {
        return !LISTED.isEmpty();
    }

    /** Server side wiring of the sites that are not a place in code: the tick length goes to the game's tick rate manager (which tells the clients too). */
    public static void register(net.neoforged.bus.api.IEventBus gameBus) {
        if (LISTED.isEmpty()) return;
        gameBus.addListener((net.neoforged.neoforge.event.server.ServerStartingEvent e) -> report());
        if (ENTITY_REMOVED.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
                net.minecraft.world.entity.Entity entity = e.getEntity();
                // 1.7.10 removed entities that died, were discarded or logged out through this method; a chunk unload went another way
                net.minecraft.world.entity.Entity.RemovalReason reason = entity.getRemovalReason();
                if (reason == null || !(reason.shouldDestroy() || entity instanceof net.minecraft.world.entity.player.Player)) return;
                if (ENTITY_REMOVED.active()) ENTITY_REMOVED.fire(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(e.getLevel()), EntityBridge.wrap(entity));
            });
        }
        if (TICK_MILLIS.listed()) {
            float[] last = {20.0F};
            gameBus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
                if (!TICK_MILLIS.active()) return;
                long millis = ((Number) TICK_MILLIS.modify(0, 50L)).longValue();
                if (millis <= 0) return;
                float rate = Math.max(1.0F, Math.min(100.0F, 1000.0F / millis));
                if (Math.abs(rate - last[0]) > 0.01F) { // only changes are applied, so /tick rate keeps working while the mods ask for the normal length
                    last[0] = rate;
                    e.getServer().tickRateManager().setTickRate(rate);
                }
            });
        }
    }

    /** One line: how many of the listed hooks have a place here. */
    private static void report() {
        if (!ready()) return;
        Map<String, Boolean> replayed = new HashMap<>();
        int sites = 0;
        for (Site s : SITES) {
            if (!s.active()) continue;
            sites++;
            for (Bound b : s.hooks) replayed.put(b.name, true);
        }
        Map<String, Integer> missing = new TreeMap<>();
        int total = 0;
        Map<String, Boolean> seen = new HashMap<>();
        for (List<Listed> list : LISTED.values()) {
            for (Listed l : list) {
                String name = l.owner.replace('/', '.') + "." + l.name + l.desc;
                if (seen.put(l.site + name, true) != null) continue;
                total++;
                if (!replayed.containsKey(name)) missing.merge(l.site, 1, Integer::sum);
            }
        }
        LOG.info("Legacy coremods put {} hook(s) into Minecraft 1.7.10's classes; {} hook method(s) are replayed at {} place(s) of the modern game, {} place(s) have no "
                + "equivalent yet (features of those mods that depend on them do nothing)", total, replayed.size(), sites, missing.size());
        if ("full".equals(System.getProperty("legacyconnector.coremodHooks.report"))) {
            missing.forEach((site, n) -> LOG.info("  no place yet: {} ({} hook(s): {})", site, n, LISTED.get(site).stream().map(l -> l.owner + "." + l.name).distinct().toList()));
        } else {
            missing.forEach((site, n) -> LOG.debug("  no place yet: {} ({} hook(s))", site, n));
        }
    }

    /** Hooks are mod classes: callable once every 1.7.10 mod has been constructed (their classes must not be initialised earlier). */
    private static boolean ready() {
        if (ready) return true;
        boolean any = false;
        for (LegacyModRecord rec : LegacyMods.all()) {
            if (!"1.7.10".equals(LegacyMods.family(rec.version))) continue;
            if (rec.mainClass == null) return false;
            any = true;
        }
        return ready = any;
    }

    private static Bound bind(Listed l, Class<?>[] offered, boolean combiner) {
        String className = l.owner.replace('/', '.');
        try {
            Class<?> owner = null;
            for (LegacyModRecord rec : LegacyMods.all()) {
                if (rec.mainClass == null) continue;
                try {
                    owner = Class.forName(className, true, rec.mainClass.getClassLoader());
                    break;
                } catch (ClassNotFoundException e) {
                    // another mod's loader
                }
            }
            if (owner == null) {
                LOG.debug("Coremod hook {}.{}: the class is not loaded (its mod is not in this pack any more)", className, l.name);
                return null;
            }
            String[] params = parameters(l.desc);
            Method method = null;
            for (Method m : owner.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) || !m.getName().equals(l.name) || m.getParameterCount() != params.length) continue;
                boolean same = true;
                for (int i = 0; i < params.length && same; i++) same = legacyName(m.getParameterTypes()[i]).equals(params[i]);
                if (same) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                LOG.debug("Coremod hook {}.{}{} was not found in the translated mod", className, l.name, l.desc);
                return null;
            }
            Class<?>[] types = method.getParameterTypes();
            int[] from = new int[types.length];
            Class<?>[] narrowed = new Class<?>[types.length];
            boolean[] taken = new boolean[offered.length];
            for (int i = 0; i < types.length; i++) {
                int pick = -1;
                for (int o = 0; o < offered.length && pick < 0; o++) if (!taken[o] && types[i].isAssignableFrom(offered[o])) pick = o;
                for (int o = 0; o < offered.length && pick < 0; o++) {
                    if (!taken[o] && !types[i].isPrimitive() && offered[o].isAssignableFrom(types[i])) {
                        pick = o;
                        narrowed[i] = types[i];
                    }
                }
                if (pick < 0) {
                    LOG.debug("Coremod hook {}.{}{} takes a value its place does not offer (parameter {})", className, l.name, l.desc, i + 1);
                    return null;
                }
                taken[pick] = true;
                from[i] = pick;
            }
            method.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            handle = handle.asType(handle.type().generic().changeReturnType(Object.class)).asSpreader(Object[].class, types.length)
                    .asType(MethodType.methodType(Object.class, Object[].class));
            return new Bound(className + "." + l.name + l.desc, handle, from, narrowed, combiner, owner.getClassLoader());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOG.warn("Coremod hook {}.{} is not available: {}", className, l.name, e.toString());
            return null;
        }
    }

    /** The 1.7.10 name of a translated parameter type, as descriptors spell it ({@code F}, {@code net/minecraft/entity/Entity}, {@code [I}). */
    private static String legacyName(Class<?> type) {
        if (type.isArray()) return "[" + legacyName(type.getComponentType());
        if (type.isPrimitive()) return MethodType.methodType(type).toMethodDescriptorString().substring(2);
        String name = type.getName();
        if (name.startsWith(SHIM)) name = name.substring(SHIM.length());
        else if (name.startsWith(SYNTH)) name = name.substring(SYNTH.length());
        return name.replace('.', '/');
    }

    private static String[] parameters(String desc) {
        List<String> out = new ArrayList<>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            int start = i;
            while (desc.charAt(i) == '[') i++;
            if (desc.charAt(i) == 'L') {
                int end = desc.indexOf(';', i);
                out.add(desc.substring(start, i) + desc.substring(i + 1, end));
                i = end + 1;
            } else {
                out.add(desc.substring(start, ++i));
            }
        }
        return out.toArray(new String[0]);
    }

    private static Map<String, List<Listed>> load() {
        Map<String, List<Listed>> out = new HashMap<>();
        String file = System.getProperty("legacyconnector.coremodHooks");
        if (file == null) return out;
        try {
            Path path = Path.of(file);
            if (!Files.isRegularFile(path)) return out;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length < 4) continue;
                String site = p[0].replaceFirst("@\\d+$", "");
                out.computeIfAbsent(site, k -> new ArrayList<>()).add(new Listed(site, p[1], p[2], p[3]));
            }
        } catch (java.io.IOException | RuntimeException e) {
            LOG.warn("The coremod hook list {} could not be read: {}", file, e.toString());
        }
        return out;
    }
}
