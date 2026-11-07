package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/** Beta 1.7.3 {@code EntityPlayer}: inventory and name. The layer's player is a view of the modern one ({@code PlayerViewB173}). */
public abstract class PlayerEntity extends LivingEntity {
    public PlayerInventory inventory = new PlayerInventory(this);
    public String name;
    public String playerCapeUrl;

    public PlayerEntity(World world) {
        super(world);
        this.standingEyeHeight = 1.62F;
    }

    public ItemStack getHand() {
        return inventory.getSelectedItem();
    }

    @Override
    public ItemStack getHeldItem() {
        return inventory.getSelectedItem();
    }

    public void sendMessage(String message) {}

    /** MCP {@code displayGUIChest}: a chest screen over the inventory (a modern chest menu). */
    public void openChestScreen(Inventory inventory) {
        MenusB173.openChest(this, inventory);
    }

    /** MCP {@code closeScreen}. */
    protected void closeHandledScreen() {
        MenusB173.close(this);
    }

    /** MCP {@code dropPlayerItem}. */
    public void dropItem(ItemStack stack) {
        dropItem(stack, false);
    }

    /** MCP {@code dropPlayerItemWithRandomChoice}: the modern player throws the stack (scattered when {@code scatter}). */
    public void dropItem(ItemStack stack, boolean scatter) {
        MenusB173.drop(this, stack, scatter);
    }
}
