package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vanilla entities whose 1.12 class carries a translated mod's mixins (Wolf Armor and Storage makes EntityWolf an armoured wolf with an
 * inventory). The mixin is applied to the shim class (see the loader's {@code LegacyMixinConfigs}), but a vanilla wolf is a modern wolf
 * with a view object, and the game never calls the view's {@code onUpdate}, {@code processInteract} or NBT methods the mixin injects into.
 * For views of such classes (only those) this class does what 1.12 did:
 *
 * <ul>
 *     <li>each tick the view's {@code onUpdate} and {@code onLivingUpdate} run (their shim bodies do nothing for a view; the injected code runs);</li>
 *     <li>a right click on the entity first runs the view's {@code processInteract}; when the mod's code takes the click, the modern
 *     interaction is cancelled with a success;</li>
 *     <li>what the view writes in {@code writeEntityToNBT} is kept in the entity's persistent data (saved with it) and read back when the
 *     view is made after loading;</li>
 *     <li>values the mod keeps in the view's data manager (armour stack, chest flag) travel to the clients tracking the entity, as the
 *     data watcher did.</li>
 * </ul>
 *
 * The entity's own behaviour is untouched: the modern entity still ticks, interacts and saves as vanilla does.
 */
public final class MixedViews1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Entities");
    private static final String FAMILY = "1.12.2-viewdata";
    /** Where a mixed view's own NBT is kept in the modern entity's persistent data. */
    public static final String NBT_KEY = "LegacyConnector1122View";
    private static final String MIXIN_MERGED = "org.spongepowered.asm.mixin.transformer.meta.MixinMerged";
    private static final ThreadLocal<Boolean> FORWARDING = new ThreadLocal<>();
    private static final AtomicInteger FAILURES = new AtomicInteger();

    /** Whether a shim class (or a shim superclass) got methods from a mixin that is not this layer's own. */
    private static final ClassValue<Boolean> MIXED = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c.getName().startsWith("legacyconnector."); c = c.getSuperclass()) {
                try {
                    for (var m : c.getDeclaredMethods()) {
                        // only injector callbacks count (a parameter of Mixin's CallbackInfo types): code that runs inside the class's
                        // own methods; merged accessors or shadow stubs (on EntityLiving, so on every mob) need no forwarding
                        boolean callback = false;
                        for (Class<?> p : m.getParameterTypes()) callback |= p.getName().startsWith("org.spongepowered.asm.mixin.injection.callback.");
                        if (!callback) continue;
                        for (var a : m.getDeclaredAnnotations()) {
                            if (!a.annotationType().getName().equals(MIXIN_MERGED)) continue;
                            String mixin = mixinOf(a);
                            if (mixin != null && !mixin.startsWith("org.kylepitcock.")) return true;
                        }
                    }
                } catch (RuntimeException | LinkageError e) {
                    return false;
                }
            }
            return false;
        }
    };

    /** Modern type -> whether its vanilla legacy class is mixed (absent: not looked up yet). */
    private static final Map<EntityType<?>, Boolean> TYPES = new ConcurrentHashMap<>();
    /** Whether any type found so far is mixed (the per-tick check is this one read for everyone else). */
    private static volatile boolean any;
    /** The mixed views alive now (weak), for saving. */
    private static final Map<net.minecraft.world.entity.Entity, Entity> VIEWS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private MixedViews1122() {}

    private static String mixinOf(java.lang.annotation.Annotation a) {
        try {
            Object v = a.annotationType().getMethod("mixin").invoke(a);
            return v instanceof String s ? s : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** True while a modern call is being forwarded to a view (the view's shim body leaves the work to the modern entity). */
    public static boolean forwarding() {
        return FORWARDING.get() != null;
    }

    public static boolean isMixed(Class<?> viewClass) {
        return viewClass != null && MIXED.get(viewClass);
    }

    private static boolean mixedType(EntityType<?> type) {
        Boolean known = TYPES.get(type);
        if (known != null) return known;
        boolean mixed = false;
        try {
            String name = VanillaEntities1122.mcc$legacyNameOf(type);
            Class<?> cls = name == null ? null : VanillaEntities1122.mcc$legacyClass(name);
            mixed = cls != null && isMixed(cls);
        } catch (RuntimeException | LinkageError e) {
            // not a vanilla legacy class
        }
        TYPES.put(type, mixed);
        if (mixed) any = true;
        if (mixed) LOG.info("1.12.2 mods' mixins on the legacy class of {}: its views get ticks, clicks, NBT and data sync", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return mixed;
    }

    /** The mixed view of a modern entity, or null when the entity has none (made on demand for mixed types). */
    private static Entity view(net.minecraft.world.entity.Entity modern, boolean create) {
        if (modern == null || !any && !create) return null;
        Entity v = VIEWS.get(modern);
        if (v != null) return v;
        if (!mixedType(modern.getType())) return null;
        v = create ? EntityBridge1122.wrap(modern) : EntityBridge1122.existing(modern);
        if (v == null || v.mcc$hosted() || !isMixed(v.getClass())) return null;
        VIEWS.put(modern, v);
        return v;
    }

    /** Called by {@link EntityBridge1122#wrap} for a new view: reads back what a mixed view saved. */
    public static void restore(Entity view, net.minecraft.world.entity.Entity modern) {
        if (view == null || modern == null || view.mcc$hosted() || !isMixed(view.getClass())) return;
        mixedType(modern.getType());
        any = true;
        VIEWS.put(modern, view);
        CompoundTag saved = modern.getPersistentData().getCompound(NBT_KEY);
        if (saved.isEmpty()) return;
        forward(view, "reading the NBT of", () -> view.mcc$readEntityFromNBT(new NBTTagCompound(saved.copy())));
    }

    private static void save(net.minecraft.world.entity.Entity modern, Entity view) {
        if (modern.level().isClientSide()) return;
        NBTTagCompound tag = new NBTTagCompound();
        forward(view, "writing the NBT of", () -> view.mcc$writeEntityToNBT(tag));
        if (!tag.mcc$modern().isEmpty()) modern.getPersistentData().put(NBT_KEY, tag.mcc$modern());
    }

    private static void saveAll() {
        for (var e : new java.util.ArrayList<>(VIEWS.entrySet())) save(e.getKey(), e.getValue());
    }

    private static void forward(Entity view, String what, Runnable call) {
        Boolean previous = FORWARDING.get();
        FORWARDING.set(Boolean.TRUE);
        try {
            call.run();
        } catch (RuntimeException | LinkageError e) {
            int n = FAILURES.incrementAndGet();
            if (n <= 5) LOG.warn("1.12.2 mixed view: {} {} failed", what, view.getClass().getName(), e);
            else if (n == 6) LOG.warn("1.12.2 mixed view: further failures are not logged");
        } finally {
            if (previous == null) FORWARDING.remove();
        }
    }

    public static void register(net.neoforged.bus.api.IEventBus game) {
        LegacyNetwork.registerReceiver(FAMILY, MixedViews1122::receive);
        game.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> view(e.getEntity(), true));
        game.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) -> {
            if (!any) return;
            var modern = e.getEntity();
            Entity v = view(modern, false);
            if (v == null) return;
            forward(v, "ticking", () -> {
                v.onUpdate();
                if (v instanceof EntityLivingBase l) l.onLivingUpdate();
            });
            if (!modern.level().isClientSide()) {
                CompoundTag data = DataSync1122.changed(v);
                if (data != null) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(modern, payload(modern, data));
                }
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) -> {
            Entity v = view(e.getTarget(), false);
            if (v == null || !(v instanceof EntityLiving living)) return;
            if (!(EntityBridge1122.wrap(e.getEntity()) instanceof EntityPlayer player)) return;
            boolean[] used = new boolean[1];
            forward(v, "a click on", () -> used[0] = living.mcc$processInteract(player, EnumHand.mcc$of(e.getHand())));
            if (used[0]) {
                e.setCancellationResult(InteractionResult.SUCCESS);
                e.setCanceled(true);
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking e) -> {
            if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            Entity v = view(e.getTarget(), false);
            if (v == null) return;
            CompoundTag data = DataSync1122.encode(v);
            if (data != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, payload(e.getTarget(), data));
        });
        game.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> saveAll());
        game.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> saveAll());
        game.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
            if (e.getLevel().isClientSide()) return;
            Entity v = VIEWS.get(e.getEntity());
            if (v != null) save(e.getEntity(), v);
        });
    }

    private static LegacyPayload payload(net.minecraft.world.entity.Entity modern, CompoundTag data) {
        return new LegacyPayload(FAMILY, "", modern.getId(), data.getByteArray("d"));
    }

    /** Client: data manager values of a mixed view, from the server. */
    private static void receive(LegacyPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player() == null ? null : context.player().level();
            if (level == null || !level.isClientSide()) return;
            var modern = level.getEntity(payload.discriminator());
            Entity v = view(modern, true);
            if (v == null) return;
            CompoundTag tag = new CompoundTag();
            tag.putByteArray("d", payload.data());
            DataSync1122.apply(v, tag);
        });
    }
}
