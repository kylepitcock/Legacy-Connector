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

    /** Players whose data file NeoForge reported loading since they last logged in. */
    private static final java.util.Set<String> LOADED_FROM_FILE = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        gameBus.addListener((PlayerTickEvent.Post e) -> armorTick(e.getEntity()));
        gameBus.addListener((PlayerTickEvent.Post e) -> {
            if (wants(fml(), TickEvent.PlayerTickEvent.class)) fml().post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player(e.getEntity())));
        });

        // ---- Forge bus: player data files (Thaumcraft keeps research in <uuid>.thaum next to the player's .dat)
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.LoadFromFile e) -> {
            LOADED_FROM_FILE.add(e.getPlayerUUID());
            if (wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile.class)) {
                forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile(player(e.getEntity()),
                        e.getPlayerDirectory(), e.getPlayerUUID()));
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.SaveToFile e) -> {
            if (wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile.class)) {
                forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile(player(e.getEntity()),
                        e.getPlayerDirectory(), e.getPlayerUUID()));
            }
        });

        // ---- FML bus: players
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            // The single-player host's data comes from level.dat, which fires no LoadFromFile; Forge 1.7.10 fired it for the host
            // too, and mods load their own player files there (Thaumcraft's research and starting entries).
            if (!LOADED_FROM_FILE.remove(e.getEntity().getStringUUID()) && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp
                    && wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile.class)) {
                java.io.File dir = sp.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
                forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile(player(sp), dir, sp.getStringUUID()));
            }
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
        // 1.7.10 fired RIGHT_CLICK_AIR on the client for an empty hand too (Fisk's Heroes shoots Spider-Man's web lines on it)
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty e) -> {
            if (e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !wants(forge(), PlayerInteractEvent.class)) return;
            var p = e.getEntity().blockPosition();
            forge().post(new PlayerInteractEvent(player(e.getEntity()), PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                    p.getX(), p.getY(), p.getZ(), -1, World.mcc$wrap(e.getLevel())));
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
        // Bone meal, as 1.7.10's ItemDye.applyBonemeal: the legacy BonemealEvent first (handlers grow the block and answer ALLOW),
        // then a legacy IGrowable block (Thaumcraft's greatwood/silverwood saplings) is fertilized directly.
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.BonemealEvent e) -> {
            var block = BlockBridge.legacyBlock(e.getState());
            var p = e.getPos();
            World world = World.mcc$wrap(e.getLevel());
            if (wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.BonemealEvent.class)) {
                var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.BonemealEvent(player(e.getPlayer()), world,
                        block, p.getX(), p.getY(), p.getZ());
                if (forge().post(legacy)) {
                    e.setCanceled(true);
                    return;
                }
                if (legacy.getResult() == Event.Result.ALLOW) {
                    // a canceled event's item is not consumed by the game: 1.7.10 used one bone meal on ALLOW
                    if (!e.getLevel().isClientSide) e.getStack().shrink(1);
                    e.setSuccessful(true);
                    e.setCanceled(true);
                    return;
                }
            }
            if (block instanceof legacyconnector.v1_7_10.net.minecraft.block.IGrowable growable && !(e.getState().getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock)) {
                boolean remote = e.getLevel().isClientSide;
                if (growable.canFertilize(world, p.getX(), p.getY(), p.getZ(), remote)) {
                    if (!remote && growable.shouldFertilize(world, world.rand, p.getX(), p.getY(), p.getZ())) {
                        growable.fertilize(world, world.rand, p.getX(), p.getY(), p.getZ());
                    }
                    if (!remote) e.getStack().shrink(1);
                    e.setSuccessful(true);
                }
                e.setCanceled(true);
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
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start e) -> {
            if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player p) || !(e.getItem().getItem() instanceof net.minecraft.world.item.BowItem)
                    || !wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ArrowNockEvent.class)) return;
            var stack = ItemStack.mcc$wrap(e.getItem());
            if (forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ArrowNockEvent(player(p), stack))) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent e) -> {
            if (e.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.HOE_TILL || e.isSimulated() || e.getPlayer() == null
                    || !(e.getLevel() instanceof net.minecraft.world.level.Level level)
                    || !wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.UseHoeEvent.class)) return;
            var p = e.getPos();
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.UseHoeEvent(player(e.getPlayer()),
                    ItemStack.mcc$wrap(e.getHeldItemStack()), World.mcc$wrap(level), p.getX(), p.getY(), p.getZ());
            if (forge().post(legacy)) e.setCanceled(true);
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
            // an item entity that was merged away has an empty stack; 1.7.10 never reported those, and handlers read the stack unchecked
            if (e.getItemEntity().getItem().isEmpty()) return;
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
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkDataEvent.Save.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)
                    || !(e.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk)) return; // a chunk still generating never got its load event
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
            if (!(player(e.getPlayer()) instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.EntityPlayerSP)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.client.event.FOVUpdateEvent(player(e.getPlayer()) instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.EntityPlayerSP sp ? sp : null, e.getFovModifier());
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
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start e) -> {
            var legacy = playerUse(e, 0);
            if (legacy != null && forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick e) -> {
            var legacy = playerUse(e, 1);
            if (legacy != null && forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Stop e) -> {
            var legacy = playerUse(e, 2);
            if (legacy != null && forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish e) -> {
            var legacy = playerUse(e, 3);
            if (legacy != null) forge().post(legacy);
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.ChunkWatchEvent.Watch e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkWatchEvent.Watch.class)) return;
            if (!(EntityBridge.wrap(e.getPlayer()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkWatchEvent.Watch(
                    new legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair(e.getPos().x, e.getPos().z), player));
        });
        gameBus.addListener((net.neoforged.neoforge.event.level.ChunkWatchEvent.UnWatch e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkWatchEvent.UnWatch.class)) return;
            if (!(EntityBridge.wrap(e.getPlayer()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkWatchEvent.UnWatch(
                    new legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair(e.getPos().x, e.getPos().z), player));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.Clone.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer now)) return;
            if (!(EntityBridge.wrap(e.getOriginal()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer before)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.Clone(now, before, e.isWasDeath()));
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingDropsEvent e) -> {
            if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player)) return;
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDropsEvent.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player)) return;
            ArrayList<EntityItem> drops = new ArrayList<>();
            for (var item : e.getDrops()) if (EntityBridge.wrap(item) instanceof EntityItem legacyItem) drops.add(legacyItem);
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDropsEvent(player, legacySource(e.getSource()), drops, e.isRecentlyHit());
            if (forge().post(legacy)) {
                e.setCanceled(true);
                return;
            }
            e.getDrops().clear();
            for (EntityItem item : legacy.drops) {
                if (item != null && item.mcc$entity() instanceof net.minecraft.world.entity.item.ItemEntity modern) e.getDrops().add(modern);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDestroyItemEvent.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player)) return;
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDestroyItemEvent(player, ItemStack.mcc$wrap(e.getOriginal())));
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

    /**
     * FML's RenderTickEvent: posted once per frame at the start and the end of the HUD pass, inside a GUI render frame so the
     * legacy drawing that mods do from it (oxygen and radiation meters, custom HUDs) has somewhere to draw.
     */
    public static void renderTick(TickEvent.Phase phase, float partial, net.minecraft.client.gui.GuiGraphics graphics) {
        if (!wants(fml(), TickEvent.RenderTickEvent.class)) return;
        org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.beginGui(graphics);
        try {
            fml().post(new TickEvent.RenderTickEvent(phase, partial));
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Hud").warn("A render tick handler failed: {}", e.toString());
        } finally {
            org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.end();
        }
    }

    private static legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderPlayer legacyRenderPlayer;

    /** The one legacy player renderer handed to RenderPlayerEvent handlers. */
    private static synchronized legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderPlayer legacyRenderPlayer() {
        if (legacyRenderPlayer == null) legacyRenderPlayer = legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager.instance.mcc$playerRenderer();
        return legacyRenderPlayer;
    }

    /** The legacy PlayerUseItemEvent (kind 0 start, 1 tick, 2 stop, 3 finish) for a player using an item, or null when nothing listens. */
    private static legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent playerUse(
            net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent e, int kind) {
        if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player)) return null;
        Class<? extends legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent> type = switch (kind) {
            case 0 -> legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Start.class;
            case 1 -> legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Tick.class;
            case 2 -> legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Stop.class;
            default -> legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Finish.class;
        };
        if (!wants(forge(), type)) return null;
        if (!(EntityBridge.wrap(e.getEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player)) return null;
        ItemStack stack = ItemStack.mcc$wrap(e.getItem());
        int duration = e.getDuration();
        return switch (kind) {
            case 0 -> new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Start(player, stack, duration);
            case 1 -> new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Tick(player, stack, duration);
            case 2 -> new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Stop(player, stack, duration);
            default -> new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Finish(player, stack, duration);
        };
    }

    /** The legacy RenderLivingEvent for an entity about to be drawn, or null when nothing listens or the entity has no legacy view. */
    private static legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderLivingEvent livingRenderEvent(net.minecraft.world.entity.LivingEntity entity, boolean pre) {
        boolean wanted = wants(forge(), pre ? legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderLivingEvent.Pre.class
                : legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderLivingEvent.Post.class);
        if (!wanted) return null;
        if (!(EntityBridge.wrap(entity) instanceof EntityLivingBase living)) return null;
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double x = entity.getX() - camera.x;
        double y = entity.getY() - camera.y;
        double z = entity.getZ() - camera.z;
        return pre ? new legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderLivingEvent.Pre(living, null, x, y, z)
                : new legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderLivingEvent.Post(living, null, x, y, z);
    }

    /** Client-only events. Registered from the client entrypoint so servers never load client classes. */
    public static void registerClient(IEventBus gameBus) {
        org.kylepitcock.legacyconnector.v1_7_10.client.DimensionSounds1710.register(gameBus);
        org.kylepitcock.legacyconnector.v1_7_10.client.LegacyHorizon.register(gameBus);
        gameBus.addListener((net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.EntityViewRenderEvent.FogColors.class)) return;
            if (!(EntityBridge.wrap(e.getCamera().getEntity()) instanceof EntityLivingBase living)) return;
            var legacy = new legacyconnector.v1_7_10.net.minecraftforge.client.event.EntityViewRenderEvent.FogColors(living, e.getPartialTick(),
                    e.getRed(), e.getGreen(), e.getBlue());
            forge().post(legacy);
            e.setRed(legacy.red);
            e.setGreen(legacy.green);
            e.setBlue(legacy.blue);
        });
        // 1.7.10 fired these from its input loop while no screen was open, after updating the key bindings (as InputEvent.Key
        // and MouseButton.Post fire here): mods handle their key bindings in them (Fisk's Heroes' suit abilities)
        gameBus.addListener((net.neoforged.neoforge.client.event.InputEvent.Key e) -> {
            if (e.getAction() == org.lwjgl.glfw.GLFW.GLFW_REPEAT || net.minecraft.client.Minecraft.getInstance().screen != null) return;
            legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.mcc$setEvent(org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.fromGlfw(e.getKey()),
                    e.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS, '\0');
            try {
                if (wants(fml(), legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent.class)) {
                    fml().post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent());
                }
            } finally {
                legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.mcc$setEvent(0, false, '\0');
            }
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post e) -> {
            if (net.minecraft.client.Minecraft.getInstance().screen != null) return;
            legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEvent(e.getButton(), e.getAction() != 0);
            try {
                if (wants(fml(), legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent.class)) {
                    fml().post(new legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent());
                }
            } finally {
                legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEvent(-1, false);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.MouseEvent.class)) return;
            var event = new legacyconnector.v1_7_10.net.minecraftforge.client.event.MouseEvent((int) e.getMouseX(), (int) e.getMouseY(), 0, 0,
                    (int) Math.signum(e.getScrollDeltaY()) * 120, -1, false);
            if (forge().post(event)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.MouseEvent.class)) return;
            var mouse = net.minecraft.client.Minecraft.getInstance().mouseHandler;
            var event = new legacyconnector.v1_7_10.net.minecraftforge.client.event.MouseEvent((int) mouse.xpos(), (int) mouse.ypos(), 0, 0, 0,
                    e.getButton(), e.getAction() != 0);
            legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEvent(e.getButton(), e.getAction() != 0);
            try {
                if (forge().post(event)) e.setCanceled(true);
            } finally {
                legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEvent(-1, false);
            }
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Pre.class)
                    && !wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Specials.Pre.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player)) return;
            var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double x = e.getEntity().getX() - camera.x;
            double y = e.getEntity().getY() - camera.y;
            double z = e.getEntity().getZ() - camera.z;
            float partial = e.getPartialTick();
            var pre = new legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Pre(player, legacyRenderPlayer(), partial, x, y, z);
            if (forge().post(pre)) {
                e.setCanceled(true);
                return;
            }
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Specials.Pre(player, legacyRenderPlayer(), partial, x, y, z));
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.RenderPlayerEvent.Post e) -> {
            if (!wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Post.class)
                    && !wants(forge(), legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Specials.Post.class)) return;
            if (!(EntityBridge.wrap(e.getEntity()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player)) return;
            var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double x = e.getEntity().getX() - camera.x;
            double y = e.getEntity().getY() - camera.y;
            double z = e.getEntity().getZ() - camera.z;
            float partial = e.getPartialTick();
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Specials.Post(player, legacyRenderPlayer(), partial, x, y, z));
            forge().post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderPlayerEvent.Post(player, legacyRenderPlayer(), partial, x, y, z));
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> e) -> {
            var legacy = livingRenderEvent(e.getEntity(), true);
            if (legacy != null && forge().post(legacy)) e.setCanceled(true);
        });
        gameBus.addListener((net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> e) -> {
            var legacy = livingRenderEvent(e.getEntity(), false);
            if (legacy != null) forge().post(legacy);
        });
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
        // 1.7.10 posted WorldTickEvent for server worlds only (the client had ClientTickEvent); handlers cast event.world to
        // WorldServer without checking the side (Galacticraft's TickHandlerServer).
        if (level.isClientSide) return;
        if (wants(fml(), TickEvent.WorldTickEvent.class)) {
            fml().post(new TickEvent.WorldTickEvent(Side.SERVER, phase, World.mcc$wrap(level)));
        }
    }

    /** Load, Unload and Save: the interest check must use the concrete event class (listeners subscribe to {@code WorldEvent.Load}, not to the base class). */
    private static void worldEvent(LevelAccessor level, java.util.function.Function<World, WorldEvent> factory) {
        WorldEvent event = factory.apply(World.mcc$wrap(level));
        if (!wants(forge(), event.getClass())) return;
        forge().post(event);
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

    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
    private static int armorTickFailures;

    /** 1.7.10 called {@code Item.onArmorTick} for every worn armor stack on every player tick (boots of the traveller, hover armor, ...). */
    private static void armorTick(net.minecraft.world.entity.player.Player player) {
        if (armorTickFailures > 20) return;
        for (var slot : ARMOR_SLOTS) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemAdapter adapter)
                    || !(adapter.legacyItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.Item item)) continue;
            try {
                item.onArmorTick(World.mcc$wrap(player.level()), player(player), ItemStack.mcc$wrap(stack));
            } catch (RuntimeException | LinkageError ex) {
                if (armorTickFailures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Events").error("onArmorTick of {} failed", item.getClass().getName(), ex);
            }
        }
    }
}
