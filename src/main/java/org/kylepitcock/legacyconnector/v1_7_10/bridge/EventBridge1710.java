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
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingDropsEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.EntityInteractEvent;
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
        gameBus.addListener((net.neoforged.neoforge.event.level.BlockDropsEvent e) -> {
            var p = e.getPos();
            // Shears on a block that is shearable the legacy way (leaves, tall grass, vines) drop what the block says.
            if (e.getTool().getItem() instanceof net.minecraft.world.item.ShearsItem
                    && BlockBridge.legacyBlock(e.getState()) instanceof legacyconnector.v1_7_10.net.minecraftforge.common.IShearable shearable) {
                try {
                    var view = BlockAccessView.of(e.getLevel());
                    ItemStack tool = ItemStack.mcc$wrap(e.getTool());
                    if (shearable.isShearable(tool, view, p.getX(), p.getY(), p.getZ())) {
                        var sheared = shearable.onSheared(tool, view, p.getX(), p.getY(), p.getZ(), 0);
                        e.getDrops().clear();
                        if (sheared != null) {
                            for (ItemStack s : sheared) {
                                var modern = s == null ? net.minecraft.world.item.ItemStack.EMPTY : s.mcc$modern();
                                if (!modern.isEmpty()) e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.getLevel(), p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, modern));
                            }
                        }
                    }
                } catch (RuntimeException | LinkageError ex) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/Shears").warn("Shearing {} failed: {}", e.getState(), ex.toString());
                }
            }
            if (!wants(forge(), BlockEvent.HarvestDropsEvent.class)) return;
            java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
            for (var item : e.getDrops()) stacks.add(ItemStack.mcc$wrap(item.getItem()));
            boolean silk = false;
            BlockEvent.HarvestDropsEvent legacy = new BlockEvent.HarvestDropsEvent(p.getX(), p.getY(), p.getZ(), World.mcc$wrap(e.getLevel()),
                    BlockBridge.legacyBlock(e.getState()), BlockBridge.legacyMeta(e.getState()), 0, 1.0F, stacks,
                    e.getBreaker() instanceof net.minecraft.world.entity.player.Player pl ? player(pl) : null, silk);
            forge().post(legacy);
            // Handlers edit the list of stacks; the modern drops are rebuilt from it.
            e.getDrops().clear();
            for (ItemStack s : legacy.drops) {
                if (s == null) continue;
                var modern = s.mcc$modern();
                if (modern.isEmpty() || legacy.dropChance < 1.0F && e.getLevel().random.nextFloat() > legacy.dropChance) continue;
                e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.getLevel(), p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, modern));
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed.class)) return;
            var pos = e.getPosition().orElse(net.minecraft.core.BlockPos.ZERO);
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(player(e.getEntity()),
                    BlockBridge.legacyBlock(e.getState()), BlockBridge.legacyMeta(e.getState()), e.getOriginalSpeed(), pos.getX(), pos.getY(), pos.getZ());
            if (forge().post(legacy)) e.setCanceled(true);
            else e.setNewSpeed(legacy.newSpeed);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.BonemealEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.BonemealEvent.class)) return;
            var p = e.getPos();
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.BonemealEvent(player(e.getPlayer()), World.mcc$wrap(e.getLevel()),
                    BlockBridge.legacyBlock(e.getState()), p.getX(), p.getY(), p.getZ());
            if (forge().post(legacy)) {
                e.setCanceled(true);
            } else if (legacy.getResult() == Event.Result.ALLOW) {
                e.setSuccessful(true);
                e.setCanceled(true);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.EnderTeleportEvent.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.EnderTeleportEvent((EntityLivingBase) EntityBridge.wrap(e.getEntityLiving()),
                    e.getTargetX(), e.getTargetY(), e.getTargetZ(), 0.0F);
            if (forge().post(legacy)) {
                e.setCanceled(true);
            } else {
                e.setTargetX(legacy.targetX);
                e.setTargetY(legacy.targetY);
                e.setTargetZ(legacy.targetZ);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.ServerChatEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.ServerChatEvent.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.ServerChatEvent(
                    (legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP) EntityBridge.wrap(e.getPlayer()), e.getRawText(), null);
            if (forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.item.ItemTossEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.item.ItemTossEvent.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.item.ItemTossEvent((EntityItem) EntityBridge.wrap(e.getEntity()), player(e.getPlayer()));
            if (forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((ItemEntityPickupEvent.Pre e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.EntityItemPickupEvent.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.EntityItemPickupEvent(player(e.getPlayer()), (EntityItem) EntityBridge.wrap(e.getItemEntity()));
            if (forge().post(legacy) || legacy.getResult() == Event.Result.DENY) e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            else if (legacy.getResult() == Event.Result.ALLOW) e.setCanPickup(net.neoforged.neoforge.common.util.TriState.TRUE);
        });
        gameBus.addListener((net.neoforged.neoforge.event.AnvilUpdateEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.AnvilUpdateEvent.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.AnvilUpdateEvent(ItemStack.mcc$wrap(e.getLeft()), ItemStack.mcc$wrap(e.getRight()),
                    e.getName(), (int) e.getCost());
            if (forge().post(legacy)) {
                e.setCanceled(true);
            } else if (legacy.output != null) {
                e.setOutput(legacy.output.mcc$modern());
                e.setCost((long) legacy.cost);
                e.setMaterialCost(legacy.materialCost);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkEvent.Load.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkEvent.Load(new legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk(World.mcc$wrap(level), e.getChunk())));
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Unload e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkEvent.Unload.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkEvent.Unload(new legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk(World.mcc$wrap(level), e.getChunk())));
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.ChunkDataEvent.Load e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkDataEvent.Load.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkDataEvent.Load(
                    new legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk(World.mcc$wrap(level), e.getChunk()),
                    new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound(e.getData())));
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.ChunkDataEvent.Save e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkDataEvent.Save.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkDataEvent.Save(
                    new legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk(World.mcc$wrap(level), e.getChunk()),
                    new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound(e.getData())));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn(
                    (EntityLivingBase) EntityBridge.wrap(e.getEntity()), World.mcc$wrap(e.getLevel()), (float) e.getX(), (float) e.getY(), (float) e.getZ());
            forge().post(legacy);
            if (legacy.getResult() == Event.Result.ALLOW) e.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.SUCCEED);
            else if (legacy.getResult() == Event.Result.DENY) e.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.ComputeFovModifierEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.FOVUpdateEvent.class)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.client.event.FOVUpdateEvent(player(e.getPlayer()), e.getFovModifier());
            forge().post(legacy);
            e.setNewFovModifier(legacy.newfov);
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
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingDropsEvent e) -> {
            if (!wants(forge(), LivingDropsEvent.class)) return;
            ArrayList<EntityItem> drops = new ArrayList<>();
            for (var item : e.getDrops()) if (EntityBridge.wrap(item) instanceof EntityItem legacyItem) drops.add(legacyItem);
            LivingDropsEvent legacy = new LivingDropsEvent((EntityLivingBase) EntityBridge.wrap(e.getEntity()), legacySource(e.getSource()), drops,
                    0, e.isRecentlyHit(), 0);
            if (forge().post(legacy)) {
                e.setCanceled(true);
                return;
            }
            // listeners may have removed drops or added their own: the modern list becomes what the legacy list holds
            e.getDrops().clear();
            for (EntityItem item : legacy.drops) {
                if (item != null && item.mcc$entity() instanceof net.minecraft.world.entity.item.ItemEntity modern) e.getDrops().add(modern);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) -> {
            var held = e.getItemStack();
            if (!(held.getItem() instanceof net.minecraft.world.item.ShearsItem) || e.getLevel().isClientSide()) return;
            if (!(EntityBridge.wrap(e.getTarget()) instanceof legacyconnector.v1_7_10.net.minecraftforge.common.IShearable shearable)) return;
            try {
                var target = e.getTarget();
                var view = World.mcc$wrap(e.getLevel());
                ItemStack tool = ItemStack.mcc$wrap(held);
                int x = target.getBlockX();
                int y = target.getBlockY();
                int z = target.getBlockZ();
                if (!shearable.isShearable(tool, view, x, y, z)) return;
                var sheared = shearable.onSheared(tool, view, x, y, z, 0);
                if (sheared != null) {
                    for (ItemStack s : sheared) {
                        var modern = s == null ? net.minecraft.world.item.ItemStack.EMPTY : s.mcc$modern();
                        if (modern.isEmpty()) continue;
                        var drop = new net.minecraft.world.entity.item.ItemEntity(e.getLevel(), target.getX(), target.getY() + 1.0D, target.getZ(), modern);
                        drop.setDeltaMovement((e.getLevel().random.nextFloat() - e.getLevel().random.nextFloat()) * 0.1F, e.getLevel().random.nextFloat() * 0.05F,
                                (e.getLevel().random.nextFloat() - e.getLevel().random.nextFloat()) * 0.1F);
                        e.getLevel().addFreshEntity(drop);
                    }
                }
                held.hurtAndBreak(1, e.getEntity(), net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                e.setCanceled(true);
            } catch (RuntimeException | LinkageError ex) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Shears").warn("Shearing {} failed: {}", e.getTarget(), ex.toString());
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) -> {
            if (!wants(forge(), EntityInteractEvent.class)) return;
            if (forge().post(new EntityInteractEvent(player(e.getEntity()), EntityBridge.wrap(e.getTarget())))) e.setCanceled(true);
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
