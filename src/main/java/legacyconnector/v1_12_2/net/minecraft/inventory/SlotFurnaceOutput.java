package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The output slot of a furnace: taking from it gives the smelting experience and counts as smelting. */
public class SlotFurnaceOutput extends Slot {
    private final EntityPlayer player;
    private int removeCount;

    public SlotFurnaceOutput(EntityPlayer player, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition) {
        super(inventoryIn, slotIndex, xPosition, yPosition);
        this.player = player;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) removeCount += Math.min(amount, getStack().getCount());
        return super.decrStackSize(amount);
    }

    @Override
    public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
        onCrafting(stack);
        super.onTake(thePlayer, stack);
        return stack;
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        removeCount += amount;
        onCrafting(stack);
    }

    /** The experience of the smelted items (fractions by chance) as orbs at the player, and the smelted event. */
    @Override
    protected void onCrafting(ItemStack stack) {
        var world = player.getEntityWorld();
        stack.onCrafting(world, player, removeCount);
        var modern = player.mcc$player();
        if (world != null && !world.isRemote && modern != null && modern.level() instanceof net.minecraft.server.level.ServerLevel level) {
            int i = removeCount;
            float f = legacyconnector.v1_12_2.net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingExperience(stack);
            if (f == 0.0F) {
                i = 0;
            } else if (f < 1.0F) {
                int j = net.minecraft.util.Mth.floor((float) i * f);
                if (j < net.minecraft.util.Mth.ceil((float) i * f) && Math.random() < (double) ((float) i * f - (float) j)) ++j;
                i = j;
            }
            if (i > 0) net.minecraft.world.entity.ExperienceOrb.award(level, modern.position(), i);
        }
        removeCount = 0;
        if (modern != null) net.neoforged.neoforge.event.EventHooks.firePlayerSmeltedEvent(modern, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack));
    }
}
