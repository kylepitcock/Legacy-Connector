package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Double-chest state adaptation, invoked only for explicit Beta World writes. */
public final class VanillaChestPlacementB173 {
    private VanillaChestPlacementB173(){}
    private static final Direction[] SIDES={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static boolean chest(BlockState state){return state.is(Blocks.CHEST);}
    /** Reorient an existing pair together; its facing must remain perpendicular to the join. */
    public static BlockState metadata(BlockPos pos,BlockState before,BlockState next,
            Function<BlockPos,BlockState> read,BiConsumer<BlockPos,BlockState> write){
        if(!chest(before)||before.getValue(ChestBlock.TYPE)==ChestType.SINGLE)return next;
        var join=ChestBlock.getConnectedDirection(before);var otherPos=pos.relative(join);var other=read.apply(otherPos);
        if(!chest(other)||other.getValue(ChestBlock.TYPE)!=before.getValue(ChestBlock.TYPE).getOpposite()
                ||other.getValue(ChestBlock.FACING)!=before.getValue(ChestBlock.FACING))return next;
        var facing=next.getValue(ChestBlock.FACING);
        if(facing.getAxis()==join.getAxis())facing=before.getValue(ChestBlock.FACING);
        var type=join==facing.getClockWise()?ChestType.LEFT:ChestType.RIGHT;
        write.accept(otherPos,other.setValue(ChestBlock.FACING,facing).setValue(ChestBlock.TYPE,type.getOpposite()));
        return next.setValue(ChestBlock.FACING,facing).setValue(ChestBlock.TYPE,type);
    }
    public static void placed(BlockPos pos,Function<BlockPos,BlockState> read,BiConsumer<BlockPos,BlockState> write){
        var state=read.apply(pos);
        if(!chest(state))return;
        if(state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
            var partner=read.apply(pos.relative(ChestBlock.getConnectedDirection(state)));
            if(chest(partner)&&partner.getValue(ChestBlock.FACING)==state.getValue(ChestBlock.FACING)
                    &&partner.getValue(ChestBlock.TYPE)==state.getValue(ChestBlock.TYPE).getOpposite())return;
            state=state.setValue(ChestBlock.TYPE,ChestType.SINGLE);write.accept(pos,state);
        }
        Direction adjacent=null;
        for(var side:SIDES)if(chest(read.apply(pos.relative(side)))){if(adjacent!=null)return;adjacent=side;}
        if(adjacent==null)return;
        var otherPos=pos.relative(adjacent);var other=read.apply(otherPos);
        if(other.getValue(ChestBlock.TYPE)!=ChestType.SINGLE)return;
        for(var side:SIDES)if(!otherPos.relative(side).equals(pos)&&chest(read.apply(otherPos.relative(side))))return;
        var facing=state.getValue(ChestBlock.FACING);
        if(facing.getAxis()==adjacent.getAxis())facing=other.getValue(ChestBlock.FACING);
        if(facing.getAxis()==adjacent.getAxis())facing=adjacent.getAxis()==Direction.Axis.X?Direction.NORTH:Direction.WEST;
        var type=adjacent==facing.getClockWise()?ChestType.LEFT:ChestType.RIGHT;
        write.accept(pos,state.setValue(ChestBlock.FACING,facing).setValue(ChestBlock.TYPE,type));
        write.accept(otherPos,other.setValue(ChestBlock.FACING,facing).setValue(ChestBlock.TYPE,type.getOpposite()));
    }
}
