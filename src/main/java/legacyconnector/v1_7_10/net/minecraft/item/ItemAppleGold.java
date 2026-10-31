package legacyconnector.v1_7_10.net.minecraft.item;

/** The golden apple (and the enchanted one as its subtype): a food, so {@code instanceof ItemFood} holds for it. */
public class ItemAppleGold extends ItemFood {
    public ItemAppleGold(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemAppleGold(int heal, float saturation, boolean wolfMeat) {
        super(heal, saturation, wolfMeat);
        setHasSubtypes(true);
    }
}
