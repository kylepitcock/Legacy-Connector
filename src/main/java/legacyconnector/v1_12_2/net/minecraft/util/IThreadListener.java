package legacyconnector.v1_12_2.net.minecraft.util;

/** Something that runs tasks on its own thread (the server, a server world, the client). */
public interface IThreadListener {
    com.google.common.util.concurrent.ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule);

    boolean isCallingFromMinecraftThread();
}
