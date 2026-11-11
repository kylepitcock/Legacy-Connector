package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.kylepitcock.legacyconnector.vb1_7_3.BlockQueryB173;
import org.kylepitcock.legacyconnector.vb1_7_3.BlockTintB173;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem;

/** Native color registration for original blocks and their metadata-bearing inventory forms. */
final class BlockColorsB173 {
    private BlockColorsB173(){}
    private static boolean tintsSprite(legacyconnector.vb1_7_3.net.minecraft.item.Item item){
        try{return item.getClass().getMethod("getColorMultiplier",int.class).getDeclaringClass()!=legacyconnector.vb1_7_3.net.minecraft.item.Item.class;}
        catch(ReflectiveOperationException e){return false;}
    }
    static void register(IEventBus bus) {
        bus.addListener((RegisterColorHandlersEvent.Block event)->{
            for(var block:ContentB173.blocks())if(block.mcc$modern instanceof LegacyB173Block&&BlockTintB173.hasColor(block))
                event.register((state,level,pos,tint)->level==null||pos==null
                        ?BlockTintB173.inventoryColor(block,state.getValue(LegacyB173Block.META))
                        :BlockTintB173.worldColor(block,new BlockQueryB173(level,pos,state),pos.getX(),pos.getY(),pos.getZ()),block.mcc$modern);
        });
        bus.addListener((RegisterColorHandlersEvent.Item event)->{
            for(var block:ContentB173.blocks())if(block.mcc$modern instanceof LegacyB173Block&&BlockTintB173.hasColor(block)
                    &&block.mcc$modern.asItem() instanceof LegacyB173BlockItem item)
                event.register((stack,tint)->BlockTintB173.inventoryColor(block,LegacyB173BlockItem.meta(stack)),item);
            // Beta items tint their sprite through getColorMultiplier (dyed or metal-coloured armor, rings); the colour comes from the original item.
            for(var legacy:ContentB173.items())if(legacy.mcc$owner!=null&&legacy.mcc$modern!=null&&tintsSprite(legacy))
                event.register((stack,tint)->{
                    if(tint!=0)return -1;
                    var original=org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toLegacy(stack);
                    if(original==null)return -1;
                    try{return 0xFF000000|legacy.getColorMultiplier(original.getDamage());}
                    catch(RuntimeException|LinkageError failed){return -1;}
                },legacy.mcc$modern);
        });
    }
}
