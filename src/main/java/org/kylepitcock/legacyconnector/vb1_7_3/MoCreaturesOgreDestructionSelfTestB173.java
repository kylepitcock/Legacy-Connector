package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Original renderer-owned world effects must reach the authoritative world before chunk serialization. */
public final class MoCreaturesOgreDestructionSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173OgreDestructionTest");
    private MoCreaturesOgreDestructionSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC OGRE DESTRUCTION TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);field.set(object,value);return;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static BlockState serialized(net.minecraft.server.level.ServerLevel level,BlockPos pos){
        var saved=ChunkSerializer.write(level,level.getChunkAt(pos));var sections=saved.getList("sections",10);
        for(int i=0;i<sections.size();i++){var section=sections.getCompound(i);if(section.getByte("Y")==net.minecraft.core.SectionPos.blockToSectionCoord(pos.getY())){
            var codec=PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY,BlockState.CODEC,PalettedContainer.Strategy.SECTION_STATES,Blocks.AIR.defaultBlockState());
            return codec.parse(NbtOps.INSTANCE,section.getCompound("block_states")).getOrThrow().get(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
        }}throw new IllegalStateException("fixture section missing from native chunk serialization");
    }
    private static void run(net.minecraft.server.level.ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")&&r.type().getSimpleName().equals("EntityOgre")).findFirst();if(selected.isEmpty())return;
        RenderersB173.discover();var failures=new ArrayList<String>();
        // Separate proofs distinguish native block persistence from the absent server callback dispatch.
        for(boolean render:new boolean[]{true,false})try{proof(level,selected.get(),render);LOG.info("B1.7.3 MOC OGRE DESTRUCTION TEST: {} passed",render?"original renderer mutation and native chunk serialization":"server tick authoritative destruction without a viewer");}
        catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(error.toString());LOG.error("B1.7.3 MOC OGRE DESTRUCTION TEST failed: {}",render?"original renderer persistence":"server callback dispatch",error);}
        if(!failures.isEmpty())LOG.error("B1.7.3 MOC OGRE DESTRUCTION TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC OGRE DESTRUCTION TEST: all original-callback proofs passed");
    }
    private static void proof(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration,boolean render)throws ReflectiveOperationException {
        var origin=level.getSharedSpawnPos().offset(8,80,8);var target=origin.above();var area=new net.minecraft.world.phys.AABB(origin).inflate(24);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunk(origin.offset(x*16,0,z*16));
        var before=new HashSet<>(level.getEntities((net.minecraft.world.entity.Entity)null,area));var previous=level.getBlockState(target);var world=World.mcc$wrap(level);Random randomBefore=world.random;
        LegacyB173Animal host=null;
        try{
            world.random=new Random(3173);
            var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            put(original,"ogreattack",true);put(original,"ogrehasenemy",false);put(original,"destroyForce",4F);original.attackCooldown=0;
            var info=AnimalsB173.all().stream().filter(i->i.registration().type()==registration.type()).findFirst().orElseThrow();host=new LegacyB173Animal(info.type(),level,original);
            require(RendererBlockEffectsB173.hasBlockEffects(host),"shared call graph finds original renderer-to-world mutation");
            require(level.setBlock(target,Blocks.DIRT.defaultBlockState(),3),"controlled destructible native block placed");require(serialized(level,target).is(Blocks.DIRT),"native chunk serialization initially contains fixture block");
            var chunk=level.getChunkAt(target);chunk.setUnsaved(false);
            if(render)AnimalRenderingB173.draw(host,.5F,(draw,texture)->{});else host.tick();
            require(level.getBlockState(target).isAir(),render?"actual original renderer removes authoritative server block":"pending original renderer gameplay must execute on server ticks without client-only ghost edits");
            require(chunk.isUnsaved(),"original destruction marks authoritative native chunk dirty");require(serialized(level,target).isAir(),"native chunk save representation retains original destruction");
        }finally{world.random=randomBefore;level.setBlock(target,previous,3);if(host!=null)host.discard();for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,area))if(!before.contains(entity))entity.discard();EntityViewsB173.flush();}
    }
}
