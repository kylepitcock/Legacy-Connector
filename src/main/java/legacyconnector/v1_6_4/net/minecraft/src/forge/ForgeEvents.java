package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.BonemealEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.EntityInteractEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.ChunkEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary;

/** Forge 3.x's handler registrations as subscribers of the shim's event bus (each registration is one small subscriber object). */
final class ForgeEvents {
    private ForgeEvents() {}

    static void pickup(IPickupHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onPickup(EntityItemPickupEvent e) {
                // Forge 3.x (ForgeHooks.onItemPickup, checked against its source): the handler returns TRUE to let processing continue; false stops the
                // pickup, and so does a stack the handler used up (the entity is then gone, as the game destroyed it)
                if (e.isCanceled()) return;
                boolean cont = handler.onItemPickup(e.entityPlayer, e.item);
                boolean used = e.item.getEntityItem() != null && e.item.getEntityItem().mcc$count() <= 0;
                if (used) e.item.setDead();
                if (!cont || used) e.setCanceled(true);
            }
        });
    }

    static void interact(IEntityInteractHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onInteract(EntityInteractEvent e) {
                // IEntityInteractHandler: "True to continue processing, false to cancel" (ForgeHooks.onEntityInteract stops at the first false)
                if (!e.isCanceled() && !handler.onEntityInteract(e.entityPlayer, e.target, false)) e.setCanceled(true);
            }

            @SubscribeEvent
            public void onAttack(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.AttackEntityEvent e) {
                if (!e.isCanceled() && !handler.onEntityInteract(e.entityPlayer, e.target, true)) e.setCanceled(true);
            }
        });
    }

    static void bonemeal(IBonemealHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onBonemeal(BonemealEvent e) {
                if (handler.onUseBonemeal(e.world, Ids.blockId(e.block), e.x, e.y, e.z)) e.setResult(Event.Result.ALLOW);
            }
        });
    }

    static void arrowNock(IArrowNockHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onNock(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ArrowNockEvent e) {
                legacyconnector.v1_7_10.net.minecraft.item.ItemStack result = handler.onArrowNock(e.result, legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(e.entityPlayer), e.entityPlayer);
                if (result != null) {
                    e.result = result;
                    e.setCanceled(true);
                }
            }
        });
    }

    static void arrowLoose(IArrowLooseHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onLoose(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ArrowLooseEvent e) {
                if (handler.onArrowLoose(e.bow, legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(e.entityPlayer), e.entityPlayer, e.charge)) e.setCanceled(true);
            }
        });
    }

    static void save(ISaveEventHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onLoad(WorldEvent.Load e) {
                handler.onWorldLoad(e.world);
            }

            @SubscribeEvent
            public void onSave(WorldEvent.Save e) {
                handler.onWorldSave(e.world);
            }

            @SubscribeEvent
            public void onChunkLoad(ChunkEvent.Load e) {
                handler.onChunkLoad(e.world, e.getChunk());
            }

            @SubscribeEvent
            public void onChunkUnload(ChunkEvent.Unload e) {
                handler.onChunkUnload(e.world, e.getChunk());
            }
        });
    }

    static void ore(IOreHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onOre(OreDictionary.OreRegisterEvent e) {
                handler.registerOre(e.Name, e.Ore);
            }
        });
    }

    // ---- the handler interfaces Forge 3.x had that later Forge turned into events

    static void sleep(ISleepHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onSleep(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerSleepInBedEvent e) {
                if (e.result != null) return;
                legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.EnumStatus status = handler.sleepInBedAt(e.entityPlayer, e.x, e.y, e.z);
                if (status != null) e.result = status;
            }
        });
    }

    static void hoe(IHoeHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onHoe(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.UseHoeEvent e) {
                if (handler.onUseHoe(e.current, e.entityPlayer, e.world, e.x, e.y, e.z)) e.setResult(Event.Result.ALLOW);
            }
        });
    }

    static void destroyTool(IDestroyToolHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onDestroy(legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDestroyItemEvent e) {
                handler.onDestroyCurrentItem(e.entityPlayer, e.original);
            }
        });
    }

    static void minecart(IMinecartHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onUpdate(legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartUpdateEvent e) {
                handler.onMinecartUpdate(e.minecart, (int) Math.floor(e.x), (int) Math.floor(e.y), (int) Math.floor(e.z));
            }

            @SubscribeEvent
            public void onCollision(legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartCollisionEvent e) {
                handler.onMinecartEntityCollision(e.minecart, e.collider);
            }

            @SubscribeEvent
            public void onInteract(legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart.MinecartInteractEvent e) {
                // ForgeHooks.onMinecartInteract: "canceled" starts true and becomes the AND of the answers; a handler returns whether the player MAY interact,
                // the cart blocks the interaction when the final value is false
                boolean allowed = handler.onMinecartInteract(e.minecart, e.player, !e.isCanceled());
                if (!allowed) e.setCanceled(true);
            }
        });
    }

    static void specialSpawn(ISpecialMobSpawnHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onSpawn(legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.SpecialSpawn e) {
                if (handler.onSpecialEntitySpawn(e.entityLiving, e.world, e.x, e.y, e.z)) e.setCanceled(true);
            }
        });
    }

    /** The server half of the chat handler: player chat, player commands, console commands and {@code say}. */
    static void chat(IChatHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onChat(legacyconnector.v1_7_10.net.minecraftforge.event.ServerChatEvent e) {
                // handlers chain: each gets the text the previous one returned (ForgeHooks.onServerChat), null cancels
                String prefix = "<" + e.username + "> ";
                String current = e.line != null && e.line.startsWith(prefix) ? e.line.substring(prefix.length()) : e.message;
                String result = handler.onServerChat(e.player, current);
                if (result == null) e.setCanceled(true);
                else if (!result.equals(current)) e.line = prefix + result;
            }

            @SubscribeEvent
            public void onCommand(legacyconnector.v1_7_10.net.minecraftforge.event.CommandEvent e) {
                String name = e.command.getCommandName();
                String params = e.parameters == null ? "" : String.join(" ", e.parameters);
                if (e.isCanceled()) return;
                String line = params.isEmpty() ? name : name + " " + params;
                if (e.sender instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
                    if (handler.onChatCommand(player, e.sender.canCommandSenderUseCommand(2, name), line)) e.setCanceled(true);
                } else if (handler.onServerCommand(e.sender, e.sender.getCommandSenderName(), line)) {
                    e.setCanceled(true);
                }
                if (!e.isCanceled() && name.equals("say")) {
                    params = e.parameters == null ? "" : String.join(" ", e.parameters);
                    String said = handler.onServerCommandSay(e.sender, e.sender.getCommandSenderName(), params);
                    if (said == null) e.setCanceled(true);
                    else if (!said.equals(params)) e.parameters = new String[] {said};
                }
            }
        });
    }

    private static final java.util.List<IChunkLoadHandler> CHUNK_HANDLERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.Map<Object, java.util.Set<Long>> FORCED = new java.util.WeakHashMap<>();

    /**
     * Chunks the handlers want ticked and loaded ({@code addActiveChunks}): ForgeHooks.addActiveChunks gave EVERY handler the same set each tick, so
     * one ticker collects all of them and forces the union, releasing a chunk only when no handler names it any more.
     */
    static void chunkLoad(IChunkLoadHandler handler) {
        CHUNK_HANDLERS.add(handler);
        if (CHUNK_HANDLERS.size() > 1) return;
        legacyconnector.v1_6_4.cpw.mods.fml.common.registry.TickRegistry.registerTickHandler(new legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler() {
            @Override
            public void tickStart(java.util.EnumSet<legacyconnector.v1_6_4.cpw.mods.fml.common.TickType> type, Object... tickData) {}

            @Override
            public void tickEnd(java.util.EnumSet<legacyconnector.v1_6_4.cpw.mods.fml.common.TickType> type, Object... tickData) {
                if (tickData.length == 0 || !(tickData[0] instanceof legacyconnector.v1_7_10.net.minecraft.world.World world)) return;
                if (!(world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
                java.util.Set<legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair> asked = new java.util.LinkedHashSet<>();
                for (IChunkLoadHandler h : CHUNK_HANDLERS) h.addActiveChunks(world, asked);
                java.util.List<Long> wanted = new java.util.ArrayList<>();
                for (legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair c : asked) wanted.add(org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.key(c.chunkXPos, c.chunkZPos));
                java.util.Set<Long> held;
                synchronized (FORCED) {
                    held = FORCED.computeIfAbsent(level, k -> new java.util.HashSet<>());
                }
                org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.reconcile(held, wanted,
                        k -> level.setChunkForced(org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.x(k), org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.z(k), true),
                        k -> level.setChunkForced(org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.x(k), org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.z(k), false));
            }

            @Override
            public java.util.EnumSet<legacyconnector.v1_6_4.cpw.mods.fml.common.TickType> ticks() {
                return java.util.EnumSet.of(legacyconnector.v1_6_4.cpw.mods.fml.common.TickType.WORLD);
            }

            @Override
            public String getLabel() {
                return "Forge 3.x chunk load handlers";
            }
        }, legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.SERVER);
    }
}
