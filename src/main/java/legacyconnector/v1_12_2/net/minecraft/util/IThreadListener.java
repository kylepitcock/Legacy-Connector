package legacyconnector.v1_12_2.net.minecraft.util;

import com.google.common.util.concurrent.ListenableFuture;

/** Something with a main thread tasks can be handed to (the server, the client). */
public interface IThreadListener {
    ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule);

    boolean isCallingFromMinecraftThread();

    /** The listener for a modern executor (the server or the client), running tasks at once when already on its thread. */
    static IThreadListener mcc$of(net.minecraft.util.thread.BlockableEventLoop<? extends Runnable> loop) {
        return new IThreadListener() {
            @Override
            public ListenableFuture<Object> addScheduledTask(Runnable task) {
                if (loop.isSameThread()) {
                    try {
                        task.run();
                        return com.google.common.util.concurrent.Futures.immediateFuture(null);
                    } catch (Throwable t) {
                        return com.google.common.util.concurrent.Futures.immediateFailedFuture(t);
                    }
                }
                com.google.common.util.concurrent.SettableFuture<Object> f = com.google.common.util.concurrent.SettableFuture.create();
                loop.execute(() -> {
                    try {
                        task.run();
                        f.set(null);
                    } catch (Throwable t) {
                        f.setException(t);
                    }
                });
                return f;
            }

            @Override
            public boolean isCallingFromMinecraftThread() {
                return loop.isSameThread();
            }
        };
    }
}
