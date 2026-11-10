package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.SleepAttemptResult;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Original bed state joins the native sleeping pose, occupancy, night vote and wake lifecycle. */
public final class BedsB173 {
    private BedsB173(){}
    private static boolean installed;
    public static synchronized void install(){
        if(installed)return;installed=true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BedsB173::onWake);
    }
    private static void onWake(net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent event){
        var player=event.getEntity();var pos=player.getSleepingPos().orElse(null);
        if(pos==null)return;
        var level=player.level();var state=level.getBlockState(pos);
        if(!event.wakeImmediately()&&isBed(state)&&player instanceof net.minecraft.server.level.ServerPlayer server)
            server.setRespawnPosition(player.level().dimension(),pos,player.getYRot(),false,true);
        // NeoForge exposes custom bed direction, but the following native wake lambda still reads
        // vanilla FACING directly. Complete only that placement step before it examines sleepingPos.
        prepareWake(state,
                ()->net.minecraft.world.level.block.BedBlock.findStandUpPosition(player.getType(),level,pos,
                        state.getBedDirection(level,pos),player.getYRot()).orElseGet(()->net.minecraft.world.phys.Vec3.atBottomCenterOf(pos.above()).add(0,.1,0)),
                ()->state.setBedOccupied(level,pos,player,false),
                destination->{
                    var toward=net.minecraft.world.phys.Vec3.atBottomCenterOf(pos).subtract(destination).normalize();
                    player.setPos(destination.x,destination.y,destination.z);
                    player.setYRot((float)net.minecraft.util.Mth.wrapDegrees(net.minecraft.util.Mth.atan2(toward.z,toward.x)*180/Math.PI-90));
                    player.setXRot(0);
                },player::clearSleepingPos);
    }
    /** Let native wake retain pose, timer, vote and networking; replace only its incompatible bed-placement lambda. */
    static boolean prepareWake(BlockState state,java.util.function.Supplier<net.minecraft.world.phys.Vec3> standUp,
            Runnable freeBed,java.util.function.Consumer<net.minecraft.world.phys.Vec3> place,Runnable clearSleepingPos){
        if(!isBed(state)||state.hasProperty(net.minecraft.world.level.block.BedBlock.FACING))return false;
        freeBed.run();place.accept(standUp.get());clearSleepingPos.run();return true;
    }
    private static final class Fields {
        static final java.lang.reflect.Field COUNTER=net.neoforged.fml.util.ObfuscationReflectionHelper.findField(Player.class,"sleepCounter");
    }
    public static boolean isBed(BlockState state){return state.getBlock() instanceof LegacyB173Block host&&host.legacy().getRenderType()==14;}
    public static Direction direction(int meta){return switch(meta&3){case 0->Direction.SOUTH;case 1->Direction.WEST;case 2->Direction.NORTH;default->Direction.EAST;};}
    public static int occupied(int meta,boolean value){return value?meta|4:meta&~4;}
    static BlockPos requestedPosition(PlayerEntity view){
        if(view.sleepingPos!=null)return new BlockPos(view.sleepingPos.x,view.sleepingPos.y,view.sleepingPos.z);
        // Original custom beds may set only sleeping plus the standard within-block sleep placement.
        return BlockPos.containing(view.x,view.y,view.z);
    }
    public static void writeBack(PlayerEntity view,Player player,boolean before){
        if(view.sleeping==before)return;
        if(!view.sleeping){if(player.isSleeping())player.stopSleepInBed(true,true);view.mcc$restoreAwakeEyeHeight();return;}
        var pos=requestedPosition(view);var level=player.level();
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isBed(level,pos,player))return;
        install();
        player.startSleeping(pos);
        try{Fields.COUNTER.setInt(player,0);}catch(IllegalAccessException e){throw new IllegalStateException("Cannot reset native sleep timer",e);}
        view.sleepingPos=new Vec3i(pos.getX(),pos.getY(),pos.getZ());view.sleepTimer=0;
        if(level instanceof net.minecraft.server.level.ServerLevel server)server.updateSleepingPlayerList();
    }
    public static SleepAttemptResult trySleep(PlayerEntity view,int x,int y,int z){
        if(!(view.mcc$modern instanceof Player player))return SleepAttemptResult.OTHER_PROBLEM;
        var result=player.startSleepInBed(new BlockPos(x,y,z));
        if(result.right().isPresent()){
            view.sleeping=true;view.sleepTimer=0;view.sleepingPos=new Vec3i(x,y,z);return SleepAttemptResult.OK;
        }
        return switch(result.left().orElseThrow()){
            case NOT_POSSIBLE_HERE->SleepAttemptResult.NOT_POSSIBLE_HERE;
            case NOT_POSSIBLE_NOW->SleepAttemptResult.NOT_POSSIBLE_NOW;
            case TOO_FAR_AWAY->SleepAttemptResult.TOO_FAR_AWAY;
            default->SleepAttemptResult.OTHER_PROBLEM;
        };
    }
    public static void wake(PlayerEntity view,boolean immediately,boolean updateWorld,boolean setSpawn){
        if(view.mcc$modern instanceof Player player){
            var pos=player.getSleepingPos().orElse(null);player.stopSleepInBed(immediately,updateWorld);
            if(setSpawn&&pos!=null&&player instanceof net.minecraft.server.level.ServerPlayer server)
                server.setRespawnPosition(player.level().dimension(),pos,player.getYRot(),false,true);
        }
        view.sleeping=false;view.sleepTimer=immediately?0:100;view.mcc$restoreAwakeEyeHeight();view.setBoundingBoxSpacing(.6F,1.8F);
    }
}
