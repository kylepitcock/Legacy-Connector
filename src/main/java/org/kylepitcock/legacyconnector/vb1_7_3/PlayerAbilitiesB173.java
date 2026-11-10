package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.world.entity.player.Player;

/** Native event baselines and original PlayerAPI replacement/query hooks share one actor-scoped boundary. */
public final class PlayerAbilitiesB173 {
    enum Hook {HEAL,WATER,MINING,DISTANCE}
    private static final ThreadLocal<IdentityHashMap<Object,EnumSet<Hook>>> ACTIVE=ThreadLocal.withInitial(IdentityHashMap::new);
    private static boolean installed;
    private PlayerAbilitiesB173(){}
    static boolean active(Object subject,Hook hook){var set=ACTIVE.get().get(subject);return set!=null&&set.contains(hook);}
    static final class Scope implements AutoCloseable {
        final Object subject;final Hook hook;final boolean added;
        Scope(Object subject,Hook hook){this.subject=subject;this.hook=hook;added=ACTIVE.get().computeIfAbsent(subject,key->EnumSet.noneOf(Hook.class)).add(hook);}
        public void close(){if(!added)return;var map=ACTIVE.get();var set=map.get(subject);set.remove(hook);if(set.isEmpty())map.remove(subject);if(map.isEmpty())ACTIVE.remove();}
    }
    private static Object subject(ClientPlayerEntity view){return view.mcc$modern==null?view:view.mcc$modern;}
    private static ClientPlayerEntity view(Player player){
        var actor=PlayerApiB173.currentPlayer();if(actor!=null&&actor.mcc$modern==player)return actor;
        actor=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();if(actor!=null&&actor.mcc$modern==player)return actor;
        var rendered=PlayerRenderingB173.cachedClientView(player);if(rendered!=null)return rendered;
        var cached=PlayerViewB173.peek(player);return cached!=null?cached:PlayerViewB173.of(player);
    }
    public static boolean integralHeal(float amount){return Float.isFinite(amount)&&amount>0&&(double)amount<=Integer.MAX_VALUE&&amount==Math.rint(amount);}
    public static synchronized void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event)->{
            var player=event.getEntity();if(!PlayerApiB173.hasAbility("getCurrentPlayerStrVsBlock")||active(player,Hook.MINING))return;
            int id=BlocksB173.legacyId(event.getState());var blocks=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS;
            if(id<=0||id>=blocks.length||blocks[id]==null)return;
            try(var scope=new Scope(player,Hook.MINING)){event.setNewSpeed(PlayerApiB173.miningStrength(view(player),blocks[id],event.getNewSpeed()));}
        });
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,(net.neoforged.neoforge.event.entity.living.LivingHealEvent event)->{
            if(!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)||!integralHeal(event.getAmount())
                    ||!PlayerApiB173.hasAbility("heal")||active(player,Hook.HEAL))return;
            try(var scope=new Scope(player,Hook.HEAL)){
                var view=view(player);if(view instanceof PlayerViewB173 nativeView)nativeView.mcc$prepareNativeHeal();
                boolean replaced=PlayerApiB173.heal(view,(int)event.getAmount());
                if(view instanceof PlayerViewB173 nativeView)nativeView.writeBack();
                if(replaced)event.setCanceled(true);
            }
        });
    }
    public static void heal(ClientPlayerEntity view,int amount,Runnable baseline){
        Object key=subject(view);
        if(!PlayerApiB173.hasAbility("heal")||active(key,Hook.HEAL)){baseline.run();return;}
        try(var scope=new Scope(key,Hook.HEAL)){
            boolean replaced=PlayerApiB173.heal(view,amount);
            if(view instanceof PlayerViewB173 nativeView)nativeView.writeBack();
            if(!replaced)baseline.run();
        }
    }
    public static boolean water(ClientPlayerEntity view,boolean baseline){
        return water(view,()->baseline);
    }
    public static boolean water(ClientPlayerEntity view,java.util.function.BooleanSupplier baseline){
        Object key=subject(view);if(!PlayerApiB173.hasAbility("isInWater")||active(key,Hook.WATER))return baseline.getAsBoolean();
        try(var scope=new Scope(key,Hook.WATER)){return PlayerApiB173.waterPredicate(view,baseline.getAsBoolean());}
    }
    public static boolean nativeWater(Player player,boolean baseline){
        if(!PlayerApiB173.hasAbility("isInWater")||active(player,Hook.WATER)||!player.isAddedToLevel())return baseline;
        // Guard before first view initialization: sync itself queries the native water predicate.
        try(var scope=new Scope(player,Hook.WATER)){return PlayerApiB173.waterPredicate(view(player),baseline);}
    }
    /** NeoForge also selects water travel through fluid membership, independently of Entity.isInWater. */
    public static boolean travelWaterMembership(net.minecraft.world.entity.LivingEntity entity,boolean water,boolean baseline){
        return water&&entity instanceof Player player?nativeWater(player,baseline):baseline;
    }
    public static boolean nativeWaterBaseline(Player player){try(var scope=new Scope(player,Hook.WATER)){return player.isInWater();}}
    public static double distance(ClientPlayerEntity view,double x,double y,double z,double baseline){
        Object key=subject(view);if(!PlayerApiB173.hasAbility("getDistanceSq")||active(key,Hook.DISTANCE))return baseline;
        try(var scope=new Scope(key,Hook.DISTANCE)){return PlayerApiB173.squaredDistance(view,x,y,z,baseline);}
    }
    public static double nativeDistance(net.minecraft.world.entity.Entity entity,double x,double y,double z,double baseline){
        if(!(entity instanceof Player player)||!PlayerApiB173.hasAbility("getDistanceSq")||active(player,Hook.DISTANCE)
                ||!player.isAddedToLevel()||player.isRemoved())return baseline;
        // A geometry query must never initialize a player extension or refresh over pending callback writes.
        ClientPlayerEntity actor=PlayerApiB173.currentPlayer();
        if(actor==null||actor.mcc$modern!=player)actor=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
        if(actor==null||actor.mcc$modern!=player)actor=PlayerRenderingB173.cachedClientView(player);
        if(actor==null||actor.mcc$modern!=player)actor=PlayerViewB173.peek(player);
        if(actor==null||actor.mcc$modern!=player||actor.world==null||actor.world.mcc$level()!=player.level())return baseline;
        try(var scope=new Scope(player,Hook.DISTANCE)){return PlayerApiB173.existingSquaredDistance(actor,x,y,z,baseline);}
    }
}
