package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites;

/** Native region writes and writable tile stand-ins for legacy structures spanning several chunks. */
public final class WorldGenWritesB173 {
    private static final ThreadLocal<Boolean> ATTACHING=ThreadLocal.withInitial(()->false);
    private WorldGenWritesB173(){}
    public static boolean generation(){return World.mcc$generationActive()||ATTACHING.get();}
    public static boolean outside(World world,BlockPos pos){
        if(!(world.mcc$access() instanceof WorldGenRegion region))return false;
        var center=region.getCenter();return Math.abs((pos.getX()>>4)-center.x)>1||Math.abs((pos.getZ()>>4)-center.z)>1;
    }
    public static BlockState pending(World world,BlockPos pos){return world.mcc$access() instanceof WorldGenRegion region?DeferredGenWrites.pendingState(region.getLevel(),pos):null;}
    public static boolean defer(World world,BlockPos pos,BlockState state){
        if(!outside(world,pos))return false;
        var region=(WorldGenRegion)world.mcc$access();
        org.kylepitcock.legacyconnector.core.worldgen.LegacyGeneratorStructures.observeDeferred(region,pos,state);
        var level=region.getLevel();var key=pos.immutable();Runnable write=()->DeferredGenWrites.block(level,key,state);
        if(!org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.holdDeferred(region,write))write.run();
        return true;
    }
    public static BlockEntity tile(World world,BlockPos pos){
        if(!(world.mcc$access() instanceof WorldGenRegion region))return null;
        var level=region.getLevel();var key=pos.immutable();
        Object tile=DeferredGenWrites.tile(level,key,state->{
            BlockEntity view=null;
            if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)view=new ChestBlockEntity();
            else if(state.getBlock() instanceof net.minecraft.world.level.block.SpawnerBlock)view=new MobSpawnerBlockEntity();
            else if(state.getBlock() instanceof EntityBlock block&&block.newBlockEntity(key,state) instanceof LegacyB173BlockEntity host)view=host.legacy();
            if(view!=null){view.world=world;view.x=key.getX();view.y=key.getY();view.z=key.getZ();}
            return view;
        },nativeEntity->{
            Object stand=DeferredGenWrites.standIn(level,key);
            if(stand instanceof BlockEntity view)attach(view,nativeEntity);
        });
        return tile instanceof BlockEntity view?view:null;
    }
    public static void replace(World world,BlockPos pos,BlockEntity view){
        if(!(world.mcc$access() instanceof WorldGenRegion region)||view==null)return;
        // Ensure the pending block has an attachment callback before replacing its stand-in.
        tile(world,pos);view.world=world;view.x=pos.getX();view.y=pos.getY();view.z=pos.getZ();
        DeferredGenWrites.replaceStandIn(region.getLevel(),pos,view);
    }
    private static void attach(BlockEntity view,net.minecraft.world.level.block.entity.BlockEntity nativeEntity){
        boolean previous=ATTACHING.get();ATTACHING.set(true);
        try {
            if(view instanceof ChestBlockEntity chest&&nativeEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity nativeChest){BlockEntityViewsB173.attachChest(chest,nativeChest);BlockEntityViewsB173.flush(chest);}
            else if(view instanceof MobSpawnerBlockEntity spawner&&nativeEntity instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity nativeSpawner){spawner.mcc$host=nativeSpawner;spawner.mcc$apply();}
            else if(nativeEntity instanceof LegacyB173BlockEntity host)host.setLegacy(view);
        }finally{try{BlockEntityViewsB173.flush();}finally{ATTACHING.set(previous);}}
    }
}
