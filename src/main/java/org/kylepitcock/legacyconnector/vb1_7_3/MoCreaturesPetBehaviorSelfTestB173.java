package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Advanced decisions execute the actual mod; no mod classes are compile-time dependencies. */
public final class MoCreaturesPetBehaviorSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173PetBehaviorTest");
    private MoCreaturesPetBehaviorSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC PET BEHAVIOR TEST failed",error);}});}
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
    private static void run(net.minecraft.server.level.ServerLevel level){var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;var failures=new ArrayList<String>();var cases=List.of(new Case("kitty original fear exclusions",MoCreaturesPetBehaviorSelfTestB173::fear),new Case("kitty tree search and timeout",MoCreaturesPetBehaviorSelfTestB173::tree),new Case("bird native seed taming",MoCreaturesPetBehaviorSelfTestB173::bird));for(var proof:cases)try(var c=new Context(level,selected.get())){proof.proof.run(c);LOG.info("B1.7.3 MOC PET BEHAVIOR TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC PET BEHAVIOR TEST failed: {}",proof.name,error);}if(!failures.isEmpty())LOG.error("B1.7.3 MOC PET BEHAVIOR TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC PET BEHAVIOR TEST: all {} original-callback proofs passed",cases.size());}
    private static void fear(Context c)throws ReflectiveOperationException {
        var kitty=c.spawn("EntityKitty",.5);var deer=c.spawn("EntityDeer",1);var horse=c.spawn("EntityHorse",2);var bird=c.spawn("EntityBird",3);
        var method=kitty.legacy().getClass().getMethod("getBoogey",double.class,boolean.class);
        require(method.invoke(kitty.legacy(),6D,true)==null,"original fear utility excludes deer, horse and tiny bird");
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(c.level);require(zombie!=null,"native fear fixture constructed");zombie.setNoAi(true);zombie.setPos(c.origin.getX()+4,c.origin.getY(),c.origin.getZ()+.5);c.entities.add(zombie);require(c.level.addFreshEntity(zombie),"native fear fixture admitted");EntityViewsB173.flush();
        require(((LivingEntity)method.invoke(kitty.legacy(),6D,false)).mcc$modern==zombie,"original fear utility sees native non-player monster through generic living view");
    }
    private static void tree(Context c)throws ReflectiveOperationException {
        var kitty=c.spawn("EntityKitty",.5);put(kitty.legacy(),"adult",true);put(kitty.legacy(),"hungry",false);put(kitty.legacy(),"kittystate",16);
        var trunk=c.origin.offset(1,0,0);for(int y=-1;y<=2;y++)c.block(trunk.above(y),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
        c.decision(kitty,50);kitty.aiStep();require(!(Boolean)get(kitty.legacy(),"foundTree"),"original kitty rejects bare wood without leaves above selected column");
        c.block(trunk.above(3),net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState());kitty.aiStep();require((Boolean)get(kitty.legacy(),"foundTree"),"original kitty discovers native wood column with leaves");
        c.decision(kitty,10);kitty.aiStep();require((Boolean)get(kitty.legacy(),"onTree"),"original near-tree distance decision enters climbing state");
        put(kitty.legacy(),"kittytimer",500);kitty.aiStep();require((Integer)get(kitty.legacy(),"kittystate")==16,"original tree timeout does not abandon kitty already on tree");
        put(kitty.legacy(),"onTree",false);kitty.aiStep();require((Integer)get(kitty.legacy(),"kittystate")==7&&!(Boolean)get(kitty.legacy(),"foundTree")&&(Integer)get(kitty.legacy(),"kittytimer")==0,"original tree timeout clears search state and timer");
    }
    private static net.minecraft.world.entity.item.ItemEntity drop(Context c,LegacyB173Animal bird,double x,net.minecraft.world.item.Item item,int count){
        var entity=new net.minecraft.world.entity.item.ItemEntity(c.level,bird.getX()+x,bird.getY(),bird.getZ(),new net.minecraft.world.item.ItemStack(item,count));c.entities.add(entity);require(c.level.addFreshEntity(entity),"native dropped seed fixture admitted");return entity;
    }
    private static void bird(Context c)throws ReflectiveOperationException {
        var bird=c.spawn("EntityBird",.5);var unsupported=drop(c,bird,.1,net.minecraft.world.item.Items.PUMPKIN_SEEDS,1);
        require(c.world.getEntities(bird.legacy(),bird.legacy().boundingBox.expand(4,4,4)).stream().noneMatch(view->view.mcc$modern==unsupported)
                &&c.world.entities.stream().noneMatch(view->view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity&&entity.mcc$modern==unsupported),"legacy spatial and live-list queries omit unrepresentable post-Beta item payloads");
        var wrong=drop(c,bird,.1,net.minecraft.world.item.Items.WHEAT,1);c.decision(bird,50);bird.aiStep();
        require(!(Boolean)get(bird.legacy(),"tamed")&&!wrong.isRemoved(),"original bird rejects non-wheat seeds");
        require(!unsupported.isRemoved()&&unsupported.getItem().is(net.minecraft.world.item.Items.PUMPKIN_SEEDS)&&unsupported.getItem().getCount()==1,"unsupported modern item entity and native stack survive original callback unchanged");
        var far=drop(c,bird,3,net.minecraft.world.item.Items.WHEAT_SEEDS,1);bird.aiStep();require(!(Boolean)get(bird.legacy(),"tamed")&&!far.isRemoved(),"original bird seeks distant seeds without taming outside one block");
        var near=drop(c,bird,.2,net.minecraft.world.item.Items.WHEAT_SEEDS,3);c.decision(bird,-1);bird.aiStep();require(!(Boolean)get(bird.legacy(),"tamed")&&!near.isRemoved(),"original bird waits for one-in-fifty taming decision even beside wheat seeds");
        c.decision(bird,50);bird.aiStep();require((Boolean)get(bird.legacy(),"tamed")&&near.isRemoved()&&!far.isRemoved()&&!wrong.isRemoved()&&!unsupported.isRemoved(),"original bird removes entire nearest seed entity and tames while other drops survive");
    }
}
