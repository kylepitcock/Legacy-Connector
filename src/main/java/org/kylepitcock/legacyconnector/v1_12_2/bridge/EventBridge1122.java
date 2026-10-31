package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.PlayerEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent.TickEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;
import net.neoforged.bus.api.IEventBus;

/**
 * Posts 1.12.2's gameplay events on {@code MinecraftForge.EVENT_BUS} (FML's bus is the same one in 1.12) from the modern
 * events: ticks, players logging in and out, respawning and changing dimension, crafting and smelting, entities joining a
 * world, worlds loading, unloading and saving. Events nobody subscribed to are not built.
 */
public final class EventBridge1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Events1122");
    private static boolean registered;
    private static final java.util.Map<Class<?>, Long> LAST_WARNING = new java.util.concurrent.ConcurrentHashMap<>();

    private EventBridge1122() {}

    private static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus bus() {
        return MinecraftForge.EVENT_BUS;
    }

    /** The 1.12 numeric id of a dimension (the id {@link WorldBridge1122#dimensionId} gives its level). */
    public static int dimensionId(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var level = server == null || key == null ? null : server.getLevel(key);
        return level != null ? WorldBridge1122.dimensionId(level) : legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$idOf(key);
    }

    private static boolean wants(Class<? extends Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    /** Posts the event; a failing handler is logged (throttled by type) instead of breaking the modern tick. */
    private static boolean post(Event event) {
        try {
            return MinecraftForge.EVENT_BUS.post(event);
        } catch (RuntimeException | LinkageError e) {
            long now = System.currentTimeMillis();
            Long last = LAST_WARNING.get(event.getClass());
            if (last == null || now - last > 60_000L) {
                LAST_WARNING.put(event.getClass(), now);
                LOG.warn("A 1.12.2 {} handler failed (repeats are logged once a minute): {}", event.getClass().getSimpleName(), e.toString(), e);
            }
            return false;
        }
    }

    private static EntityPlayer player(net.minecraft.world.entity.player.Player p) {
        return EntityBridge1122.wrap(p) instanceof EntityPlayer player ? player : null;
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        IEventBus bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) -> {
            if (wants(TickEvent.ServerTickEvent.class)) post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        });
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            if (wants(TickEvent.ServerTickEvent.class)) post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        });
        bus.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Pre e) -> worldTick(e.getLevel(), TickEvent.Phase.START));
        bus.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post e) -> worldTick(e.getLevel(), TickEvent.Phase.END));
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e) -> playerTick(e.getEntity(), TickEvent.Phase.START));
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> playerTick(e.getEntity(), TickEvent.Phase.END));

        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (wants(PlayerEvent.PlayerLoggedInEvent.class) && player(e.getEntity()) != null) post(new PlayerEvent.PlayerLoggedInEvent(player(e.getEntity())));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (wants(PlayerEvent.PlayerLoggedOutEvent.class) && player(e.getEntity()) != null) post(new PlayerEvent.PlayerLoggedOutEvent(player(e.getEntity())));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) -> {
            if (wants(PlayerEvent.PlayerRespawnEvent.class) && player(e.getEntity()) != null) post(new PlayerEvent.PlayerRespawnEvent(player(e.getEntity()), e.isEndConquered()));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (!wants(PlayerEvent.PlayerChangedDimensionEvent.class) || player(e.getEntity()) == null) return;
            var server = e.getEntity().getServer();
            int from = dimension(server, e.getFrom()), to = dimension(server, e.getTo());
            post(new PlayerEvent.PlayerChangedDimensionEvent(player(e.getEntity()), from, to));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) -> {
            if (wants(PlayerEvent.ItemCraftedEvent.class) && player(e.getEntity()) != null) {
                post(new PlayerEvent.ItemCraftedEvent(player(e.getEntity()), Stacks1122.fromModern(e.getCrafting()), null));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent e) -> {
            if (wants(PlayerEvent.ItemSmeltedEvent.class) && player(e.getEntity()) != null) {
                post(new PlayerEvent.ItemSmeltedEvent(player(e.getEntity()), Stacks1122.fromModern(e.getSmelting())));
            }
        });

        bus.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityJoinWorldEvent.class)) return;
            var view = EntityBridge1122.wrap(e.getEntity());
            if (view != null && post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityJoinWorldEvent(view, WorldBridge1122.wrap(e.getLevel())))) {
                e.setCanceled(true);
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Load e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level && wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Load.class)) {
                post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Load(WorldBridge1122.wrap(level)));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level && wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Unload.class)) {
                post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Unload(WorldBridge1122.wrap(level)));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level && wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Save.class)) {
                post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.Save(WorldBridge1122.wrap(level)));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.AnvilUpdateEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.AnvilUpdateEvent.class)) return;
            var legacy = new legacyconnector.v1_12_2.net.minecraftforge.event.AnvilUpdateEvent(Stacks1122.fromModern(e.getLeft()),
                    Stacks1122.fromModern(e.getRight()), e.getName(), (int) e.getCost());
            post(legacy);
            if (legacy.isCanceled()) {
                e.setCanceled(true);
            } else if (legacy.getOutput() != null && !legacy.getOutput().isEmpty()) {
                e.setOutput(Stacks1122.toModern(legacy.getOutput()));
                e.setCost(legacy.getCost());
                e.setMaterialCost(legacy.getMaterialCost());
            }
        });
        bus.addListener((net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent e) -> {
            if (e.getItemStack().isEmpty() || !wants(legacyconnector.v1_12_2.net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent.class)) return;
            var legacy = new legacyconnector.v1_12_2.net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent(Stacks1122.fromModern(e.getItemStack()),
                    e.getBurnTime());
            post(legacy);
            if (legacy.isCanceled()) e.setBurnTime(legacy.getBurnTime());
        });
        bus.addListener((net.neoforged.neoforge.event.ServerChatEvent e) -> {
            EntityPlayer p;
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.ServerChatEvent.class) || !((p = player(e.getPlayer())) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP mp)) return;
            var component = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation("chat.type.text", p.getName(), e.getRawText());
            var legacy = new legacyconnector.v1_12_2.net.minecraftforge.event.ServerChatEvent(mp, e.getRawText(), component);
            post(legacy);
            if (legacy.isCanceled()) e.setCanceled(true);
            else if (legacy.getComponent() != component && legacy.getComponent() != null) e.setMessage(legacy.getComponent().mcc$modern());
        });
        registerLiving(bus);
        registerInteractions(bus);
        registerAreaF(bus);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) Client.register(bus);
    }

    // ------------------------------------------------------------------ Forge living, player and block events

    private static legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living(net.minecraft.world.entity.Entity e) {
        return EntityBridge1122.wrap(e) instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase l ? l : null;
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos(net.minecraft.core.BlockPos p) {
        return new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d vec(net.minecraft.world.phys.Vec3 v) {
        return v == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(v.x, v.y, v.z);
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.EnumFacing face(net.minecraft.core.Direction d) {
        return d == null ? null : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(d);
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand(net.minecraft.world.InteractionHand h) {
        return h == net.minecraft.world.InteractionHand.OFF_HAND ? legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND
                : legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND;
    }

    private static net.neoforged.neoforge.common.util.TriState tri(Event.Result r) {
        return r == Event.Result.ALLOW ? net.neoforged.neoforge.common.util.TriState.TRUE
                : r == Event.Result.DENY ? net.neoforged.neoforge.common.util.TriState.FALSE : net.neoforged.neoforge.common.util.TriState.DEFAULT;
    }

    private static net.minecraft.world.InteractionResult result(legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult r) {
        return r == legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.SUCCESS ? net.minecraft.world.InteractionResult.SUCCESS
                : r == legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.FAIL ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS;
    }

    private static void registerLiving(IEventBus bus) {
        // 1.12.2 posted LivingAttackEvent and then LivingHurtEvent before armour, LivingDamageEvent after it
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) -> {
            boolean attack = wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingAttackEvent.class);
            boolean hurt = wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingHurtEvent.class);
            if (!attack && !hurt) return;
            var entity = living(e.getEntity());
            if (entity == null) return;
            var source = legacyconnector.v1_12_2.net.minecraft.util.DamageSource.mcc$of(e.getSource());
            if (attack && post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingAttackEvent(entity, source, e.getAmount()))) {
                e.setCanceled(true);
                return;
            }
            if (hurt) {
                var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingHurtEvent(entity, source, e.getAmount());
                if (post(event)) e.setCanceled(true);
                else e.setAmount(event.getAmount());
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDamageEvent.class)) return;
            var entity = living(e.getEntity());
            if (entity == null) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDamageEvent(entity,
                    legacyconnector.v1_12_2.net.minecraft.util.DamageSource.mcc$of(e.getSource()), e.getNewDamage());
            // the modern event can no longer be canceled here: a canceled legacy one deals no damage
            if (post(event)) e.setNewDamage(0);
            else e.setNewDamage(event.getAmount());
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDeathEvent.class)) return;
            var entity = living(e.getEntity());
            if (entity != null && post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDeathEvent(entity,
                    legacyconnector.v1_12_2.net.minecraft.util.DamageSource.mcc$of(e.getSource())))) {
                e.setCanceled(true);
            }
        });
        // 1.12.2: a dying player's drops go through PlayerDropsEvent (Tombstone, soulbound mods), everything else through LivingDropsEvent
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingDropsEvent e) -> {
            boolean isPlayer = e.getEntity() instanceof net.minecraft.world.entity.player.Player;
            Class<? extends Event> type = isPlayer ? legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerDropsEvent.class
                    : legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDropsEvent.class;
            if (!wants(type) && !(isPlayer && wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDropsEvent.class))) return;
            var entity = living(e.getEntity());
            if (entity == null) return;
            java.util.ArrayList<legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem> drops = new java.util.ArrayList<>();
            for (var item : e.getDrops()) if (EntityBridge1122.wrap(item) instanceof legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem legacyItem) drops.add(legacyItem);
            var source = legacyconnector.v1_12_2.net.minecraft.util.DamageSource.mcc$of(e.getSource());
            legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDropsEvent legacy = isPlayer && entity instanceof EntityPlayer p
                    ? new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerDropsEvent(p, source, drops, e.isRecentlyHit())
                    : new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingDropsEvent(entity, source, drops, 0, e.isRecentlyHit());
            if (post(legacy)) {
                e.setCanceled(true);
                return;
            }
            // listeners may have removed drops or added their own: the modern list becomes what the legacy list holds
            e.getDrops().clear();
            for (var item : drops) {
                if (item != null && item.mcc$entity instanceof net.minecraft.world.entity.item.ItemEntity modern) e.getDrops().add(modern);
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingFallEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingFallEvent.class)) return;
            var entity = living(e.getEntity());
            if (entity == null) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingFallEvent(entity, e.getDistance(), e.getDamageMultiplier());
            if (post(event)) e.setCanceled(true);
            e.setDistance(event.getDistance());
            e.setDamageMultiplier(event.getDamageMultiplier());
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent.class)) return;
            var entity = living(e.getEntity());
            if (entity != null) post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent(entity));
        });
        bus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) -> {
            if (!(e.getEntity() instanceof net.minecraft.world.entity.LivingEntity)
                    || !wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent.class)) return;
            var entity = living(e.getEntity());
            if (entity != null && post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent(entity))) e.setCanceled(true);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.Clone.class)) return;
            EntityPlayer player = player(e.getEntity()), original = player(e.getOriginal());
            if (player != null && original != null) post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.Clone(player, original, e.isWasDeath()));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed.class)) return;
            EntityPlayer player = player(e.getEntity());
            if (player == null) return;
            var at = e.getPosition().map(EventBridge1122::pos).orElse(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.ORIGIN);
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(player, StatesBridge1122.toLegacy(e.getState()), e.getOriginalSpeed(), at);
            if (post(event)) e.setCanceled(true);
            else e.setNewSpeed(event.getNewSpeed());
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.StartTracking.class)) return;
            EntityPlayer player = player(e.getEntity());
            if (player != null) post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.StartTracking(player, EntityBridge1122.wrap(e.getTarget())));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StopTracking e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.StopTracking.class)) return;
            EntityPlayer player = player(e.getEntity());
            if (player != null) post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent.StopTracking(player, EntityBridge1122.wrap(e.getTarget())));
        });
    }

    private static void registerInteractions(IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class)) return;
            EntityPlayer player = player(e.getEntity());
            if (player == null) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player, hand(e.getHand()),
                    pos(e.getPos()), face(e.getFace()), vec(e.getHitVec().getLocation()));
            if (post(event)) {
                e.setCanceled(true);
                e.setCancellationResult(result(event.getCancellationResult()));
                return;
            }
            e.setUseBlock(tri(event.getUseBlock()));
            e.setUseItem(tri(event.getUseItem()));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem.class)) return;
            EntityPlayer player = player(e.getEntity());
            if (player == null) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(player, hand(e.getHand()));
            if (post(event)) {
                e.setCanceled(true);
                e.setCancellationResult(result(event.getCancellationResult()));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.class)) return;
            EntityPlayer player = player(e.getEntity());
            if (player == null) return;
            var p = e.getPos();
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player, pos(p), face(e.getFace()),
                    new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D));
            if (post(event)) {
                e.setCanceled(true);
                return;
            }
            e.setUseBlock(tri(event.getUseBlock()));
            e.setUseItem(tri(event.getUseItem()));
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract.class)) return;
            EntityPlayer player = player(e.getEntity());
            var target = EntityBridge1122.wrap(e.getTarget());
            if (player == null || target == null) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(player, hand(e.getHand()), target);
            if (post(event)) {
                e.setCanceled(true);
                e.setCancellationResult(result(event.getCancellationResult()));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific.class)) return;
            EntityPlayer player = player(e.getEntity());
            var target = EntityBridge1122.wrap(e.getTarget());
            if (player == null || target == null) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific(player, hand(e.getHand()), target, vec(e.getLocalPos()));
            if (post(event)) {
                e.setCanceled(true);
                e.setCancellationResult(result(event.getCancellationResult()));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.BreakEvent.class)) return;
            if (!(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.BreakEvent(WorldBridge1122.wrap(level), pos(e.getPos()),
                    StatesBridge1122.toLegacy(e.getState()), player(e.getPlayer()));
            if (post(event)) e.setCanceled(true);
        });
        bus.addListener((net.neoforged.neoforge.event.level.BlockDropsEvent e) -> {
            if (!wants(legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent.class)) return;
            harvestDrops(e);
        });
    }

    /**
     * HarvestDropsEvent over the modern drops: the legacy list holds the dropped stacks; if handlers change it (or lower the drop
     * chance) the item entities are rebuilt from it at the block.
     */
    private static void harvestDrops(net.neoforged.neoforge.event.level.BlockDropsEvent e) {
        var level = e.getLevel();
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> drops = new java.util.ArrayList<>();
        for (var entity : e.getDrops()) drops.add(Stacks1122.fromModern(entity.getItem()));
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> before = new java.util.ArrayList<>();
        for (var d : drops) before.add(d.copy());
        var breaker = e.getBreaker() instanceof net.minecraft.world.entity.player.Player p ? player(p) : null;
        int fortune = 0;
        boolean silk = false;
        if (!e.getTool().isEmpty()) {
            var enchantments = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            fortune = e.getTool().getEnchantmentLevel(enchantments.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE));
            silk = e.getTool().getEnchantmentLevel(enchantments.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) > 0;
        }
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent(WorldBridge1122.wrap(level), pos(e.getPos()),
                StatesBridge1122.toLegacy(e.getState()), fortune, 1.0F, drops, breaker, silk);
        post(event);
        boolean changed = event.getDropChance() < 1.0F || drops.size() != before.size();
        for (int i = 0; !changed && i < drops.size(); i++) {
            changed = !legacyconnector.v1_12_2.net.minecraft.item.ItemStack.areItemStacksEqual(drops.get(i), before.get(i));
        }
        if (!changed) return;
        e.getDrops().clear();
        var at = e.getPos();
        for (var stack : drops) {
            if (stack == null || stack.isEmpty() || level.random.nextFloat() > event.getDropChance()) continue;
            var item = new net.minecraft.world.entity.item.ItemEntity(level, at.getX() + 0.5D, at.getY() + 0.5D, at.getZ() + 0.5D, Stacks1122.toModern(stack));
            item.setDefaultPickUpDelay();
            e.getDrops().add(item);
        }
    }

    private static int dimension(net.minecraft.server.MinecraftServer server, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key) {
        var level = server == null ? null : server.getLevel(key);
        return level == null ? 0 : WorldBridge1122.dimensionId(level);
    }

    /** 1.12.2 posted WorldTickEvent for server worlds (the client had ClientTickEvent). */
    private static void worldTick(net.minecraft.world.level.Level level, TickEvent.Phase phase) {
        if (level.isClientSide() || !wants(TickEvent.WorldTickEvent.class)) return;
        post(new TickEvent.WorldTickEvent(Side.SERVER, phase, WorldBridge1122.wrap(level)));
    }

    private static void playerTick(net.minecraft.world.entity.player.Player p, TickEvent.Phase phase) {
        if (!wants(TickEvent.PlayerTickEvent.class)) return;
        EntityPlayer player = player(p);
        if (player != null) post(new TickEvent.PlayerTickEvent(phase, player));
    }

    private static final class Client {
        static void register(IEventBus bus) {
            bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre e) -> {
                if (wants(TickEvent.ClientTickEvent.class)) post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            });
            bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> {
                if (wants(TickEvent.ClientTickEvent.class)) post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            });
            bus.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e) -> {
                if (wants(TickEvent.RenderTickEvent.class)) post(new TickEvent.RenderTickEvent(TickEvent.Phase.START, e.getPartialTick().getGameTimeDeltaPartialTick(false)));
            });
            bus.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post e) -> {
                if (wants(TickEvent.RenderTickEvent.class)) post(new TickEvent.RenderTickEvent(TickEvent.Phase.END, e.getPartialTick().getGameTimeDeltaPartialTick(false)));
            });
        }
    }

    // ------------------------------------------------------------------ area F: FML item pickup

    private static void registerAreaF(IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post e) -> {
            if (!wants(PlayerEvent.ItemPickupEvent.class) || player(e.getPlayer()) == null) return;
            var item = EntityBridge1122.wrap(e.getItemEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem i ? i : null;
            var picked = e.getOriginalStack().copy();
            picked.setCount(e.getOriginalStack().getCount() - e.getCurrentStack().getCount());
            if (picked.isEmpty()) return;
            post(new PlayerEvent.ItemPickupEvent(player(e.getPlayer()), item, Stacks1122.fromModern(picked)));
        });
    }
}
