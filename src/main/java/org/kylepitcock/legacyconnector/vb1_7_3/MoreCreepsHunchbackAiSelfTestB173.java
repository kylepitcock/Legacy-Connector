package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Autonomous original Hunchback decisions, independent of the recruitment/army interaction proof. */
public final class MoreCreepsHunchbackAiSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173HunchbackAiTest");
    private MoreCreepsHunchbackAiSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 HUNCHBACK AI TEST failed",error);}
    });}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object get(Object object,String name)throws ReflectiveOperationException{return field(object,name).get(object);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException{field(object,name).set(object,value);}
    private static final class Decisions extends Random {
        final boolean timer;Decisions(boolean timer){this.timer=timer;}
        @Override public int nextInt(int bound){return bound==10&&timer?0:bound==700?37:bound-1;}
        @Override public float nextFloat(){return .5F;}
    }
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:hunchback-ai".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173HunchAI");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private static final class Context implements AutoCloseable {
        final ServerLevel level;final World world;final BlockPos origin;final net.minecraft.server.level.ServerPlayer player;
        final LegacyB173Animal host;final MobEntity original;final Set<Integer> before=new HashSet<>();
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new LinkedHashMap<>();
        Context(ServerLevel level,ContentB173.Registration registration)throws ReflectiveOperationException {
            this.level=level;world=World.mcc$wrap(level);for(var entity:level.getAllEntities())before.add(entity.getId());
            origin=level.getSharedSpawnPos().offset(8,30,8);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk((origin.getX()>>4)+dx,(origin.getZ()>>4)+dz);
            for(int x=-3;x<=23;x++)for(int z=-3;z<=3;z++){
                block(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<=4;y++)block(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
            }
            player=actor(level);field(player,"spawnInvulnerableTime").setInt(player,0);
            player.setPos(origin.getX()+10.5,origin.getY(),origin.getZ()+.5);level.addNewPlayer(player);
            original=(MobEntity)registration.type().getConstructor(World.class).newInstance(world);
            original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"actual Hunchback spawned in loaded grounded habitat");
            host=(LegacyB173Animal)original.mcc$modern;host.setOnGround(true);put(original,"random",new Decisions(false));
        }
        void block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){level.getChunk(pos);blocks.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,state,3);}
        void tick(){MenusB173.withActor(player,host::tick);EntityViewsB173.flush();}
        void tame(boolean value,int timer)throws ReflectiveOperationException {put(original,"tamed",value);put(original,"caketimer",timer);}
        legacyconnector.vb1_7_3.net.minecraft.entity.Entity target(){
            try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,original);var views=EntityViewsB173.callbackScope()){
                host.syncView();var target=original.mcc$targetInRange();host.flushView();return target;
            }
        }
        @Override public void close(){
            EntityViewsB173.flush();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))added.add(entity);
            added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();blocks.forEach((pos,state)->level.setBlock(pos,state,3));
        }
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&r.type().getSimpleName().equals("CREEPSEntityHunchback")).findFirst();if(selected.isEmpty())return;
        var failures=new ArrayList<String>();
        for(var proof:List.of(new Case("autonomous follow and visibility gates",MoreCreepsHunchbackAiSelfTestB173::follow),new Case("cake timer lottery and expiry",MoreCreepsHunchbackAiSelfTestB173::timer),new Case("tamed damage command and retaliation",MoreCreepsHunchbackAiSelfTestB173::damage),new Case("Beta player arrow owner attribution",MoreCreepsHunchbackAiSelfTestB173::playerArrow)))
            try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 HUNCHBACK AI TEST: {} passed using actual original callbacks",proof.name);}
            catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 HUNCHBACK AI TEST failed: {}",proof.name,error);}
        require(failures.isEmpty(),"B1.7.3 HUNCHBACK AI TEST: "+failures);
    }
    private static void follow(Context c)throws ReflectiveOperationException {
        c.tame(false,1000);require(c.target()==null,"untamed original does not autonomously follow player");
        c.tame(true,1000);var playerView=EntityViewsB173.wrap(c.player);EntityViewsB173.flush();
        require(c.target()==playerView,"tamed original chooses exact real-player view in visible five-to-sixteen range");
        c.player.setPos(c.host.getX()+20,c.host.getY(),c.host.getZ());require(c.target()==null,"original follow range excludes player beyond sixteen");
        c.player.setPos(c.host.getX()+3,c.host.getY(),c.host.getZ());c.original.target=playerView;
        require(c.target()==null&&c.original.target==null,"original near-five stop clears retained player target");
        c.player.setPos(c.origin.getX()+10.5,c.origin.getY(),c.origin.getZ()+.5);
        for(int y=0;y<=4;y++)for(int z=-2;z<=2;z++)c.block(c.origin.offset(5,y,z),Blocks.STONE.defaultBlockState());
        require(c.target()==null,"original follow rejects a player behind an opaque wall");
        for(int y=0;y<=4;y++)for(int z=-2;z<=2;z++)c.block(c.origin.offset(5,y,z),Blocks.AIR.defaultBlockState());
        c.original.target=playerView;c.original.setPath(null);c.host.getNavigation().stop();c.host.setOnGround(true);
        c.host.serverAIPhase();EntityViewsB173.flush();
        require(c.original.target==null&&c.host.getNavigation().isDone(),"original retained-player attack clears target without creating a fresh route from a stale target reference");
        c.original.target=null;c.host.getNavigation().stop();c.host.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        var start=c.host.position();int ai=c.host.baseAICount();boolean routed=false;
        for(int tick=0;tick<16;tick++){c.tick();routed|=!c.host.getNavigation().isDone();}
        require(routed&&c.host.baseAICount()==ai+16&&!c.host.isRemoved(),"normal native ticks execute original follow and install a live native route");
        require(c.host.getX()>start.x+.02&&c.host.position().distanceTo(c.player.position())<start.distanceTo(c.player.position()),"original autonomous follow moves native host toward real player");
        require(c.player.getHealth()==c.player.getMaxHealth(),"distant follow does not damage player");
    }
    private static void timer(Context c)throws ReflectiveOperationException {
        c.tame(true,2);put(c.original,"interest",41);put(c.original,"basetexture","/mob/creeps/hunchbackcake.png");put(c.original,"texture","/mob/creeps/hunchback.png");
        c.tick();require((Integer)get(c.original,"caketimer")==2&&(Boolean)get(c.original,"tamed")&&c.host.texturePath().endsWith("hunchbackcake.png"),"losing timer lottery retains tame duration and restores original base texture");
        put(c.original,"random",new Decisions(true));c.tick();require((Integer)get(c.original,"caketimer")==1&&(Boolean)get(c.original,"tamed"),"winning original timer lottery decrements exactly once per native tick");
        c.tick();require(!(Boolean)get(c.original,"tamed")&&(Integer)get(c.original,"caketimer")==337&&c.host.texturePath().equals("/mob/creeps/hunchback.png"),"expiry restores wild texture and original random cooldown");
        require((Integer)get(c.original,"interest")==41,"actual expiry preserves original accumulated interest quirk");
        c.tame(true,0);c.tick();require((Boolean)get(c.original,"tamed")&&(Integer)get(c.original,"caketimer")==0,"original zero timer guard does not silently untame");
    }
    private static void damage(Context c)throws ReflectiveOperationException {
        c.tame(true,1000);c.original.target=null;float before=c.host.getHealth();
        require(c.host.hurt(c.level.damageSources().playerAttack(c.player),5)&&c.host.getHealth()==before,"tamed original player hit uses integer divide-six, preserving sub-six zero damage");
        c.host.invulnerableTime=0;require(c.host.hurt(c.level.damageSources().playerAttack(c.player),12)&&c.host.getHealth()==before-2&&c.original.target==null,"tamed original player hit reduces damage and does not retaliate");
        require(Math.abs(c.host.getDeltaMovement().x-.25)<.000001&&Math.abs(c.host.getDeltaMovement().y-.45)<.000001&&Math.abs(c.host.getDeltaMovement().z-.15)<.000001,"actual tamed hit command writes original launch velocity to native host");
        c.tame(false,1000);c.host.invulnerableTime=0;before=c.host.getHealth();
        require(c.host.hurt(c.level.damageSources().playerAttack(c.player),2)&&c.host.getHealth()==before-2,"wild original hit retains full damage");
        try(var views=EntityViewsB173.callbackScope()){require(c.original.target==EntityViewsB173.wrap(c.player),"wild original retaliates against exact native attacker identity");}
    }
    private static void playerArrow(Context c)throws ReflectiveOperationException {
        c.tame(true,1000);c.original.target=null;
        legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity arrow;
        try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(c.world,c.original);var views=EntityViewsB173.callbackScope()){
            var owner=(LivingEntity)EntityViewsB173.wrap(c.player);
            arrow=new legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity(c.world,owner);
            arrow.setPosition(c.host.getX()-1,c.host.getY()+1,c.host.getZ());
            arrow.velocityX=3;arrow.velocityY=0;arrow.velocityZ=0;
            require(c.world.spawnEntity(arrow),"exact Beta player-owned arrow enters native world");
        }
        var projectile=arrow.mcc$modern;
        require(projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow,"Beta arrow has native collision host");
        java.lang.reflect.Method hit=null;
        for(Class<?> type=projectile.getClass();type!=null&&hit==null;type=type.getSuperclass())try{hit=type.getDeclaredMethod("onHitEntity",net.minecraft.world.phys.EntityHitResult.class);}catch(NoSuchMethodException ignored){}
        require(hit!=null,"native arrow impact callback available");hit.setAccessible(true);
        float health=c.host.getHealth();c.host.invulnerableTime=0;
        hit.invoke(projectile,new net.minecraft.world.phys.EntityHitResult(c.host));EntityViewsB173.flush();
        require(c.host.getHealth()==health&&c.original.target==null,"actual tamed Hunchback sees Beta arrow player owner: fixed four damage divides to zero without retaliation");
    }
}
