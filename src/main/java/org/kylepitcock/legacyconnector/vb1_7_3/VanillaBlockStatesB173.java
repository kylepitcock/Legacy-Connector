package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/** Beta metadata fields on native states; placement and paired-block updates belong to the World bridge. */
public final class VanillaBlockStatesB173 {
    private VanillaBlockStatesB173(){}

    /** Null means this utility does not handle the id. Existing unrelated state properties are retained. */
    public static BlockState modernState(int id,int meta,BlockState base){
        if(id==84&&base.is(Blocks.JUKEBOX))return base.setValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD,meta!=0);
        if(id==54&&base.is(Blocks.CHEST))return base.setValue(net.minecraft.world.level.block.ChestBlock.FACING,
                switch(meta&15){case 3->Direction.SOUTH;case 4->Direction.WEST;case 5->Direction.EAST;default->Direction.NORTH;});
        if(isTorch(id,base)){
            meta&=15;
            var torch=id==50?(meta>=1&&meta<=4?Blocks.WALL_TORCH:Blocks.TORCH)
                    :(meta>=1&&meta<=4?Blocks.REDSTONE_WALL_TORCH:Blocks.REDSTONE_TORCH);
            BlockState state=base.is(torch)?base:torch.defaultBlockState();
            if(state.hasProperty(WallTorchBlock.FACING))state=state.setValue(WallTorchBlock.FACING,
                    switch(meta){case 1->Direction.EAST;case 2->Direction.WEST;case 3->Direction.SOUTH;default->Direction.NORTH;});
            return id==50?state:state.setValue(RedstoneTorchBlock.LIT,id==76);
        }
        if((id==53&&base.is(Blocks.OAK_STAIRS))||(id==67&&base.is(Blocks.COBBLESTONE_STAIRS))){
            Direction facing=switch(meta&3){case 0->Direction.EAST;case 1->Direction.WEST;case 2->Direction.SOUTH;default->Direction.NORTH;};
            // Upside-down stairs were added after Beta; bit four is not a top-half flag here.
            return base.setValue(StairBlock.FACING,facing).setValue(StairBlock.HALF,Half.BOTTOM);
        }
        if((id==64&&base.is(Blocks.OAK_DOOR))||(id==71&&base.is(Blocks.IRON_DOOR))){
            Direction facing=switch(meta&3){case 0->Direction.EAST;case 1->Direction.SOUTH;case 2->Direction.WEST;default->Direction.NORTH;};
            // Both Beta halves carry facing/open; unlike later versions the upper bits do not encode hinge/power.
            return base.setValue(DoorBlock.HALF,(meta&8)==0?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER)
                    .setValue(DoorBlock.FACING,facing).setValue(DoorBlock.OPEN,(meta&4)!=0);
        }
        return null;
    }

    /** Only the original native torch families; mod-owned blocks keep their own metadata callbacks. */
    public static boolean isTorch(int id,BlockState state){
        return id==50&&(state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH))
                ||(id==75||id==76)&&(state.is(Blocks.REDSTONE_TORCH)||state.is(Blocks.REDSTONE_WALL_TORCH));
    }

    /** Null means unsupported. Do not interpret modern upper-door hinge bits as Beta metadata. */
    public static Integer legacyMeta(BlockState state){
        if(state.is(Blocks.JUKEBOX))return state.getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD)?1:0;
        if(state.is(Blocks.CHEST))return switch(state.getValue(net.minecraft.world.level.block.ChestBlock.FACING)){
            case NORTH->2;case SOUTH->3;case WEST->4;case EAST->5;default->throw new IllegalStateException("Nonhorizontal chest facing");};
        if(state.is(Blocks.TORCH)||state.is(Blocks.REDSTONE_TORCH))return 5;
        if(state.is(Blocks.WALL_TORCH)||state.is(Blocks.REDSTONE_WALL_TORCH))
            return switch(state.getValue(WallTorchBlock.FACING)){case EAST->1;case WEST->2;case SOUTH->3;case NORTH->4;default->throw new IllegalStateException("Nonhorizontal torch facing");};
        if(state.is(Blocks.OAK_STAIRS)||state.is(Blocks.COBBLESTONE_STAIRS)){
            return switch(state.getValue(StairBlock.FACING)){case EAST->0;case WEST->1;case SOUTH->2;case NORTH->3;default->throw new IllegalStateException("Nonhorizontal stair facing");};
        }
        if(state.is(Blocks.OAK_DOOR)||state.is(Blocks.IRON_DOOR)){
            int facing=switch(state.getValue(DoorBlock.FACING)){case EAST->0;case SOUTH->1;case WEST->2;case NORTH->3;default->throw new IllegalStateException("Nonhorizontal door facing");};
            return facing|(state.getValue(DoorBlock.OPEN)?4:0)|(state.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?8:0);
        }
        return null;
    }
}
