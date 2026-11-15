package legacyconnector.v1_7_10.net.minecraft.inventory;

/** The inventory of a horse (1.7.10 {@code AnimalChest}; slot 0 saddle, slot 1 armor, the rest the chest of a donkey or mule). */
public class AnimalChest extends InventoryBasic {
    public AnimalChest(String title, int slots) {
        super(title, false, slots);
    }
}
