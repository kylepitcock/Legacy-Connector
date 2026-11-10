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
    static class PoisonHelper {
        static int poisonTime;
        static long clock;
        static double motion,rotation;
        static String label;
        static legacyconnector.vb1_7_3.net.minecraft.world.World realm;
        static int sharedConfig=23;
        static int constructorRegistry;
        static final int constant=31;
        PoisonHelper(){constructorRegistry++;}
        static void world(legacyconnector.vb1_7_3.net.minecraft.world.World world){realm=world;}
        static void tick(){poisonTime+=sharedConfig;clock++;motion+=.25;rotation-=.5;label="poison";recursive(2);}
        static void recursive(int depth){if(depth>0)cycle(depth-1);}
        static void cycle(int depth){recursive(depth);}
    }
    static class HelperHook extends BaseMod {
        @Override public String Version(){return "helper";}
        @Override public boolean OnTickInGame(Minecraft mc){new PoisonHelper();PoisonHelper.world(mc==null?null:mc.world);PoisonHelper.tick();return true;}
    }
    static class SharedLedgerHelper {
        static final List<Double> remaining=new ArrayList<>();
        static long lastTime,now;
        static double delta;
        static legacyconnector.vb1_7_3.net.minecraft.world.World world;
        static void world(legacyconnector.vb1_7_3.net.minecraft.world.World value){world=value;}
        static void tick(){delta=now-lastTime;lastTime=now;for(int i=0;i<remaining.size();i++)remaining.set(i,remaining.get(i)-delta);}
    }
    static class LedgerClockCaller {
        static int ticks;
        static void tick(){ticks++;SharedLedgerHelper.tick();}
    }
    static class LedgerHook extends BaseMod {
        static int oldHierarchyCounter;
        @Override public String Version(){return "ledger";}
        @Override public boolean OnTickInGame(Minecraft mc){oldHierarchyCounter++;SharedLedgerHelper.world(mc==null?null:mc.world);LedgerClockCaller.tick();PoisonHelper.tick();return true;}
    }
    @Test void twoActorHooksShareLedgerClockWithoutNarrowingOldHierarchyOrPurePoisonState(){
        var fields=HookActorStateB173.fields(LedgerHook.class);
        assertFalse(fields.stream().anyMatch(f->f.getDeclaringClass()==SharedLedgerHelper.class||f.getDeclaringClass()==LedgerClockCaller.class));
        assertTrue(fields.stream().anyMatch(f->f.getDeclaringClass()==LedgerHook.class&&f.getName().equals("oldHierarchyCounter")));
        assertTrue(fields.stream().anyMatch(f->f.getDeclaringClass()==PoisonHelper.class&&f.getName().equals("poisonTime")));
        SharedLedgerHelper.remaining.clear();SharedLedgerHelper.remaining.add(1000D);SharedLedgerHelper.lastTime=0;SharedLedgerHelper.now=50;
        LedgerClockCaller.ticks=0;LedgerHook.oldHierarchyCounter=0;PoisonHelper.poisonTime=0;PoisonHelper.clock=0;PoisonHelper.sharedConfig=23;
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();var types=List.<Class<?>>of(LedgerHook.class);
        try(var scope=HookActorStateB173.open(first,types)){new LedgerHook().OnTickInGame(null);assertEquals(1,LedgerHook.oldHierarchyCounter);assertEquals(23,PoisonHelper.poisonTime);}
        assertEquals(50,SharedLedgerHelper.lastTime);assertEquals(950D,SharedLedgerHelper.remaining.getFirst());
        try(var scope=HookActorStateB173.open(second,types)){assertEquals(0,PoisonHelper.poisonTime);new LedgerHook().OnTickInGame(null);assertEquals(1,LedgerHook.oldHierarchyCounter);}
        assertEquals(950D,SharedLedgerHelper.remaining.getFirst(),"Shared ledger must not lose another full elapsed interval for the second actor");
        assertEquals(2,LedgerClockCaller.ticks);assertEquals(0,LedgerHook.oldHierarchyCounter);
        try(var scope=HookActorStateB173.open(first,types)){assertEquals(23,PoisonHelper.poisonTime);assertEquals(1,PoisonHelper.clock);}
    }
    static class MapArrayHelper {
        static final Map<String,Integer> shared=new HashMap<>();
        static final int[] totals={0};
        static int ticks;
        static void tick(){ticks++;shared.merge("all",1,Integer::sum);totals[0]++;}
    }
    static class MapArrayHook extends BaseMod {
        @Override public String Version(){return "map-array";}
        @Override public boolean OnTickInGame(Minecraft mc){MapArrayHelper.tick();return true;}
    }
    @Test void finalMapAndArrayReferencesKeepTheirCoordinatingHelperScalarShared(){
        assertTrue(HookActorStateB173.fields(MapArrayHook.class).isEmpty());
        MapArrayHelper.shared.clear();MapArrayHelper.totals[0]=0;MapArrayHelper.ticks=0;
        for(int i=0;i<2;i++)try(var scope=HookActorStateB173.open(UUID.randomUUID(),List.of(MapArrayHook.class))){new MapArrayHook().OnTickInGame(null);}
        assertEquals(2,MapArrayHelper.ticks);assertEquals(2,MapArrayHelper.shared.get("all"));assertEquals(2,MapArrayHelper.totals[0]);
    }
    static class DormantLedgerHelper {
        static final List<String> unused=new ArrayList<>();
        static int ticks;
        static void unrelated(){unused.add("not reached by a hook");}
        static void tick(){ticks++;}
    }
    static class DormantLedgerHook extends BaseMod {
        @Override public String Version(){return "dormant";}
        @Override public boolean OnTickInGame(Minecraft mc){DormantLedgerHelper.tick();return true;}
    }
    @Test void unreachableLedgerDoesNotDisqualifyOtherwiseClosedHelperState(){
        var fields=HookActorStateB173.fields(DormantLedgerHook.class);assertEquals(1,fields.size());assertEquals("ticks",fields.getFirst().getName());
    }
    @Test void sameArtifactHelperWritesAreActorOwnedButConfigAndInitializationRemainShared(){
        PoisonHelper.poisonTime=0;PoisonHelper.clock=0;PoisonHelper.motion=0;PoisonHelper.rotation=0;PoisonHelper.label=null;PoisonHelper.sharedConfig=23;PoisonHelper.constructorRegistry=0;
        var names=HookActorStateB173.fields(HelperHook.class).stream().map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("poisonTime","clock","motion","rotation","label","realm"),names);
        var first=UUID.randomUUID();var second=UUID.randomUUID();var types=List.<Class<?>>of(HelperHook.class);
        try(var scope=HookActorStateB173.open(first,types)){new HelperHook().OnTickInGame(null);assertEquals(23,PoisonHelper.poisonTime);}
        PoisonHelper.sharedConfig=7;
        try(var scope=HookActorStateB173.open(second,types)){assertEquals(0,PoisonHelper.poisonTime);assertNull(PoisonHelper.label);new HelperHook().OnTickInGame(null);assertEquals(7,PoisonHelper.poisonTime);}
        try(var scope=HookActorStateB173.open(first,types)){assertEquals(23,PoisonHelper.poisonTime);assertEquals(1,PoisonHelper.clock);assertEquals(.25,PoisonHelper.motion);assertEquals(-.5,PoisonHelper.rotation);assertEquals("poison",PoisonHelper.label);assertEquals(7,PoisonHelper.sharedConfig);}
        assertEquals(2,PoisonHelper.constructorRegistry);assertEquals(31,PoisonHelper.constant);assertEquals(0,PoisonHelper.poisonTime);
    }
    @Test void recursiveHelperGraphTerminatesAndHardBoundsRejectBeforeReturningPartialSchema(){
        assertEquals(6,HookActorStateB173.fields(HelperHook.class).size());
        assertThrows(IllegalStateException.class,()->HookActorStateB173.fields(HelperHook.class,1,16384,131072));
        assertThrows(IllegalStateException.class,()->HookActorStateB173.fields(HelperHook.class,512,1,131072));
        assertThrows(IllegalStateException.class,()->HookActorStateB173.fields(HelperHook.class,512,16384,1));
    }
    static class TargetCallbacks {
        static void afflict(legacyconnector.vb1_7_3.net.minecraft.entity.Entity victim){PoisonHelper.poisonTime=500;}
        static boolean comparison(legacyconnector.vb1_7_3.net.minecraft.entity.Entity other){return other!=null;}
    }
    @Test void targetFallbackRequiresWriterOverlapAndNeverOverridesExplicitCaller(){
        var types=List.<Class<?>>of(HelperHook.class);String entity="Llegacyconnector/vb1_7_3/net/minecraft/entity/Entity;";
        assertTrue(HookActorStateB173.writesActorFields(TargetCallbacks.class,"afflict","("+entity+")V",types));
        assertFalse(HookActorStateB173.writesActorFields(TargetCallbacks.class,"comparison","("+entity+")Z",types));
        UUID attacker=UUID.randomUUID(),victim=UUID.randomUUID();
        assertNull(HookActorStateB173.targetActor(true,victim,true));assertNull(HookActorStateB173.targetActor(false,victim,false));assertNull(HookActorStateB173.targetActor(false,null,true));
        assertEquals(victim,HookActorStateB173.targetActor(false,victim,true));
        PoisonHelper.poisonTime=0;
        try(var caller=HookActorStateB173.open(attacker,types)){PoisonHelper.poisonTime=12;assertNull(HookActorStateB173.targetActor(true,victim,true));assertEquals(12,PoisonHelper.poisonTime);}
        try(var projectile=HookActorStateB173.open(null,types)){
            UUID target=HookActorStateB173.targetActor(false,victim,true);
            try(var hit=HookActorStateB173.open(target,types)){TargetCallbacks.afflict(null);}
            assertEquals(0,PoisonHelper.poisonTime);
        }
        try(var victimState=HookActorStateB173.open(victim,types)){assertEquals(500,PoisonHelper.poisonTime);}
        try(var attackerState=HookActorStateB173.open(attacker,types)){assertEquals(12,PoisonHelper.poisonTime);}
    }
    @Test void writtenWorldMetadataIsolatedPerActorAndUnloadedWorldReferencesAreReleased(){
        PoisonHelper.realm=null;var types=List.<Class<?>>of(HelperHook.class);UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        var a=new legacyconnector.vb1_7_3.net.minecraft.world.World(){};var b=new legacyconnector.vb1_7_3.net.minecraft.world.World(){};
        try(var scope=HookActorStateB173.open(first,types)){PoisonHelper.realm=a;}
        try(var scope=HookActorStateB173.open(second,types)){assertNull(PoisonHelper.realm);PoisonHelper.realm=b;}
        try(var scope=HookActorStateB173.open(first,types)){assertSame(a,PoisonHelper.realm);}
        HookActorStateB173.clearWorld(a);
        try(var scope=HookActorStateB173.open(first,types)){assertNull(PoisonHelper.realm);}
        try(var scope=HookActorStateB173.open(second,types)){assertSame(b,PoisonHelper.realm);}
    }
    static class InitializationFailureSwitch {static boolean fail;}
    static class FailingHelper {
        static int state;
        static {if(InitializationFailureSwitch.fail)throw new IllegalStateException("fixture initialization");}
        static void tick(){state++;}
    }
    static class FailingHook extends BaseMod {
        @Override public String Version(){return "failing";}
        @Override public boolean OnTickInGame(Minecraft mc){FailingHelper.tick();return true;}
    }
    @Test void helperInitializationFailureRollsBackAlreadySwappedFields(){
        var actor=UUID.randomUUID();
        try(var initial=HookActorStateB173.open(actor,List.of(Original.class))){}
        Original.counter=99;InitializationFailureSwitch.fail=true;
        try{
            assertThrows(LinkageError.class,()->HookActorStateB173.open(UUID.randomUUID(),List.of(Original.class,FailingHook.class)));
            assertEquals(99,Original.counter);
            try(var restored=HookActorStateB173.open(actor,List.of(Original.class))){assertEquals(17,Original.counter);}
            assertEquals(99,Original.counter);
        }finally{InitializationFailureSwitch.fail=false;}
    }
    static class InitializationObserver {static boolean initialized;}
    static class LazyHelper {
        static legacyconnector.vb1_7_3.net.minecraft.world.World initializedWorld;
        static int timer;
        static {initializedWorld=Minecraft.mcc$instance().world;InitializationObserver.initialized=true;}
        static void tick(){timer++;}
    }
    static class LazyHook extends BaseMod {
        @Override public String Version(){return "lazy";}
        @Override public boolean OnTickInGame(Minecraft mc){LazyHelper.tick();return true;}
    }
    @Test void discoveryDoesNotInitializeHelpersAndFirstScalarReadUsesEstablishedWorldContext(){
        assertFalse(InitializationObserver.initialized);
        assertEquals(1,HookActorStateB173.fields(LazyHook.class).size());assertFalse(InitializationObserver.initialized);
        var world=new legacyconnector.vb1_7_3.net.minecraft.world.World(){};var minecraft=Minecraft.mcc$instance();var previous=minecraft.world;
        try(var context=Minecraft.mcc$context(world,null);var actor=HookActorStateB173.open(UUID.randomUUID(),List.of(LazyHook.class))){
            assertTrue(InitializationObserver.initialized);assertSame(world,LazyHelper.initializedWorld);assertEquals(0,LazyHelper.timer);
        }finally{minecraft.world=previous;}
    }
    @BeforeEach void reset(){
        HookActorStateB173.clear();Original.enabled=false;Original.unreachable=false;Original.label=null;Original.counter=17;
        Original.sharedArray=new int[]{0};Original.sharedList.clear();Parent.inherited=false;GlobalTimer.ticks=0;
    }
    @Test void discoversOnlyHookReachableScalarWritesAcrossOriginalAncestry(){
        var names=HookActorStateB173.fields(Original.class).stream().map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("enabled","label","counter","inherited","ticks"),names);
        assertFalse(names.contains("sharedArray"));assertFalse(names.contains("sharedList"));assertFalse(names.contains("unreachable"));assertTrue(names.contains("ticks"));
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
        assertEquals(0,GlobalTimer.ticks,"neutral helper state is restored after actor callbacks");
        try(var scope=HookActorStateB173.open(first,types)){assertEquals(1,GlobalTimer.ticks);}
        try(var scope=HookActorStateB173.open(second,types)){assertEquals(0,GlobalTimer.ticks);}
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
