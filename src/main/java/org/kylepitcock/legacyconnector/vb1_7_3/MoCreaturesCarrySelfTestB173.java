package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoCreaturesCarrySelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CarryTest");
    private MoCreaturesCarrySelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC CARRY TEST failed",error);}});}
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
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("carried bunny native ray pick and release",MoCreaturesCarrySelfTestB173::bunnyTargeting),new Case("bird carried offset",c->carried(c,"EntityBird",1.15F)),new Case("mouse carried offset",c->carried(c,"EntityMouse",1.7F)),new Case("kitty bed carried offset and pickup flag",c->carried(c,"EntityKittyBed",1.15F)),new Case("litter carried offset and pickup flag",c->carried(c,"EntityLitterBox",1.15F)));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC CARRY TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC CARRY TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC CARRY TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC CARRY TEST: all {} original-callback proofs passed",cases.size());}
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:moc-carry".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Carry");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private static void bunnyTargeting(Context c)throws ReflectiveOperationException {
        var bunny=c.spawn("EntityBunny",.5);
        var player=actor(c.level);c.entities.add(player);
        player.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);c.level.addNewPlayer(player);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
        require(bunny.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction()&&bunny.getVehicle()==player,
                "original bunny interaction mounts the native player");
        EntityViewsB173.flush();player.positionRider(bunny);
        var start=player.getEyePosition();
        var end=start.add(bunny.getBoundingBox().getCenter().subtract(start).normalize().scale(6));
        require(bunny.getRootVehicle()==player.getRootVehicle(),"carried bunny shares native picker root vehicle");
        require(!bunny.getBoundingBox().inflate(bunny.getPickRadius()).contains(start),
                "ray starts outside bunny pick bounds so native origin-inside bypass cannot mask rider exclusion");
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,start,end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity->entity==bunny&&!entity.isSpectator()&&entity.isPickable(),36);
        require(hit!=null&&hit.getEntity()==bunny,"native GameRenderer ray-pick helper selects actual carried bunny");
        require(bunny.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),
                "second actual server interaction executes original bunny release callback");
        require(bunny.getVehicle()==null&&!player.hasPassenger(bunny)&&bunny.legacy().vehicle==null
                        &&(Boolean)get(bunny.legacy(),"h")&&bunny.getDeltaMovement().y>0,
                "original same-carrier toggle releases reciprocal native relations and retains throw state/impulse");
    }
    private static void carried(Context c,String name,float reduction)throws ReflectiveOperationException {
        var host=c.spawn(name,.5);if(name.equals("EntityBird"))put(host.legacy(),"tamed",true);
        var player=actor(c.level);c.entities.add(player);player.setPos(c.origin.getX()+3.5,c.origin.getY(),c.origin.getZ()+.5);c.level.addNewPlayer(player);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
        require(host.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction()&&host.getVehicle()==player,"original interaction carries actual "+name);
        boolean furniture=name.equals("EntityKittyBed")||name.equals("EntityLitterBox");if(furniture)put(host.legacy(),"pickedUp",false);
        EntityViewsB173.flush();
        var carrier=PlayerViewB173.of(player);
        // This is the exact Beta Entity.updatePassengerPosition arithmetic, without calling the
        // original passenger getter first (that would mask its missing pickedUp side effect).
        double expected=carrier.y+carrier.getPassengerRidingHeight()+(double)(host.legacy().standingEyeHeight-reduction)-host.legacy().standingEyeHeight;
        EntityViewsB173.flush();player.positionRider(host);
        require(Math.abs(host.getY()-expected)<.00001&&Math.abs(host.getX()-player.getX())<.00001&&Math.abs(host.getZ()-player.getZ())<.00001,"native carrier placement uses original carried offset for "+name+": actual="+host.position()+" expectedY="+expected);
        if(furniture)require((Boolean)get(host.legacy(),"pickedUp"),"native carrier placement executes original furniture getter side effect: "+name);
        player.setPos(player.getX()+2,player.getY()+1,player.getZ()+3);player.positionRider(host);
        require(Math.abs(host.getY()-(expected+1))<.00001&&Math.abs(host.getX()-player.getX())<.00001&&Math.abs(host.getZ()-player.getZ())<.00001,"actual carried placement tracks moving native player: "+name);
        host.stopRiding();require(host.getVehicle()==null,"carried actor cleanly releases native player: "+name);
    }
}
