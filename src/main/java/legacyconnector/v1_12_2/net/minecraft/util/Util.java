package legacyconnector.v1_12_2.net.minecraft.util;

/** 1.12.2's small helpers: the operating system, running a future task, the last element of a list. */
public class Util {
    public static EnumOS getOSType() {
        String s = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        if (s.contains("win")) return EnumOS.WINDOWS;
        if (s.contains("mac")) return EnumOS.OSX;
        if (s.contains("solaris") || s.contains("sunos")) return EnumOS.SOLARIS;
        if (s.contains("linux") || s.contains("unix")) return EnumOS.LINUX;
        return EnumOS.UNKNOWN;
    }

    public static <V> V runTask(java.util.concurrent.FutureTask<V> task, org.apache.logging.log4j.Logger logger) {
        try {
            task.run();
            return task.get();
        } catch (java.util.concurrent.ExecutionException e) {
            logger.fatal("Error executing task", e);
        } catch (InterruptedException e) {
            logger.fatal("Error executing task", e);
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public static <T> T getLastElement(java.util.List<T> list) {
        return list.get(list.size() - 1);
    }

    public enum EnumOS {
        LINUX,
        SOLARIS,
        WINDOWS,
        OSX,
        UNKNOWN
    }
}
