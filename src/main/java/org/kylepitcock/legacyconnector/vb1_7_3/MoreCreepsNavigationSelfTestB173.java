package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controlled actual MoreCreeps navigation callbacks. No production behavior is selected by these names. */
public final class MoreCreepsNavigationSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsNavigationTest");
    private MoreCreepsNavigationSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS NAVIGATION TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    private static Object call(Object object,String name,Class<?>[] signature,Object... args)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var method=type.getDeclaredMethod(name,signature);method.setAccessible(true);return method.invoke(object,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
    private static final class ZeroRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    /** FakePlayer deliberately disables riding, menus and advancements; these proofs need normal native behavior. */
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level,int index){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("legacyconnector:creeps-navigation:"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Feature"+index);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final net.minecraft.server.level.ServerPlayer player;
        final ContentB173.Registration registration;final net.minecraft.core.BlockPos origin;final ArrayList<net.minecraft.world.entity.Entity> entities=new ArrayList<>();
        final Set<Integer> baseline=new HashSet<>();final Map<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new LinkedHashMap<>();
        final net.minecraft.nbt.ListTag inventory=new net.minecraft.nbt.ListTag();final net.minecraft.world.phys.Vec3 previousPosition,previousMotion;final int selected;final Random random;final boolean shifted;
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration,int index){this.level=level;for(var entity:level.getAllEntities())baseline.add(entity.getId());this.registration=registration;world=World.mcc$wrap(level);player=actor(level,index);previousPosition=player.position();previousMotion=player.getDeltaMovement();selected=player.getInventory().selected;shifted=player.isShiftKeyDown();player.setShiftKeyDown(false);player.getInventory().save(inventory);player.getInventory().clearContent();origin=level.getSharedSpawnPos().offset(8,24,8);level.getChunk(origin);player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);player.setDeltaMovement(0,0,0);random=world.random;world.random=new ZeroRandom();}
        Class<?> type(String name)throws ClassNotFoundException{return Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());}
        LegacyB173Animal spawn(String name)throws ReflectiveOperationException {var original=(LivingEntity)type(name).getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"original spawn rejected: "+name);require(original.mcc$modern instanceof LegacyB173Animal,"generic living host required");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);require(level.getEntities((net.minecraft.world.entity.Entity)null,host.getBoundingBox().inflate(.1)).contains(host),"fixture original host must enter loaded native entity query sections");put(original,"random",new ZeroRandom());EntityViewsB173.flush();return host;}
        void block(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state){level.getChunk(pos);blocks.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,state,3);}
        void floor(){for(int x=-2;x<=11;x++)for(int z=-2;z<=2;z++){block(origin.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());for(int y=0;y<=3;y++)block(origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}}
        @Override public void close(){MenusB173.withActor(player,()->legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().setScreen(null));player.stopRiding();player.closeContainer();EntityViewsB173.flush();var descendants=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!baseline.contains(entity.getId()))descendants.add(entity);for(var entity:descendants)entity.discard();for(var entity:entities)entity.discard();player.getInventory().clearContent();player.getInventory().load(inventory);player.getInventory().selected=selected;player.setPos(previousPosition.x,previousPosition.y,previousPosition.z);player.setDeltaMovement(previousMotion);world.random=random;player.setShiftKeyDown(shifted);EntityViewsB173.flush();player.discard();blocks.forEach((pos,state)->level.setBlock(pos,state,3));}
    }
    public static void run(net.minecraft.server.level.ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(registration.isEmpty())return;
        var failures=new ArrayList<String>();
        for(boolean target:List.of(true,false))try(var c=new Context(level,registration.get(),target?0:1)){stay(c,target);LOG.info("B1.7.3 CREEPS NAVIGATION TEST: actual flower stay with {} passed",target?"retained target":"explicit path");}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add((target?"retained target":"explicit path")+": "+error);LOG.error("B1.7.3 CREEPS NAVIGATION TEST failed",error);}
        require(failures.isEmpty(),"B1.7.3 CREEPS NAVIGATION TEST: "+failures);LOG.info("B1.7.3 CREEPS NAVIGATION TEST: all 2 original command/navigation proofs passed");
    }
    private static void flower(Context c,LegacyB173Animal pig){EntityViewsB173.flush();c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DANDELION,2));require(pig.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction(),"actual original flower callback handles command");}
    private static void stay(Context c,boolean target)throws ReflectiveOperationException {
        c.floor();c.player.setPos(c.origin.getX()+8.5,c.origin.getY(),c.origin.getZ()+.5);var pig=c.spawn("CREEPSEntityGuineaPig");put(pig.legacy(),"tamed",true);put(pig.legacy(),"wanderstate",0);put(pig.legacy(),"speedboost",1000);pig.setOnGround(true);pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        var creature=(legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity)pig.legacy();
        try(var scope=EntityViewsB173.callbackScope()){var view=EntityViewsB173.wrap(c.player);if(target)creature.target=view;var path=c.world.findPath(creature,view,16F);require(path!=null&&path.mcc$modern()!=null,"controlled grounded original-to-native path exists");creature.setPath(path);}
        require(!pig.getNavigation().isDone(),"fixture starts with active path before stay command");
        MenusB173.withActor(c.player,pig::tick);require(pig.getSpeed()>0&&!pig.getNavigation().isDone(),"fixture prewarms native positive-speed path and cached timeout before pause");
        var retainedPath=pig.getNavigation().getPath();
        flower(c,pig);require((Integer)get(pig.legacy(),"wanderstate")==1&&pig.legacy().mcc$movementSpeed()==0,"actual flower stay sets exact original zero movement speed");
        EntityViewsB173.flush();pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);var start=pig.position();double maxSpeed=0;
        var clock=(net.minecraft.world.level.storage.ServerLevelData)c.level.getLevelData();long savedTime=c.level.getGameTime();
        try{for(int tick=0;tick<240;tick++){clock.setGameTime(savedTime+tick+1);MenusB173.withActor(c.player,pig::tick);maxSpeed=Math.max(maxSpeed,pig.getSpeed());}
        require(pig.getNavigation().getPath()==retainedPath&&!pig.getNavigation().isDone(),"240 advancing world ticks preserve exact paused native route");
        double drift=pig.position().subtract(start).horizontalDistance();
        require(drift<.00001&&maxSpeed==0,"original zero-speed stay must prevent native path movement: target="+target+" drift="+drift+" maxNativeSpeed="+maxSpeed+" originalSpeed="+pig.legacy().mcc$movementSpeed()+" navDone="+pig.getNavigation().isDone());
        // The next original command enters FOLLOW, restoring positive baseSpeed plus its active boost.
        flower(c,pig);require((Integer)get(pig.legacy(),"wanderstate")==2&&pig.legacy().mcc$movementSpeed()>0,"next actual flower command restores original follow speed");
        // Do not assign a new path: the no-target branch must resume the same route after the long pause.
        start=pig.position();for(int tick=0;tick<20;tick++){clock.setGameTime(savedTime+241+tick);MenusB173.withActor(c.player,pig::tick);}
        require(pig.position().subtract(start).horizontalDistance()>.01,"native movement resumes after original positive-speed follow command");require(c.player.getMainHandItem().getCount()==2,"both original flower commands retain held flowers");
        }finally{clock.setGameTime(savedTime);}
    }
}
