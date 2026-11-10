package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.src.IReach;
import legacyconnector.vb1_7_3.net.minecraft.src.IReachBlock;
import legacyconnector.vb1_7_3.net.minecraft.src.IReachEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;

/** Original held-item reach, expressed as an owned transient contribution to native interaction attributes. */
public final class ReachB173 {
    static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath("legacyconnector","b173_held_item_reach");
    enum Kind { BOTH,BLOCK,ENTITY }
    record Handler(Object callback,Kind kind) {}
    record Desired(Float block,Float entity) {}
    static final class Registry {
        final CopyOnWriteArrayList<Handler> handlers=new CopyOnWriteArrayList<>();
        void add(Object callback,Kind kind){handlers.add(new Handler(java.util.Objects.requireNonNull(callback),kind));}
        Desired resolve(ItemStack stack,Function<Handler,Desired> evaluate){
            Float block=null,entity=null;
            for(var handler:handlers){
                if((block!=null||handler.kind==Kind.ENTITY)&&(entity!=null||handler.kind==Kind.BLOCK))continue;
                var value=evaluate.apply(handler);
                if(block==null)block=value.block;if(entity==null)entity=value.entity;
                if(block!=null&&entity!=null)break;
            }
            return new Desired(block,entity);
        }
        Desired resolve(ItemStack stack){return resolve(stack,handler->invoke(handler,stack));}
        Float query(ItemStack stack,boolean block){
            for(var handler:handlers){
                if(block&&handler.kind==Kind.ENTITY||!block&&handler.kind==Kind.BLOCK)continue;
                var value=invoke(handler,stack);Float requested=block?value.block:value.entity;
                if(requested!=null)return requested;
            }
            return null;
        }
    }
    private static final Registry REGISTRY=new Registry();
    private static final Set<Class<?>> REPORTED=ConcurrentHashMap.newKeySet();
    private static boolean installed;
    private ReachB173(){}
    public static void register(IReach callback){REGISTRY.add(callback,Kind.BOTH);}
    public static void register(IReachBlock callback){REGISTRY.add(callback,Kind.BLOCK);}
    public static void register(IReachEntity callback){REGISTRY.add(callback,Kind.ENTITY);}
    static Desired invoke(Handler handler,ItemStack stack){
        return switch(handler.kind){
            case BOTH->{var callback=(IReach)handler.callback;Float value=callback.reachItemMatches(stack)?callback.getReach(stack):null;yield new Desired(value,value);}
            case BLOCK->{var callback=(IReachBlock)handler.callback;yield new Desired(callback.reachBlockItemMatches(stack)?callback.getReachBlock(stack):null,null);}
            case ENTITY->{var callback=(IReachEntity)handler.callback;yield new Desired(null,callback.reachEntityItemMatches(stack)?callback.getReachEntity(stack):null);}
        };
    }
    public static float query(ItemStack stack,boolean block){Float requested=REGISTRY.query(stack,block);return requested==null?(block?4:3):requested;}
    public static synchronized void install(){
        if(installed)return;installed=true;
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event)->{if(event.getEntity() instanceof ServerPlayer player)update(player);});
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event)->{if(event.getEntity() instanceof ServerPlayer player&&event.getSlot()==net.minecraft.world.entity.EquipmentSlot.MAINHAND)update(player);});
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)->{if(event.getEntity() instanceof ServerPlayer player)update(player);});
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event)->{if(event.getEntity() instanceof ServerPlayer player)update(player);});
    }
    public static void update(ServerPlayer player){
        if(REGISTRY.handlers.isEmpty())return;
        var actor=Minecraft.mcc$currentPlayerContext();
        // Reuse pending original inventory edits in an active callback; never refresh that view midway through it.
        if(actor==null||actor.mcc$modern!=player)actor=PlayerViewB173.of(player);
        var stack=actor.inventory.getSelectedItem();
        var minecraft=Minecraft.mcc$instance();var previousWorld=minecraft.world;
        try(var context=Minecraft.mcc$context(actor.world,actor)){
            var desired=REGISTRY.resolve(stack,handler->{
                Desired[] value={new Desired(null,null)};
                try{LegacyModLocks.serverSide(LegacyModLocks.of(handler.callback),()->value[0]=invoke(handler,stack));}
                catch(RuntimeException|LinkageError error){if(REPORTED.add(handler.callback.getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Reach").error("Original reach callback failed: {}",handler.callback.getClass().getName(),error);}
                return value[0];
            });
            apply(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE),desired.block,Attributes.BLOCK_INTERACTION_RANGE.value().getDefaultValue());
            apply(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE),desired.entity,Attributes.ENTITY_INTERACTION_RANGE.value().getDefaultValue());
        }finally{minecraft.world=previousWorld;}
    }
    static void apply(AttributeInstance attribute,Float requested,double nativeDefault){
        if(attribute==null)return;
        if(requested==null||!Float.isFinite(requested)||requested<0){attribute.removeModifier(MODIFIER);return;}
        double amount=requested-nativeDefault;
        var current=attribute.getModifier(MODIFIER);
        if(current!=null&&current.operation()==AttributeModifier.Operation.ADD_VALUE&&Double.compare(current.amount(),amount)==0)return;
        attribute.removeModifier(MODIFIER);
        if(amount!=0)attribute.addTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_VALUE));
    }
}
