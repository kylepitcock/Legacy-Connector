package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.vb1_7_3.net.minecraft.src.IInterceptBlockSet;
import legacyconnector.vb1_7_3.net.minecraft.src.Loc;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** First matching original callback may replace a representable block ID before storage changes. */
public final class BlockInterceptionB173 {
    private static final CopyOnWriteArrayList<IInterceptBlockSet> INTERCEPTORS=new CopyOnWriteArrayList<>();
    private record Write(World world,int x,int y,int z,int id){}
    private static final ThreadLocal<ArrayDeque<Write>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
    private static final class NativeWrite {
        final Level level;final BlockPos position;final BlockState state;boolean entered;
        NativeWrite(Level level,BlockPos position,BlockState state){this.level=level;this.position=position.immutable();this.state=state;}
    }
    private static final ThreadLocal<ArrayDeque<NativeWrite>> NATIVE_WRITES=ThreadLocal.withInitial(ArrayDeque::new);
    private BlockInterceptionB173(){}
    public static void register(IInterceptBlockSet interceptor){INTERCEPTORS.add(java.util.Objects.requireNonNull(interceptor));}
    public static boolean hasInterceptors(){return !INTERCEPTORS.isEmpty();}
    public static int intercept(World world,Loc position,int id){
        if(INTERCEPTORS.isEmpty())return id;
        var writes=ACTIVE.get();var write=new Write(world,position.x(),position.y(),position.z(),id);
        if(writes.contains(write))return id;
        writes.push(write);
        var mc=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=mc.world;
        var actor=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,actor!=null&&actor.world==world?actor:null)){
            for(var interceptor:INTERCEPTORS)if(interceptor.canIntercept(world,position,id))return interceptor.intercept(world,position,id);
            return id;
        }finally{mc.world=previousWorld;writes.pop();if(writes.isEmpty())ACTIVE.remove();}
    }
    /** Null means retain the exact incoming native state, including properties with no legacy representation. */
    public static BlockState replacement(Level level,BlockPos position,BlockState incoming){
        if(INTERCEPTORS.isEmpty())return null;
        var writes=NATIVE_WRITES.get();var pending=writes.peek();
        if(pending!=null&&!pending.entered&&pending.level==level&&pending.position.equals(position)&&pending.state==incoming){pending.entered=true;return null;}
        if(!(incoming.getBlock() instanceof LegacyB173Block)&&!incoming.isAir()&&!BlocksB173.hasVanillaId(incoming))return null;
        int id=BlocksB173.legacyId(incoming);
        int replacement=intercept(World.mcc$wrap(level),new Loc(position.getX(),position.getY(),position.getZ()),id);
        if(replacement==id)return null;
        if(replacement<0||replacement>=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS.length)return null;
        var state=BlocksB173.modernState(replacement,BlocksB173.legacyMeta(incoming));
        if(replacement!=0&&state.isAir())return null;
        return state;
    }
    /** Runs only the replacement write without feeding that same replacement through the API twice. */
    public static boolean writeReplacement(Level level,BlockPos position,BlockState state,int flags,int recursion){
        var writes=NATIVE_WRITES.get();writes.push(new NativeWrite(level,position,state));
        try{return level.setBlock(position,state,flags,recursion);}
        finally{writes.pop();if(writes.isEmpty())NATIVE_WRITES.remove();}
    }
}
