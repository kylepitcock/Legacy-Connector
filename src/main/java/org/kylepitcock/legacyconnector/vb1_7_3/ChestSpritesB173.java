package org.kylepitcock.legacyconnector.vb1_7_3;

import java.awt.image.BufferedImage;

/**
 * Beta's vanilla chest sprites (terrain.png 25 top, 26 side, 27 front with the lock) as block textures. Modern chests are entities with
 * smaller, inset tiles, so the Beta tiles are recreated here pixel for pixel: each row is a string of palette symbols.
 */
final class ChestSpritesB173 {
    private ChestSpritesB173() {}
    private static final String SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";
    private static final int[] PALETTE = {0x2B261F, 0x332E25, 0x373127, 0x363026, 0x28241D, 0x84622F, 0x90662A, 0x926323, 0x7F5F22, 0x956C2E, 0x8F691D, 0xA47227, 0xAB792D, 0xA76E1F, 0x352F26, 0x8E6126, 0xA26B23, 0x362F25, 0x2A251D, 0x382F25, 0x322D26, 0x463E32, 0x413B2F, 0x453D31, 0x443C30, 0x1F1C17, 0x775A30, 0x73562E, 0x695229, 0x755429, 0x453C2F, 0x825B26, 0x2A261E, 0x2C251D, 0x2A251E, 0x27231E, 0x211D17, 0x855C1F, 0x8B6224, 0x885919, 0xCDCDCD, 0xA5A5A5, 0x795825, 0xC2C2C2, 0x868686, 0x543E16, 0x2C271F, 0x767676, 0x2C271E, 0x614927, 0x919191, 0x45351A, 0x734F1E, 0x614116, 0x77501C};
    private static final String[] TOP = {
            "abbbbccdbcccccce",
            "cfghggiiiiigigfb",
            "cjkkkkllmmmlnmjb",
            "opqnnnnnnnqqqqhr",
            "cikkkmmkmkknllgc",
            "binlllmkllllkkib",
            "cjmlmmmkkknmmmjc",
            "rhqqqqqnnnqqqqhr",
            "cjmmmnkmmlllmmjb",
            "bikkkmllkkknmkib",
            "cgllmmkkkkllllgb",
            "rpnnqqqnnnnnqqpr",
            "cjmmmkmkkkkklnjc",
            "dinmmmmkmllkkkid",
            "cfiiijjijjigjjfc",
            "srrtootorrrouoob"};
    private static final String[] SIDE = {
            "vwwwwvvxwvvvvvvb",
            "vmlnllkkkkklklme",
            "vmkkkkllmmmlnmme",
            "yqqnnnnnnnqqqqns",
            "viiiijjijiihggga",
            "wbrdddcbddddbbbz",
            "vAABAAACCCDAAAAa",
            "EhppppphhhppppFs",
            "vmmmmnkmmlllmmje",
            "wkkkkmllkkknmkie",
            "vlllmmkkkkllllge",
            "Eqnnqqqnnnnnqqps",
            "vmmmmkmkkkkklnja",
            "xknmmmmkmllkkkiG",
            "vjiiijjijjigjjfa",
            "sssHIIHIsssIJIIK"};
    private static final String[] FRONT = {
            "vwwwwvvxwvvvvvvb",
            "vmlnllkkkkklklme",
            "vmkkkklLMmmlnmme",
            "yqqnnnNOPNqqqqns",
            "viiiijQRSTihggga",
            "wbrdddUPVWddbbbz",
            "vAABAAXYVZDAAAAa",
            "Ehpppp0112ppppFs",
            "vmmmmnkmmlllmmje",
            "wkkkkmllkkknmkie",
            "vlllmmkkkkllllge",
            "Eqnnqqqnnnnnqqps",
            "vmmmmkmkkkkklnja",
            "xknmmmmkmllkkkiG",
            "vjiiijjijjigjjfa",
            "sssHIIHIsssIJIIK"};

    static BufferedImage sprite(int index) {
        String[] rows = switch (index) {case 25 -> TOP; case 26 -> SIDE; case 27 -> FRONT; default -> null;};
        return rows == null ? null : image(rows);
    }

    static BufferedImage image(String[] rows) {
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) out.setRGB(x, y, 0xFF000000 | PALETTE[SYMBOLS.indexOf(rows[y].charAt(x))]);
        return out;
    }
}
