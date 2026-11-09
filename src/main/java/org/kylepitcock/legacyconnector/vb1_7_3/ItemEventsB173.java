package org.kylepitcock.legacyconnector.vb1_7_3;

/** Native result-slot events invoke the original ModLoader callbacks, including vanilla recipe outputs. */
public final class ItemEventsB173 {
    private static boolean installed;
    private ItemEventsB173(){}
    public static synchronized void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e)->nativeTaken(e.getEntity(),e.getCrafting(),false));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent e)->nativeTaken(e.getEntity(),e.getSmelting(),true));
    }
    private static void nativeTaken(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack,boolean furnace){
        if(player.level().isClientSide)return;var original=StacksB173.toLegacy(stack);if(original==null)return;
        try(var views=EntityViewsB173.callbackScope()){
            var actor=(PlayerViewB173)EntityViewsB173.wrap(player);
            try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(actor.world,actor)){
                original.onCraft(actor.world,actor);taken(actor,original,furnace);actor.writeBack();
            }
        }finally{EntityViewsB173.flush();BlockEntityViewsB173.flush();}
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
