package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBanner;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's shield recipes: decorating a plain shield with a banner. */
public class ShieldRecipes {
    /** A plain shield and a banner: the shield takes the banner's colour and patterns. */
    public static class Decoration extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            ItemStack shield = ItemStack.EMPTY;
            ItemStack banner = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() == Items.BANNER) {
                    if (!banner.isEmpty()) return false;
                    banner = stack;
                } else {
                    if (stack.getItem() != Items.SHIELD || !shield.isEmpty() || stack.getSubCompound("BlockEntityTag") != null) return false;
                    shield = stack;
                }
            }
            return !shield.isEmpty() && !banner.isEmpty();
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack banner = ItemStack.EMPTY;
            ItemStack shield = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() == Items.BANNER) banner = stack;
                else if (stack.getItem() == Items.SHIELD) shield = stack.copy();
            }
            if (shield.isEmpty()) return shield;
            NBTTagCompound tag = banner.getSubCompound("BlockEntityTag");
            NBTTagCompound copy = tag == null ? new NBTTagCompound() : tag.copy();
            copy.setInteger("Base", TileEntityBanner.getColor(banner).getDyeDamage());
            shield.setTagInfo("BlockEntityTag", copy);
            return shield;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return ItemStack.EMPTY;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            NonNullList<ItemStack> list = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
            for (int i = 0; i < list.size(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem().hasContainerItem(stack)) list.set(i, stack.getItem().getContainerItem(stack));
            }
            return list;
        }

        @Override
        public boolean isDynamic() {
            return true;
        }

        @Override
        public boolean canFit(int width, int height) {
            return width * height >= 2;
        }
    }
}
