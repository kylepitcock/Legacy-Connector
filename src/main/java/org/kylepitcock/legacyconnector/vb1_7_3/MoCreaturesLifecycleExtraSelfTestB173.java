package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoCreaturesLifecycleExtraSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173LifecycleExtraTest");
    private MoCreaturesLifecycleExtraSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC LIFECYCLE EXTRA TEST failed",error);}});}
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
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("thrown bunny landing attracts native monsters",MoCreaturesLifecycleExtraSelfTestB173::bunnyLanding),new Case("original juvenile maturity boundaries",MoCreaturesLifecycleExtraSelfTestB173::maturity),new Case("mouse climbs plain native wall",MoCreaturesLifecycleExtraSelfTestB173::mouseClimb));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC LIFECYCLE EXTRA TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC LIFECYCLE EXTRA TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC LIFECYCLE EXTRA TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC LIFECYCLE EXTRA TEST: all {} original-callback proofs passed",cases.size());}
    private static void bunnyLanding(Context c)throws ReflectiveOperationException {
        var bunny=c.spawn("EntityBunny",.5);var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(c.level);var far=net.minecraft.world.entity.EntityType.ZOMBIE.create(c.level);
        require(zombie!=null&&far!=null,"native monster fixtures constructed");zombie.setPos(c.origin.getX()+4,c.origin.getY(),c.origin.getZ()+.5);far.setPos(c.origin.getX()+20,c.origin.getY(),c.origin.getZ()+.5);zombie.setNoAi(true);far.setNoAi(true);c.entities.add(zombie);c.entities.add(far);require(c.level.addFreshEntity(zombie)&&c.level.addFreshEntity(far),"native monsters enter loaded sections");
        put(bunny.legacy(),"h",true);EntityViewsB173.flush();bunny.setOnGround(false);bunny.serverAIPhase();
        require((Boolean)get(bunny.legacy(),"h")&&zombie.getTarget()==null,"original airborne thrown bunny retains pending landing and does not attract monsters");
        bunny.setOnGround(true);bunny.serverAIPhase();
        require(!(Boolean)get(bunny.legacy(),"h")&&zombie.getTarget()==bunny&&far.getTarget()==null,"original bunny landing clears thrown flag and retargets only nearby native monsters");
    }
    private static void maturity(Context c)throws ReflectiveOperationException {
        var cat=c.spawn("EntityBigCat",.5);put(cat.legacy(),"adult",false);put(cat.legacy(),"edad",.995F);put(cat.legacy(),"killedByOtherEntity",true);put(cat.legacy(),"hungry",false);put(cat.legacy(),"sitting",true);c.decision(cat,250);cat.aiStep();
        require((Boolean)get(cat.legacy(),"adult")&&(Float)get(cat.legacy(),"edad")>=1F&&!(Boolean)get(cat.legacy(),"killedByOtherEntity"),"original bigcat maturity threshold advances growth and clears juvenile death policy");
        require(cat.renderState().equals(AnimalRenderStateB173.capture(cat.legacy())),"original bigcat maturity publishes initial adult render state");
        var fish=c.spawn("EntityFishy",5);put(fish.legacy(),"adult",false);put(fish.legacy(),"b",.99F);put(fish.legacy(),"killedByOtherEntity",true);c.decision(fish,100);fish.aiStep();
        require((Boolean)get(fish.legacy(),"adult")&&(Float)get(fish.legacy(),"b")>=1F&&(Boolean)get(fish.legacy(),"killedByOtherEntity"),"original fish maturity retains its distinct death policy");
    }
    private static void mouseClimb(Context c)throws ReflectiveOperationException {
        var mouse=c.spawn("EntityMouse",.5);
        for(int y=-2;y<=3;y++)for(int z=-1;z<=1;z++)c.block(c.origin.offset(1,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        EntityViewsB173.flush();mouse.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(1,0,0));mouse.setOnGround(false);mouse.syncView();
        require(mouse.horizontalCollision&&mouse.onClimbable()&&(Boolean)mouse.legacy().getClass().getMethod("climbing").invoke(mouse.legacy()),"actual mouse treats plain solid-wall collision as climbing without ladder blocks");
        mouse.setDeltaMovement(.2,-.4,0);mouse.travel(net.minecraft.world.phys.Vec3.ZERO);
        require(mouse.getDeltaMovement().y>0,"original mouse ladder override enters native wall-climb travel response: "+mouse.getDeltaMovement());
    }
}
