package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

/** Original record sound names enter the native jukebox registry and lifecycle. */
public final class RecordsB173 {
    private static final Map<ResourceLocation,String> CUSTOM=new LinkedHashMap<>();
    private RecordsB173(){}
    /** Copies one record into an empty native container; the original caller owns hand consumption. */
    static boolean insert(net.minecraft.world.item.ItemStack input,net.minecraft.world.item.ItemStack stored,
            java.util.function.Consumer<net.minecraft.world.item.ItemStack> write){
        if(input.isEmpty()||!stored.isEmpty()||!input.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE))return false;
        write.accept(input.copyWithCount(1));return true;
    }
    public static void insert(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z,int item){
        var level=world.mcc$level();if(level==null||level.isClientSide)return;
        if(level.getBlockEntity(new net.minecraft.core.BlockPos(x,y,z)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box)
            insert(insertionStack(world,item,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext()),box.getTheItem(),box::setTheItem);
    }
    static net.minecraft.world.item.ItemStack insertionStack(legacyconnector.vb1_7_3.net.minecraft.world.World world,int item,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity actor){
        if(actor!=null&&actor.world!=null&&(actor.world==world
                ||world!=null&&world.mcc$level()!=null&&actor.world.mcc$level()==world.mcc$level())){
            // The original callback may already have changed its selected stack. Never refresh the view here.
            var held=actor.inventory.getSelectedItem();
            if(held!=null&&held.count>0&&held.itemId==item)return StacksB173.toModern(held).copyWithCount(1);
        }
        return StacksB173.toModern(new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(item,1,0));
    }
    public static void eject(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z){
        var level=world.mcc$level();if(level==null||level.isClientSide)return;
        if(level.getBlockEntity(new net.minecraft.core.BlockPos(x,y,z)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box)box.popOutTheItem();
    }
    record Event(int id,int data){}
    static Event recordEvent(int data,java.util.function.IntUnaryOperator songId){
        if(data==0)return new Event(1011,0);
        int song=songId.applyAsInt(data);return song<0?null:new Event(1010,song);
    }
    public static void worldEvent(legacyconnector.vb1_7_3.net.minecraft.world.World world,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,int event,int x,int y,int z,int data){
        var level=world.mcc$level();if(level==null||event!=1005)return;
        var pos=new net.minecraft.core.BlockPos(x,y,z);
        var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG);
        var translated=recordEvent(data,id->{
            var stack=StacksB173.toModern(new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(id,1,0));
            return JukeboxSong.fromStack(level.registryAccess(),stack).map(holder->{
                // Native insertion already starts this exact song. The subsequent original event is redundant.
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box
                        &&box.getSongPlayer().isPlaying()&&box.getSongPlayer().getSong()==holder.value())return -1;
                return registry.getId(holder.value());
            }).orElse(-1);
        });
        if(translated!=null)level.levelEvent(player!=null&&player.mcc$modern instanceof net.minecraft.world.entity.player.Player p?p:null,translated.id(),pos,translated.data());
    }
    public static synchronized ResourceKey<JukeboxSong> song(ResourceLocation item,String sound){
        // The two Beta built-in streaming resources already have native song definitions.
        if("cat".equals(sound)||"13".equals(sound))return ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,ResourceLocation.withDefaultNamespace(sound));
        var id=ResourceLocation.fromNamespaceAndPath(item.getNamespace(),"beta/"+item.getPath());CUSTOM.put(id,sound);
        return ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,id);
    }
    public static synchronized void contribute(Map<String,Supplier<byte[]>> out){
        for(var entry:CUSTOM.entrySet()){
            var json=new com.google.gson.JsonObject();
            var sound=new com.google.gson.JsonObject();sound.addProperty("sound_id",AudioB173.identifier(AudioB173.Kind.STREAMING,entry.getValue()).toString());
            json.add("sound_event",sound);
            json.addProperty("description",entry.getValue());
            // Beta did not time out its jukebox state. The sound stream itself ends at EOF.
            json.addProperty("length_in_seconds",100000000F);json.addProperty("comparator_output",1);
            byte[] bytes=json.toString().getBytes(StandardCharsets.UTF_8);var id=entry.getKey();
            out.put("data/"+id.getNamespace()+"/jukebox_song/"+id.getPath()+".json",()->bytes);
        }
    }
}
