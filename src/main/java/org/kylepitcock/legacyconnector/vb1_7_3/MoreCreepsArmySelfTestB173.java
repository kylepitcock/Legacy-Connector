package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoreCreepsArmySelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsArmyTest");
    private MoreCreepsArmySelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS ARMY TEST failed",error);}});}
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
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("original allegiance and visibility",MoreCreepsArmySelfTestB173::allegiance),new Case("normal acquisition ranged cooldown and navigation",MoreCreepsArmySelfTestB173::combat),new Case("attack flag preserves route and suppresses repath",MoreCreepsArmySelfTestB173::attackRoute),new Case("low speed actual arrow fixed damage and attribution",MoreCreepsArmySelfTestB173::arrowImpact),new Case("actual army arrow air and water flight",MoreCreepsArmySelfTestB173::arrowFlight),new Case("army arrow registry NBT and spawn packet",MoreCreepsArmySelfTestB173::arrowRegistry),new Case("original allied attack fall-through",MoreCreepsArmySelfTestB173::friendlyAttack),new Case("timer texture boundaries and expiration",MoreCreepsArmySelfTestB173::lifetime),new Case("original NBT omits army timer",MoreCreepsArmySelfTestB173::nbt));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 CREEPS ARMY TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 CREEPS ARMY TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 CREEPS ARMY TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 CREEPS ARMY TEST: all {} original-callback proofs passed",cases.size());}
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:creeps-army".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Follow");
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
    private static void floor(Context c){for(int x=-3;x<=21;x++)for(int z=-5;z<=5;z++){c.block(c.origin.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());for(int y=0;y<=3;y++)c.block(c.origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}}
    private static LegacyB173Animal soldier(Context c)throws ReflectiveOperationException {floor(c);var host=c.spawn("CREEPSEntityHunchbackSkeleton",.5);host.setOnGround(true);host.setDeltaMovement(0,-.08,0);host.syncView();put(host.legacy(),"timeleft",650);return host;}
    private static net.minecraft.world.entity.animal.Cow cow(Context c,double x){var cow=net.minecraft.world.entity.EntityType.COW.create(c.level);require(cow!=null,"native cow constructed");cow.setNoAi(true);cow.setPos(c.origin.getX()+x,c.origin.getY(),c.origin.getZ()+.5);c.entities.add(cow);require(c.level.addFreshEntity(cow),"native target enters loaded sections");return cow;}
    private static Object closest(LegacyB173Animal soldier,double range)throws ReflectiveOperationException {try(var scope=EntityViewsB173.callbackScope()){soldier.syncView();return soldier.legacy().getClass().getMethod("getClosestTarget",legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,double.class).invoke(soldier.legacy(),soldier.legacy(),range);}}
    private static long arrows(Context c){return c.level.getEntities((net.minecraft.world.entity.Entity)null,c.area()).stream().filter(e->e instanceof net.minecraft.world.entity.projectile.AbstractArrow).count();}
    private static void allegiance(Context c)throws ReflectiveOperationException {
        var army=soldier(c);var player=roper(c,1.5);
        for(String name:List.of("CREEPSEntityGuineaPig","CREEPSEntitySnowDevil","CREEPSEntityHunchback","CREEPSEntityHunchbackSkeleton"))c.spawn(name,2.5);
        var target=cow(c,8.5);var far=cow(c,18.5);
        require(closest(army,16)==EntityViewsB173.wrap(target),"original target scan excludes player and four allied species and selects native living enemy");
        for(int y=0;y<=3;y++)for(int z=-5;z<=5;z++)c.block(c.origin.offset(5,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        require(closest(army,16)==null,"original reversed candidate visibility test rejects enemy behind solid wall");
        for(int y=0;y<=3;y++)for(int z=-5;z<=5;z++)c.block(c.origin.offset(5,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        target.discard();require(closest(army,16)==null&&closest(army,-1)==EntityViewsB173.wrap(far),"original strict bounded scan and negative-range unlimited scan preserved");
        var creature=(MobEntity)army.legacy();creature.target=PlayerViewB173.of(player);
        require(creature.mcc$targetInRange()==null,"original acquisition checks existing player target rather than candidate");creature.target=null;
    }
    private static void combat(Context c)throws ReflectiveOperationException {
        var army=soldier(c);var target=cow(c,12.5);army.serverAIPhase();
        require(((MobEntity)army.legacy()).target==EntityViewsB173.wrap(target)&&!army.getNavigation().isDone(),"normal AI phase runs original acquisition and installs native navigation beyond firing range");
        require(arrows(c)==0,"original soldier does not shoot beyond ten blocks");
        target.setPos(c.origin.getX()+5.5,c.origin.getY(),c.origin.getZ()+.5);EntityViewsB173.flush();army.legacy().attackCooldown=0;float health=target.getHealth();army.serverAIPhase();
        require(arrows(c)==1&&army.legacy().attackCooldown==30&&target.getHealth()==health,"normal original attack emits one native arrow, sets thirty tick cooldown and does no immediate melee damage");
        var arrow=(net.minecraft.world.entity.projectile.AbstractArrow)c.level.getEntities((net.minecraft.world.entity.Entity)null,c.area()).stream().filter(e->e instanceof net.minecraft.world.entity.projectile.AbstractArrow).findFirst().orElseThrow();
        require(arrow.getOwner()==army&&arrow.getDeltaMovement().length()>.4&&arrow.getDeltaMovement().x>0,"original arrow owns native army shooter and receives aimed velocity");
        army.serverAIPhase();require(arrows(c)==1&&army.legacy().attackCooldown==30,"successive AI calls respect original ranged cooldown");
        target.setPos(c.origin.getX()+1.5,c.origin.getY(),c.origin.getZ()+.5);EntityViewsB173.flush();army.legacy().attackCooldown=0;army.serverAIPhase();
        require(arrows(c)==2&&target.getHealth()==health,"point blank original soldier still fires arrow instead of inherited melee");
        require(army.getMainHandItem().is(net.minecraft.world.item.Items.BOW),"native held equipment publishes original army bow");
    }
    private static void attackRoute(Context c)throws ReflectiveOperationException {
        var army=soldier(c);var target=cow(c,12.5);army.serverAIPhase();var path=army.getNavigation().getPath();
        require(path!=null&&!path.isDone(),"army starts with active route outside firing range");int cursor=path.getNextNodeIndex();
        target.setPos(c.origin.getX()+5.5,c.origin.getY(),c.origin.getZ()+2.5);EntityViewsB173.flush();army.legacy().attackCooldown=0;army.serverAIPhase();
        require(arrows(c)==1&&(Boolean)get(army.legacy(),"movementBlocked"),"original arrow attack writes post-attack flag");
        require(army.getNavigation().getPath()==path&&!path.isDone()&&path.getNextNodeIndex()>=cursor,"original attack flag suppresses repath while existing native route remains active and advances normally");
        double expectedYaw=Math.atan2(target.getZ()-army.getZ(),target.getX()-army.getX())*180/Math.PI-90;
        require(Math.abs(net.minecraft.util.Mth.wrapDegrees(army.getYRot()-(float)expectedYaw))<.001F,"native route controls preserve actual original off-axis attack facing while old route stays active: yaw="+army.getYRot()+" expected="+expectedYaw);
        ((MobEntity)army.legacy()).setPath(null);army.legacy().attackCooldown=0;army.serverAIPhase();
        require(arrows(c)==2&&army.getNavigation().isDone(),"original in-range attack flag suppresses creating a fresh target path when no route exists");
    }
    private static void arrowImpact(Context c)throws ReflectiveOperationException {
        var army=soldier(c);cow(c,5.5);var victim=c.spawn("CREEPSEntityHunchback",8.5);put(victim.legacy(),"tamed",false);victim.legacy().health=40;victim.flushView();army.serverAIPhase();
        require(arrows(c)==0,"fresh in-range acquisition creates route without attacking until next original phase");army.serverAIPhase();
        var arrow=(net.minecraft.world.entity.projectile.AbstractArrow)c.level.getEntities((net.minecraft.world.entity.Entity)null,c.area()).stream().filter(e->e instanceof net.minecraft.world.entity.projectile.AbstractArrow).findFirst().orElseThrow();
        require(arrow.getOwner()==army,"actual army callback creates arrow with canonical shooter");arrow.setDeltaMovement(.05,0,0);float before=victim.getHealth();
        hitArrow(arrow,victim);EntityViewsB173.flush();
        var damage=victim.getLastDamageSource();LOG.info("B1.7.3 CREEPS ARMY TEST impact: before={} after={} source={} direct={} causing={} originalTarget={} nativeLastHurt={}",before,victim.getHealth(),damage,damage==null?null:damage.getDirectEntity(),damage==null?null:damage.getEntity(),((MobEntity)victim.legacy()).target,victim.getLastHurtByMob());
        require(before-victim.getHealth()==4F,"actual army arrow preserves fixed original four damage even at low speed: before="+before+" after="+victim.getHealth());
        require(((MobEntity)victim.legacy()).target==army.legacy(),"actual wild hunchback retaliates against canonical original arrow owner rather than projectile");
        var nativeVictim=cow(c,14.5);float nativeBefore=nativeVictim.getHealth();var control=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,c.level);control.setPos(nativeVictim.position());control.setDeltaMovement(.05,0,0);c.entities.add(control);require(c.level.addFreshEntity(control),"native arrow control enters level");hitArrow(control,nativeVictim);
        require(nativeBefore-nativeVictim.getHealth()==1F,"ordinary native low-speed arrow retains native velocity-scaled one damage");
        army.legacy().attackCooldown=0;army.serverAIPhase();var rejected=(net.minecraft.world.entity.projectile.AbstractArrow)c.level.getEntities((net.minecraft.world.entity.Entity)null,c.area()).stream().filter(e->e instanceof net.minecraft.world.entity.projectile.AbstractArrow&&e!=control&&!e.isRemoved()).findFirst().orElseThrow();
        var invulnerable=cow(c,17.5);invulnerable.setInvulnerable(true);float rejectedBefore=invulnerable.getHealth();rejected.setDeltaMovement(.05,0,0);hitArrow(rejected,invulnerable);
        require(invulnerable.getHealth()==rejectedBefore&&!rejected.isRemoved()&&rejected.getDeltaMovement().x<0,"rejected actual Beta arrow impact preserves victim health and bounces live projectile");
    }
    private static net.minecraft.world.entity.projectile.AbstractArrow freshArmyArrow(Context c,LegacyB173Animal army){
        Set<net.minecraft.world.entity.Entity> before=new HashSet<>(c.level.getEntities((net.minecraft.world.entity.Entity)null,c.area()));army.legacy().attackCooldown=0;army.serverAIPhase();
        return (net.minecraft.world.entity.projectile.AbstractArrow)c.level.getEntities((net.minecraft.world.entity.Entity)null,c.area()).stream().filter(e->e instanceof net.minecraft.world.entity.projectile.AbstractArrow&&!before.contains(e)).findFirst().orElseThrow();
    }
    private static void arrowFlight(Context c)throws ReflectiveOperationException {
        var army=soldier(c);cow(c,5.5);army.serverAIPhase();var air=freshArmyArrow(c,army);
        var start=new net.minecraft.world.phys.Vec3(c.origin.getX()+.5,c.origin.getY()+8,c.origin.getZ()+.5);
        for(int x=-1;x<=3;x++)for(int y=7;y<=10;y++)for(int z=-1;z<=1;z++)c.block(c.origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        air.setPos(start);air.setDeltaMovement(.6,0,0);air.tick();
        require(Math.abs(air.getX()-start.x-.6)<.00001&&Math.abs(air.getY()-start.y)<.00001&&Math.abs(air.getDeltaMovement().x-.594)<.00001&&Math.abs(air.getDeltaMovement().y+.03)<.00001,"actual Army arrow native air tick follows original displacement, .99 drag and .03 gravity: position="+air.position()+" velocity="+air.getDeltaMovement());air.discard();
        var water=freshArmyArrow(c,army);for(int x=-1;x<=3;x++)for(int y=7;y<=10;y++)for(int z=-1;z<=1;z++)c.block(c.origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        water.setPos(start);water.setDeltaMovement(.6,0,0);water.tick();
        require(water.isInWater()&&Math.abs(water.getX()-start.x-.6)<.00001&&Math.abs(water.getDeltaMovement().x-.48)<.00001&&Math.abs(water.getDeltaMovement().y+.03)<.00001,"actual Army arrow native water tick follows original .8 drag and .03 gravity: position="+water.position()+" velocity="+water.getDeltaMovement());
        // This is server physics proof; dedicated client factory/prediction is not exercised here.
    }
    private static void arrowRegistry(Context c)throws ReflectiveOperationException {
        var army=soldier(c);cow(c,5.5);army.serverAIPhase();var arrow=freshArmyArrow(c,army);
        require(arrow.getType()!=net.minecraft.world.entity.EntityType.ARROW&&arrow instanceof LegacyB173VanillaArrow,"actual Army arrow uses dedicated Beta registry type");
        var created=arrow.getType().create(c.level);require(created instanceof LegacyB173VanillaArrow,"dedicated type factory preserves Beta projectile class");c.entities.add(created);
        var tag=new net.minecraft.nbt.CompoundTag();require(arrow.save(tag),"actual Army arrow saves registry NBT");var loaded=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,c.level,e->e);require(loaded instanceof LegacyB173VanillaArrow&&loaded.getType()==arrow.getType(),"saved Army arrow reloads same Beta class and registry type");c.entities.add(loaded);
        require(((net.minecraft.world.entity.projectile.AbstractArrow)loaded).getOwner()==army,"saved Army arrow resolves canonical native shooter UUID after reload");
        var spawn=new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(arrow.getId(),arrow.getUUID(),arrow.getX(),arrow.getY(),arrow.getZ(),arrow.getXRot(),arrow.getYRot(),arrow.getType(),army.getId(),arrow.getDeltaMovement(),0);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),c.level.registryAccess());
        try{net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer,spawn);var decoded=net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.decode(buffer);require(decoded.getType()==arrow.getType(),"native spawn codec preserves dedicated Beta type");created.recreateFromPacket(decoded);require(created instanceof LegacyB173VanillaArrow&&created.getUUID().equals(arrow.getUUID())&&((net.minecraft.world.entity.projectile.AbstractArrow)created).getOwner()==army,"native decoded spawn reconstructs Beta projectile and shooter relation");}finally{buffer.release();}
        var nativeArrow=net.minecraft.world.entity.EntityType.ARROW.create(c.level);require(nativeArrow!=null&&!(nativeArrow instanceof LegacyB173VanillaArrow)&&nativeArrow.getType()==net.minecraft.world.entity.EntityType.ARROW,"ordinary native Arrow registry factory retains its own projectile class");c.entities.add(nativeArrow);
        // Server-level codec/factory proof; no live client or renderer is exercised here.
    }
    private static void hitArrow(net.minecraft.world.entity.projectile.AbstractArrow arrow,net.minecraft.world.entity.Entity victim)throws ReflectiveOperationException {
        java.lang.reflect.Method hit=null;for(Class<?> type=arrow.getClass();type!=null&&hit==null;type=type.getSuperclass())try{hit=type.getDeclaredMethod("onHitEntity",net.minecraft.world.phys.EntityHitResult.class);}catch(NoSuchMethodException ignored){}
        require(hit!=null,"native arrow impact callback available");hit.setAccessible(true);hit.invoke(arrow,new net.minecraft.world.phys.EntityHitResult(victim));
    }
    private static void friendlyAttack(Context c)throws ReflectiveOperationException {
        var army=soldier(c);var ally=c.spawn("CREEPSEntityHunchbackSkeleton",4.5);var creature=(MobEntity)army.legacy();creature.target=ally.legacy();army.legacy().attackCooldown=0;
        // The original clears allegiance targets but deliberately has no return before shooting.
        try(var scope=EntityViewsB173.callbackScope()){creature.mcc$attack(ally.legacy(),4);army.flushView();}
        require(creature.target==null&&arrows(c)==1&&army.legacy().attackCooldown==30,"original allied target clearing retains its ranged fall-through quirk");
    }
    private static void lifetime(Context c)throws ReflectiveOperationException {
        var army=soldier(c);c.decision(army,-1);
        for(int[] boundary:new int[][]{{499,2},{300,2},{299,3},{200,3},{199,4},{100,4},{99,5},{1,5}}){put(army.legacy(),"timeleft",boundary[0]);army.aiStep();require((Integer)get(army.legacy(),"timeleft")==boundary[0]&&army.texturePath().endsWith("hunchbackskeleton"+boundary[1]+".png"),"original strict texture interval and boundary retention at "+boundary[0]+": "+army.texturePath());}
        put(army.legacy(),"timeleft",1);c.decision(army,2);army.aiStep();
        require((Integer)get(army.legacy(),"timeleft")==0&&army.legacy().health==0&&army.isRemoved(),"original random lifetime decrement expires army at zero and removes native host");
    }
    private static void nbt(Context c)throws ReflectiveOperationException {
        var army=soldier(c);put(army.legacy(),"timeleft",17);put(army.legacy(),"weapon",7);var tag=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();army.legacy().writeNbt(tag);
        var restored=c.spawn("CREEPSEntityHunchbackSkeleton",5.5);put(restored.legacy(),"timeleft",612);put(restored.legacy(),"weapon",9);restored.legacy().readNbt(tag);
        require((Integer)get(restored.legacy(),"timeleft")==612&&(Integer)get(restored.legacy(),"weapon")==9,"original NBT delegates to superclass and intentionally does not persist timer or weapon");
    }
}
