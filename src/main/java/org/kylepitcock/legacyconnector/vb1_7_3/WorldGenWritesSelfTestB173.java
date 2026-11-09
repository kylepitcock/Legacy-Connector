package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.concurrent.atomic.AtomicReference;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;

/** Explicitly armed, private-jar-independent native-region and cross-chunk loot regression. */
public final class WorldGenWritesSelfTestB173 {
    private static final AtomicReference<String> RESULT=new AtomicReference<>("not armed");
    private static int targetX,targetZ;
    private static boolean installed;
    private WorldGenWritesSelfTestB173(){}
    public static String result(){return RESULT.get();}
    public static synchronized void arm(int chunkX,int chunkZ){
        targetX=chunkX;targetZ=chunkZ;RESULT.set("armed");
        if(installed)return;installed=true;
        LegacyWorldGenFeature.addDecorator((region,x,z,seed)->{
            if(x!=targetX||z!=targetZ||!RESULT.compareAndSet("armed","running"))return;
            if(!(region instanceof net.minecraft.server.level.WorldGenRegion)) {RESULT.set("failed: native WorldGenRegion required");return;}
            var world=World.mcc$wrap(region.getLevel());
            try(var scope=World.mcc$generation(region)){
                int insideX=(x<<4)+2,outsideX=(x<<4)+50,baseZ=(z<<4)+2,y=220;
                require(world.setBlockWithoutNotifyingNeighbors(insideX,y,baseZ,54),"native in-region chest write");
                var inside=(ChestBlockEntity)world.getBlockEntity(insideX,y,baseZ);require(inside!=null,"native in-region chest view");inside.setStack(0,new ItemStack(264,4,0));
                var nativeInside=(net.minecraft.world.level.block.entity.ChestBlockEntity)region.getBlockEntity(new BlockPos(insideX,y,baseZ));
                require(nativeInside.getItem(0).getCount()==4,"native in-region original loot write");
                require(world.setBlockWithoutNotifyingNeighbors(outsideX,y,baseZ,54),"deferred chest placement accepted");
                var replacement=new ChestBlockEntity();world.setBlockEntity(outsideX,y,baseZ,replacement);replacement.setStack(5,new ItemStack(266,6,0));
                require(world.getBlockEntity(outsideX,y,baseZ)==replacement,"setBlockTileEntity replaces deferred stand-in by identity");
                require(world.setBlockWithoutNotifyingNeighbors(outsideX+1,y,baseZ,52),"deferred spawner accepted");
                var spawner=new MobSpawnerBlockEntity();world.setBlockEntity(outsideX+1,y,baseZ,spawner);spawner.setSpawnedEntityId("Pig");
                require(world.setBlockWithoutNotifyingNeighbors(outsideX+2,y,baseZ,35,0),"deferred metadata base accepted");world.setBlockMeta(outsideX+2,y,baseZ,5);
                require(world.getBlockId(outsideX+2,y,baseZ)==35&&world.getBlockMeta(outsideX+2,y,baseZ)==5,"pending modern-state reads preserve Beta metadata");
                BlockEntityViewsB173.flush();RESULT.set("generated: native in-region loot and deferred replacement/metadata accepted at "+outsideX+","+y+","+baseZ);
            }catch(RuntimeException|LinkageError error){RESULT.set("failed: "+error);throw error;}
        });
    }
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException(detail);}
    /** Run after the adjacent target chunk has completed and the queued native write tick has run. */
    public static String verify(ServerLevel level){
        int x=(targetX<<4)+50,z=(targetZ<<4)+2,y=220;
        require(RESULT.get().startsWith("generated:"),RESULT.get());
        require(level.getChunkSource().getChunkNow(x>>4,z>>4)!=null,"deferred target chunk is loaded");
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(new BlockPos(x,y,z));require(chest!=null,"deferred native chest exists");
        require(chest.getItem(5).is(net.minecraft.world.item.Items.GOLD_INGOT)&&chest.getItem(5).getCount()==6,"deferred original chest loot survived attachment");
        var spawner=(net.minecraft.world.level.block.entity.SpawnerBlockEntity)level.getBlockEntity(new BlockPos(x+1,y,z));require(spawner!=null,"deferred native spawner exists");
        var nbt=spawner.saveWithFullMetadata(level.registryAccess());require("minecraft:pig".equals(nbt.getCompound("SpawnData").getCompound("entity").getString("id")),"deferred native spawner retains original entity name");
        require(level.getBlockState(new BlockPos(x+2,y,z)).is(net.minecraft.world.level.block.Blocks.LIME_WOOL),"deferred absolute metadata state became native block");
        return "passed: native region chest loot, cross-boundary original chest replacement, native spawner SpawnData, absolute metadata writes";
    }
}
