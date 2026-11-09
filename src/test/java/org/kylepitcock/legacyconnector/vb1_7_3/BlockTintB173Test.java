package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import org.junit.jupiter.api.Test;

class BlockTintB173Test {
    static class Tinted extends Block {
        Tinted(){super(254,Material.STONE);}
        @Override public int getColor(int metadata){return metadata==14?0xFF0000:0x12AB34;}
        @Override public int getColorMultiplier(BlockView view,int x,int y,int z){return view.getBlockMeta(x,y,z)==3?0x345678:0xFFFFFF;}
    }
    private static <T extends Block>T block(Class<T> type)throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        T block=type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
        block.mcc$initVanilla(254,0,Material.STONE,1,Block.DEFAULT_SOUND_GROUP,"fixture");block.setBoundingBox(0,0,0,1,1,1);return block;
    }
    @Test void worldAndInventoryUseSeparateOriginalCallbacksAndOpaqueNativeColor()throws Exception {
        var block=block(Tinted.class);
        assertTrue(BlockTintB173.hasColor(block));assertFalse(BlockTintB173.hasColor(block(Block.class)));
        assertEquals(0xFFFF0000,BlockTintB173.inventoryColor(block,14));
        assertEquals(0xFF12AB34,BlockTintB173.inventoryColor(block,1));
        assertEquals(0xFF345678,BlockTintB173.worldColor(block,new BlockCallbacksB173Test.Scene(),2,4,6));
        assertEquals(-1,BlockTintB173.inventoryColor(block(Block.class),14));
    }
    private static JsonObject model(Block block)throws Exception {
        var method=AssetsB173.class.getDeclaredMethod("boundedBlockModel",Block.class,int.class,String[].class,boolean.class,int.class,int.class);
        method.setAccessible(true);return (JsonObject)method.invoke(null,block,0,new String[]{"fixture:down","fixture:up","fixture:north","fixture:south","fixture:west","fixture:east"},true,0,14);
    }
    private static void tintedFaces(JsonObject model) {
        var faces=model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(6,faces.size());for(var entry:faces.entrySet())assertEquals(0,entry.getValue().getAsJsonObject().get("tintindex").getAsInt());
    }
    @Test void generatedFullAndInsetCuboidsExposeTintToBothWorldAndItemModels()throws Exception {
        var block=block(Tinted.class);var full=model(block);tintedFaces(full);
        assertEquals("minecraft:block/block",full.get("parent").getAsString());
        assertEquals("#north",full.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString());
        block.setBoundingBox(.25F,0,0,.75F,.5F,1);var inset=model(block);tintedFaces(inset);
        assertEquals(4,inset.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("from").get(0).getAsInt());
        assertEquals(8,inset.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to").get(1).getAsInt());
        var plain=model(block(Block.class));assertEquals("minecraft:block/cube",plain.get("parent").getAsString());assertFalse(plain.has("elements"));
    }
}
