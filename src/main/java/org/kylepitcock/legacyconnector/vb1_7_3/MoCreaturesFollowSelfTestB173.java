package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoCreaturesFollowSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173FollowTest");
    private MoCreaturesFollowSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC FOLLOW TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var result=type.getDeclaredField(name);result.setAccessible(true);return result;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    /** Zero only the requested decision bound; suppress unrelated growth, mood and hunger transitions. */
    private static final class DecisionRandom extends Random {private final int bound;DecisionRandom(int bound){this.bound=bound;}@Override public int nextInt(int limit){return limit==bound?0:limit-1;}@Override public float nextFloat(){return .5F;}}
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final ContentB173.Registration registration;final net.minecraft.core.BlockPos origin;
        final Map<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new LinkedHashMap<>();
        final List<net.minecraft.world.entity.Entity> entities=new ArrayList<>();final Set<net.minecraft.world.entity.Entity> before;
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration){this.level=level;this.registration=registration;world=World.mcc$wrap(level);origin=level.getSharedSpawnPos().offset(8,80,8);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk(origin.offset(x*16,0,z*16));before=new HashSet<>(level.getEntities((net.minecraft.world.entity.Entity)null,area()));}
        net.minecraft.world.phys.AABB area(){return new net.minecraft.world.phys.AABB(origin).inflate(40);}
        LegacyB173Animal spawn(String name,double x)throws ReflectiveOperationException {var type=Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());var original=(LivingEntity)type.getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+x,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"original advanced fixture spawn admitted");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);put(original,"random",new DecisionRandom(-1));require(level.getEntities((net.minecraft.world.entity.Entity)null,area()).contains(host),"advanced fixture enters loaded native query sections");return host;}
        void decision(LegacyB173Animal host,int bound)throws ReflectiveOperationException {put(host.legacy(),"random",new DecisionRandom(bound));}
        void block(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state){blocks.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,state,3);}
        @Override public void close(){blocks.forEach((pos,state)->level.setBlock(pos,state,3));for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,area()))if(!before.contains(entity))entity.discard();for(var entity:entities)entity.discard();EntityViewsB173.flush();}
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("bigcat rope path and sitting boundaries",MoCreaturesFollowSelfTestB173::ropeBoundaries),new Case("bigcat original teleport fallback",MoCreaturesFollowSelfTestB173::teleport),new Case("nightmare rider fire clearing",MoCreaturesFollowSelfTestB173::nightmare));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC FOLLOW TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC FOLLOW TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC FOLLOW TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC FOLLOW TEST: all {} original-callback proofs passed",cases.size());}
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:moc-follow".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Follow");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private static net.minecraft.server.level.ServerPlayer roper(Context c,double x){
        var player=actor(c.level);c.entities.add(player);player.setPos(c.origin.getX()+x,c.origin.getY(),c.origin.getZ()+.5);c.level.addNewPlayer(player);return player;
    }
    private static void prepareCat(Context c,LegacyB173Animal cat,net.minecraft.server.level.ServerPlayer player)throws ReflectiveOperationException {
        put(cat.legacy(),"tamed",true);put(cat.legacy(),"adult",true);put(cat.legacy(),"hungry",false);put(cat.legacy(),"sitting",false);put(cat.legacy(),"roper",PlayerViewB173.of(player));c.decision(cat,20);
    }
    private static void ropeBoundaries(Context c)throws ReflectiveOperationException {
        for(int x=-2;x<=23;x++)for(int z=-3;z<=3;z++){
            c.block(c.origin.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for(int y=0;y<=3;y++)c.block(c.origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        var cat=c.spawn("EntityBigCat",.5);var player=roper(c,10.5);prepareCat(c,cat,player);cat.setOnGround(true);cat.setDeltaMovement(0,-.08,0);cat.syncView();
        var preflight=c.world.findPath(cat.legacy(),PlayerViewB173.of(player),16);
        require(preflight!=null&&preflight.mcc$modern()!=null&&!preflight.isFinished(),"grounded clear-corridor fixture has native path before original rope callback: ground="+cat.onGround()+" position="+cat.position());
        cat.aiStep();
        var originalPath=((MobEntity)cat.legacy()).mcc$path();
        require(originalPath!=null&&!originalPath.isFinished(),"original rope callback retains unfinished shim path: ground="+cat.onGround()+" position="+cat.position()+" distance="+cat.legacy().getDistance(PlayerViewB173.of(player))+" sitting="+get(cat.legacy(),"sitting")+" roper="+get(cat.legacy(),"roper")+" nativePath="+cat.getNavigation().getPath());
        require(!cat.getNavigation().isDone(),"original rope callback installs active native path: shimLength="+originalPath.length+" shimIndex="+originalPath.currentNodeIndex+" nativePath="+cat.getNavigation().getPath());
        // A fresh cat has no steering state from the successful follow proof. Beta distance
        // includes each entity's eye offset, so align original Y rather than native feet Y.
        cat.discard();cat=c.spawn("EntityBigCat",.5);prepareCat(c,cat,player);put(cat.legacy(),"sitting",true);
        cat.setOnGround(true);cat.setDeltaMovement(0,-.08,0);cat.syncView();cat.legacy().mcc$movementInputs(0,0,false);cat.flushView();
        double alignedPlayerY=cat.legacy().y-PlayerViewB173.of(player).standingEyeHeight;
        player.setPos(c.origin.getX()+18.5,alignedPlayerY,c.origin.getZ()+.5);EntityViewsB173.flush();
        float before=cat.legacy().getDistance(PlayerViewB173.of(player));
        require(before==18F,"sitting boundary fixture starts at original distance eighteen: "+followState(cat,player));
        LOG.info("B1.7.3 MOC FOLLOW TEST sitting boundary before: {}",followState(cat,player));cat.aiStep();
        LOG.info("B1.7.3 MOC FOLLOW TEST sitting boundary after: {}",followState(cat,player));
        require(get(cat.legacy(),"roper")!=null&&!((MobEntity)cat.legacy()).hasPath()&&cat.legacy().getDistance(PlayerViewB173.of(player))==18F,"sitting cat retains rope at exactly eighteen blocks and does not follow: "+followState(cat,player));
        player.setPos(c.origin.getX()+19.5,alignedPlayerY,c.origin.getZ()+.5);EntityViewsB173.flush();cat.aiStep();
        require(get(cat.legacy(),"roper")==null,"original sitting cat releases rope beyond eighteen blocks: "+followState(cat,player));
    }
    private static String followState(LegacyB173Animal cat,net.minecraft.server.level.ServerPlayer player)throws ReflectiveOperationException {
        return "roper="+get(cat.legacy(),"roper")+" sitting="+get(cat.legacy(),"sitting")+" originalDistance="+cat.legacy().getDistance(PlayerViewB173.of(player))+" cat="+cat.position()+" player="+player.position()+" catLegacyY="+cat.legacy().y+" playerLegacyY="+PlayerViewB173.of(player).y+" shimPath="+((MobEntity)cat.legacy()).mcc$path()+" nativePath="+cat.getNavigation().getPath();
    }
    private static void teleport(Context c)throws ReflectiveOperationException {
        var cat=c.spawn("EntityBigCat",.5);var player=roper(c,15.5);prepareCat(c,cat,player);
        // Enclose the start so native navigation cannot reach the isolated landing pad.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=3;y++)if(x!=0||z!=0||y==-1||y==3)c.block(c.origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        c.block(c.origin.offset(13,-1,-2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        cat.setOnGround(true);cat.setDeltaMovement(0,-.08,0);cat.syncView();
        var failedNative=cat.getNavigation().createPath(player.blockPosition(),0,16);
        require(failedNative!=null&&!failedNative.canReach()&&failedNative.getNodeCount()==1,"grounded enclosed original cat exposes native unreached start-only path");
        require(c.world.findPath(cat.legacy(),PlayerViewB173.of(player),16)==null,"teleport fixture proves native path failure before original callback");
        cat.getNavigation().moveTo(failedNative,.2);
        require(c.world.findPath(cat.legacy(),PlayerViewB173.of(player),16)==null,"reused native start-only result retains original null-path fallback contract");
        cat.aiStep();
        require(Math.abs(cat.getX()-(c.origin.getX()+13.5))<.00001&&Math.abs(cat.getY()-c.origin.getY())<.00001&&Math.abs(cat.getZ()-(c.origin.getZ()-1.5))<.00001,"original rope fallback selects first solid-ground, two-clear-block perimeter position: "+cat.position());
        require(get(cat.legacy(),"roper")!=null,"walking cat preserves rope after original fallback teleport");
    }
    private static void nightmare(Context c)throws ReflectiveOperationException {
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)c.block(c.origin.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var horse=c.spawn("EntityHorse",.5);horse.setOnGround(true);horse.legacy().getClass().getMethod("setType",int.class).invoke(horse.legacy(),7);put(horse.legacy(),"tamed",true);put(horse.legacy(),"adult",true);put(horse.legacy(),"nightmareInt",2);
        var player=roper(c,.5);require(player.startRiding(horse,true),"nightmare player rider mounted");player.setRemainingFireTicks(100);EntityViewsB173.flush();c.decision(horse,2);
        var fire=c.origin.offset(-1,0,-1);c.block(fire.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());c.block(fire,c.level.getBlockState(fire));horse.aiStep();
        require(player.getRemainingFireTicks()==0,"actual nightmare callback clears native rider fire through shared player view");
        require((Integer)get(horse.legacy(),"nightmareInt")==1&&c.level.getBlockState(fire).is(net.minecraft.world.level.block.Blocks.FIRE),"original nightmare effect decrements charge and places fire at offset");
        player.setRemainingFireTicks(100);put(horse.legacy(),"nightmareInt",0);EntityViewsB173.flush();horse.aiStep();
        require(player.getRemainingFireTicks()==100,"exhausted nightmare preserves native rider fire");player.stopRiding();
    }
}
