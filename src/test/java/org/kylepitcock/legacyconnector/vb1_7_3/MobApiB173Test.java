package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class MobApiB173Test {
    static class SupportedMob extends AnimalEntity {
        int jumps;
        SupportedMob(){super(null);}
        @Override public void jump(){jumps++;super.jump();velocityY+=.18;}
        @Override public int getMinAmbientSoundDelay(){return 240;}
        @Override public boolean bypassesSteppingEffects(){return false;}
        NbtList doubles(double... values){return super.toNbtList(values);}
        NbtList floats(float... values){return super.toNbtList(values);}
    }
    static class UnsupportedMob extends AnimalEntity {
        UnsupportedMob(){super(null);}
        @Override public void setWorld(World world){super.setWorld(world);}
    }
    static class Player extends PlayerEntity {Player(){super(null);}}
    @Test void virtualJumpRunsOriginalOnceAndPreservesSuperclassBaseline(){
        var mob=new SupportedMob();mob.velocityY=-2;mob.mcc$jump();assertEquals(1,mob.jumps);assertEquals(.6,mob.velocityY,1E-12);
        assertEquals(240,mob.getMinAmbientSoundDelay());assertFalse(mob.mcc$steppingEffects());
    }
    @Test void nbtListsRetainNumericTypesOrderPrecisionAndIndependentStorage(){
        var mob=new SupportedMob();double[] values={1.25,-7,Math.PI};var doubles=mob.doubles(values);values[0]=99;
        assertEquals(3,doubles.size());assertEquals(6,doubles.mcc$elementType());assertEquals(1.25,((NbtDouble)doubles.get(0)).value);assertEquals(Math.PI,((NbtDouble)doubles.get(2)).value);
        var floats=mob.floats(1.5F,-3.25F);assertEquals(5,floats.mcc$elementType());assertEquals(-3.25F,((NbtFloat)floats.get(1)).value);
        assertEquals(0,mob.doubles().size());assertEquals(0,mob.floats().size());
    }
    @Test void fancyGraphicsReadsInstalledNativeProviderAndKeepsDedicatedFallback(){
        var options=new GameOptions();GameOptions.mcc$graphics(null);
        assertTrue(GameOptions.mcc$get$fancyGraphics(options));options.fancyGraphics=false;assertFalse(GameOptions.mcc$get$fancyGraphics(options));
        var nativeValue=new AtomicBoolean(true);
        try{GameOptions.mcc$graphics(nativeValue::get);assertTrue(GameOptions.mcc$get$fancyGraphics(options));nativeValue.set(false);assertFalse(GameOptions.mcc$get$fancyGraphics(options));}
        finally{GameOptions.mcc$graphics(null);}
    }
    @Test void swingHandImmediatelyUpdatesOriginalPlayerView(){var player=new Player();assertFalse(player.handSwinging);player.swingHand();assertTrue(player.handSwinging);}
    @Test void onlyImplementedCallbackClosureIsAdmitted(){assertNull(AnimalsB173.unsupportedBehaviour(SupportedMob.class));assertNotNull(AnimalsB173.unsupportedBehaviour(UnsupportedMob.class));}
}
