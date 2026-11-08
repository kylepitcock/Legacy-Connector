package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.LevelChunk;

/** Beta candidate walks for hosted original mods; ordinary native entities retain their own spawn path. */
public final class BetaSpawnPlacementB173 {
    private BetaSpawnPlacementB173(){}
    record Roll<T>(T choice,BlockPos origin){}

    static <T> Roll<T> roll(Supplier<T> weightedChoice,IntUnaryOperator random,ChunkPos chunk){
        T choice=weightedChoice.get();
        if(choice==null)return null;
        return new Roll<>(choice,new BlockPos(chunk.getMinBlockX()+random.applyAsInt(16),random.applyAsInt(128),chunk.getMinBlockZ()+random.applyAsInt(16)));
    }
    static BlockPos walk(BlockPos previous,IntUnaryOperator random){
        int x=previous.getX()+random.applyAsInt(6)-random.applyAsInt(6);
        int y=previous.getY()+random.applyAsInt(1)-random.applyAsInt(1);
        int z=previous.getZ()+random.applyAsInt(6)-random.applyAsInt(6);
        return new BlockPos(x,y,z);
    }
    static boolean originAllowed(SpawnGroup group,Material material,boolean solid){return !solid&&material==group.getSpawnMaterial();}
    static boolean candidateAllowed(SpawnGroup group,Material material,boolean below,boolean here,boolean above){
        return group.getSpawnMaterial()==Material.WATER?material.isFluid()&&!above:below&&!here&&!material.isFluid()&&!above;
    }
    static boolean spawnDistanceAllowed(double squared){return squared>=576;}

    static boolean solid(World world,BlockPos pos){
        var access=world.mcc$access();
        if(access==null)return false;
        var state=access.getBlockState(pos);
        if(state.getBlock() instanceof LegacyB173Block hosted)
            return hosted.legacy().material.suffocates()&&hosted.legacy().isFullCube();
        // Original render-normal flags differ from modern collision shapes (soul sand, pistons).
        if(BlocksB173.hasVanillaId(state)){
            Boolean full=VanillaB173.fullCube(BlocksB173.legacyId(state));
            if(full!=null)return VanillaB173.materialOf(state.getBlock()).suffocates()&&full;
        }
        // Modern blocks with no old counterpart use their native geometry.
        return VanillaB173.materialOf(state.getBlock()).suffocates()&&state.isCollisionShapeFullBlock(access,pos);
    }
    private static Material material(World world,BlockPos pos){return world.getMaterial(pos.getX(),pos.getY(),pos.getZ());}

    public static int spawnChunk(SpawnGroup group,ServerLevel level,LevelChunk chunk,WeightedRandomList<SpawnerData> choices,
            NaturalSpawner.SpawnPredicate cost,NaturalSpawner.AfterSpawnCallback after){
        var rolled=roll(()->choices.getRandom(level.random).orElse(null),level.random::nextInt,chunk.getPos());
        if(rolled==null)return 0;
        World world=World.mcc$wrap(level);BlockPos origin=rolled.origin();
        if(!originAllowed(group,material(world,origin),solid(world,origin)))return 0;
        int count=0;
        for(int groupAttempt=0;groupAttempt<3;groupAttempt++){
            BlockPos candidate=origin;
            for(int attempt=0;attempt<4;attempt++){
                candidate=walk(candidate,level.random::nextInt);
                if(!level.hasChunk(candidate.getX()>>4,candidate.getZ()>>4))continue;
                if(!candidateAllowed(group,material(world,candidate),solid(world,candidate.below()),solid(world,candidate),solid(world,candidate.above())))continue;
                double x=candidate.getX()+.5,y=candidate.getY(),z=candidate.getZ()+.5;
                if(level.getNearestPlayer(x,y,z,24,false)!=null)continue;
                var spawn=level.getSharedSpawnPos();double dx=x-spawn.getX(),dy=y-spawn.getY(),dz=z-spawn.getZ();
                if(!spawnDistanceAllowed(dx*dx+dy*dy+dz*dz))continue;
                if(!cost.test(rolled.choice().type,candidate,chunk))continue;
                var entity=rolled.choice().type.create(level);
                if(!(entity instanceof Mob mob)||!(mob instanceof B173EntityHost||mob instanceof LegacyB173Cow))return count;
                mob.moveTo(x,y,z,level.random.nextFloat()*360,0);
                if(!positionAllowed(mob,level)){mob.discard();continue;}
                count++;
                finalizeOriginal(mob,level);
                if(!mob.isSpawnCancelled())level.addFreshEntityWithPassengers(mob);
                if(level.getEntity(mob.getUUID())==mob)after.run(mob,chunk);else mob.discard();
                if(count>=net.neoforged.neoforge.event.EventHooks.getMaxSpawnClusterSize(mob))return count;
            }
        }
        return count;
    }
    private static boolean positionAllowed(Mob mob,ServerLevel level){
        return (mob instanceof B173EntityHost||mob instanceof LegacyB173Cow)
                &&net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(mob,level,MobSpawnType.NATURAL);
    }
    private static void finalizeOriginal(Mob mob,ServerLevel level){
        ((org.kylepitcock.legacyconnector.mixin.BetaSpawnMobAccess)(Object)mob).legacyconnector$spawnType(MobSpawnType.NATURAL);
        var event=new net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent(mob,level,mob.getX(),mob.getY(),mob.getZ(),level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.NATURAL,null,null);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
    }
}
