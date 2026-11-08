package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoCreaturesRemainingSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173RemainingTest");
    private MoCreaturesRemainingSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC REMAINING TEST failed",error);}});}
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
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("kitty native prey size policy",MoCreaturesRemainingSelfTestB173::nativePreySize),new Case("bird leaf scan and tree ascent",MoCreaturesRemainingSelfTestB173::birdTree));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC REMAINING TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC REMAINING TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC REMAINING TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC REMAINING TEST: all {} original-callback proofs passed",cases.size());}
    private static Object call(Object object,String name,Class<?>[] signature,Object...args)throws ReflectiveOperationException {
        var method=object.getClass().getDeclaredMethod(name,signature);method.setAccessible(true);return method.invoke(object,args);
    }
    private static void nativePreySize(Context c)throws ReflectiveOperationException {
        var kitty=c.spawn("EntityKitty",.5);
        var cow=net.minecraft.world.entity.EntityType.COW.create(c.level);var chicken=net.minecraft.world.entity.EntityType.CHICKEN.create(c.level);
        require(cow!=null&&chicken!=null,"native prey fixture construction");
        cow.setPos(c.origin.getX()+2,c.origin.getY(),c.origin.getZ()+.5);chicken.setPos(c.origin.getX()+4,c.origin.getY(),c.origin.getZ()+.5);cow.setNoAi(true);chicken.setNoAi(true);c.entities.add(cow);c.entities.add(chicken);
        require(c.level.addFreshEntity(cow)&&c.level.addFreshEntity(chicken),"native prey enter loaded query sections");
        var signature=new Class<?>[]{legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,double.class};
        var prey=call(kitty.legacy(),"getClosestTarget",signature,kitty.legacy(),10D);
        require(prey==EntityViewsB173.wrap(chicken),"original Kitty skips closer large cow and selects visible small chicken");
        var chickenView=EntityViewsB173.wrap(chicken);var cowView=EntityViewsB173.wrap(cow);
        require(chickenView.width==chicken.getBbWidth()&&chickenView.height==chicken.getBbHeight()&&cowView.width==cow.getBbWidth()&&cowView.height==cow.getBbHeight(),"ordinary native prey views expose current native dimensions");EntityViewsB173.flush();
        chicken.discard();cow.setBaby(true);
        require(call(kitty.legacy(),"getClosestTarget",signature,kitty.legacy(),10D)==EntityViewsB173.wrap(cow),"original Kitty accepts native baby cow when width is at most .5");EntityViewsB173.flush();
        cow.setBaby(false);
        require(call(kitty.legacy(),"getClosestTarget",signature,kitty.legacy(),10D)==null,"original Kitty rejects same cached native cow view after adult dimensions return");EntityViewsB173.flush();
    }
    private static void birdTree(Context c)throws ReflectiveOperationException {
        var bird=c.spawn("EntityBird",.5);
        // Original search returns first scan-order material, not Euclidean nearest: retain that quirk.
        var leaf=c.origin.offset(4,0,0);var trunk=c.origin.offset(5,0,0);
        for(int x=-2;x<=11;x++)for(int y=0;y<=7;y++)for(int z=-5;z<=5;z++)c.block(c.origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        c.block(leaf,net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState());c.block(trunk,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());c.block(trunk.above(),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
        var material=legacyconnector.vb1_7_3.net.minecraft.block.material.Material.LEAVES;
        var scan=(int[])call(bird.legacy(),"ReturnNearestMaterialCoord",new Class<?>[]{legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,material.getClass(),Double.class},bird.legacy(),material,8D);
        require(Arrays.equals(scan,new int[]{leaf.getX(),leaf.getY(),leaf.getZ()}),"original bird scan recognizes native leaf material identity");
        EntityViewsB173.flush();bird.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);bird.syncView();
        require(!(Boolean)call(bird.legacy(),"FlyToNextTree",new Class<?>[]{}),"original distant tree remains a flight destination");bird.flushView();EntityViewsB173.flush();
        require(bird.getDeltaMovement().x>.04&&bird.getDeltaMovement().y>.14,"original tree flight resolves wood trunk and adds ascent plus horizontal steering");
    }
}
