package legacyconnector.v1_12_2.net.minecraft.world.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.12.2's background file writer: queued writers ({@link IThreadedFileIO}) are run on one daemon thread until each says it is done;
 * {@link #waitForFinish} blocks until the queue is empty (mods flush their region images and saves with it).
 */
public class ThreadedFileIOBase implements Runnable {
    private static final ThreadedFileIOBase INSTANCE = new ThreadedFileIOBase();

    private final List<IThreadedFileIO> threadedIOQueue = Collections.synchronizedList(new ArrayList<>());
    private volatile long writeQueuedCounter;
    private volatile long savedIOCounter;
    private volatile boolean isThreadWaiting;

    private ThreadedFileIOBase() {
        Thread thread = new Thread(this, "File IO Thread (legacy)");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            processQueue();
        }
    }

    private void processQueue() {
        for (int i = 0; i < threadedIOQueue.size(); i++) {
            IThreadedFileIO io;
            try {
                io = threadedIOQueue.get(i);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            boolean more;
            try {
                more = io.writeNextIO();
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/FileIO").warn("A legacy file writer failed; dropping it", e);
                more = false;
            }
            if (!more) {
                threadedIOQueue.remove(i--);
                savedIOCounter++;
            }
            sleep(isThreadWaiting ? 0L : 10L);
        }
        if (threadedIOQueue.isEmpty()) sleep(25L);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static ThreadedFileIOBase getThreadedIOInstance() {
        return INSTANCE;
    }

    public void queueIO(IThreadedFileIO io) {
        if (io == null || threadedIOQueue.contains(io)) return;
        writeQueuedCounter++;
        threadedIOQueue.add(io);
    }

    /** Blocks until every queued writer has finished. */
    public void waitForFinish() throws InterruptedException {
        isThreadWaiting = true;
        try {
            while (writeQueuedCounter != savedIOCounter) Thread.sleep(10L);
        } finally {
            isThreadWaiting = false;
        }
    }
}
