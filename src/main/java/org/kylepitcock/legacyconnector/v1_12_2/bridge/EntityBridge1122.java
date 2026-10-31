package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy views of modern entities, one per entity (weakly cached by identity) so that legacy identity checks ({@code entity == player})
 * and per-entity legacy state (capabilities, AI task lists) keep working. Vanilla mobs are viewed as their 1.12 class (hand-written or
 * synthesized), other modern entities as the closest legacy base class.
 */
public final class EntityBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Entities");
    /**
     * Identity-keyed (Guava weak keys compare by ==): Minecraft's Entity.equals compares ids, and in single player the client and server
     * players share one.
     */
    private static final Map<net.minecraft.world.entity.Entity, Entity> ENTITIES = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    /** The modern entity a legacy constructor running on this thread is to view (views of synthesized vanilla classes). */
    private static final ThreadLocal<net.minecraft.world.entity.Entity> VIEW_BINDING = new ThreadLocal<>();
    /** Where the capabilities of vanilla entities (players) are kept in the modern persistent data. */
    public static final String CAPS_KEY = "LegacyConnector1122Caps";

    static {
        org.kylepitcock.legacyconnector.core.ViewSweeper.register("1.12.2 entity", ENTITIES,
                e -> e.isRemoved() && !(e instanceof net.minecraft.world.entity.player.Player)); // players change dimension in place
    }

    private EntityBridge1122() {}

    public static Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern == null) return null;
        Entity view = ENTITIES.get(modern);
        if (view != null) return view;
        synchronized (ENTITIES) {
            view = ENTITIES.get(modern);
            if (view != null) return view;
            view = create(modern);
            ENTITIES.put(modern, view);
        }
        // legacy fired EntityConstructing and attached capabilities for every entity
        view.mcc$fireConstructing();
        if (view.mcc$hasCaps()) {
            var saved = modern.getPersistentData().getCompound(CAPS_KEY);
            if (!saved.isEmpty()) view.mcc$readCaps(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(saved));
        }
        return view;
    }

    /** The view already made for a modern entity, or null (no view is created). */
    public static Entity existing(net.minecraft.world.entity.Entity modern) {
        return modern == null ? null : ENTITIES.get(modern);
    }

    /** Called by the legacy constructors: the modern entity to view, if a view is being made on this thread (read once). */
    public static net.minecraft.world.entity.Entity takeViewBinding() {
        var m = VIEW_BINDING.get();
        if (m != null) VIEW_BINDING.remove();
        return m;
    }

    private static Entity create(net.minecraft.world.entity.Entity modern) {
        if (modern instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122 h && h.mcc$legacy() != null) return h.mcc$legacy();
        if (modern instanceof net.neoforged.neoforge.common.util.FakePlayer fp) return new legacyconnector.v1_12_2.net.minecraftforge.common.util.FakePlayer(fp);
        if (modern instanceof ServerPlayer sp) return new legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP(sp);
        if (modern instanceof net.minecraft.world.entity.player.Player p) {
            if (!p.level().isClientSide()) return new PlayerView(p);
            // the local player is a LocalPlayer (client class); compared by name to keep this class server-safe
            boolean local = false;
            for (Class<?> c = p.getClass(); c != null && !local; c = c.getSuperclass()) local = c.getName().equals("net.minecraft.client.player.LocalPlayer");
            return local ? new legacyconnector.v1_12_2.net.minecraft.client.entity.EntityPlayerSP(p)
                    : new legacyconnector.v1_12_2.net.minecraft.client.entity.EntityOtherPlayerMP(p);
        }
        if (modern instanceof net.minecraft.world.entity.item.ItemEntity item) return new legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem(item);
        if (modern instanceof net.minecraft.world.entity.ExperienceOrb orb) return new legacyconnector.v1_12_2.net.minecraft.entity.item.EntityXPOrb(orb);
        if (modern instanceof net.minecraft.world.entity.LightningBolt bolt) return new legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt(bolt);
        Entity typed = vanillaView(modern);
        if (typed != null) return typed;
        return baseView(modern);
    }

    /** A view of the vanilla legacy class of the modern type, constructed with the view binding set (null when there is none). */
    private static Entity vanillaView(net.minecraft.world.entity.Entity modern) {
        String name = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaEntities1122.mcc$legacyNameOf(modern.getType());
        Class<?> cls = name == null ? null : org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaEntities1122.mcc$legacyClass(name);
        if (cls == null) return null;
        var previous = VIEW_BINDING.get();
        VIEW_BINDING.set(modern);
        try {
            java.lang.reflect.Constructor<?> ctor;
            Object made;
            try {
                ctor = cls.getDeclaredConstructor(World.class);
                ctor.setAccessible(true);
                made = ctor.newInstance(WorldBridge1122.wrap(modern.level()));
            } catch (NoSuchMethodException e) {
                ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                made = ctor.newInstance();
            }
            Entity view = (Entity) made;
            if (view.mcc$entity != modern) view.mcc$entity = modern;
            return view;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.debug("No {} view for {}: {}", cls.getName(), modern.getType(), e.toString());
            return null;
        } finally {
            if (previous != null) VIEW_BINDING.set(previous);
            else VIEW_BINDING.remove();
        }
    }

    /** The closest legacy base class of a modern entity without a vanilla legacy class. */
    private static Entity baseView(net.minecraft.world.entity.Entity modern) {
        if (modern instanceof net.minecraft.world.entity.TamableAnimal a) return new TameableView(a);
        if (modern instanceof net.minecraft.world.entity.animal.Animal a) return new AnimalView(a);
        if (modern instanceof net.minecraft.world.entity.AgeableMob a) return new AgeableView(a);
        if (modern instanceof net.minecraft.world.entity.monster.Enemy && modern instanceof net.minecraft.world.entity.PathfinderMob m) return new MonsterView(m);
        if (modern instanceof net.minecraft.world.entity.animal.WaterAnimal w) return new WaterMobView(w);
        if (modern instanceof net.minecraft.world.entity.animal.AbstractGolem g) return new GolemView(g);
        if (modern instanceof net.minecraft.world.entity.PathfinderMob m) return new CreatureView(m);
        if (modern instanceof net.minecraft.world.entity.ambient.AmbientCreature a) return new AmbientView(a);
        if (modern instanceof net.minecraft.world.entity.FlyingMob f) return new FlyingView(f);
        if (modern instanceof net.minecraft.world.entity.Mob m) return new LivingView(m);
        if (modern instanceof LivingEntity l) return new LivingBaseView(l);
        if (modern instanceof net.minecraft.world.entity.projectile.AbstractArrow a) return new ArrowView(a);
        if (modern instanceof net.minecraft.world.entity.projectile.ThrowableProjectile t) return new ThrowableView(t);
        return new View(modern);
    }

    /** Registers a legacy object as the view of a modern entity (a host's legacy object, or a vanilla class a mod constructed). */
    public static void bind(Entity legacy, net.minecraft.world.entity.Entity modern) {
        legacy.mcc$entity = modern;
        ENTITIES.put(modern, legacy);
    }

    /**
     * The modern entity of a legacy entity about to be spawned: its own (a view or a hosted mod entity), or a new host for a mod entity
     * constructed without a world; null when none can be made. (Area W's {@code WorldBridge1122.hostEntity} should call this.)
     */
    public static net.minecraft.world.entity.Entity host(World world, Entity entity) {
        if (entity == null) return null;
        if (entity.mcc$entity == null && world != null && world.mcc$level() != null) {
            var host = org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.adopt(entity, world);
            if (host == null) return null;
            entity.mcc$entity = host.mcc$self();
            entity.mcc$markHosted();
        }
        if (entity.mcc$entity != null) ENTITIES.put(entity.mcc$entity, entity);
        return entity.mcc$entity;
    }

    /** {@code World.spawnEntity}: adds a legacy entity's modern entity to the level (hosting a mod entity that has none yet). */
    public static boolean spawn(World world, Entity entity) {
        if (world == null || entity == null) return false;
        Level level = world.mcc$level();
        if (level == null) return false;
        if (entity.mcc$entity == null) {
            var host = org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.adopt(entity, world);
            if (host == null) {
                LOG.debug("1.12.2 entity {} could not be given a modern host", entity.getClass().getName());
                return false;
            }
            entity.mcc$entity = host.mcc$self();
            entity.mcc$markHosted();
        }
        var modern = entity.mcc$entity;
        ENTITIES.put(modern, entity);
        // 1.12 added a dead entity and dropped it on the next tick; the modern level refuses it (and logs every attempt)
        if (modern.isRemoved()) return false;
        if (level instanceof ServerLevel sl && sl.getEntity(modern.getUUID()) == modern) return true;
        return level.addFreshEntity(modern);
    }

    /** 1.12 dimension ids: 0 overworld, -1 nether, 1 the end; other levels by their registered legacy dimension type. */
    public static int dimensionId(Level level) {
        if (level == null) return 0;
        var key = level.dimension();
        if (key == Level.OVERWORLD) return 0;
        if (key == Level.NETHER) return -1;
        if (key == Level.END) return 1;
        Integer modId = org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.idOf(key.location());
        if (modId != null) return modId;
        String path = key.location().getPath();
        for (var type : legacyconnector.v1_12_2.net.minecraft.world.DimensionType.values()) {
            if (path.equals(type.getName())) return type.getId();
        }
        return key.location().hashCode();
    }

    /** The modern level of a 1.12 dimension id on the server, or null. */
    public static ServerLevel level(net.minecraft.server.MinecraftServer server, int dimension) {
        if (server == null) return null;
        if (dimension == 0) return server.overworld();
        if (dimension == -1) return server.getLevel(Level.NETHER);
        if (dimension == 1) return server.getLevel(Level.END);
        for (ServerLevel l : server.getAllLevels()) if (dimensionId(l) == dimension) return l;
        return null;
    }

    /** Entity.changeDimension: the same position in the target level; returns the entity there (null when it could not travel). */
    public static Entity changeDimension(Entity entity, int dimension) {
        var m = entity.mcc$entity;
        if (m == null || !(m.level() instanceof ServerLevel current)) return null;
        ServerLevel target = level(current.getServer(), dimension);
        if (target == null || target == current) return target == current ? entity : null;
        if (m instanceof ServerPlayer sp) {
            sp.teleportTo(target, sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
            return entity;
        }
        var moved = m.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(target, m.position(), m.getDeltaMovement(), m.getYRot(),
                m.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        return moved == null ? null : wrap(moved);
    }

    // ------------------------------------------------------------------ capabilities of vanilla entities

    /** Saves the capabilities of every vanilla entity view (world saves, logouts, server stop). */
    public static void registerSaving(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> saveCaps(e.getEntity()));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.SaveToFile e) -> saveCaps(e.getEntity()));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> saveAllCaps());
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> saveAllCaps());
        bus.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && !(e.getEntity() instanceof net.minecraft.world.entity.player.Player)) saveCaps(e.getEntity());
        });
    }

    private static void saveAllCaps() {
        java.util.List<net.minecraft.world.entity.Entity> entities;
        synchronized (ENTITIES) {
            entities = new java.util.ArrayList<>(ENTITIES.keySet());
        }
        for (var e : entities) if (!e.level().isClientSide()) saveCaps(e);
    }

    /** Writes the capabilities of a vanilla entity's view into its persistent data (before it is saved). */
    public static void saveCaps(net.minecraft.world.entity.Entity modern) {
        Entity view = ENTITIES.get(modern);
        if (view == null || view.mcc$hosted() || !view.mcc$hasCaps()) return;
        var tag = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound();
        view.mcc$writeCaps(tag);
        if (tag.hasKey("ForgeCaps")) modern.getPersistentData().put(CAPS_KEY, tag.mcc$modern());
    }

    /** Client: the local player's view, or null (also on a dedicated server). */
    public static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer clientPlayer() {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return null;
        return ClientAccess.player();
    }

    /** Isolated so the dedicated server never loads client classes. */
    static final class ClientAccess {
        static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player() {
            var p = net.minecraft.client.Minecraft.getInstance().player;
            return p == null ? null : (legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer) wrap(p);
        }
    }

    // ------------------------------------------------------------------ base views

    /** A server-side player that is not a ServerPlayer. */
    public static final class PlayerView extends legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer {
        PlayerView(net.minecraft.world.entity.player.Player modern) {
            super(modern);
        }

        @Override
        public boolean isSpectator() {
            return mcc$player().isSpectator();
        }

        @Override
        public boolean isCreative() {
            return mcc$player().isCreative();
        }
    }

    public static final class LivingBaseView extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase {
        LivingBaseView(LivingEntity modern) {
            super(modern);
        }
    }

    public static final class LivingView extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving {
        LivingView(net.minecraft.world.entity.Mob modern) {
            super(modern);
        }
    }

    public static final class CreatureView extends legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature {
        CreatureView(net.minecraft.world.entity.PathfinderMob modern) {
            super(modern);
        }
    }

    public static final class AgeableView extends legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable {
        AgeableView(net.minecraft.world.entity.AgeableMob modern) {
            super(modern);
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
            return null;
        }
    }

    public static final class AnimalView extends legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal {
        AnimalView(net.minecraft.world.entity.animal.Animal modern) {
            super(modern);
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
            return null;
        }
    }

    public static final class TameableView extends legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityTameable {
        TameableView(net.minecraft.world.entity.TamableAnimal modern) {
            super(modern);
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
            return null;
        }
    }

    public static final class MonsterView extends legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityMob {
        MonsterView(net.minecraft.world.entity.PathfinderMob modern) {
            super(modern);
        }
    }

    public static final class GolemView extends legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityGolem {
        GolemView(net.minecraft.world.entity.animal.AbstractGolem modern) {
            super(modern);
        }
    }

    public static final class WaterMobView extends legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityWaterMob {
        WaterMobView(net.minecraft.world.entity.animal.WaterAnimal modern) {
            super(modern);
        }
    }

    public static final class AmbientView extends legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAmbientCreature {
        AmbientView(net.minecraft.world.entity.ambient.AmbientCreature modern) {
            super(modern);
        }
    }

    public static final class FlyingView extends legacyconnector.v1_12_2.net.minecraft.entity.EntityFlying {
        FlyingView(net.minecraft.world.entity.FlyingMob modern) {
            super(modern);
        }
    }

    public static final class ArrowView extends legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow {
        ArrowView(net.minecraft.world.entity.projectile.AbstractArrow modern) {
            super(modern);
        }

        @Override
        protected legacyconnector.v1_12_2.net.minecraft.item.ItemStack getArrowStack() {
            return Stacks1122.fromModern(((net.minecraft.world.entity.projectile.AbstractArrow) mcc$entity).getPickupItemStackOrigin());
        }
    }

    public static final class ThrowableView extends legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable {
        ThrowableView(net.minecraft.world.entity.projectile.ThrowableProjectile modern) {
            super(modern);
        }

        @Override
        protected void onImpact(legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult result) {
        }
    }

    /** A modern entity with no legacy class of its own. */
    public static final class View extends Entity {
        View(net.minecraft.world.entity.Entity modern) {
            super(modern);
        }
    }
}
