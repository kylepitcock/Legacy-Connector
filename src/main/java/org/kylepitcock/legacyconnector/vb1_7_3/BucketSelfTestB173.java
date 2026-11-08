package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Actual BuildCraft oil pickup/placement through native buckets and the unmodified mod classes. */
final class BucketSelfTestB173 {
    static void run(ServerLevel level,ServerPlayer player,BlockPos base){
        var oil=ContentB173.blocks().stream().filter(b->b.getClass().getSimpleName().equals("BlockOilStill")&&b.mcc$modern!=null).findFirst();
        var bucket=ContentB173.items().stream().filter(i->i.getClass().getSimpleName().equals("ItemBucketOil")&&i.mcc$modern!=null).findFirst();
        if(oil.isEmpty()||bucket.isEmpty())return;
        var pos=base.offset(0,0,12);var support=pos.below();
        var old=level.getBlockState(pos);var oldSupport=level.getBlockState(support);
        boolean creative=player.getAbilities().instabuild;
        boolean mayBuild=player.getAbilities().mayBuild;
        try{
            player.getAbilities().instabuild=false;player.getInventory().clearContent();
            require(bucket.get().getMaxCount()==1,"vanilla bucket constructor is unstackable");
            require(bucket.get().getCraftingReturnItem()!=null&&bucket.get().getCraftingReturnItem().id==325,"original crafting remainder");
            var state=oil.get().mcc$modern.defaultBlockState();
            level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
            level.setBlock(pos,state.setValue(LegacyB173Block.META,1),3);
            aim(player,pos);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            var flowing=Items.BUCKET.use(level,player,InteractionHand.MAIN_HAND);
            require(flowing.getObject().is(Items.BUCKET)&&level.getBlockState(pos).getValue(LegacyB173Block.META)==1,"original handler refuses flowing metadata");
            level.setBlock(pos,state,3);
            var filled=Items.BUCKET.use(level,player,InteractionHand.MAIN_HAND);
            require(filled.getObject().is(bucket.get().mcc$modern)&&level.isEmptyBlock(pos),"native bucket raycast calls original oil handler");
            player.setItemInHand(InteractionHand.MAIN_HAND,filled.getObject());
            aim(player,pos);
            bucket.get().mcc$modern.use(level,player,InteractionHand.MAIN_HAND);
            require(player.getMainHandItem().is(Items.BUCKET),"filled bucket returns empty bucket");
            require(level.getBlockState(pos).getBlock() instanceof LegacyB173Block placed && placed.legacy().getClass().getSimpleName().equals("BlockOilFlowing"),"filled bucket places its declared fluid");
            player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET,3));
            var stacked=Items.BUCKET.use(level,player,InteractionHand.MAIN_HAND);
            require(stacked.getObject().is(Items.BUCKET)&&stacked.getObject().getCount()==2
                    &&player.getInventory().contains(new ItemStack(bucket.get().mcc$modern))&&level.isEmptyBlock(pos),"stacked empty buckets keep extras and receive original filled result");
            player.getInventory().clearContent();player.getAbilities().instabuild=true;
            level.setBlock(pos,state,3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            var infinite=Items.BUCKET.use(level,player,InteractionHand.MAIN_HAND);
            require(infinite.getObject().is(Items.BUCKET)&&infinite.getObject().getCount()==1
                    &&player.getInventory().contains(new ItemStack(bucket.get().mcc$modern))&&level.isEmptyBlock(pos),"creative native pickup retains empty bucket");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(bucket.get().mcc$modern));
            bucket.get().mcc$modern.use(level,player,InteractionHand.MAIN_HAND);
            require(player.getMainHandItem().is(bucket.get().mcc$modern)&&!level.isEmptyBlock(pos),"creative placement retains filled bucket");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            player.getAbilities().instabuild=false;player.getAbilities().mayBuild=false;
            bucket.get().mcc$modern.use(level,player,InteractionHand.MAIN_HAND);
            require(player.getMainHandItem().is(bucket.get().mcc$modern)&&level.isEmptyBlock(pos),"denied placement changes neither world nor bucket");
            player.getAbilities().mayBuild=mayBuild;
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            var noFill=legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge.fillCustomBucket(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level),pos.getX(),pos.getY(),pos.getZ());
            require(noFill==null&&level.getBlockState(pos).is(Blocks.STONE),"unhandled solid block remains unchanged");
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 FORGE BUCKET TEST: original oil source pickup, flowing rejection, inherited placement, remainder, stacked/creative buckets and denied placement passed");
        }finally{player.getAbilities().instabuild=creative;player.getAbilities().mayBuild=mayBuild;player.getInventory().clearContent();level.setBlock(pos,old,3);level.setBlock(support,oldSupport,3);}
    }
    private static void aim(ServerPlayer player,BlockPos pos){player.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);player.setYRot(0);player.setXRot(90);}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("B1.7.3 FORGE BUCKET TEST failed: "+detail);}
}
