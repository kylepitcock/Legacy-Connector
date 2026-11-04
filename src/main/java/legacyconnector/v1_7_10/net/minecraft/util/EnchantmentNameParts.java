package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.Random;

/** The Standard Galactic Alphabet gibberish the enchanting table shows. */
public class EnchantmentNameParts {
    public static final EnchantmentNameParts instance = new EnchantmentNameParts();
    private final Random rand = new Random();
    private final String[] namePartsArray = ("the elder scrolls klaatu berata niktu xyzzy bless curse light darkness fire air earth water hot dry cold wet "
            + "ignite snuff embiggen twist shorten stretch fiddle destroy imbue galvanize enchant free limited range of towards inside sphere cube "
            + "self other ball mental physical grow shrink demon elemental spirit animal creature beast humanoid undead fresh stale phnglui mglwnafh "
            + "cthulhu rlyeh wgahnagl fhtagn baguette").split(" ");

    public String generateNewRandomName() {
        int words = rand.nextInt(2) + 3;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words; i++) {
            if (i > 0) sb.append(' ');
            sb.append(namePartsArray[rand.nextInt(namePartsArray.length)]);
        }
        return sb.toString();
    }

    public void reseedRandomGenerator(long seed) {
        rand.setSeed(seed);
    }
}
