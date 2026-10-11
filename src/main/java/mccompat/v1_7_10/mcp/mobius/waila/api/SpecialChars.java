package mccompat.v1_7_10.mcp.mobius.waila.api;

/** Waila's formatting characters. Colour codes are the vanilla "§" codes, which modern text still renders. */
public class SpecialChars {
    public static final String MCStyle = "§";
    public static final String BLACK = MCStyle + "0";
    public static final String DBLUE = MCStyle + "1";
    public static final String DGREEN = MCStyle + "2";
    public static final String DAQUA = MCStyle + "3";
    public static final String DRED = MCStyle + "4";
    public static final String DPURPLE = MCStyle + "5";
    public static final String GOLD = MCStyle + "6";
    public static final String LGRAY = MCStyle + "7";
    public static final String GRAY = MCStyle + "8";
    public static final String BLUE = MCStyle + "9";
    public static final String GREEN = MCStyle + "a";
    public static final String AQUA = MCStyle + "b";
    public static final String RED = MCStyle + "c";
    public static final String LPURPLE = MCStyle + "d";
    public static final String YELLOW = MCStyle + "e";
    public static final String WHITE = MCStyle + "f";
    public static final String OBF = MCStyle + "k";
    public static final String BOLD = MCStyle + "l";
    public static final String STRIKE = MCStyle + "m";
    public static final String UNDER = MCStyle + "n";
    public static final String ITALIC = MCStyle + "o";
    public static final String RESET = MCStyle + "r";
    public static final String WailaStyle = "¤";
    public static final String WailaIcon = "¥";
    public static final String TAB = WailaStyle + WailaStyle + "a";
    public static final String ALIGNRIGHT = WailaStyle + WailaStyle + "b";
    public static final String ALIGNCENTER = WailaStyle + WailaStyle + "c";
    public static final String HPROGRESS = WailaStyle + WailaStyle + "d";
    public static final String HICON = WailaStyle + WailaStyle + "e";
    public static final String HHEART = WailaStyle + WailaIcon + "a";
    public static final String HHALFHEART = WailaStyle + WailaIcon + "b";
    public static final String HNHEART = WailaStyle + WailaIcon + "c";

    public static String getRenderString(String name, String... args) {
        StringBuilder s = new StringBuilder(WailaStyle + WailaStyle + "{" + name);
        for (String a : args) s.append("+").append(a);
        return s.append("}").toString();
    }
}
