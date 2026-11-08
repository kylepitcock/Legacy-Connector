package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Original Kitty decisions, with controlled random branches and timers rather than replacement gameplay. */
public final class MoCreaturesKittySelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173KittyTest");
    private MoCreaturesKittySelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC KITTY TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var result=type.getDeclaredField(name);result.setAccessible(true);return result;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    private static int state(LegacyB173Animal host)throws ReflectiveOperationException{return (Integer)get(host.legacy(),"kittystate");}
    /** Zero only the requested decision bound; suppress unrelated growth, mood and hunger transitions. */
    private static final class DecisionRandom extends Random {private final int bound;DecisionRandom(int bound){this.bound=bound;}@Override public int nextInt(int limit){return limit==bound?0:limit-1;}@Override public float nextFloat(){return .5F;}}
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final ContentB173.Registration registration;final net.minecraft.core.BlockPos origin;
        final List<net.minecraft.world.entity.Entity> entities=new ArrayList<>();final Set<net.minecraft.world.entity.Entity> before;
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration){this.level=level;this.registration=registration;world=World.mcc$wrap(level);origin=level.getSharedSpawnPos().offset(8,24,8);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk(origin.offset(x*16,0,z*16));before=new HashSet<>(level.getEntities((net.minecraft.world.entity.Entity)null,area()));}
        net.minecraft.world.phys.AABB area(){return new net.minecraft.world.phys.AABB(origin).inflate(40);}
        LegacyB173Animal spawn(String name,double x)throws ReflectiveOperationException {var type=Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());var original=(LivingEntity)type.getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+x,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"original Kitty fixture spawn admitted");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);put(original,"random",new DecisionRandom(-1));require(level.getEntities((net.minecraft.world.entity.Entity)null,area()).contains(host),"Kitty fixture enters loaded native query sections");return host;}
        LegacyB173Animal kitty(int state,double x)throws ReflectiveOperationException {var host=spawn("EntityKitty",x);put(host.legacy(),"kittystate",state);put(host.legacy(),"hungry",false);return host;}
        void decision(LegacyB173Animal host,int bound)throws ReflectiveOperationException {put(host.legacy(),"random",new DecisionRandom(bound));}
        @Override public void close(){for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,area()))if(!before.contains(entity))entity.discard();for(var entity:entities)entity.discard();EntityViewsB173.flush();}
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("litter selection and completion",MoCreaturesKittySelfTestB173::litter),new Case("mate courtship and bed decisions",MoCreaturesKittySelfTestB173::mating),new Case("kitten family protection and maturity",MoCreaturesKittySelfTestB173::family));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC KITTY TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC KITTY TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC KITTY TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC KITTY TEST: all {} original-callback proofs passed",cases.size());}
    private static void litter(Context c)throws ReflectiveOperationException {
        var kitty=c.kitty(5,.5);var dirty=c.spawn("EntityLitterBox",.8);put(dirty.legacy(),"usedlitter",true);var clean=c.spawn("EntityLitterBox",1.2);c.decision(kitty,20);kitty.tick();
        require(state(kitty)==6&&kitty.getVehicle()==clean&&clean.getFirstPassenger()==kitty,"original kitty rejects nearer used litter and occupies clean litter");
        put(kitty.legacy(),"kittytimer",300);kitty.tick();
        require(state(kitty)==7&&kitty.getVehicle()==null&&clean.getFirstPassenger()==null,"original completed litter state releases native mount");
        require((Boolean)get(clean.legacy(),"usedlitter")&&(Integer)get(clean.legacy(),"littertime")==0,"original litter completion marks furniture used and resets its timer");
        put(clean.legacy(),"littertime",5000);clean.tick();require(!(Boolean)get(clean.legacy(),"usedlitter"),"original litter automatically becomes clean after5000 ticks");
    }
    private static void mating(Context c)throws ReflectiveOperationException {
        var mother=c.kitty(9,.5);var mate=c.kitty(9,1.2);c.decision(mother,50);mother.tick();
        require(state(mother)==18&&state(mate)==18&&((MobEntity)mother.legacy()).target==mate.legacy()&&((MobEntity)mate.legacy()).target==mother.legacy(),"original ready pair enters courtship with reciprocal retained targets");
        put(mother.legacy(),"kittytimer",500);mother.tick();require(state(mother)==19&&state(mate)==7&&((MobEntity)mother.legacy()).target==null&&((MobEntity)mate.legacy()).target==null,"original close courtship makes initiating parent pregnant and resets mate");
        var occupied=c.spawn("EntityKittyBed",.9);var occupant=c.kitty(23,.9);var available=c.spawn("EntityKittyBed",1.6);
        // Finish the original placement transaction before a separate native mounting action. Production
        // placements finish their callback scope here; retaining spawned baselines across actions is artificial.
        EntityViewsB173.flush();require(occupant.startRiding(occupied,true),"controlled occupied nearest bed");c.decision(mother,20);mother.tick();
        // Original nearest-bed utility does not filter occupied beds: do not invent a fallback to the farther empty bed.
        require(state(mother)==19&&mother.getVehicle()==null,"original nearest occupied bed blocks pregnancy bed choice: state="+state(mother)+" motherVehicle="+mother.getVehicle()+" nativePassenger="+occupied.getFirstPassenger()+" legacyPassenger="+occupied.legacy().passenger+" distance="+mother.distanceTo(occupied));
        occupant.stopRiding();mother.tick();require(state(mother)==20&&mother.getVehicle()==occupied&&available.getFirstPassenger()==null,"original pregnancy accepts nearest empty bed even without food");
        mother.stopRiding();mother.tick();require(state(mother)==19,"original lost pregnancy bed returns to searching state");
    }
    private static void family(Context c)throws ReflectiveOperationException {
        var mother=c.kitty(21,14);var kitten=c.kitty(10,.5);put(kitten.legacy(),"adult",false);c.decision(kitten,50);kitten.tick();
        require(((MobEntity)kitten.legacy()).target==mother.legacy(),"original distant kitten retains nearby mother as follow target");
        var attacker=c.spawn("EntityBear",2);require(kitten.hurt(c.level.damageSources().mobAttack(attacker),1),"native damage reaches original kitten callback");require(((MobEntity)mother.legacy()).target==attacker.legacy(),"original mother defends damaged kitten against native attacker");
        put(mother.legacy(),"kittytimer",2000);mother.tick();require(state(mother)==21&&(Integer)get(mother.legacy(),"kittytimer")==1000,"original mother retains family state while juvenile exists");
        put(kitten.legacy(),"adult",true);kitten.tick();require(state(kitten)==7&&((MobEntity)kitten.legacy()).target==null,"original mature kitten leaves juvenile state and clears follow target");
        put(mother.legacy(),"kittytimer",2000);mother.tick();require(state(mother)==7&&((MobEntity)mother.legacy()).target==null,"original mother releases family state after last juvenile matures");
    }
}
