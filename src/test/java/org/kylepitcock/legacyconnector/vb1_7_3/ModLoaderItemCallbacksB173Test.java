package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.item.*;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import org.junit.jupiter.api.Test;

class ModLoaderItemCallbacksB173Test {
    @Test void onlyEnabledHooksRequestClientFrames(){
        var mod=new Mod();ContentB173.setTickHook(mod,false,false,false);ContentB173.setTickHook(mod,false,false,true);
        assertFalse(ContentB173.hasClientHooks());
        try{ContentB173.setTickHook(mod,true,false,true);assertTrue(ContentB173.hasClientHooks());}
        finally{ContentB173.setTickHook(mod,false,false,true);}
        assertFalse(ContentB173.hasClientHooks());
    }
    private static class Mod extends BaseMod {
        int frames,gui;
        boolean retain=true;
        @Override public String Version(){return "fixture";}
        @Override public boolean OnTickInGame(Minecraft mc){frames++;return retain;}
        @Override public boolean OnTickInGUI(Minecraft mc,legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen screen){gui++;return retain;}
    }
    @Test void clockHooksWaitForTickButFrameHooksRunEveryFrame(){
        var clock=new Mod();var frame=new Mod();var mc=Minecraft.mcc$instance();
        try{
            ContentB173.setTickHook(clock,true,true,false);ContentB173.setTickHook(frame,true,false,false);
            ContentB173.clientHooks(mc,false,false);assertEquals(0,clock.frames);assertEquals(1,frame.frames);
            ContentB173.clientHooks(mc,false,true);assertEquals(1,clock.frames);assertEquals(2,frame.frames);
        }finally{ContentB173.setTickHook(clock,false,false,false);ContentB173.setTickHook(frame,false,false,false);}
    }
    @Test void falseReturnUnregistersOnlyThatHook(){
        var mod=new Mod();var mc=Minecraft.mcc$instance();mod.retain=false;
        ContentB173.setTickHook(mod,true,false,false);ContentB173.setTickHook(mod,true,false,true);
        ContentB173.clientHooks(mc,false,true);ContentB173.clientHooks(mc,false,true);assertEquals(1,mod.frames);assertEquals(0,mod.gui);
        ContentB173.clientHooks(mc,true,true);ContentB173.clientHooks(mc,true,true);assertEquals(1,mod.gui);
    }
    @Test void foodConsumesImmediatelyEvenAtFullHealthAndPreservesStackIdentity(){
        int[] mark=ContentB173.mark();var previous=Item.ITEMS[31000];
        try{
            var item=new FoodItem(31000-256,4,true);var player=new PlayerEntity(null){};player.health=player.maxHealth;
            var stack=new ItemStack(item,1,0);assertSame(stack,item.use(stack,null,player));assertEquals(0,stack.count);assertEquals(player.maxHealth,player.health);
            assertEquals(4,item.getHealthRestored());assertTrue(item.isMeat());assertEquals(1,item.getMaxCount());
        }finally{ContentB173.release(mark);Item.ITEMS[31000]=previous;}
    }
    @Test void stackableFoodConstructorRetainsOriginalDescriptorAndHealsHealth(){
        int[] mark=ContentB173.mark();var previous=Item.ITEMS[31000];
        try{
            var item=new StackableFoodItem(31000-256,2,false,8);var player=new PlayerEntity(null){};player.health=3;
            var stack=new ItemStack(item,8,0);item.use(stack,null,player);assertEquals(7,stack.count);assertEquals(5,player.health);
            assertEquals(8,item.getMaxCount());assertFalse(item.isMeat());
        }finally{ContentB173.release(mark);Item.ITEMS[31000]=previous;}
    }
    @Test void screenScalerKeepsBetaMinimumAreaAndCeilRounding(){
        var options=new legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions();options.guiScale=0;
        var auto=new legacyconnector.vb1_7_3.net.minecraft.client.util.ScreenScaler(options,854,480);
        assertEquals(2,auto.scaleFactor);assertEquals(427,auto.getScaledWidth());assertEquals(240,auto.getScaledHeight());
        options.guiScale=1;var small=new legacyconnector.vb1_7_3.net.minecraft.client.util.ScreenScaler(options,321,241);
        assertEquals(1,small.scaleFactor);assertEquals(321,small.getScaledWidth());assertEquals(241,small.getScaledHeight());
    }
    @Test void builtInRecordsUseNativeSongDefinitions(){
        var id=net.minecraft.resources.ResourceLocation.parse("fixture:disc");
        assertEquals("minecraft:cat",RecordsB173.song(id,"cat").location().toString());
        assertEquals("minecraft:13",RecordsB173.song(id,"13").location().toString());
    }
}
