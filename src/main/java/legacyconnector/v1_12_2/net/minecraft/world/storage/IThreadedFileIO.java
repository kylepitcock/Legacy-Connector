package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** A writer the file IO thread calls until it reports no more work ({@code false}). */
public interface IThreadedFileIO {
    boolean writeNextIO();
}
