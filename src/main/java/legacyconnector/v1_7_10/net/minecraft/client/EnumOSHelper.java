package legacyconnector.v1_7_10.net.minecraft.client;

import legacyconnector.v1_7_10.net.minecraft.util.Util;

/**
 * Vanilla's switch map of {@code EnumOS} (1.4.7 to 1.6.4: {@code field_90049_a}, case numbers 1 to 4 for Linux, Solaris, Windows, macOS). Mods that
 * switch over the operating system (to find the application data folder) compile against it instead of a map of their own.
 */
public class EnumOSHelper {
    public static final int[] field_90049_a = new int[Util.EnumOS.values().length];

    static {
        field_90049_a[Util.EnumOS.LINUX.ordinal()] = 1;
        field_90049_a[Util.EnumOS.SOLARIS.ordinal()] = 2;
        field_90049_a[Util.EnumOS.WINDOWS.ordinal()] = 3;
        field_90049_a[Util.EnumOS.OSX.ordinal()] = 4;
    }
}
