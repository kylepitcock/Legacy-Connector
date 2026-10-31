package org.kylepitcock.legacyconnector.v1_7_10.compat.dragonapi;

import java.lang.reflect.Constructor;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOD-SPECIFIC COMPAT (mod id "DragonAPI", Reika's library for ChromatiCraft, RotaryCraft, ReactorCraft, ElectriCraft,
 * GeoStrata, VoidMonster, MeteorCraft, CritterPet, ExpandedRedstone). Remove this package and its one registration call
 * in {@code EventBridge1710.register} (plus {@code mixin/ClientLevelSkyColorMixin}) to drop it.
 *
 * <p>DragonAPI's ASM transformer ({@code Reika.DragonAPI.ASM.DragonAPIClassTransformer}, patch classes under
 * {@code Reika/DragonAPI/ASM/Patchers/Hooks/Event/**}) inserts calls into vanilla methods that post DragonAPI's own events
 * ({@code Reika.DragonAPI.Instantiable.Event.**}) on {@code MinecraftForge.EVENT_BUS}. Coremod transformers never run under
 * Legacy Connector (and the vanilla code they patched does not exist), so those events never fired: VoidMonster's
 * darkening ({@code MonsterFX.monsterScreenFactor}) only decays in its {@code EntityRenderingLoopEvent} handler and stayed at
 * full strength for good after one Void Monster was drawn, ChromatiCraft's liquid-chroma pool recipes run from
 * {@code ItemUpdateEvent}, RotaryCraft builds its worktable from {@code PlayerPlaceBlockEvent}, and so on. This class posts
 * the events Reika's mods subscribe to from the closest NeoForge event, building them through their own constructors (the
 * classes are DragonAPI's, loaded from its class loader by name). Every hook first checks that a legacy listener exists, so
 * nothing is built without DragonAPI or without a subscriber.</p>
 *
 * <p>Events that are subscribed but NOT reproduced (no modern point to post them from, or the patched vanilla logic no
 * longer exists): {@code SetBlockEvent.Pre} (before a block changes: NeoForge has no pre-set hook), {@code BlockTickEvent}
 * (vanilla random/scheduled ticks), {@code BlockConsumedByFireEvent}, {@code FireSpreadEvent}, {@code FireChanceEvent},
 * {@code LavaSpawnFireEvent}, {@code LeafDecayEvent}, {@code BlockSpreadEvent}, {@code IceFreezeEvent},
 * {@code LavaFreezeEvent}, {@code CanSeeSkyEvent}, {@code GrassSustainCropEvent} (all inside vanilla block logic),
 * {@code ApplyPotionEvent} (replaces the effect being added; NeoForge's MobEffectEvent.Applicable can only veto),
 * {@code SplashPotionEvent} (VoidMonster: instant health hurting ghost monsters), {@code MobTargetingEvent},
 * {@code AttackAggroEvent}, {@code PigZombieAggroSpreadEvent}, {@code EnderAttackTPEvent}, {@code EnderLookAggroEvent},
 * {@code SpawnerCheckPlayerEvent}, {@code EntitySpawnerCheckEvent}, {@code EntityPushOutOfBlocksEvent},
 * {@code EntityCollisionEvents}, {@code VillagerTradeEvent}, {@code PlayerKeepInventoryEvent}, {@code PlayerSprintEvent},
 * {@code HarvestLevelEvent}, {@code BlockTillEvent}, {@code SlotEvent.*}, {@code PostItemUseEvent},
 * {@code ChunkGenerationEvent}, {@code ChunkRequestEvent}, {@code GenLayerRiverEvent}, {@code GenLayerBeachEvent} (vanilla
 * generation internals), {@code AddRecipeEvent} (DragonAPI's replacement recipe list). Client ones are listed in
 * {@link DragonApiClientEvents}.</p>
 */
public final class DragonApiEvents {
    static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/DragonAPI");
    static final String MOD_ID = "DragonAPI";
    private static final String EVENTS = "Reika.DragonAPI.Instantiable.Event.";

    private static volatile ClassLoader loader;
    private static volatile boolean absent;

    // ItemBlock.placeBlockAt (DragonAPI posts it before the block is set; cancelling stops the placement)
    static final LegacyEvent PLAYER_PLACE_BLOCK = new LegacyEvent(EVENTS + "PlayerPlaceBlockEvent", 9);
    // EntityItem.onUpdate (ChromatiCraft's liquid chroma pool recipes, DragonAPI's item clearing)
    static final LegacyEvent ITEM_UPDATE = new LegacyEvent(EVENTS + "ItemUpdateEvent", 1);
    // EntityXPOrb.onUpdate (DragonAPI merges orbs)
    static final LegacyEvent XP_UPDATE = new LegacyEvent(EVENTS + "XPUpdateEvent", 1);
    // ItemStack.updateAnimation, called for every stack of InventoryPlayer.mainInventory each tick (ChromatiCraft's tools)
    static final LegacyEvent ITEMSTACK_UPDATE = new LegacyEvent(EVENTS + "ItemStackUpdateEvent", 4);
    // BlockFarmland.onFallenUpon (ChromatiCraft floatstone boots, RotaryCraft spring boots)
    static final LegacyEvent FARMLAND_TRAMPLE = new LegacyEvent(EVENTS + "FarmlandTrampleEvent", 6);
    // EntityLiving.canDespawn (RotaryCraft keeps mobs of its spawner controllers)
    static final LegacyEvent FAR_DESPAWN = new LegacyEvent(EVENTS + "LivingFarDespawnEvent", 1);
    // EntityLivingBase.onEntityUpdate, the "inside water" test of the air counter (GeoStrata's smoke vents)
    static final LegacyEvent DECREASE_AIR = new LegacyEvent(EVENTS + "EntityDecreaseAirEvent", 1);
    // EntityCreeper.explode, server side, before the explosion (ReactorCraft's irradiated creepers)
    static final LegacyEvent CREEPER_EXPLODE = new LegacyEvent(EVENTS + "CreeperExplodeEvent", 1);
    // Chunk.setBlock after the change (ChromatiCraft's crystal network path cache, RotaryCraft's EMP)
    static final LegacyEvent SET_BLOCK_POST = new LegacyEvent(EVENTS + "SetBlockEvent$Post", 4);

    private DragonApiEvents() {}

    /** DragonAPI's class loader once its main class is loaded; null without DragonAPI (cached) or before it constructs. */
    static ClassLoader loader() {
        ClassLoader l = loader;
        if (l != null || absent) return l;
        LegacyModRecord rec = LegacyMods.byLegacyId(MOD_ID);
        if (rec == null) {
            // legacy mods are discovered before any world exists, so this is final by the time a hook runs
            absent = true;
            return null;
        }
        if (rec.mainClass == null) return null;
        return loader = rec.mainClass.getClassLoader();
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, (net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent e) -> onPlace(e));
        bus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) -> {
            var entity = e.getEntity();
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                if (ITEM_UPDATE.wanted()) ITEM_UPDATE.post(EntityBridge.wrap(entity));
            } else if (entity instanceof net.minecraft.world.entity.ExperienceOrb) {
                if (XP_UPDATE.wanted()) XP_UPDATE.post(EntityBridge.wrap(entity));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
            if (!ITEMSTACK_UPDATE.wanted()) return;
            var player = e.getEntity();
            var items = player.getInventory().items;
            Object legacy = null;
            for (int i = 0; i < items.size(); i++) {
                var stack = items.get(i);
                if (stack.isEmpty()) continue;
                if (legacy == null) legacy = EntityBridge.wrap(player);
                ITEMSTACK_UPDATE.post(ItemStack.mcc$wrap(stack), legacy, i, i == player.getInventory().selected);
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent e) -> {
            if (!FARMLAND_TRAMPLE.wanted() || !(e.getLevel() instanceof Level level)) return;
            var p = e.getPos();
            Event ev = FARMLAND_TRAMPLE.create(World.mcc$wrap(level), p.getX(), p.getY(), p.getZ(), EntityBridge.wrap(e.getEntity()), e.getFallDistance());
            // DragonAPI: DENY keeps the farmland, ALLOW tramples, DEFAULT keeps vanilla's decision (NeoForge only asks when vanilla would trample)
            if (ev != null && (FARMLAND_TRAMPLE.send(ev) || ev.getResult() == Event.Result.DENY)) e.setCanceled(true);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.MobDespawnEvent e) -> {
            if (e.getResult() != net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.DEFAULT || e.getEntity().isPersistenceRequired()
                    || !FAR_DESPAWN.wanted()) return;
            // the patched canDespawn() returned "vanilla && !post(event)": a cancelled event keeps the mob
            Event ev = FAR_DESPAWN.create(EntityBridge.wrap(e.getEntity()));
            if (ev != null && FAR_DESPAWN.send(ev)) e.setResult(net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.DENY);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingBreatheEvent e) -> {
            if (!DECREASE_AIR.wanted()) return;
            // the patched test: ALLOW loses air (even out of water), DENY keeps it, DEFAULT is vanilla's "inside water"
            Event ev = DECREASE_AIR.create(EntityBridge.wrap(e.getEntity()));
            if (ev == null) return;
            DECREASE_AIR.send(ev);
            if (ev.getResult() == Event.Result.ALLOW) e.setCanBreathe(false);
            else if (ev.getResult() == Event.Result.DENY) e.setCanBreathe(true);
        });
        bus.addListener((net.neoforged.neoforge.event.level.ExplosionEvent.Start e) -> {
            if (!(e.getExplosion().getDirectSourceEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper)
                    || e.getLevel().isClientSide() || !CREEPER_EXPLODE.wanted()) return;
            CREEPER_EXPLODE.post(EntityBridge.wrap(creeper)); // DragonAPI discarded the post result: not cancellable
        });
        // SetBlockEvent.Post: NeoForge has no event for every block change; a neighbour notification follows each change made
        // with the "update neighbours" flag (players, most mod and vanilla block changes), which is where this posts it.
        bus.addListener((net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent e) -> {
            if (!SET_BLOCK_POST.wanted() || !(e.getLevel() instanceof Level level)) return;
            var p = e.getPos();
            var chunk = World.mcc$wrap(level).getChunkFromChunkCoords(p.getX() >> 4, p.getZ() >> 4);
            if (chunk != null) SET_BLOCK_POST.post(chunk, p.getX() & 15, p.getY(), p.getZ() & 15);
        });
        if (FMLEnvironment.dist.isClient()) DragonApiClientEvents.register(bus);
    }

    /** PlayerPlaceBlockEvent(world, x, y, z, side, block, meta, held stack, player); cancelling it undoes the placement. */
    private static void onPlace(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player pl) || !(e.getLevel() instanceof Level level)
                || !PLAYER_PLACE_BLOCK.wanted()) return;
        var p = e.getPos();
        Event ev = PLAYER_PLACE_BLOCK.create(World.mcc$wrap(level), p.getX(), p.getY(), p.getZ(), side(pl, p),
                BlockBridge.legacyBlock(e.getPlacedBlock()), BlockBridge.legacyMeta(e.getPlacedBlock()),
                ItemStack.mcc$wrap(pl.getMainHandItem()), EntityBridge.wrap(pl));
        if (ev != null && PLAYER_PLACE_BLOCK.send(ev)) e.setCanceled(true);
    }

    /** The face the player clicked to place at {@code pos} (NeoForge's event does not carry it): the look ray, else top. */
    private static int side(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        try {
            if (player.pick(player.blockInteractionRange(), 1.0F, false) instanceof net.minecraft.world.phys.BlockHitResult hit
                    && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    && (hit.getBlockPos().equals(pos) || hit.getBlockPos().relative(hit.getDirection()).equals(pos))) {
                return hit.getDirection().get3DDataValue();
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return 1;
    }

    /**
     * One DragonAPI event class, resolved by name from DragonAPI's class loader on first use, with its constructor of the
     * given arity (each of these events has one). Arguments are checked against the constructor's parameter types, so a
     * vanilla object without the expected legacy view skips the event instead of throwing.
     */
    static final class LegacyEvent {
        private final String className;
        private final int arity;
        private volatile boolean resolved;
        private Class<? extends Event> type;
        private Constructor<?> ctor;
        private int failures;

        LegacyEvent(String className, int arity) {
            this.className = className;
            this.arity = arity;
        }

        /** True when DragonAPI is loaded, the event class exists and a legacy handler listens to it. */
        boolean wanted() {
            if (!resolved && !resolve()) return false;
            return type != null && MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
        }

        private synchronized boolean resolve() {
            if (resolved) return true;
            ClassLoader l = loader();
            if (l == null) {
                if (absent) resolved = true;
                return resolved;
            }
            try {
                Class<?> c = Class.forName(className, true, l);
                for (Constructor<?> k : c.getConstructors()) {
                    if (k.getParameterCount() == arity) ctor = k;
                }
                if (ctor != null && Event.class.isAssignableFrom(c)) type = c.asSubclass(Event.class);
                else LOG.warn("DragonAPI event {} has no {}-argument constructor; not bridged", className, arity);
            } catch (ClassNotFoundException | LinkageError ex) {
                LOG.warn("DragonAPI event {} could not be loaded; not bridged: {}", className, ex.toString());
            }
            resolved = true;
            return true;
        }

        /** The event built from the arguments, or null when they do not fit its constructor or it threw. */
        Event create(Object... args) {
            if (ctor == null) return null;
            Class<?>[] params = ctor.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (!params[i].isPrimitive() && args[i] != null && !params[i].isInstance(args[i])) return null;
                if (params[i].isPrimitive() && args[i] == null) return null;
            }
            try {
                return (Event) ctor.newInstance(args);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                failed(ex instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : ex);
                return null;
            }
        }

        /** Posts the event; true when a handler cancelled it. */
        boolean send(Event ev) {
            try {
                return MinecraftForge.EVENT_BUS.post(ev);
            } catch (RuntimeException | LinkageError ex) {
                failed(ex);
                return false;
            }
        }

        /** Builds and posts; true when cancelled. */
        boolean post(Object... args) {
            Event ev = create(args);
            return ev != null && send(ev);
        }

        private void failed(Throwable ex) {
            if (failures++ < 3) LOG.warn("Posting DragonAPI's {} failed: {}", className, ex.toString());
        }
    }
}
