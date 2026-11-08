package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Original jail construction with real native neighbor updates, unlike a block-snapshot capture. */
public final class MoreCreepsJailSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173JailTest");
    private MoreCreepsJailSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS JAIL TEST failed",error);}
    });}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException{field(object,name).set(object,value);}
    private static final class FixedRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:jail-state".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Jail");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private record Saved(net.minecraft.world.level.block.state.BlockState state,net.minecraft.nbt.CompoundTag tile){}
    private static void door(ServerLevel level,World world,BlockPos pos,DoorBlock block){
        var lower=level.getBlockState(pos);var upper=level.getBlockState(pos.above());
        require(lower.is(block)&&upper.is(block),"actual jail keeps both native door blocks: "+pos);
        require(lower.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&upper.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER,"actual jail lower/upper metadata survive native neighbor updates");
        for(var state:List.of(lower,upper))require(state.getValue(DoorBlock.FACING)==Direction.EAST&&!state.getValue(DoorBlock.OPEN)
                &&state.getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT,"actual jail metadata zero/eight yields matching closed east-facing left-hinge pair");
        require(world.getBlockMeta(pos.getX(),pos.getY(),pos.getZ())==0&&world.getBlockMeta(pos.getX(),pos.getY()+1,pos.getZ())==8,"jail door metadata round-trips through legacy view");
    }
    private static void torch(ServerLevel level,World world,BlockPos pos,int meta,Direction facing){
        var state=level.getBlockState(pos);
        require(state.is(facing==Direction.UP?Blocks.TORCH:Blocks.WALL_TORCH),"legacy torch uses native supported torch family: "+pos+" state="+state);
        if(facing!=Direction.UP)require(state.getValue(WallTorchBlock.FACING)==facing,"original wall torch preserves Beta support direction: "+pos);
        require(state.canSurvive(level,pos)&&state.getLightEmission()==14,"original jail torch has native support and light emission14: "+pos);
        require(world.getBlockId(pos.getX(),pos.getY(),pos.getZ())==50&&world.getBlockMeta(pos.getX(),pos.getY(),pos.getZ())==meta,"native torch maps back to Beta id50/meta"+meta+": "+pos);
    }
    private static int torchDrops(ServerLevel level,BlockPos pos){return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1.5)).stream().filter(e->e.getItem().is(net.minecraft.world.item.Items.TORCH)).mapToInt(e->e.getItem().getCount()).sum();}
    private static void torches(ServerLevel level,World world,BlockPos origin){
        // Original Lawyer places id50 with implicit metadata0 at these three coordinates.
        // Clean Beta TorchBlock.onAdded chooses WEST/EAST/NORTH/SOUTH/floor support in that order.
        var positions=List.of(origin.offset(0,2,13),origin.offset(12,2,1),origin.offset(6,2,9));
        int[] metas={1,2,4};Direction[] facings={Direction.EAST,Direction.WEST,Direction.NORTH};
        for(int i=0;i<positions.size();i++)torch(level,world,positions.get(i),metas[i],facings[i]);
        var rule=level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);boolean before=rule.get();rule.set(true,level.getServer());
        try{
            for(int i=0;i<positions.size();i++){
                var pos=positions.get(i);int drops=torchDrops(level,pos);var support=pos.relative(facings[i].getOpposite());
                require(level.getBlockState(support).is(Blocks.STONE),"actual original jail torch attaches to stone wall");
                require(level.setBlock(support,Blocks.AIR.defaultBlockState(),3),"native wall support removal admitted");
                require(level.getBlockState(pos).isAir()&&torchDrops(level,pos)==drops+1,"native support loss removes original torch and drops one torch item");
            }
            // Reuse a bounded empty cell inside the fixture's saved region for all four wall directions and floor.
            var pos=origin.offset(18,19,14);Direction[] directions={Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH,Direction.UP};
            for(int i=0;i<directions.length;i++){
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);for(Direction direction:Direction.values())level.setBlock(pos.relative(direction),Blocks.AIR.defaultBlockState(),3);
                var facing=directions[i];level.setBlock(pos.relative(facing.getOpposite()),Blocks.STONE.defaultBlockState(),3);
                require(world.setBlock(pos.getX(),pos.getY(),pos.getZ(),50),"legacy metadata0 torch placement admitted for support "+facing);
                torch(level,world,pos,i+1,facing);
                world.setBlockMeta(pos.getX(),pos.getY(),pos.getZ(),i+1);torch(level,world,pos,i+1,facing);
            }
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);for(Direction direction:Direction.values())level.setBlock(pos.relative(direction),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
            require(world.setBlock(pos.getX(),pos.getY(),pos.getZ(),50),"legacy torch chooses wall before available floor");torch(level,world,pos,1,Direction.EAST);
            world.setBlockMeta(pos.getX(),pos.getY(),pos.getZ(),5);torch(level,world,pos,5,Direction.UP);
            world.setBlockMeta(pos.getX(),pos.getY(),pos.getZ(),1);torch(level,world,pos,1,Direction.EAST);
            require(level.setBlock(pos,Blocks.TORCH.defaultBlockState(),3),"direct native standing torch placement admitted");torch(level,world,pos,5,Direction.UP);
            require(!world.setBlock(pos.getX(),pos.getY(),pos.getZ(),50),"same legacy torch id write remains a no-op");torch(level,world,pos,5,Direction.UP);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.west(),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos.below(),Blocks.OAK_FENCE.defaultBlockState(),3);
            require(world.setBlock(pos.getX(),pos.getY(),pos.getZ(),50),"original fence supports floor torch");torch(level,world,pos,5,Direction.UP);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);for(Direction direction:Direction.values())level.setBlock(pos.relative(direction),Blocks.AIR.defaultBlockState(),3);
            int unsupportedDrops=torchDrops(level,pos);world.setBlock(pos.getX(),pos.getY(),pos.getZ(),50);
            require(level.getBlockState(pos).isAir()&&torchDrops(level,pos)==unsupportedDrops+1,"unsupported original torch placement drops exactly one torch and leaves native air");
            // Ordinary native stone remains an ordinary stone block through the same legacy write API.
            require(world.setBlock(pos.getX(),pos.getY(),pos.getZ(),1)&&level.getBlockState(pos).is(Blocks.STONE),"non-torch legacy placement keeps native block identity");
        }finally{rule.set(before,level.getServer());}
    }
    private static void run(ServerLevel level)throws ReflectiveOperationException {
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&r.type().getSimpleName().equals("CREEPSEntityLawyerFromHell")).findFirst();if(selected.isEmpty())return;
        var registration=selected.get();var world=World.mcc$wrap(level);var origin=level.getSharedSpawnPos().offset(8,60,8);
        var before=new HashSet<Integer>();for(var entity:level.getAllEntities())before.add(entity.getId());
        var saved=new LinkedHashMap<BlockPos,Saved>();
        for(int x=-2;x<=19;x++)for(int z=-1;z<=16;z++)for(int y=-2;y<=21;y++){
            var pos=origin.offset(x,y,z);level.getChunk(pos);var tile=level.getBlockEntity(pos);
            saved.put(pos,new Saved(level.getBlockState(pos),tile==null?null:tile.saveWithFullMetadata(level.registryAccess())));
        }
        var globals=new LinkedHashMap<Field,Object>();var mod=registration.mod().instance;
        for(String name:List.of("jailBuilt","currentJailX","currentJailY","currentJailZ","currentfine")){var f=field(mod,name);globals.put(f,f.get(null));}
        var player=actor(level);
        try{
            require(!level.captureBlockSnapshots,"jail regression must exercise live native neighbor updates");
            player.setPos(origin.getX()+7,origin.getY()+2,origin.getZ()+7);level.addNewPlayer(player);
            var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(world);
            original.setPosition(origin.getX()+1,origin.getY()+2,origin.getZ()+1);require(world.spawnEntity(original),"actual Lawyer native host admitted");put(original,"random",new FixedRandom());put(original,"area",0);
            put(mod,"jailBuilt",true);put(mod,"currentJailX",origin.getX());put(mod,"currentJailY",origin.getY());put(mod,"currentJailZ",origin.getZ());put(mod,"currentfine",3000);
            final boolean[] built={false};
            try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,PlayerViewB173.of(player));var views=EntityViewsB173.callbackScope()){
                MenusB173.withActor(player,()->{try{built[0]=(Boolean)original.getClass().getMethod("buildJail").invoke(original);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}});
            }
            require(built[0],"actual Lawyer buildJail accepted controlled loaded reuse site");
            var wood=origin.offset(3,0,8);var iron=origin.offset(13,0,7);door(level,world,wood,(DoorBlock)Blocks.OAK_DOOR);door(level,world,iron,(DoorBlock)Blocks.IRON_DOOR);
            int[] metas={3,0,2,1};Direction[] facings={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
            for(int i=0;i<18;i++){
                int slot=i%4;var pos=origin.offset(15+(slot==1||slot==2?1:0),i,7+(slot>=2?1:0));
                var state=level.getBlockState(pos);int meta=i==0?0:metas[slot];var facing=i==0?Direction.EAST:facings[slot];
                require(state.is(Blocks.COBBLESTONE_STAIRS)&&state.getValue(StairBlock.FACING)==facing&&state.getValue(StairBlock.HALF)==Half.BOTTOM,"actual eighteen-step jail spiral preserves legacy orientation at level "+i);
                require(world.getBlockMeta(pos.getX(),pos.getY(),pos.getZ())==meta,"actual stair facing reads back original metadata at level "+i);
            }
            world.setBlockMeta(wood.getX(),wood.getY(),wood.getZ(),4);
            require(level.getBlockState(wood).getValue(DoorBlock.OPEN)&&level.getBlockState(wood.above()).getValue(DoorBlock.OPEN),"legacy door-open bit updates native pair");
            ((DoorBlock)Blocks.OAK_DOOR).setOpen(player,level,level.getBlockState(wood),wood,false);
            require(world.getBlockMeta(wood.getX(),wood.getY(),wood.getZ())==0&&!level.getBlockState(wood.above()).getValue(DoorBlock.OPEN),"native door interaction remains reflected in legacy metadata");
            require(level.getBlockState(origin.offset(7,1,4)).is(Blocks.GLASS)&&level.getBlockState(origin.offset(7,-1,7)).is(Blocks.STONE),"unrelated original jail glass and stone remain unchanged");
            torches(level,world,origin);
            LOG.info("B1.7.3 CREEPS JAIL TEST: actual live-neighbor door halves, facing/open metadata, native door interaction, eighteen spiral stairs, three wall torches, all support directions/light and native torch drops passed");
        }finally{
            for(var entry:globals.entrySet())entry.getKey().set(null,entry.getValue());EntityViewsB173.flush();
            var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))added.add(entity);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();
            for(var entry:saved.entrySet())level.setBlock(entry.getKey(),entry.getValue().state,2|16);
            for(var entry:saved.entrySet())if(entry.getValue().tile!=null){var tile=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(entry.getKey(),entry.getValue().state,entry.getValue().tile,level.registryAccess());if(tile!=null)level.setBlockEntity(tile);}
            EntityViewsB173.flush();
        }
    }
}
