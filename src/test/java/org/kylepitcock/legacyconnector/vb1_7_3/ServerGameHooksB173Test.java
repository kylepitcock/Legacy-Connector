package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import org.junit.jupiter.api.Test;

class ServerGameHooksB173Test {
    static class Mod extends BaseMod {@Override public String Version(){return "fixture";}}
    @Test void schedulerDeduplicatesServerTicksPerActorAndResetsWorldClockOnDimensionChange(){
        var first=new ServerGameHooksB173.Clock();var second=new ServerGameHooksB173.Clock();Object overworld=new Object(),other=new Object();
        assertTrue(first.enter(100));assertFalse(first.enter(100));assertTrue(second.enter(100));
        assertTrue(first.advanced(overworld,50));assertFalse(first.advanced(overworld,50));assertTrue(second.advanced(overworld,50));
        assertTrue(first.enter(101));assertTrue(first.advanced(other,50),"different worlds may have equal game times");
        assertFalse(first.advanced(other,50));assertTrue(first.advanced(other,51));
    }
    @Test void falseAndFailureUnregisterOnlyTheCurrentActorAndHookKind(){
        var mod=new Mod();var first=new ContentB173.ActorHooks();var second=new ContentB173.ActorHooks();var calls=new AtomicInteger();
        first.invoke(mod,false,1,true,false,true,()->{calls.incrementAndGet();return false;});
        first.invoke(mod,false,1,true,false,true,()->{throw new AssertionError("stopped actor cannot run again");});
        second.invoke(mod,false,1,true,false,true,()->{calls.incrementAndGet();return true;});
        first.invoke(mod,true,1,true,false,true,()->{calls.incrementAndGet();return true;});
        assertEquals(3,calls.get());
        first.invoke(mod,false,2,true,false,true,()->{calls.incrementAndGet();throw new IllegalStateException("fixture callback failure");});
        first.invoke(mod,false,2,true,false,true,()->{throw new AssertionError("failed actor cannot run again");});
        second.invoke(mod,false,2,true,false,true,()->{calls.incrementAndGet();return true;});assertEquals(5,calls.get());
    }
    @Test void clockedCallbacksWaitForWorldAdvanceAndExplicitReregistrationRestartsRetention(){
        var state=new ContentB173.ActorHooks();var mod=new Mod();var count=new AtomicInteger();
        state.invoke(mod,false,1,true,true,false,()->{throw new AssertionError("world clock did not advance");});
        state.invoke(mod,false,1,true,true,true,()->{count.incrementAndGet();return false;});
        state.invoke(mod,false,1,true,true,true,()->{throw new AssertionError("unregistered");});
        state.invoke(mod,false,2,true,true,true,()->{count.incrementAndGet();return true;});
        state.invoke(mod,false,2,false,false,true,()->{throw new AssertionError("explicit registration is disabled");});assertEquals(2,count.get());
    }
    @Test void clientPresentationContextIsBoundedAndCannotExposeMutableMetricStorage(){
        int[] metrics={-1,999};var context=new ServerGameHooksB173.Context(-100,100000,0,90000,-1,true,false,true,true,metrics);
        assertEquals(1,context.width());assertEquals(8192,context.height());assertEquals(1,context.guiWidth());assertEquals(8192,context.guiHeight());assertEquals(0,context.scale());
        assertEquals(256,context.metrics().length);assertEquals(0,context.metrics()[0]);assertEquals(64,context.metrics()[1]);assertEquals(6,context.metrics()[2]);
        metrics[0]=40;var copy=context.metrics();copy[1]=40;assertEquals(0,context.metrics()[0]);assertEquals(64,context.metrics()[1]);
    }
}
