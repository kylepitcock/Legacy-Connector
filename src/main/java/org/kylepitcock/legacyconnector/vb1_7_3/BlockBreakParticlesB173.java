package org.kylepitcock.legacyconnector.vb1_7_3;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;

/** One particles-only request per original block break; no sound or block mutation is implied. */
public final class BlockBreakParticlesB173 {
    private static final String FAMILY="b1.7.3-block-break-particles";
    public record Request(ResourceLocation dimension,BlockPos position,BlockState state){}
    public record Sample(double x,double y,double z,double velocityX,double velocityY,double velocityZ){}
    private static volatile Consumer<Request> client;
    private static boolean installed;
    private BlockBreakParticlesB173(){}
    private static synchronized void init(){
        if(installed)return;installed=true;
        LegacyNetwork.registerReceiver(FAMILY,(payload,context)->{
            if(!context.flow().isClientbound())return;
            context.enqueueWork(()->{var sink=client;if(sink!=null)sink.accept(decode(payload.data()));});
        });
    }
    public static void installClient(Consumer<Request> sink){init();client=sink;}
    public static void request(World world,int x,int y,int z,int blockId,int metadata){
        if(world==null||world.mcc$level()==null||blockId==0)return;
        var state=BlocksB173.modernState(blockId,metadata);if(state.isAir())return;
        init();var level=world.mcc$level();var request=new Request(level.dimension().location(),new BlockPos(x,y,z),state);
        if(level instanceof net.minecraft.server.level.ServerLevel server){
            var payload=new LegacyPayload(FAMILY,"break",0,encode(request));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server,null,x+.5,y+.5,z+.5,64,payload);
        }else{var sink=client;if(sink!=null)sink.accept(request);}
    }
    /** Original fixed 4x4x4 grid and outward velocity, independent of the block's current world state or shape. */
    public static void samples(BlockPos position,Consumer<Sample> sink){
        for(int x=0;x<4;x++)for(int y=0;y<4;y++)for(int z=0;z<4;z++){
            double dx=(x+.5)/4,dy=(y+.5)/4,dz=(z+.5)/4;
            sink.accept(new Sample(position.getX()+dx,position.getY()+dy,position.getZ()+dz,dx-.5,dy-.5,dz-.5));
        }
    }
    public static byte[] encode(Request request){
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{buffer.writeResourceLocation(request.dimension());buffer.writeBlockPos(request.position());buffer.writeVarInt(Block.getId(request.state()));byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);return bytes;}finally{buffer.release();}
    }
    public static Request decode(byte[] bytes){
        var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try{return new Request(buffer.readResourceLocation(),buffer.readBlockPos(),Block.stateById(buffer.readVarInt()));}finally{buffer.release();}
    }
}
