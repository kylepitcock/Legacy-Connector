package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HookActorStateB173Test {
    static class Parent extends BaseMod {
        static boolean inherited;
        void inheritedHook(){inherited=true;}
        @Override public String Version(){return "fixture";}
    }
    static class Original extends Parent {
        static boolean enabled,unreachable;
        static String label;
        static int counter=17;
        static int[] sharedArray={0};
        static final List<String> sharedList=new ArrayList<>();
        private void state(){enabled=true;label="hook";counter++;inheritedHook();}
        static void outsideHook(){unreachable=true;}
        @Override public boolean OnTickInGame(Minecraft minecraft){state();sharedArray=new int[]{3};sharedList.add("shared");GlobalTimer.tick();return true;}
        @Override public boolean OnTickInGUI(Minecraft minecraft,Screen screen){label="gui";return true;}
    }
    static class GlobalTimer {static int ticks;static void tick(){ticks++;}}
    @BeforeEach void reset(){
        HookActorStateB173.clear();Original.enabled=false;Original.unreachable=false;Original.label=null;Original.counter=17;
        Original.sharedArray=new int[]{0};Original.sharedList.clear();Parent.inherited=false;GlobalTimer.ticks=0;
    }
    @Test void discoversOnlyHookReachableScalarWritesAcrossOriginalAncestry(){
        var names=HookActorStateB173.fields(Original.class).stream().map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("enabled","label","counter","inherited"),names);
        assertFalse(names.contains("sharedArray"));assertFalse(names.contains("sharedList"));assertFalse(names.contains("unreachable"));assertFalse(names.contains("ticks"));
    }
    @Test void twoActorsAndNeutralCallbacksNeverBorrowAnotherActorsDerivedScalars(){
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();var types=List.<Class<?>>of(Original.class);
        try(var scope=HookActorStateB173.open(first,types)){
            assertFalse(Original.enabled);assertNull(Original.label);new Original().OnTickInGame(null);Original.label="first";
        }
        assertFalse(Original.enabled);assertNull(Original.label);assertFalse(Parent.inherited);assertEquals(17,Original.counter);
        try(var scope=HookActorStateB173.open(second,types)){
            assertFalse(Original.enabled);assertNull(Original.label);assertEquals(17,Original.counter);Original.label="second";
        }
        try(var scope=HookActorStateB173.open(first,types)){
            assertTrue(Original.enabled);assertEquals("first",Original.label);assertTrue(Parent.inherited);assertEquals(18,Original.counter);
        }
        try(var scope=HookActorStateB173.open(null,types)){assertFalse(Original.enabled);assertNull(Original.label);assertFalse(Parent.inherited);}
        assertEquals(1,GlobalTimer.ticks,"helper-class global timer is never copied or restored");
        assertArrayEquals(new int[]{3},Original.sharedArray);assertEquals(List.of("shared"),Original.sharedList);
    }
    @Test void nestedForeignAndSameActorScopesRestoreOuterEvenAfterException(){
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();var types=List.<Class<?>>of(Original.class);
        try(var outer=HookActorStateB173.open(first,types)){
            Original.label="outer";Original.enabled=true;
            try(var same=HookActorStateB173.open(first,types)){assertEquals("outer",Original.label);Original.counter=20;}
            assertThrows(IllegalStateException.class,()->{
                try(var foreign=HookActorStateB173.open(second,types)){assertNull(Original.label);assertFalse(Original.enabled);Original.label="foreign";throw new IllegalStateException("fixture");}
            });
            assertEquals("outer",Original.label);assertTrue(Original.enabled);assertEquals(20,Original.counter);
            try(var neutral=HookActorStateB173.open(null,types)){assertNull(Original.label);assertFalse(Original.enabled);}
            assertEquals("outer",Original.label);
        }
        assertNull(Original.label);assertFalse(Original.enabled);
        try(var secondState=HookActorStateB173.open(second,types)){assertEquals("foreign",Original.label);}
    }
    @Test void logoutAndServerCleanupDiscardActorStateWithoutChangingNeutralGlobals(){
        UUID actor=UUID.randomUUID();var types=List.<Class<?>>of(Original.class);
        try(var scope=HookActorStateB173.open(actor,types)){Original.enabled=true;Original.label="old";}
        HookActorStateB173.remove(actor);
        try(var scope=HookActorStateB173.open(actor,types)){assertFalse(Original.enabled);assertNull(Original.label);}
        HookActorStateB173.clear();assertFalse(Original.enabled);assertNull(Original.label);
    }
}
