package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Explicit opt-in fixture only. Its caller selects an isolated disposable native placement area. */
public final class ChestsSelfTestB173 {
    private ChestsSelfTestB173(){}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    public static String run(ServerLevel level,int x,int y,int z){
        var world=World.mcc$wrap(level);
        try{
            for(var axis:new Direction[]{Direction.EAST,Direction.SOUTH})for(boolean notify:new boolean[]{false,true}){
                var a=new BlockPos(x,y,z);var b=a.relative(axis);x+=8;
                world.setBlock(a.getX(),a.getY(),a.getZ(),0);world.setBlock(b.getX(),b.getY(),b.getZ(),0);
                place(world,a,notify);var first=(ChestBlockEntity)level.getBlockEntity(a);
                var named=new ItemStack(Items.DIAMOND,3);named.set(DataComponents.CUSTOM_NAME,Component.literal("Beta chest component fixture"));first.setItem(0,named);
                place(world,b,notify);var second=(ChestBlockEntity)level.getBlockEntity(b);var other=new ItemStack(Items.EMERALD,2);second.setItem(0,other);
                require(level.getBlockEntity(a)==first&&first.getItem(0)==named,"pairing preserves original native BE and component-bearing stack identity");
                require(Component.literal("Beta chest component fixture").equals(first.getItem(0).get(DataComponents.CUSTOM_NAME)),"native item components retained");
                pair(level.getBlockState(a),level.getBlockState(b),a,b);
                var combined=ChestBlock.getContainer((ChestBlock)Blocks.CHEST,level.getBlockState(a),level,a,true);
                require(combined!=null&&combined.getContainerSize()==54,"native double chest exposes 54 slots for both Beta placement methods/axes");
                boolean firstAtA=ChestBlock.getBlockType(level.getBlockState(a))==DoubleBlockCombiner.BlockType.FIRST;
                require(combined.getItem(0)==(firstAtA?named:other)&&combined.getItem(27)==(firstAtA?other:named),"native FIRST/SECOND inventory order and caller stacks retained");
                var facing=level.getBlockState(a).getValue(ChestBlock.FACING);
                int oppositeMeta=switch(facing.getOpposite()){case NORTH->2;case SOUTH->3;case WEST->4;case EAST->5;default->throw new IllegalStateException();};
                world.setBlockMeta(a.getX(),a.getY(),a.getZ(),oppositeMeta);
                require(world.getBlockMeta(a.getX(),a.getY(),a.getZ())==oppositeMeta,"legacy metadata view follows native pair orientation");
                pair(level.getBlockState(a),level.getBlockState(b),a,b);
                require(level.getBlockEntity(a)==first&&first.getItem(0)==named,"metadata changes preserve BE/inventory identity");
                var third=b.relative(axis);world.setBlock(third.getX(),third.getY(),third.getZ(),0);place(world,third,notify);
                require(level.getBlockState(third).getValue(ChestBlock.TYPE)==ChestType.SINGLE,"third chest does not extend native pair");
                require(ChestBlock.getContainer((ChestBlock)Blocks.CHEST,level.getBlockState(a),level,a,true).getContainerSize()==54,"third placement retains original native double container");
            }
            deferred(level);
        }finally{BlockEntityViewsB173.flush();EntityViewsB173.flush();}
        return "passed: native X/Z 54-slot ordering, metadata, no triples, native BE/stack/components, deferred stand-in loot";
    }
    private static void place(World world,BlockPos p,boolean notify){
        require(notify?world.setBlock(p.getX(),p.getY(),p.getZ(),54):world.setBlockWithoutNotifyingNeighbors(p.getX(),p.getY(),p.getZ(),54),"explicit Beta chest placement");
    }
    private static void pair(net.minecraft.world.level.block.state.BlockState a,net.minecraft.world.level.block.state.BlockState b,BlockPos pa,BlockPos pb){
        require(a.getValue(ChestBlock.TYPE)!=ChestType.SINGLE&&a.getValue(ChestBlock.TYPE).getOpposite()==b.getValue(ChestBlock.TYPE),"reciprocal native chest types");
        require(a.getValue(ChestBlock.FACING)==b.getValue(ChestBlock.FACING)&&pa.relative(ChestBlock.getConnectedDirection(a)).equals(pb)&&pb.relative(ChestBlock.getConnectedDirection(b)).equals(pa),"matching native facing/connection");
    }
    private static void deferred(ServerLevel level){
        var step=ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);int cx=40_000,cz=40_000;
        var chunks=new java.util.HashMap<ChunkPos,ProtoChunk>();var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var cache=StaticCache2D.<GenerationChunkHolder>create(cx,cz,Math.max(4,step.directDependencies().getRadius()),(x,z)->{
            var pos=new ChunkPos(x,z);var chunk=new ProtoChunk(pos,UpgradeData.EMPTY,level,biomes,null);chunk.setPersistedStatus(ChunkStatus.CARVERS);chunks.put(pos,chunk);
            return new GenerationChunkHolder(pos){@Override public int getTicketLevel(){return 33;}@Override public int getQueueLevel(){return 0;}@Override public net.minecraft.world.level.chunk.ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status){return chunk;}};
        });
        var region=new WorldGenRegion(level,cache,step,chunks.get(new ChunkPos(cx,cz)));var world=World.mcc$wrap(level);
        var a=new BlockPos(cx*16+63,220,cz*16+2);var b=a.east();
        try(var scope=World.mcc$generation(region)){
            place(world,a,false);var first=(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity)world.getBlockEntity(a.getX(),a.getY(),a.getZ());
            first.setStack(0,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(264,3,0));
            place(world,b,true);require(first==world.getBlockEntity(a.getX(),a.getY(),a.getZ()),"pending restate retains original stand-in identity");
            var second=(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity)world.getBlockEntity(b.getX(),b.getY(),b.getZ());
            second.setStack(1,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(265,2,0));
            pair(WorldGenWritesB173.pending(world,a),WorldGenWritesB173.pending(world,b),a,b);
            for(var p:java.util.List.of(a,b)){
                var chunk=chunks.get(new ChunkPos(p));var target=new WorldGenRegion(level,cache,step,chunk);
                org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites.applyDuringGeneration(target,p.getX()>>4,p.getZ()>>4);
            }
            var ca=chunks.get(new ChunkPos(a));var cb=chunks.get(new ChunkPos(b));pair(ca.getBlockState(a),cb.getBlockState(b),a,b);
            var nativeA=(ChestBlockEntity)ca.getBlockEntity(a);var nativeB=(ChestBlockEntity)cb.getBlockEntity(b);
            require(nativeA!=null&&nativeB!=null&&first.mcc$host==nativeA&&second.mcc$host==nativeB,"original stand-ins attach to correct native BEs across deferred chunk boundary");
            require(nativeA.getItem(0).is(Items.DIAMOND)&&nativeA.getItem(0).getCount()==3&&nativeB.getItem(1).is(Items.IRON_INGOT)&&nativeB.getItem(1).getCount()==2,"both deferred inventories survive later pair-state writes");
        }
    }
}
