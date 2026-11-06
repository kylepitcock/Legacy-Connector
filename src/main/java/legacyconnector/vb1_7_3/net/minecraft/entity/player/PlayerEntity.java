package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

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
}
