package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;
import legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.PlayerEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.TickEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLServerStartedEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLServerStartingEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLServerStoppedEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLServerStoppingEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.EntityDamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.EntityDamageSourceIndirect;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityJoinWorldEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingAttackEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingDeathEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingFallEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingHurtEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ItemTooltipEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerInteractEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.kylepitcock.legacyconnector.v1_7_10.Platform1710;

/**
 * Feeds NeoForge game events into the 1.7.10 event buses ({@code MinecraftForge.EVENT_BUS} for gameplay,
 * {@code FMLCommonHandler.instance().bus()} for ticks and player connection events), and writes legacy results
 * (cancellation, modified damage, allow/deny decisions) back into the modern event.
 *
 * <p>Legacy event objects are only built if a legacy listener exists for that event type.</p>
 */
public final class EventBridge1710 {
    private EventBridge1710() {}

    private static EventBus forge() {
        return MinecraftForge.EVENT_BUS;
    }

    private static EventBus fml() {
        return FMLCommonHandler.instance().bus();
    }

    private static boolean wants(EventBus bus, Class<? extends Event> type) {
        return bus.mcc$hasListeners(type);
    }

    public static void register(IEventBus gameBus) {
        // ---- FML bus: ticks
        gameBus.addListener((ServerTickEvent.Pre e) -> {
            if (wants(fml(), TickEvent.ServerTickEvent.class)) fml().post(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        });
        gameBus.addListener((ServerTickEvent.Post e) -> {
            if (wants(fml(), TickEvent.ServerTickEvent.class)) fml().post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        });
        gameBus.addListener((LevelTickEvent.Pre e) -> worldTick(e.getLevel(), TickEvent.Phase.START));
        gameBus.addListener((LevelTickEvent.Post e) -> worldTick(e.getLevel(), TickEvent.Phase.END));
        gameBus.addListener((PlayerTickEvent.Pre e) -> {
            if (wants(fml(), TickEvent.PlayerTickEvent.class)) fml().post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player(e.getEntity())));
        });
        gameBus.addListener((PlayerTickEvent.Post e) -> {
            if (wants(fml(), TickEvent.PlayerTickEvent.class)) fml().post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player(e.getEntity())));
        });

        // ---- FML bus: players
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (wants(fml(), PlayerEvent.PlayerLoggedInEvent.class)) fml().post(new PlayerEvent.PlayerLoggedInEvent(player(e.getEntity())));
            notifyFailures(e.getEntity());
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (wants(fml(), PlayerEvent.PlayerLoggedOutEvent.class)) fml().post(new PlayerEvent.PlayerLoggedOutEvent(player(e.getEntity())));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) -> {
            if (wants(fml(), PlayerEvent.PlayerRespawnEvent.class)) fml().post(new PlayerEvent.PlayerRespawnEvent(player(e.getEntity())));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (wants(fml(), PlayerEvent.PlayerChangedDimensionEvent.class)) {
                fml().post(new PlayerEvent.PlayerChangedDimensionEvent(player(e.getEntity()), WorldProvider.dimensionId(e.getFrom()), WorldProvider.dimensionId(e.getTo())));
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e) -> {
            if (wants(fml(), PlayerEvent.ItemCraftedEvent.class)) {
                fml().post(new PlayerEvent.ItemCraftedEvent(player(e.getEntity()), ItemStack.mcc$wrap(e.getCrafting()), null));
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent e) -> {
            if (wants(fml(), PlayerEvent.ItemSmeltedEvent.class)) {
                fml().post(new PlayerEvent.ItemSmeltedEvent(player(e.getEntity()), ItemStack.mcc$wrap(e.getSmelting())));
            }
        });
        gameBus.addListener((ItemEntityPickupEvent.Post e) -> {
            if (wants(fml(), PlayerEvent.ItemPickupEvent.class)) {
                fml().post(new PlayerEvent.ItemPickupEvent(player(e.getPlayer()), (EntityItem) EntityBridge.wrap(e.getItemEntity())));
            }
        });

        // ---- Forge bus: interaction
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (!wants(forge(), PlayerInteractEvent.class)) return;
            var p = e.getPos();
            PlayerInteractEvent legacy = new PlayerInteractEvent(player(e.getEntity()), PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                    p.getX(), p.getY(), p.getZ(), e.getFace() == null ? -1 : e.getFace().get3DDataValue(), World.mcc$wrap(e.getLevel()));
            forge().post(legacy);
            if (legacy.isCanceled()) e.setCanceled(true);
            e.setUseBlock(tri(legacy.useBlock));
            e.setUseItem(tri(legacy.useItem));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) -> {
            if (!wants(forge(), PlayerInteractEvent.class)) return;
            var p = e.getEntity().blockPosition();
            PlayerInteractEvent legacy = new PlayerInteractEvent(player(e.getEntity()), PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                    p.getX(), p.getY(), p.getZ(), -1, World.mcc$wrap(e.getLevel()));
            forge().post(legacy);
            if (legacy.isCanceled() || legacy.useItem == Event.Result.DENY) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) -> {
            if (!wants(forge(), PlayerInteractEvent.class)) return;
            var p = e.getPos();
            PlayerInteractEvent legacy = new PlayerInteractEvent(player(e.getEntity()), PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
                    p.getX(), p.getY(), p.getZ(), e.getFace() == null ? -1 : e.getFace().get3DDataValue(), World.mcc$wrap(e.getLevel()));
            forge().post(legacy);
            if (legacy.isCanceled()) e.setCanceled(true);
        });

        // ---- Forge bus: blocks and worlds
        gameBus.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) -> {
            if (!wants(forge(), BlockEvent.BreakEvent.class)) return;
            var p = e.getPos();
            BlockEvent.BreakEvent legacy = new BlockEvent.BreakEvent(p.getX(), p.getY(), p.getZ(), World.mcc$wrap(e.getLevel()),
                    BlockBridge.legacyBlock(e.getState()), BlockBridge.legacyMeta(e.getState()), player(e.getPlayer()));
            if (forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent e) -> {
            if (!wants(forge(), BlockEvent.PlaceEvent.class) || !(e.getEntity() instanceof net.minecraft.world.entity.player.Player pl)) return;
            var p = e.getPos();
            BlockEvent.PlaceEvent legacy = new BlockEvent.PlaceEvent(p.getX(), p.getY(), p.getZ(), World.mcc$wrap(e.getLevel()),
                    BlockBridge.legacyBlock(e.getPlacedBlock()), BlockBridge.legacyMeta(e.getPlacedBlock()), BlockBridge.legacyBlock(e.getPlacedAgainst()),
                    player(pl), ItemStack.mcc$wrap(pl.getMainHandItem()));
            if (forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((LevelEvent.Load e) -> worldEvent(e.getLevel(), WorldEvent.Load::new));
        gameBus.addListener((LevelEvent.Unload e) -> worldEvent(e.getLevel(), WorldEvent.Unload::new));
        gameBus.addListener((LevelEvent.Save e) -> worldEvent(e.getLevel(), WorldEvent.Save::new));

        // ---- Forge bus: entities
        gameBus.addListener((EntityJoinLevelEvent e) -> {
            if (!wants(forge(), EntityJoinWorldEvent.class)) return;
            if (forge().post(new EntityJoinWorldEvent(EntityBridge.wrap(e.getEntity()), World.mcc$wrap(e.getLevel())))) e.setCanceled(true);
        });
        gameBus.addListener((EntityTickEvent.Pre e) -> {
            if (!(e.getEntity() instanceof LivingEntity l) || !wants(forge(), LivingEvent.LivingUpdateEvent.class)) return;
            EntityLivingBase view = (EntityLivingBase) EntityBridge.wrap(l);
            if (forge().post(new LivingEvent.LivingUpdateEvent(view))) e.setCanceled(true);
            view.mcc$syncPreviousEquipment();
        });
        gameBus.addListener((LivingIncomingDamageEvent e) -> {
            boolean attack = wants(forge(), LivingAttackEvent.class);
            boolean hurt = wants(forge(), LivingHurtEvent.class);
            if (!attack && !hurt) return;
            EntityLivingBase victim = (EntityLivingBase) EntityBridge.wrap(e.getEntity());
            DamageSource source = legacySource(e.getSource());
            if (attack && forge().post(new LivingAttackEvent(victim, source, e.getAmount()))) {
                e.setCanceled(true);
                return;
            }
            if (hurt) {
                LivingHurtEvent legacy = new LivingHurtEvent(victim, source, e.getAmount());
                if (forge().post(legacy)) e.setCanceled(true);
                else e.setAmount(legacy.ammount);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
            if (!wants(forge(), LivingDeathEvent.class)) return;
            if (forge().post(new LivingDeathEvent((EntityLivingBase) EntityBridge.wrap(e.getEntity()), legacySource(e.getSource())))) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingFallEvent e) -> {
            if (!wants(forge(), LivingFallEvent.class)) return;
            LivingFallEvent legacy = new LivingFallEvent((EntityLivingBase) EntityBridge.wrap(e.getEntity()), e.getDistance());
            if (forge().post(legacy)) e.setCanceled(true);
            else e.setDistance(legacy.distance);
        });

        // ---- server lifecycle -> @Mod.EventHandler methods
        gameBus.addListener((ServerAboutToStartEvent e) -> Platform1710.dispatchAll(m -> new FMLServerAboutToStartEvent(MinecraftServer.mcc$wrap(e.getServer()))));
        gameBus.addListener((ServerStartingEvent e) -> Platform1710.dispatchAll(m -> new FMLServerStartingEvent(MinecraftServer.mcc$wrap(e.getServer()))));
        gameBus.addListener((ServerStartedEvent e) -> Platform1710.dispatchAll(m -> new FMLServerStartedEvent()));
        gameBus.addListener((ServerStoppingEvent e) -> Platform1710.dispatchAll(m -> new FMLServerStoppingEvent()));
        gameBus.addListener((ServerStoppedEvent e) -> Platform1710.dispatchAll(m -> new FMLServerStoppedEvent()));
    }

    /** Client-only events. Registered from the client entrypoint so servers never load client classes. */
    public static void registerClient(IEventBus gameBus) {
        gameBus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre e) -> {
            if (wants(fml(), TickEvent.ClientTickEvent.class)) fml().post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> {
            if (wants(fml(), TickEvent.ClientTickEvent.class)) fml().post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) -> {
            if (!wants(forge(), ItemTooltipEvent.class) || e.getItemStack().isEmpty()) return;
            List<String> lines = new ArrayList<>();
            for (Component c : e.getToolTip()) lines.add(c.getString());
            int before = lines.size();
            List<String> original = List.copyOf(lines);
            forge().post(new ItemTooltipEvent(ItemStack.mcc$wrap(e.getItemStack()), e.getEntity() == null ? null : player(e.getEntity()), lines,
                    e.getFlags().isAdvanced()));
            if (lines.size() >= before && lines.subList(0, before).equals(original)) {
                for (String s : lines.subList(before, lines.size())) e.getToolTip().add(Component.literal(s));
            } else {
                e.getToolTip().clear();
                for (String s : lines) e.getToolTip().add(Component.literal(s));
            }
        });
    }

    private static void worldTick(net.minecraft.world.level.Level level, TickEvent.Phase phase) {
        if (wants(fml(), TickEvent.WorldTickEvent.class)) {
            fml().post(new TickEvent.WorldTickEvent(level.isClientSide ? Side.CLIENT : Side.SERVER, phase, World.mcc$wrap(level)));
        }
    }

    private static void worldEvent(LevelAccessor level, java.util.function.Function<World, WorldEvent> factory) {
        WorldEvent probe;
        if (!wants(forge(), WorldEvent.class)) return;
        probe = factory.apply(World.mcc$wrap(level));
        forge().post(probe);
    }

    private static EntityPlayer player(net.minecraft.world.entity.player.Player p) {
        return (EntityPlayer) EntityBridge.wrap(p);
    }

    private static TriState tri(Event.Result r) {
        return switch (r) {
            case ALLOW -> TriState.TRUE;
            case DENY -> TriState.FALSE;
            default -> TriState.DEFAULT;
        };
    }

    static DamageSource legacySource(net.minecraft.world.damagesource.DamageSource modern) {
        String type = modern.getMsgId();
        var direct = modern.getDirectEntity() == null ? null : EntityBridge.wrap(modern.getDirectEntity());
        var cause = modern.getEntity() == null ? null : EntityBridge.wrap(modern.getEntity());
        if (direct != null && cause != null && direct != cause) return new EntityDamageSourceIndirect(type, direct, cause);
        if (cause != null) return new EntityDamageSource(type, cause);
        return new DamageSource(type);
    }

    private static void notifyFailures(net.minecraft.world.entity.player.Player player) {
        var failures = org.kylepitcock.legacyconnector.core.LegacyLifecycle.failures();
        if (failures.isEmpty() || !player.hasPermissions(2)) return;
        player.sendSystemMessage(Component.literal("§c[MC Compatibility] " + failures.size()
                + " legacy mod(s) failed to load: " + String.join(", ", failures.keySet().stream().map(m -> m.legacyId).toList())
                + ". See logs/latest.log and logs/legacyconnector-report.txt."));
    }
}
