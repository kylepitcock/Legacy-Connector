package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual original collision branches; private mod jars are runtime inputs only. */
public final class MoreCreepsGameplaySelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsGameplayTest");
    private MoreCreepsGameplaySelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS GAMEPLAY TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static float number(Object object,String name)throws ReflectiveOperationException{return ((Number)field(object,name).get(object)).floatValue();}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:creepsgameplay".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173CreepsGame");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static final class Context implements AutoCloseable {
        final ServerLevel level;final World world;final ContentB173.Registration registration;final net.minecraft.server.level.ServerPlayer player;
        final Set<Integer> before=new HashSet<>();final net.minecraft.core.BlockPos origin;
        Context(ServerLevel level,ContentB173.Registration registration)throws ReflectiveOperationException {
            this.level=level;this.registration=registration;world=World.mcc$wrap(level);for(var e:level.getAllEntities())before.add(e.getId());
            origin=level.getSharedSpawnPos().offset(8,36,8);level.getChunk(origin);player=actor(level);put(player,"spawnInvulnerableTime",0);
            player.setPos(origin.getX()-5.5,origin.getY(),origin.getZ()+.5);
        }
        Class<?> type(String name)throws ClassNotFoundException{return Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());}
        LivingEntity victim(String name)throws ReflectiveOperationException {
            var original=(LivingEntity)type(name).getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            require(world.spawnEntity(original)&&original.mcc$modern instanceof LegacyB173Animal,"actual living victim enters shared host: "+name);
            EntityViewsB173.flush();require(level.getEntities((net.minecraft.world.entity.Entity)null,original.mcc$modern.getBoundingBox().inflate(.1)).contains(original.mcc$modern),"actual collision victim enters loaded query sections");return original;
        }
        Entity projectile(String name,LivingEntity shooter,net.minecraft.world.entity.Entity target)throws ReflectiveOperationException {
            var original=(Entity)type(name).getConstructor(World.class,LivingEntity.class,float.class).newInstance(world,shooter,0F);
            original.setPosition(target.getX(),target.getY()+.5,target.getZ()-1.5);original.velocityX=0;original.velocityY=0;original.velocityZ=2;
            require(world.spawnEntity(original)&&original.mcc$modern instanceof B173EntityHost,"actual original projectile enters neutral host");EntityViewsB173.flush();return original;
        }
        void tick(Entity original){MenusB173.withActor(player,original.mcc$modern::tick);}
        @Override public void close(){EntityViewsB173.flush();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var e:level.getAllEntities())if(!before.contains(e.getId()))added.add(e);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();}
    }
    private static void shrink(Context c,String species,boolean terminal)throws ReflectiveOperationException {
        var victim=c.victim(species);float initial=terminal?(species.endsWith("Ted")?.6F:.4F):2F;
        put(victim,"robotsize",initial);put(victim,"modelspeed",1F);((LegacyB173Animal)victim.mcc$modern).flushView();
        Entity projectile;
        try(var scope=EntityViewsB173.callbackScope()){projectile=c.projectile("CREEPSEntityShrink",(LivingEntity)EntityViewsB173.wrap(c.player),victim.mcc$modern);}
        c.tick(projectile);
        if(terminal)require(victim.dead&&victim.mcc$modern.isRemoved()&&projectile.dead&&projectile.mcc$modern.isRemoved(),"original threshold Shrink removes exact victim and projectile: "+species);
        else {
            float sizeCost=species.endsWith("Ted")?.5F:.4F,speedCost=species.endsWith("Ted")?.25F:.15F;
            require(Math.abs(number(victim,"robotsize")-(initial-sizeCost))<.00001&&Math.abs(number(victim,"modelspeed")-(1-speedCost))<.00001,"original Shrink mutates exact original size and model speed: "+species+" size="+number(victim,"robotsize")+" speed="+number(victim,"modelspeed"));
            require(!victim.dead&&!victim.mcc$modern.isRemoved(),"original nonterminal Shrink keeps victim alive");
            // Re-querying the host after the callback must retain the mod's own original fields.
            try(var scope=EntityViewsB173.callbackScope()){require(EntityViewsB173.wrap(victim.mcc$modern)==victim,"Shrink victim query retains exact original class identity");}
            require(Math.abs(number(victim,"robotsize")-(initial-sizeCost))<.00001,"cross-host size mutation survives later query refresh");
        }
    }
    private static void ray(Context c,boolean playerFire)throws ReflectiveOperationException {
        var target=new net.minecraft.world.entity.npc.Villager(net.minecraft.world.entity.EntityType.VILLAGER,c.level);target.setNoAi(true);target.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);require(c.level.addFreshEntity(target),"ray victim enters query sections");
        var shooter=playerFire?null:c.victim("CREEPSEntityFloob");if(shooter!=null){shooter.setPosition(c.player.getX(),c.player.getY(),c.player.getZ());((LegacyB173Animal)shooter.mcc$modern).flushView();}
        Entity projectile;try(var scope=EntityViewsB173.callbackScope()){projectile=c.projectile("CREEPSEntityRay",playerFire?(LivingEntity)EntityViewsB173.wrap(c.player):shooter,target);}
        float health=target.getHealth();int damage=(int)number(projectile,"damage");c.tick(projectile);
        require(target.getHealth()==health-damage,"actual "+(playerFire?"player":"Floob")+" Ray collision applies original damage to native victim: before="+health+" after="+target.getHealth()+" damage="+damage);
        require(target.getLastHurtByMob()==(playerFire?c.player:shooter.mcc$modern),"actual Ray preserves exact original shooter as native living damage attribution: "+(playerFire?"player":"Floob"));
    }
    private static void rocketSplash(Context c)throws ReflectiveOperationException {
        var target=new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW,c.level);target.setNoAi(true);target.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);target.setHealth(5);target.setDeltaMovement(0,0,0);require(c.level.addFreshEntity(target),"rocket splash victim enters loaded query sections");
        var mod=c.registration.mod().instance;var count=field(mod,"rocketcount");int previousCount=count.getInt(null);
        var achievement=(legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement)field(mod,"achieverocketrampage").get(null);
        var nativeAchievement=c.level.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));require(nativeAchievement!=null,"original RocketRampage achievement is loaded");c.player.getAdvancements().revoke(nativeAchievement,AdvancementsB173.CRITERION);
        var previousPlayers=c.world.players;
        try {
            count.setInt(null,50);
            // Supply the controlled real actor to the original nearest-player input; restore the live list below.
            c.world.players=new ArrayList<>();
            try(var scope=EntityViewsB173.callbackScope()){
                var actor=EntityViewsB173.wrap(c.player);c.world.players.add(actor);
                var rocket=c.projectile("CREEPSEntityRocket",(LivingEntity)actor,target);
                rocket.setPosition(target.getX(),target.getY(),target.getZ());((B173EntityHost)rocket.mcc$modern).flushView();
                var view=(LivingEntity)EntityViewsB173.wrap(target);
                try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(c.world,actor)){
                    MenusB173.withActor(c.player,()->{try{rocket.getClass().getMethod("checkSplashDamage").invoke(rocket);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}});
                }
                require(target.getHealth()==0&&view.health==0&&!view.dead,"original splash observes lethal native health synchronously before entity removal");
            }
            require(Math.abs(target.getDeltaMovement().y-.20000000298023224)<.000001,"original splash pending +.2 vertical velocity survives native lethal damage");
            require(c.player.getAdvancements().getOrStartProgress(nativeAchievement).isDone(),"actual Rocket splash lethal branch awards RocketRampage to exact real actor");
        }finally{c.world.players=previousPlayers;count.setInt(null,previousCount);c.player.getAdvancements().revoke(nativeAchievement,AdvancementsB173.CRITERION);}
    }

    private static void run(ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getSimpleName().equals("CREEPSEntityRobotTed")&&r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(registration.isEmpty())return;
        var failures=new ArrayList<String>();int passed=0;
        for(String name:List.of("RobotTed shrink","RobotTed threshold","RobotTodd shrink","RobotTodd threshold","player Ray","Floob Ray","Rocket splash")){
            try(var c=new Context(level,registration.get())){
                if(name.equals("Rocket splash"))rocketSplash(c);else if(name.endsWith("Ray"))ray(c,name.startsWith("player"));else shrink(c,"CREEPSEntity"+name.split(" ")[0],name.endsWith("threshold"));
                passed++;LOG.info("B1.7.3 CREEPS GAMEPLAY TEST: {} original collision branch passed",name);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(name+": "+error);LOG.error("B1.7.3 CREEPS GAMEPLAY TEST failed: {}",name,error);}
        }
        require(failures.isEmpty(),"B1.7.3 CREEPS GAMEPLAY TEST: "+failures);LOG.info("B1.7.3 CREEPS GAMEPLAY TEST: all {} actual collision branches passed",passed);
    }
}
