package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NativeGroundB173Test {
    @Test void generationCapturesFlagWithoutCallingNativeCollisionsAndOwnerCommitsOnce(){
        var writes=new NativeGroundB173.Writes<Object>();var key=new Object();
        var nativeFlag=new AtomicBoolean(false);var calls=new AtomicInteger();var queue=new ArrayList<Runnable>();
        java.util.function.Consumer<Boolean> nativeSetter=value->{calls.incrementAndGet();nativeFlag.set(value);};
        writes.commit(key,false,true,nativeFlag::get,nativeSetter,queue::add);
        assertTrue(queue.isEmpty());assertEquals(0,calls.get());
        writes.commit(key,true,true,nativeFlag::get,nativeSetter,queue::add);
        writes.commit(key,true,true,nativeFlag::get,nativeSetter,queue::add);
        assertFalse(nativeFlag.get(),"Generation must not change the native baseline or enter native collision queries");
        assertEquals(0,calls.get());assertEquals(1,queue.size());
        queue.getFirst().run();assertTrue(nativeFlag.get());assertEquals(1,calls.get());
        writes.commit(key,true,false,nativeFlag::get,nativeSetter,queue::add);assertEquals(1,calls.get());
    }
    @Test void laterOriginalValueAndNewerOwnerWriteCannotBeOverwrittenByOldQueuedFlag(){
        var writes=new NativeGroundB173.Writes<Object>();var key=new Object();
        var nativeFlag=new AtomicBoolean(false);var calls=new AtomicInteger();var queue=new ArrayList<Runnable>();
        java.util.function.Consumer<Boolean> setter=value->{calls.incrementAndGet();nativeFlag.set(value);};
        writes.commit(key,true,true,nativeFlag::get,setter,queue::add);
        writes.commit(key,false,true,nativeFlag::get,setter,queue::add);
        queue.removeFirst().run();assertFalse(nativeFlag.get());assertEquals(0,calls.get());
        writes.commit(key,true,true,nativeFlag::get,setter,queue::add);
        writes.commit(key,false,false,nativeFlag::get,setter,queue::add);
        queue.removeFirst().run();assertFalse(nativeFlag.get());assertEquals(0,calls.get());
        writes.commit(key,true,false,nativeFlag::get,setter,queue::add);
        assertTrue(nativeFlag.get());assertEquals(1,calls.get());assertTrue(queue.isEmpty());
    }
    @Test void ownerReconciliationAfterGenerationReleasesModLockHasNoServerWorkerCycle() throws Exception {
        var lock=new java.util.concurrent.locks.ReentrantLock();var writes=new NativeGroundB173.Writes<Object>();
        var queued=new java.util.concurrent.LinkedBlockingQueue<Runnable>();var nativeFlag=new AtomicBoolean();
        var calls=new AtomicInteger();var workerFailure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var owner=Thread.currentThread();
        var worker=new Thread(()->{
            lock.lock();try{
                writes.commit(new Object(),true,true,nativeFlag::get,value->{
                    if(Thread.currentThread()!=owner)throw new AssertionError("Native collision setter ran on generation worker");
                    assertFalse(lock.isLocked());nativeFlag.set(value);calls.incrementAndGet();
                },queued::add);
            }catch(Throwable error){workerFailure.set(error);}finally{lock.unlock();}
        });
        worker.start();worker.join(2000);assertFalse(worker.isAlive(),"Worker must release the mod lock without waiting for native server work");
        assertNull(workerFailure.get());assertFalse(nativeFlag.get());
        lock.lock();try{assertEquals(0,calls.get());}finally{lock.unlock();}
        var commit=queued.poll(2,java.util.concurrent.TimeUnit.SECONDS);assertNotNull(commit);commit.run();
        assertTrue(nativeFlag.get());assertEquals(1,calls.get());
    }
}
