package legacyconnector.v1_7_10.net.minecraft.util;

/** The operating system the game runs on (mods pick file locations and how to open folders by it). */
public class Util {
    public static Util.EnumOS getOSType() {
        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) return EnumOS.WINDOWS;
        if (os.contains("mac")) return EnumOS.OSX;
        if (os.contains("solaris") || os.contains("sunos")) return EnumOS.SOLARIS;
        if (os.contains("linux") || os.contains("unix")) return EnumOS.LINUX;
        return EnumOS.UNKNOWN;
    }

    public enum EnumOS {
        LINUX, SOLARIS, WINDOWS, OSX, UNKNOWN
    }
}
