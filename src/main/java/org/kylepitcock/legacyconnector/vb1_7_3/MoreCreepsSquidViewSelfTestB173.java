package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Squid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Native identity interoperability and an original Bullet callback; not a prior Squid-specific gameplay failure. */
public final class MoreCreepsSquidViewSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173SquidViewTest");
    private MoreCreepsSquidViewSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS SQUID VIEW TEST failed",error);}
    });}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static final String[][] ANIMATION={{"tiltAngle","xBodyRot"},{"lastTiltAngle","xBodyRotO"},{"rollAngle","zBodyRot"},{"lastRollAngle","zBodyRotO"},
            {"thrustTimer","tentacleMovement"},{"lastThrustTimer","oldTentacleMovement"},{"tentacleAngle","tentacleAngle"},{"lastTentacleAngle","oldTentacleAngle"}};
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:squid-view".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Squid");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private static void run(ServerLevel level)throws ReflectiveOperationException {
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(selected.isEmpty())return;
        var registration=selected.get();var world=World.mcc$wrap(level);var origin=level.getSharedSpawnPos().offset(8,36,8);
        var before=new HashSet<Integer>();for(var entity:level.getAllEntities())before.add(entity.getId());
        var blocks=new LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();var player=actor(level);
        try{
            for(int x=-2;x<=10;x++)for(int y=-1;y<=3;y++)for(int z=-3;z<=3;z++){
                var pos=origin.offset(x,y,z);level.getChunk(pos);blocks.put(pos,level.getBlockState(pos));level.setBlock(pos,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            }
            player.setPos(origin.getX()-5,origin.getY(),origin.getZ()-5);level.addNewPlayer(player);
            var squid=new Squid(EntityType.SQUID,level);var glow=new net.minecraft.world.entity.GlowSquid(EntityType.GLOW_SQUID,level);
            int index=0;for(var nativeSquid:List.of(squid,glow)){
                nativeSquid.setPos(origin.getX()+index*6+.5,origin.getY(),origin.getZ()+.5);require(level.addFreshEntity(nativeSquid),"native squid enters loaded query sections");
                nativeSquid.tick();require(nativeSquid.isInWater(),"actual native squid establishes controlled wet habitat");
                for(int i=0;i<ANIMATION.length;i++)field(nativeSquid,ANIMATION[i][1]).setFloat(nativeSquid,(i+1)*.125F);
                var type=nativeSquid.getType();var loot=nativeSquid.getLootTable();boolean noAi=nativeSquid.isNoAi();
                var goals=field(nativeSquid,"goalSelector").get(nativeSquid);var targets=field(nativeSquid,"targetSelector").get(nativeSquid);
                var goalEntries=new HashSet<>(((net.minecraft.world.entity.ai.goal.GoalSelector)goals).getAvailableGoals());
                Entity view;
                try(var views=EntityViewsB173.callbackScope()){
                    view=EntityViewsB173.wrap(nativeSquid);
                    require(view instanceof WaterCreatureEntity,"native Squid view must retain original water-creature ancestry");
                    // Resolve only after the existing ancestry assertion, so baseline compiles without the new shim.
                    var squidClass=Class.forName("legacyconnector.vb1_7_3.net.minecraft.entity.passive.SquidEntity");
                    require(squidClass.isInstance(view)&&view.mcc$modern==nativeSquid,"native Squid and GlowSquid share canonical Beta Squid identity");
                    var box=view.boundingBox.expand(1,1,1);
                    require(world.collectEntitiesByClass(WaterCreatureEntity.class,box).contains(view)&&world.collectEntitiesByClass(squidClass,box).contains(view),"water/Squid typed original queries include the exact native view");
                    for(var pair:ANIMATION)require(field(view,pair[0]).getFloat(view)==field(nativeSquid,pair[1]).getFloat(nativeSquid),"native squid animation field is visible: "+pair[0]);
                }
                try(var views=EntityViewsB173.callbackScope()){require(EntityViewsB173.wrap(nativeSquid)==view,"canonical native Squid identity survives separate callback scopes");}
                require(nativeSquid.getType()==type&&nativeSquid.getLootTable().equals(loot)&&nativeSquid.isNoAi()==noAi&&!nativeSquid.isRemoved(),"read-only original views preserve native species, loot and AI mode");
                require(field(nativeSquid,"goalSelector").get(nativeSquid)==goals&&field(nativeSquid,"targetSelector").get(nativeSquid)==targets
                        &&((net.minecraft.world.entity.ai.goal.GoalSelector)goals).getAvailableGoals().equals(goalEntries),"original wrapping preserves native goal objects and registrations");
                for(int i=0;i<ANIMATION.length;i++)require(field(nativeSquid,ANIMATION[i][1]).getFloat(nativeSquid)==(i+1)*.125F,"read-only wrapping does not rewrite native animation");
                Entity projectile;
                try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,view);var views=EntityViewsB173.callbackScope()){
                    var owner=(LivingEntity)EntityViewsB173.wrap(player);
                    var bulletClass=Class.forName(registration.type().getPackageName()+".CREEPSEntityBullet",true,registration.type().getClassLoader());
                    projectile=(Entity)bulletClass.getConstructor(World.class,LivingEntity.class,float.class).newInstance(world,owner,0F);
                    field(projectile,"damage").setInt(projectile,3);field(projectile,"playerFire").setBoolean(projectile,true);
                    projectile.setPosition(nativeSquid.getX(),nativeSquid.getY()+.4,nativeSquid.getZ()-2);projectile.velocityX=0;projectile.velocityY=0;projectile.velocityZ=3;
                    require(world.spawnEntity(projectile),"actual MoreCreeps Bullet admitted for native wet squid collision");
                }
                float health=nativeSquid.getHealth();MenusB173.withActor(player,projectile.mcc$modern::tick);EntityViewsB173.flush();
                require(nativeSquid.getHealth()==health-3&&projectile.mcc$modern.isRemoved(),"actual original Bullet collision damages native wet Squid once and expires");
                try(var views=EntityViewsB173.callbackScope()){require(EntityViewsB173.wrap(nativeSquid)==view,"native damage retains canonical Squid view");}
                index++;
            }
            LOG.info("B1.7.3 CREEPS SQUID VIEW TEST: native Squid/GlowSquid identity, typed queries, animation/AI/loot preservation and actual original Bullet wet collisions passed");
        }finally{
            EntityViewsB173.flush();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))added.add(entity);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();
            blocks.forEach((pos,state)->level.setBlock(pos,state,2|16));EntityViewsB173.flush();
        }
    }
}
