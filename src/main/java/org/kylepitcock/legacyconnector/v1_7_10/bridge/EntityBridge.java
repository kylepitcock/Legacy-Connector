package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerClient;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy views of modern entities. One view per modern entity (weakly cached) so that legacy identity checks
 * ({@code entity == player}) and per-entity legacy state keep working.
 */
public final class EntityBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Entities");
    /**
     * Identity-keyed (Guava weak keys compare by ==): Minecraft's Entity.equals compares ids, and in singleplayer the
     * client and server players share one, which handed the client the server player's view (and vice versa).
     */
    private static final Map<net.minecraft.world.entity.Entity, Entity> VIEWS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    static {
        org.kylepitcock.legacyconnector.core.ViewSweeper.register("1.7.10 entity", VIEWS,
                e -> e.isRemoved() && !(e instanceof net.minecraft.world.entity.player.Player)); // players change dimension in place
    }
    private static boolean warnedCustom;

    /** Where extended properties of vanilla entities (players) are kept in the modern persistent data. */
    public static final String EXTENDED_KEY = "LegacyConnectorExtended";

    private EntityBridge() {}

    /** Saves the extended properties of every vanilla entity view (world saves, logouts, server stop). */
    public static void registerSaving() {
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> saveExtended(e.getEntity()));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> saveAllExtended());
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> saveAllExtended());
    }

    private static void saveAllExtended() {
        java.util.List<net.minecraft.world.entity.Entity> entities;
        synchronized (VIEWS) {
            entities = new java.util.ArrayList<>(VIEWS.keySet());
        }
        for (var e : entities) saveExtended(e);
    }

    /** Writes the extended properties of a vanilla entity's view into its persistent data (before it is saved). */
    public static void saveExtended(net.minecraft.world.entity.Entity modern) {
        Entity view = VIEWS.get(modern);
        if (view == null || view.mcc$hosted() || !view.mcc$hasExtended()) return;
        var tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        view.mcc$saveExtended(tag);
        modern.getPersistentData().put(EXTENDED_KEY, tag.mcc$modern());
    }

    public static Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern == null) return null;
        Entity view = VIEWS.get(modern);
        if (view != null) return view;
        view = create(modern);
        VIEWS.put(modern, view);
        // Legacy fired EntityConstructing for every entity; mods attach attributes and extended properties here.
        view.mcc$fireConstructing();
        if (view.mcc$hasExtended()) {
            var saved = modern.getPersistentData().getCompound(EXTENDED_KEY);
            if (!saved.isEmpty()) view.mcc$loadExtended(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound(saved));
        }
        return view;
    }

    private static Entity create(net.minecraft.world.entity.Entity modern) {
        if (modern instanceof net.neoforged.neoforge.common.util.FakePlayer fp) return new legacyconnector.v1_7_10.net.minecraftforge.common.util.FakePlayer(fp);
        if (modern instanceof ServerPlayer sp) return new EntityPlayerMP(sp);
        if (modern instanceof Player p) {
            if (!p.level().isClientSide()) return new EntityPlayerClient(p);
            // The local player is a LocalPlayer (client class); compare by name to keep this class server-safe.
            boolean local = false;
            for (Class<?> c = p.getClass(); c != null && !local; c = c.getSuperclass()) {
                local = c.getName().equals("net.minecraft.client.player.LocalPlayer");
            }
            return local ? new legacyconnector.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP(p)
                    : new legacyconnector.v1_7_10.net.minecraft.client.entity.EntityOtherPlayerMP(p);
        }
        if (modern instanceof ItemEntity item) return new EntityItem(item);
        if (modern instanceof net.minecraft.world.entity.ExperienceOrb orb) return new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityXPOrb(orb);
        if (modern instanceof net.minecraft.world.entity.npc.Villager villager) return new VillagerView(villager);
        if (modern instanceof net.minecraft.world.entity.vehicle.AbstractMinecartContainer cart) return new MinecartContainerView(cart);
        if (modern instanceof net.minecraft.world.entity.vehicle.AbstractMinecart cart) return new MinecartView(cart);
        if (modern instanceof net.minecraft.world.entity.vehicle.Boat boat) return new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityBoat(boat);
        if (modern instanceof net.minecraft.world.entity.decoration.ItemFrame frame) return new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItemFrame(frame);
        if (modern instanceof net.minecraft.world.entity.decoration.Painting painting) return new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityPainting(painting);
        if (modern instanceof net.minecraft.world.entity.decoration.LeashFenceKnotEntity knot) return new legacyconnector.v1_7_10.net.minecraft.entity.EntityLeashKnot(knot);
        if (modern instanceof net.minecraft.world.entity.item.FallingBlockEntity falling) return new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityFallingBlock(falling);
        if (modern instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal) return new legacyconnector.v1_7_10.net.minecraft.entity.item.EntityEnderCrystal(crystal);
        if (modern instanceof net.minecraft.world.entity.LightningBolt bolt) return new legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt(bolt);
        if (modern instanceof net.minecraft.world.entity.projectile.FishingHook hook) return new legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook(hook);
        if (modern instanceof LivingEntity living) {
            Entity typed = VanillaMobViews.create(living);
            return typed != null ? typed : new LivingView(living);
        }
        return new View(modern);
    }

    /** Registers an existing legacy object as the view of a freshly created modern entity. */
    public static void bind(Entity legacy, net.minecraft.world.entity.Entity modern) {
        legacy.mcc$entity = modern;
        VIEWS.put(modern, legacy);
    }

    public static boolean spawn(World world, Entity entity) {
        Level level = world.mcc$fullLevel();
        if (level == null) return false;
        if (entity.mcc$entity == null) {
            // Constructed without a world (or before the world was known): give it a host now.
            var host = org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.adopt(entity, world);
            if (host == null) {
                if (!warnedCustom) {
                    warnedCustom = true;
                    LOG.warn("Legacy entity {} could not be given a modern host", entity.getClass().getName());
                }
                return false;
            }
            entity.mcc$entity = host.mcc$self();
        }
        VIEWS.put(entity.mcc$entity, entity);
        // 1.7.10 added a dead entity and dropped it on the next tick; the modern level refuses it (and logs every attempt).
        if (entity.mcc$entity.isRemoved()) return false;
        if (level instanceof net.minecraft.server.level.ServerLevel sl && sl.getEntity(entity.mcc$entity.getUUID()) == entity.mcc$entity) return true;
        if (entity.mcc$hosted() && !level.isClientSide()) {
            ((org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost) entity.mcc$entity).mcc$syncWatcher(entity.mcc$dataWatcher().mcc$toTag());
        }
        return world.mcc$level().addFreshEntity(entity.mcc$entity);
    }

    public static void travelToDimension(Entity entity, int dimension) {
        if (entity.mcc$entity == null || !(entity.mcc$entity.level() instanceof ServerLevel current)) return;
        ServerLevel target = DimensionBridge.level(current.getServer(), dimension);
        if (target == null) return;
        var m = entity.mcc$entity;
        if (m instanceof ServerPlayer sp) {
            sp.teleportTo(target, sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        } else {
            m.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(target, m.position(), m.getDeltaMovement(), m.getYRot(),
                    m.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        }
    }

    public static String legacyName(Entity entity) {
        if (entity.mcc$entity == null) return entity.getClass().getSimpleName();
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.mcc$entity.getType());
        return key.getPath();
    }

    public static EntityPlayer clientPlayer() {
        if (!FMLEnvironment.dist.isClient()) return null;
        return ClientAccess.player();
    }

    /** Isolated so the dedicated server never loads client classes. */
    static final class ClientAccess {
        static EntityPlayer player() {
            var p = net.minecraft.client.Minecraft.getInstance().player;
            return p == null ? null : (EntityPlayer) wrap(p);
        }
    }

    /** View for modern living entities without a dedicated legacy class. */
    public static final class LivingView extends EntityLivingBase {
        LivingView(LivingEntity modern) {
            super(modern);
        }
    }

    /** View for modern villagers. */
    public static final class VillagerView extends legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager {
        VillagerView(net.minecraft.world.entity.npc.Villager modern) {
            super(modern);
        }
    }

    /** View for modern minecarts; the legacy cart types are numbered like the modern ones. */
    public static final class MinecartView extends legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart {
        MinecartView(net.minecraft.world.entity.vehicle.AbstractMinecart modern) {
            super(modern);
        }

        @Override
        public int getMinecartType() {
            return ((net.minecraft.world.entity.vehicle.AbstractMinecart) mcc$entity).getMinecartType().ordinal();
        }
    }

    /** View for modern chest and hopper carts. */
    public static final class MinecartContainerView extends legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecartContainer {
        MinecartContainerView(net.minecraft.world.entity.vehicle.AbstractMinecartContainer modern) {
            super(modern);
        }

        @Override
        public int getMinecartType() {
            return ((net.minecraft.world.entity.vehicle.AbstractMinecart) mcc$entity).getMinecartType().ordinal();
        }

        @Override
        public int getSizeInventory() {
            return ((net.minecraft.world.entity.vehicle.AbstractMinecartContainer) mcc$entity).getContainerSize();
        }
    }

    /** View for any other modern entity. */
    public static final class View extends Entity {
        View(net.minecraft.world.entity.Entity modern) {
            super(modern);
        }
    }
}
