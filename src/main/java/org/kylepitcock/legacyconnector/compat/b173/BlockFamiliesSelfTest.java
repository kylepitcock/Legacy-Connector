package org.kylepitcock.legacyconnector.compat.b173;

import org.kylepitcock.legacyconnector.vb1_7_3.*;
import legacyconnector.vb1_7_3.net.minecraft.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real installed mod blocks/items through modern placement, shape, click and collision entry points. */
public final class BlockFamiliesSelfTest {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    public static void run(ServerLevel level,ServerPlayer player,BlockPos base) {
        BlockPos pos=base.offset(512,-20,8);
        var saved=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++)for(int z=-2;z<=2;z++){var p=pos.offset(x,y,z);saved.put(p,level.getBlockState(p));}
        int stairs=0,torches=0,doors=0,conveyors=0;
        int failures=LegacyB173Block.CALLBACK_FAILURES.get();
        var assets=new java.util.HashMap<String,java.util.function.Supplier<byte[]>>();
        new AssetsB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,assets);
        try {
            for(var block:ContentB173.blocks()) {
                if(!(block.mcc$modern instanceof LegacyB173Block))continue;
                if(block instanceof StairsBlock) {
                    for(int yaw=0;yaw<4;yaw++) {
                        clear(level,pos);player.setYRot(yaw*90);place(level,player,block.id,pos.below(),Direction.UP);
                        var state=level.getBlockState(pos);require(state.is(block.mcc$modern),"stair placed");
                        int meta=new int[]{2,1,3,0}[yaw];require(state.getValue(LegacyB173Block.META)==meta,"stair orientation");
                        var shape=state.getCollisionShape(level,pos);double volume=shape.toAabbs().stream().mapToDouble(b->b.getXsize()*b.getYsize()*b.getZsize()).sum();
                        require(Math.abs(volume-0.75)<0.0001,"stair has two steps");
                        require(!shape.toAabbs().stream().anyMatch(b->b.contains(meta==0?0.25:meta==1?0.75:0.5,0.75,meta==2?0.25:meta==3?0.75:0.5)),"stair upper gap follows facing");
                        require(model(assets,block,meta).get("parent").getAsString().equals("minecraft:block/stairs"),"stair model");
                    } stairs++;
                } else if(block instanceof TorchBlock || block.getClass().getSimpleName().equals("BlockAmbrosiumTorch")) {
                    for(Direction face:new Direction[]{Direction.UP,Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH}) {
                        clear(level,pos);var support=pos.relative(face.getOpposite());level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
                        place(level,player,block.id,support,face);
                        var state=level.getBlockState(pos);require(state.is(block.mcc$modern),"torch placed "+block.getClass()+" "+face);
                        int meta=switch(face){case UP->5;case EAST->1;case WEST->2;case SOUTH->3;case NORTH->4;default->0;};
                        require(state.getValue(LegacyB173Block.META)==meta,"torch attachment");
                        require(state.getCollisionShape(level,pos).isEmpty(),"torch has no collision");
                        require(state.getShape(level,pos).bounds().getXsize()<1,"torch selection bounds");
                        require(model(assets,block,meta).get("parent").getAsString().endsWith(face==Direction.UP?"template_torch":"template_torch_wall"),"torch model orientation");
                        level.removeBlock(support,false);require(level.getBlockState(pos).isAir(),"unsupported torch drops");
                    } torches++;
                } else if(block.getClass().getSimpleName().equals("BlockFactoryConveyor")) {
                    conveyor(level,player,pos,block);conveyors++;
                }
            }
            for(var item:ContentB173.items()) {
                if(!java.util.Set.of("ItemNDoor","ItemIC2Door").contains(item.getClass().getSimpleName()))continue;
                clear(level,pos);place(level,player,item.id,pos.below(),Direction.UP);
                var lower=level.getBlockState(pos);require(lower.getBlock() instanceof LegacyB173Block,"door item places lower half");
                var door=((LegacyB173Block)lower.getBlock()).legacy();require(door instanceof DoorBlock,"door shim used");
                require(level.getBlockState(pos.above()).is(door.mcc$modern) && (level.getBlockState(pos.above()).getValue(LegacyB173Block.META)&8)!=0,"door upper half");
                var before=lower.getCollisionShape(level,pos).bounds();require(Math.min(before.getXsize(),before.getZsize())==0.1875,"door thickness");
                var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos.above()),Direction.NORTH,pos.above(),false);
                level.getBlockState(pos.above()).useWithoutItem(level,player,hit);
                boolean metal=door.material==legacyconnector.vb1_7_3.net.minecraft.block.material.Material.METAL;
                require(((level.getBlockState(pos).getValue(LegacyB173Block.META)&4)!=0)==!metal,"door click respects material");
                if(!metal) {
                    require((level.getBlockState(pos.above()).getValue(LegacyB173Block.META)&4)!=0,"door halves open together");
                    require(!before.equals(level.getBlockState(pos).getCollisionShape(level,pos).bounds()),"door collision rotates");
                }
                level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
                require((level.getBlockState(pos).getValue(LegacyB173Block.META)&4)!=0,"door redstone opens");
                level.removeBlock(pos.east(),false);
                require((level.getBlockState(pos).getValue(LegacyB173Block.META)&4)==0,"door redstone closes");
                require(model(assets,door,0).has("elements") && model(assets,door,8).has("elements"),"door models are thin halves");
                level.removeBlock(pos.below(),false);
                require(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir(),"door support loss removes both halves");
                doors++;
            }
            require(LegacyB173Block.CALLBACK_FAILURES.get()==failures,"no failed block callbacks");
        } finally { saved.forEach((p,state)->level.setBlock(p,state,2));player.setShiftKeyDown(false);player.getInventory().clearContent(); }
        LOG.info("B1.7.3 ACTUAL BLOCK FAMILIES TEST: {} stairs, {} torches, {} doors, {} conveyors passed",stairs,torches,doors,conveyors);
    }
    private static void conveyor(ServerLevel level,ServerPlayer player,BlockPos pos,Block block) {
        clear(level,pos);
        var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+0.5,pos.getY()+0.1,pos.getZ()+0.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
        var mob=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        try {
            for(var entity:java.util.List.of(item,mob,player))for(int meta=0;meta<12;meta++) {
                level.setBlock(pos,BlocksB173.modernState(block.id,meta),2);
                entity.setPos(pos.getX()+0.5,pos.getY()+0.1,pos.getZ()+0.5);entity.setDeltaMovement(Vec3.ZERO);
                level.getBlockState(pos).entityInside(level,pos,entity);
                var motion=entity.getDeltaMovement();int dir=meta&3;
                require((dir==0?motion.x:dir==1?motion.z:dir==2?-motion.x:-motion.z)>0.19,"conveyor direction "+meta+" "+entity.getClass());
                require((meta>=4 && meta<=7)?motion.y>0:motion.y==0,"conveyor slope "+meta);
                require(level.getBlockState(pos).getCollisionShape(level,pos).bounds().maxY<0.2,"conveyor thin collision");
            }
            level.setBlock(pos,BlocksB173.modernState(block.id,0),2);
            player.setShiftKeyDown(true);player.setDeltaMovement(Vec3.ZERO);level.getBlockState(pos).entityInside(level,pos,player);
            require(player.getDeltaMovement().equals(Vec3.ZERO),"sneaking bypasses conveyor");player.setShiftKeyDown(false);
            level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
            item.setDeltaMovement(Vec3.ZERO);level.getBlockState(pos).entityInside(level,pos,item);
            require(item.getDeltaMovement().equals(Vec3.ZERO),"powered conveyor stops");level.removeBlock(pos.east(),false);
            level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(pos.east().above(),Blocks.LEVER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeverBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                    .setValue(net.minecraft.world.level.block.LeverBlock.POWERED,true),3);
            var world=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level);
            require(world.canTransferPower(pos.getX()+1,pos.getY(),pos.getZ()),"solid support receives strong power");
            require(!world.canTransferPower(pos.getX(),pos.getY(),pos.getZ()) && world.isEmittingRedstonePower(pos.getX(),pos.getY(),pos.getZ()),"strong and indirect Beta queries remain distinct");
            item.setDeltaMovement(Vec3.ZERO);level.getBlockState(pos).entityInside(level,pos,item);
            require(item.getDeltaMovement().equals(Vec3.ZERO),"indirectly powered conveyor stops");
            level.removeBlock(pos.east().above(),false);level.removeBlock(pos.east(),false);
            item.setPos(pos.getX()+0.5,pos.getY()+0.1,pos.getZ()+0.5);item.setNoGravity(true);
            double start=item.getX();for(int tick=0;tick<3;tick++)item.tick();
            require(item.getX()>start+0.1,"actual item ticks move along conveyor");
        } finally { item.discard();mob.discard();player.setShiftKeyDown(false); }
    }
    private static void clear(ServerLevel level,BlockPos pos){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)level.setBlock(pos.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
    }
    private static void place(ServerLevel level,ServerPlayer player,int id,BlockPos support,Direction face){
        player.getInventory().selected=0;player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(id,1,0));
        var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(support),face,support,false);
        player.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,hit));
    }
    private static com.google.gson.JsonObject model(java.util.Map<String,java.util.function.Supplier<byte[]>> assets,Block block,int meta){
        var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.mcc$modern);
        var states=json(assets,"assets/"+id.getNamespace()+"/blockstates/"+id.getPath()+".json");
        String name=states.getAsJsonObject("variants").getAsJsonObject("meta="+meta).get("model").getAsString();
        return json(assets,"assets/"+name.replace(":","/models/")+".json");
    }
    private static com.google.gson.JsonObject json(java.util.Map<String,java.util.function.Supplier<byte[]>> assets,String name){require(assets.containsKey(name),"generated asset "+name);return com.google.gson.JsonParser.parseString(new String(assets.get(name).get(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
