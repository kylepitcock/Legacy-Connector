package legacyconnector.v1_12_2.net.minecraft.realms;

/** 1.12.2's version constants (the protocol and version mods compare against are the legacy game's). */
public class RealmsSharedConstants {
    public static int NETWORK_PROTOCOL_VERSION = 340;
    public static int TICKS_PER_SECOND = 20;
    public static String VERSION_STRING = "1.12.2";
    public static char[] ILLEGAL_FILE_CHARACTERS = {'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
}
