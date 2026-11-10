package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.*;

/** Bounded context for original menu constructors using canonical inventories and captured scalar arguments. */
final class RemoteMenuContextB173 {
    static final int MAX_TILES=8,MAX_SLOTS=512,MAX_BYTES=16_384;
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private RemoteMenuContextB173(){}
    static CompoundTag read(net.minecraft.network.FriendlyByteBuf buffer){
        if(!buffer.isReadable())return null;
        if(buffer.readableBytes()>MAX_BYTES){LOG.warn("Ignoring oversized optional remote menu context");return null;}
        try{
            Tag tag=buffer.readNbt(NbtAccounter.create(MAX_BYTES*4L));
            return tag instanceof CompoundTag context?context:null;
        }catch(RuntimeException|LinkageError error){LOG.warn("Ignoring malformed optional remote menu context: {}",error.toString());return null;}
    }
    record Selection(Object gui,boolean forward){}
    static Selection select(Object handedOff,java.util.function.Supplier<?> pending,java.util.function.Supplier<?> reconstructed){
        if(handedOff!=null)return new Selection(handedOff,false);
        Object gui=pending.get();if(gui==null)gui=reconstructed.get();
        return new Selection(gui,gui!=null);
    }
    static CompoundTag capture(ScreenHandler container){
        var context=new CompoundTag();var tiles=new ListTag();
        if(container==null||container.slots.size()>MAX_SLOTS)return context;
        Set<BlockEntity> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        try{
            for(var slot:container.slots)if(slot.inventory instanceof BlockEntity tile&&seen.add(tile)){
                String type=BlockEntity.mcc$idOf(tile.getClass());if(type==null)continue;
                if(tiles.size()>=MAX_TILES)return new CompoundTag();
                var snapshot=new NbtCompound();tile.writeNbt(snapshot);
                var entry=new CompoundTag();entry.putString("type",type);entry.putInt("x",tile.x);entry.putInt("y",tile.y);entry.putInt("z",tile.z);
                entry.put("nbt",NbtB173.toModern(snapshot));tiles.add(entry);
            }
            context.put("tiles",tiles);
            if(PlayerMenuContextB173.isPlayerMenu(container))context.putBoolean("player",true);
            return bounded(context)?context:new CompoundTag();
        }catch(java.io.IOException|RuntimeException|LinkageError error){LOG.warn("Cannot capture original remote menu tile context",error);return new CompoundTag();}
    }
    static CompoundTag capture(HandledScreen gui){
        var context=capture(gui.container);var constructor=MenuConstructorStateB173.capture(gui);
        if(constructor==null)return context;
        context.put("constructor",constructor);
        try{return bounded(context)?context:new CompoundTag();}catch(java.io.IOException error){return new CompoundTag();}
    }
    private static boolean bounded(CompoundTag context)throws java.io.IOException{
        var bytes=new ByteArrayOutputStream();
        try(var output=new DataOutputStream(bytes)){NbtIo.write(context,output);}
        return bytes.size()<=MAX_BYTES;
    }
    /** Ownership is by installed jar identity, never a class-name or mod selector. */
    static boolean owned(Class<?> type,Class<?> owner){
        return type.getClassLoader()==owner.getClassLoader()
                &&Objects.equals(type.getProtectionDomain().getCodeSource(),owner.getProtectionDomain().getCodeSource());
    }
    private static Class<? extends HandledScreen> installed(String name){
        for(var mod:ContentB173.loadedMods())try{
            var type=Class.forName(name,false,mod.getClass().getClassLoader());
            if(HandledScreen.class.isAssignableFrom(type)&&owned(type,mod.getClass()))return type.asSubclass(HandledScreen.class);
        }catch(ClassNotFoundException|LinkageError|SecurityException ignored){}
        return null;
    }
    static HandledScreen reconstruct(String name,CompoundTag context,net.minecraft.world.entity.player.Inventory inventory,int[][] layout){
        if(inventory.player.level()==null||!inventory.player.level().isClientSide)return null;
        try{
            var type=installed(name);if(type==null||!bounded(context))return null;
            try(var views=EntityViewsB173.readOnly();
                    var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(inventory.player.level()),null)){
                var player=PlayerViewB173.of(inventory.player);
                return construct(type,context,player.inventory,player.world,layout);
            }
        }catch(java.io.IOException|ReflectiveOperationException|RuntimeException|LinkageError error){
            LOG.warn("Cannot reconstruct original remote menu {}",name,error);return null;
        }
    }
    /** Structural half kept graphics-free for regression tests; the production entry first verifies installed ownership. */
    static HandledScreen construct(Class<? extends HandledScreen> type,CompoundTag context,PlayerInventory inventory,World world,int[][] layout)throws ReflectiveOperationException{
        if(layout.length>MAX_SLOTS)return null;
        if(context.getBoolean("player"))return PlayerMenuContextB173.construct(type,inventory.player,layout);
        var entries=context.getList("tiles",Tag.TAG_COMPOUND);if(entries.isEmpty()||entries.size()>MAX_TILES)return null;
        if(context.contains("constructor",Tag.TAG_COMPOUND))return constructCaptured(type,context,inventory,world,layout,entries);
        java.lang.reflect.Constructor<?> selected=null;BlockEntity selectedTile=null;CompoundTag selectedNbt=null;
        for(Tag raw:entries){
            var entry=(CompoundTag)raw;var nbt=entry.getCompound("nbt");String id=entry.getString("type");
            int x=entry.getInt("x"),y=entry.getInt("y"),z=entry.getInt("z");
            if(id.isEmpty()||!id.equals(nbt.getString("id"))||nbt.getInt("x")!=x||nbt.getInt("y")!=y||nbt.getInt("z")!=z)return null;
            var tile=world.getBlockEntity(x,y,z);
            if(tile==null||!id.equals(BlockEntity.mcc$idOf(tile.getClass())))return null;
            for(var constructor:type.getConstructors()){
                var parameters=constructor.getParameterTypes();
                if(parameters.length!=2||parameters[0]!=PlayerInventory.class||!BlockEntity.class.isAssignableFrom(parameters[1])||!parameters[1].isInstance(tile))continue;
                if(selected!=null)return null;
                selected=constructor;selectedTile=tile;selectedNbt=nbt;
            }
        }
        if(selected==null)return null;
        selectedTile.readNbt(NbtB173.fromModern(selectedNbt));
        var gui=(HandledScreen)selected.newInstance(inventory,selectedTile);
        return matches(gui.container,layout)?gui:null;
    }
    private static HandledScreen constructCaptured(Class<? extends HandledScreen> type,CompoundTag context,PlayerInventory inventory,World world,int[][] layout,ListTag entries)throws ReflectiveOperationException{
        var tiles=new LinkedHashMap<String,BlockEntity>();var snapshots=new LinkedHashMap<BlockEntity,CompoundTag>();
        for(var raw:entries){
            var entry=(CompoundTag)raw;var nbt=entry.getCompound("nbt");String id=entry.getString("type");
            int x=entry.getInt("x"),y=entry.getInt("y"),z=entry.getInt("z");
            if(id.isEmpty()||!id.equals(nbt.getString("id"))||nbt.getInt("x")!=x||nbt.getInt("y")!=y||nbt.getInt("z")!=z)return null;
            var tile=world.getBlockEntity(x,y,z);String key=id+":"+x+":"+y+":"+z;
            if(tile==null||!id.equals(BlockEntity.mcc$idOf(tile.getClass()))||tiles.putIfAbsent(key,tile)!=null)return null;
            snapshots.put(tile,nbt);
        }
        var prepared=MenuConstructorStateB173.prepare(type,context.getCompound("constructor"),inventory,tiles);
        if(prepared==null)return null;
        for(var entry:snapshots.entrySet())entry.getKey().readNbt(NbtB173.fromModern(entry.getValue()));
        var gui=prepared.create();return matches(gui.container,layout)?gui:null;
    }
    static boolean matches(ScreenHandler container,int[][] layout){
        if(container==null||layout.length>MAX_SLOTS||container.slots.size()!=layout.length)return false;
        for(int i=0;i<layout.length;i++){
            var expected=layout[i];var slot=container.slots.get(i);
            if(expected.length!=4||expected[0]!=(LegacyB173Menu.isPlayerSlot(slot)?0:1)
                    ||expected[1]!=slot.index||expected[2]!=slot.x||expected[3]!=slot.y)return false;
        }
        return true;
    }
    static void forward(HandledScreen gui,int index,net.minecraft.world.item.ItemStack stack){
        if(gui.container!=null&&index>=0&&index<gui.container.slots.size())gui.container.setStackInSlot(index,StacksB173.toLegacy(stack));
    }
}
