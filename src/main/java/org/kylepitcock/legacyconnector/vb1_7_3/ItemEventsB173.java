package org.kylepitcock.legacyconnector.vb1_7_3;

/** Native result-slot events invoke the original ModLoader callbacks, including vanilla recipe outputs. */
public final class ItemEventsB173 {
    private static boolean installed;
    private ItemEventsB173(){}
    public static synchronized void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e)->nativeTaken(e.getEntity(),e.getCrafting(),e.getInventory(),false));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent e)->nativeTaken(e.getEntity(),e.getSmelting(),null,true));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent e)->destroyed(e.getEntity(),e.getOriginal()));
    }
    private static void nativeTaken(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.Container matrix,boolean furnace){
        if(player.level().isClientSide)return;var original=StacksB173.toLegacy(stack);if(original==null)return;
        try(var views=EntityViewsB173.callbackScope()){
            var actor=(PlayerViewB173)EntityViewsB173.wrap(player);
            try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(actor.world,actor)){
                original.onCraft(actor.world,actor);taken(actor,original,furnace);
                // Forge's SlotCrafting asked its crafting handlers with the grid, before the ingredients were used up
                if(!furnace&&matrix!=null&&legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge.hasCraftingHandlers()){
                    var grid=new ContainerViewB173(matrix);
                    try{legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge.onTakenFromCrafting(actor,original,grid);}
                    catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta Forge crafting handler failed",error);}
                    grid.writeBack();
                }
                actor.writeBack();
            }
        }finally{EntityViewsB173.flush();BlockEntityViewsB173.flush();}
    }
    /** Forge's destroy-tool handlers: told when the stack a player holds is used up. */
    private static void destroyed(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack){
        if(player.level().isClientSide||!legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge.hasDestroyToolHandlers())return;
        var original=StacksB173.toLegacy(stack);if(original==null)return;
        try(var views=EntityViewsB173.callbackScope()){
            var actor=(PlayerViewB173)EntityViewsB173.wrap(player);
            try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(actor.world,actor)){
                legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge.onDestroyCurrentItem(actor,original);actor.writeBack();
            }
        }catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta Forge destroy-tool handler failed",error);}
        finally{EntityViewsB173.flush();BlockEntityViewsB173.flush();}
    }
    public static void taken(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,boolean furnace){
        if(player==null||stack==null)return;
        for(var mod:ContentB173.loadedMods())try{
            if(furnace)mod.TakenFromFurnace(player,stack);else mod.TakenFromCrafting(player,stack);
        }catch(RuntimeException|LinkageError error){
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta ModLoader result callback failed for {}",mod.getClass().getName(),error);
        }
    }
}
