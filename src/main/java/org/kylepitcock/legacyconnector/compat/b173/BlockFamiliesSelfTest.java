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
        int stairs=0,torches=0,doors=0,conveyors=0,plates=0,furnaces=0,chests=0,fences=0;
        int failures=LegacyB173Block.CALLBACK_FAILURES.get();
        var assets=new java.util.HashMap<String,java.util.function.Supplier<byte[]>>();
        new AssetsB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,assets);
        try {
            for(var block:ContentB173.blocks()) {
                if(!(block.mcc$modern instanceof LegacyB173Block))continue;
                if(block.getRenderType()==11 || block.getClass().getSimpleName().equals("BlockPoleFence")){
                    clear(level,pos);place(level,player,block.id,pos.below(),Direction.UP);
                    require(level.getBlockState(pos).is(block.mcc$modern),"actual fence placement");
                    int renderFailures=BlockRenderB173.FAILURES.get();
                    var isolated=BlockRenderB173.capture(block,new BlockQueryB173(level,pos,level.getBlockState(pos)),pos.getX(),pos.getY(),pos.getZ());
                    require(!isolated.isEmpty()&&isolated.stream().allMatch(v->v.x()>=pos.getX()+.375 && v.x()<=pos.getX()+.625 && v.z()>=pos.getZ()+.375 && v.z()<=pos.getZ()+.625),"isolated fence renders a post "+block.getClass());
                    for(var direction:java.util.List.of(Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST)){
                        var neighbor=pos.relative(direction);level.setBlock(neighbor,BlocksB173.modernState(block.id,0),2);
                        var connected=BlockRenderB173.capture(block,new BlockQueryB173(level,pos,level.getBlockState(pos)),pos.getX(),pos.getY(),pos.getZ());
                        require(connected.size()>=isolated.size(),"fence retains post and connection rails "+direction);
                        // IC2 draws complete beams toward south/east, reaching inside the next post.
                        var pair=new java.util.ArrayList<>(connected);
                        pair.addAll(BlockRenderB173.capture(block,new BlockQueryB173(level,neighbor,level.getBlockState(neighbor)),neighbor.getX(),neighbor.getY(),neighbor.getZ()));
                        require(block.getRenderType()==11
                                ? connected.stream().anyMatch(v->switch(direction){case NORTH->v.z()==pos.getZ();case SOUTH->v.z()==pos.getZ()+1;case WEST->v.x()==pos.getX();default->v.x()==pos.getX()+1;})
                                : pair.size()>2*isolated.size(),"pair renders connecting beams "+block.getClass()+" "+direction);
                        level.setBlock(neighbor,Blocks.AIR.defaultBlockState(),2);
                    }
                    if(block.getRenderType()==11)require(level.getBlockState(pos).getCollisionShape(level,pos).bounds().maxY==1.5,"Beta fence prevents jumping over it");
                    require(BlockRenderB173.FAILURES.get()==renderFailures,"actual fence renderer completes");fences++;
                }
                if(block instanceof PressurePlateBlock){
                    clear(level,pos);place(level,player,block.id,pos.below(),Direction.UP);
                    // Earlier support-removal cases intentionally drop items at this shared test position.
                    level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
                    require(level.getBlockState(pos).is(block.mcc$modern),"actual pressure plate places");
                    require(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"plate has no collision");
                    require(level.getBlockState(pos).getShape(level,pos).bounds().maxY==0.0625,"unpressed plate outline");
                    var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
                    var mob=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
                    try{
                        for(var entity:java.util.List.of(item,mob,player)){
                            entity.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
                            level.getBlockState(pos).entityInside(level,pos,entity);
                            var pressed=level.getBlockState(pos);require(pressed.getValue(LegacyB173Block.META)==1,"plate activates for "+entity.getClass());
                            require(pressed.getShape(level,pos).bounds().maxY==0.03125,"pressed plate outline");
                            for(Direction side:Direction.values()){
                                require(pressed.getSignal(level,pos,side)==15,"plate weak power");
                                require(pressed.getDirectSignal(level,pos,side)==(side==Direction.UP?15:0),"plate strong power only into support");
                            }
                            entity.setPos(pos.getX()+4,pos.getY(),pos.getZ()+4);
                            pressed.tick(level,pos,level.random);require(level.getBlockState(pos).getValue(LegacyB173Block.META)==0,"plate releases on scheduled callback");
                        }
                        item.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);level.addFreshEntity(item);
                        level.getBlockState(pos).entityInside(level,pos,item);level.getBlockState(pos).tick(level,pos,level.random);
                        require(level.getBlockState(pos).getValue(LegacyB173Block.META)==1,"plate stays pressed while item remains");
                        require(level.getBlockTicks().hasScheduledTick(pos,block.mcc$modern),"plate schedules release checks");
                        require(model(assets,block,0).getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble()==1,"plate model 1 pixel high");
                        require(model(assets,block,1).getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble()==0.5,"pressed model half pixel high");
                        level.removeBlock(pos.below(),false);require(level.getBlockState(pos).isAir(),"plate drops without support");
                    }finally{item.discard();mob.discard();}
                    plates++;
                }
                if(block.getClass().getSimpleName().equals("BlockNFurnace")){
                    for(int yaw=0;yaw<4;yaw++){
                        clear(level,pos);player.setYRot(yaw*90);place(level,player,block.id,pos.below(),Direction.UP);
                        var state=level.getBlockState(pos);int meta=new int[]{2,5,3,4}[yaw];
                        require(state.is(block.mcc$modern) && state.getValue(LegacyB173Block.META)==meta,"actual furnace facing");
                        int[] faces=AssetsB173.faceTextures(block,level,pos,state);require(faces[meta]!=faces[meta^1],"furnace front differs from back");
                        var textures=model(assets,block,meta).getAsJsonObject("textures");
                        require(textures.get(Direction.from3DDataValue(meta).getName()).getAsString().equals(AssetsB173.worldTextures(block).get(faces[meta])),"furnace generated front follows placement");
                    }furnaces++;
                }
                if(block.getClass().getSimpleName().equals("BlockNChest")){
                    for(Direction wall:java.util.List.of(Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST)){
                        clear(level,pos);level.setBlock(pos.relative(wall),Blocks.STONE.defaultBlockState(),2);place(level,player,block.id,pos.below(),Direction.UP);
                        int[] faces=AssetsB173.faceTextures(block,level,pos,level.getBlockState(pos));
                        require(faces[wall.getOpposite().get3DDataValue()]!=faces[wall.get3DDataValue()],"chest front faces away from wall "+wall);
                        for(int face:faces)require(AssetsB173.worldTextures(block).containsKey(Math.abs(face)),"chest face available in atlas");
                    }
                    clear(level,pos);place(level,player,block.id,pos.below(),Direction.UP);
                    int[] single=AssetsB173.faceTextures(block,level,pos,level.getBlockState(pos));
                    level.setBlock(pos.east().below(),Blocks.STONE.defaultBlockState(),2);
                    place(level,player,block.id,pos.east().below(),Direction.UP);
                    require(level.getBlockState(pos.east()).is(block.mcc$modern),"second chest placed beside first");
                    int[] paired=AssetsB173.faceTextures(block,level,pos,level.getBlockState(pos));
                    require(!java.util.Arrays.equals(single,paired),"double chest chooses paired textures");
                    for(int face:paired)require(AssetsB173.worldTextures(block).containsKey(Math.abs(face)),"paired chest face in atlas");
                    doubleChest(level,player,pos,pos.east(),block);
                    clear(level,pos);level.setBlock(pos.south().below(),Blocks.STONE.defaultBlockState(),2);
                    place(level,player,block.id,pos.below(),Direction.UP);place(level,player,block.id,pos.south().below(),Direction.UP);
                    doubleChest(level,player,pos,pos.south(),block);
                    chests++;
                }
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
                    for(int meta=0;meta<12;meta++){
                        var state=BlocksB173.modernState(block.id,meta);
                        var vertices=BlockRenderB173.capture(block,new BlockQueryB173(level,pos,state),pos.getX(),pos.getY(),pos.getZ());
                        require(vertices.size()==8,"original conveyor renderer emits both belt faces "+meta);
                        var start=vertices.get(0);var end=vertices.get(1);int dir=meta&3;
                        double dx=end.x()-start.x(),dz=end.z()-start.z();
                        require((dir==0?dx:dir==1?dz:dir==2?-dx:-dz)>0.99,"original belt UV V direction agrees with motion "+meta);
                        double low=vertices.stream().mapToDouble(v->v.y()).min().orElseThrow(),high=vertices.stream().mapToDouble(v->v.y()).max().orElseThrow();
                        require(Math.abs(high-low-(meta<4?0:1))<0.0001,"original slope geometry "+meta);
                        require(Math.abs(start.u()-end.u())<1e-6 && end.v()>start.v(),"belt texture retains original UVs");
                    }
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
        LOG.info("B1.7.3 ACTUAL BLOCK FAMILIES TEST: {} stairs, {} torches, {} doors, {} conveyors, {} pressure plates, {} furnaces, {} chests (both double-chest axes), {} fences passed",stairs,torches,doors,conveyors,plates,furnaces,chests,fences);
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
    private static void doubleChest(ServerLevel level,ServerPlayer player,BlockPos firstPos,BlockPos secondPos,Block block){
        var first=(legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory)((LegacyB173BlockEntity)level.getBlockEntity(firstPos)).legacy();
        var second=(legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory)((LegacyB173BlockEntity)level.getBlockEntity(secondPos)).legacy();
        player.setPos(firstPos.getX()+.5,firstPos.getY(),firstPos.getZ()+2.5);
        try{
            for(var clicked:java.util.List.of(firstPos,secondPos)){
                player.getInventory().clearContent();for(int i=0;i<27;i++){first.setStack(i,null);second.setStack(i,null);}
                first.setStack(0,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(263,4,0));
                second.setStack(0,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(265,6,0));
                level.getBlockState(clicked).useWithoutItem(level,player,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(clicked),Direction.UP,clicked,false));
                var menu=player.containerMenu;require(menu instanceof net.minecraft.world.inventory.ChestMenu && menu.slots.size()==90,"actual NetherCraft double chest opens 54 slots from either half");
                require(menu.stillValid(player),"combined chest remains usable");
                require(menu.getSlot(0).getItem().is(net.minecraft.world.item.Items.COAL)&&menu.getSlot(27).getItem().is(net.minecraft.world.item.Items.IRON_INGOT),"combined inventory preserves half order");
                menu.quickMoveStack(player,27);require(second.getStack(0)==null && player.getInventory().countItem(net.minecraft.world.item.Items.IRON_INGOT)==6,"shift extraction writes back to second half");
                player.getInventory().setItem(9,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
                menu.quickMoveStack(player,54);require(first.getStack(1)!=null&&first.getStack(1).itemId==264&&first.getStack(1).count==3,"shift insertion writes back to first half");
                player.closeContainer();
            }
            for(var covered:java.util.List.of(firstPos,secondPos)){
                level.setBlock(covered.above(),Blocks.STONE.defaultBlockState(),3);
                level.getBlockState(firstPos).useWithoutItem(level,player,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(firstPos),Direction.UP,firstPos,false));
                require(player.containerMenu==player.inventoryMenu,"solid block above either chest prevents opening");level.removeBlock(covered.above(),false);
            }
        }finally{player.closeContainer();for(int i=0;i<27;i++){first.setStack(i,null);second.setStack(i,null);}}
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
