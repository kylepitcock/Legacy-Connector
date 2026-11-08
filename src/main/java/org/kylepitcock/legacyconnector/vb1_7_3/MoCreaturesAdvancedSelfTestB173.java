package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoCreaturesAdvancedSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173AdvancedTest");
    private MoCreaturesAdvancedSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC ADVANCED TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var result=type.getDeclaredField(name);result.setAccessible(true);return result;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    /** Zero only the requested decision bound; suppress unrelated growth, mood and hunger transitions. */
    private static final class DecisionRandom extends Random {private final int bound;DecisionRandom(int bound){this.bound=bound;}@Override public int nextInt(int limit){return limit==bound?0:limit-1;}@Override public float nextFloat(){return .5F;}}
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final ContentB173.Registration registration;final net.minecraft.core.BlockPos origin;
        final List<net.minecraft.world.entity.Entity> entities=new ArrayList<>();final Set<net.minecraft.world.entity.Entity> before;
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration){this.level=level;this.registration=registration;world=World.mcc$wrap(level);origin=level.getSharedSpawnPos().offset(8,24,8);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk(origin.offset(x*16,0,z*16));before=new HashSet<>(level.getEntities((net.minecraft.world.entity.Entity)null,area()));}
        net.minecraft.world.phys.AABB area(){return new net.minecraft.world.phys.AABB(origin).inflate(40);}
        LegacyB173Animal spawn(String name,double x)throws ReflectiveOperationException {var type=Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());var original=(LivingEntity)type.getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+x,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"original advanced fixture spawn admitted");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);put(original,"random",new DecisionRandom(-1));require(level.getEntities((net.minecraft.world.entity.Entity)null,area()).contains(host),"advanced fixture enters loaded native query sections");return host;}
        void decision(LegacyB173Animal host,int bound)throws ReflectiveOperationException {put(host.legacy(),"random",new DecisionRandom(bound));}
        @Override public void close(){for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,area()))if(!before.contains(entity))entity.discard();for(var entity:entities)entity.discard();EntityViewsB173.flush();}
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("werewolf transformation lifecycle",MoCreaturesAdvancedSelfTestB173::werewolf),new Case("rare horse subtype and landing rules",MoCreaturesAdvancedSelfTestB173::horses),new Case("winged horse animation and descent",MoCreaturesAdvancedSelfTestB173::wings),new Case("native cow and wolf predator exclusions",MoCreaturesAdvancedSelfTestB173::nativePrey));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC ADVANCED TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC ADVANCED TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC ADVANCED TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC ADVANCED TEST: all {} original-callback proofs passed",cases.size());}
    private static void werewolf(Context c)throws ReflectiveOperationException {
        var wolf=c.spawn("EntityWerewolf",.5);var target=c.spawn("EntityBear",8);
        // IsNight uses the original World's daylight predicate; do not infer it from raw time.
        boolean night=(Boolean)wolf.legacy().getClass().getMethod("IsNight").invoke(wolf.legacy());
        put(wolf.legacy(),"humanform",night);put(wolf.legacy(),"transforming",false);put(wolf.legacy(),"tcounter",30);
        put(wolf.legacy(),"random",new Random(){@Override public int nextInt(int n){return n==250||n==3?0:n-1;}@Override public float nextFloat(){return .5F;}});
        wolf.aiStep();
        require((Boolean)get(wolf.legacy(),"humanform")!=night&&!(Boolean)get(wolf.legacy(),"transforming")&&(Integer)get(wolf.legacy(),"tcounter")==0,"original mismatched daylight form starts and completes transformation after counter30");
        require(wolf.getHealth()==(night?40:15),"original transformation assigns native form health: "+wolf.getHealth());
        put(wolf.legacy(),"humanform",true);((MobEntity)wolf.legacy()).target=target.legacy();c.decision(wolf,-1);wolf.aiStep();
        require(((MobEntity)wolf.legacy()).target==null,"original human form clears hostile retained target");
        put(wolf.legacy(),"humanform",false);var tag=new net.minecraft.nbt.CompoundTag();require(wolf.save(tag),"werewolf saves native registry form");
        var loaded=(LegacyB173Animal)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,c.level,e->e);require(loaded!=null,"werewolf reloads through native entity registry");c.entities.add(loaded);
        require(!(Boolean)get(loaded.legacy(),"humanform"),"original HumanForm NBT survives native save/load");
        // Transform explicitly refuses to resurrect a dying actor.
        put(wolf.legacy(),"deathTime",1);var transform=wolf.legacy().getClass().getDeclaredMethod("Transform");transform.setAccessible(true);transform.invoke(wolf.legacy());
        require(!(Boolean)get(wolf.legacy(),"humanform"),"original death guard prevents transforming dying werewolf");
    }
    private static void horses(Context c)throws ReflectiveOperationException {
        int[] types={1,3,5,7,8};int[] health={25,35,40,50,50};double[] speeds={.9,1.1,1.2,1.3,1.3};int[] damage={9,3,0,3,0};
        for(int i=0;i<types.length;i++){
            var horse=c.spawn("EntityHorse",i*5+.5);horse.legacy().getClass().getMethod("setType",int.class).invoke(horse.legacy(),types[i]);
            // Flush original type mutation through a native callback before reading native attributes.
            horse.flushView();c.decision(horse,-1);horse.aiStep();
            require((Integer)get(horse.legacy(),"maxhealth")==health[i]&&Math.abs((Double)get(horse.legacy(),"HorseSpeed")-speeds[i])<.00001,"original rare subtype health/speed table "+types[i]);
            require(horse.getHealth()==health[i],"original subtype health reaches native host "+types[i]+": "+horse.getHealth());
            if(types[i]>=7)require((Boolean)get(horse.legacy(),"fireImmune"),"original nightmare and black pegasus retain fire immunity");
            float before=horse.getHealth();horse.causeFallDamage(12,1,c.level.damageSources().fall());
            require(horse.getHealth()==before-damage[i],"original landing distinguishes ordinary reduced and wing-immune damage subtype"+types[i]+": "+horse.getHealth());
        }
    }
    private static void wings(Context c)throws ReflectiveOperationException {
        for(int type:new int[]{5,8}){
            var horse=c.spawn("EntityHorse",type*2);horse.legacy().getClass().getMethod("setType",int.class).invoke(horse.legacy(),type);put(horse.legacy(),"adult",true);put(horse.legacy(),"fwingc",0F);put(horse.legacy(),"fwingh",1F);
            EntityViewsB173.flush();horse.setOnGround(false);horse.setDeltaMovement(0,-1,0);horse.aiStep();
            require((Float)get(horse.legacy(),"fwingc")==1F&&(Float)get(horse.legacy(),"fwingb")>0F,"original airborne wing animation clamps extension and advances flap subtype"+type);
            require(horse.getDeltaMovement().y>-.9,"original wing descent damping survives native travel subtype"+type+": "+horse.getDeltaMovement());
        }
    }
    private static void nativePrey(Context c)throws ReflectiveOperationException {
        var setting=(legacyconnector.vb1_7_3.net.minecraft.src.SettingBoolean)c.registration.mod().instance.getClass().getField("attackwolves").get(null);
        var previous=new java.util.HashMap<>(setting.values);
        var context=legacyconnector.vb1_7_3.net.minecraft.src.ModSettings.currentContext;
        try {
            var bear=c.spawn("EntityBear",.5);var wolf=net.minecraft.world.entity.EntityType.WOLF.create(c.level);
            require(wolf!=null,"native wolf fixture constructed");wolf.setPos(c.origin.getX()+3,c.origin.getY(),c.origin.getZ()+.5);wolf.setNoAi(true);c.entities.add(wolf);require(c.level.addFreshEntity(wolf),"native wolf inserted into loaded query sections");
            var select=bear.legacy().getClass().getMethod("getClosestTarget",legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,double.class);
            setting.values.put(context,true);require(select.invoke(bear.legacy(),bear.legacy(),10D)==EntityViewsB173.wrap(wolf),"original bear accepts visible native wolf when attackwolves enabled");EntityViewsB173.flush();
            setting.values.put(context,false);require(select.invoke(bear.legacy(),bear.legacy(),10D)==null,"original bear protects native wolf when attackwolves disabled");EntityViewsB173.flush();
            // Horse's separate original damage branch refuses wolves and clears their attack target.
            var horse=c.spawn("EntityHorse",6);wolf.setTarget(horse);float health=horse.getHealth();
            require(!horse.hurt(c.level.damageSources().mobAttack(wolf),2)&&horse.getHealth()==health&&wolf.getTarget()==null,"original horse recognizes native wolf attacker and clears its hostile target");
            bear.discard();wolf.discard();horse.discard();EntityViewsB173.flush();
            var hunter=c.spawn("EntityWWolf",.5);var cow=net.minecraft.world.entity.EntityType.COW.create(c.level);var sheep=net.minecraft.world.entity.EntityType.SHEEP.create(c.level);
            require(cow!=null&&sheep!=null,"native cow and sheep fixtures constructed");
            cow.setPos(c.origin.getX()+2,c.origin.getY(),c.origin.getZ()+.5);sheep.setPos(c.origin.getX()+4,c.origin.getY(),c.origin.getZ()+.5);cow.setNoAi(true);sheep.setNoAi(true);c.entities.add(cow);c.entities.add(sheep);require(c.level.addFreshEntity(cow)&&c.level.addFreshEntity(sheep),"native prey enter loaded query sections");
            var prey=hunter.legacy().getClass().getMethod("getClosestTarget",legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,double.class).invoke(hunter.legacy(),hunter.legacy(),10D);
            require(prey==EntityViewsB173.wrap(sheep),"original wild wolf skips nearer native cow and selects eligible sheep");EntityViewsB173.flush();
        } finally {setting.values.clear();setting.values.putAll(previous);}
    }

}
