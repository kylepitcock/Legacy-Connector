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
}
