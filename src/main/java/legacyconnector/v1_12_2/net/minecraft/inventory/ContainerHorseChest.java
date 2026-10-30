package legacyconnector.v1_12_2.net.minecraft.inventory;

/** A horse's inventory (saddle, armor or decor, chest slots). */
public class ContainerHorseChest extends InventoryBasic {
    public ContainerHorseChest(String inventoryTitle, int slotCount) {
        super(inventoryTitle, false, slotCount);
    }

    public ContainerHorseChest(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent inventoryTitle, int slotCount) {
        super(inventoryTitle, slotCount);
    }
}
