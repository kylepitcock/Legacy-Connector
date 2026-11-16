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
                if (handler.onItemPickup(e.entityPlayer, e.item)) e.setCanceled(true);
            }
        });
    }

    static void interact(IEntityInteractHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onInteract(EntityInteractEvent e) {
                if (handler.onEntityInteract(e.entityPlayer, e.target, false)) e.setCanceled(true);
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
                if (handler.onMinecartInteract(e.minecart, e.player, true)) e.setCanceled(true);
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
                String result = handler.onServerChat(e.player, e.message);
                if (result == null) e.setCanceled(true);
                else if (!result.equals(e.message)) e.line = "<" + e.username + "> " + result;
            }

            @SubscribeEvent
            public void onCommand(legacyconnector.v1_7_10.net.minecraftforge.event.CommandEvent e) {
                String name = e.command.getCommandName();
                String params = e.parameters == null ? "" : String.join(" ", e.parameters);
                String line = params.isEmpty() ? name : name + " " + params;
                if (e.sender instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
                    if (handler.onChatCommand(player, e.sender.canCommandSenderUseCommand(2, name), line)) e.setCanceled(true);
                } else if (handler.onServerCommand(e.sender, e.sender.getCommandSenderName(), line)) {
                    e.setCanceled(true);
                }
                if (!e.isCanceled() && name.equals("say")) {
                    String said = handler.onServerCommandSay(e.sender, e.sender.getCommandSenderName(), params);
                    if (said == null) e.setCanceled(true);
                    else if (!said.equals(params)) e.parameters = new String[] {said};
                }
            }
        });
    }

    /** Chunks the handler wants ticked and loaded ({@code addActiveChunks}): forced while it names them, released when it stops. */
    static void chunkLoad(IChunkLoadHandler handler) {
        java.util.Map<Object, java.util.Set<Long>> held = new java.util.WeakHashMap<>();
        legacyconnector.v1_6_4.cpw.mods.fml.common.registry.TickRegistry.registerTickHandler(new legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler() {
            @Override
            public void tickStart(java.util.EnumSet<legacyconnector.v1_6_4.cpw.mods.fml.common.TickType> type, Object... tickData) {}

            @Override
            public void tickEnd(java.util.EnumSet<legacyconnector.v1_6_4.cpw.mods.fml.common.TickType> type, Object... tickData) {
                if (tickData.length == 0 || !(tickData[0] instanceof legacyconnector.v1_7_10.net.minecraft.world.World world)) return;
                if (!(world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
                java.util.Set<legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair> asked = new java.util.LinkedHashSet<>();
                handler.addActiveChunks(world, asked);
                java.util.List<Long> wanted = new java.util.ArrayList<>();
                for (legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair c : asked) wanted.add(org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.key(c.chunkXPos, c.chunkZPos));
                org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.reconcile(held.computeIfAbsent(level, k -> new java.util.HashSet<>()), wanted,
                        k -> level.setChunkForced(org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.x(k), org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.z(k), true),
                        k -> level.setChunkForced(org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.x(k), org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125.z(k), false));
            }

            @Override
            public java.util.EnumSet<legacyconnector.v1_6_4.cpw.mods.fml.common.TickType> ticks() {
                return java.util.EnumSet.of(legacyconnector.v1_6_4.cpw.mods.fml.common.TickType.WORLD);
            }

            @Override
            public String getLabel() {
                return "Forge 3.x chunk load handler";
            }
        }, legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.SERVER);
    }
}
