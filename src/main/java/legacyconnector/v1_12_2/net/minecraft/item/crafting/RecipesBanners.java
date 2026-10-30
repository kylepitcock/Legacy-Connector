package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.tileentity.BannerPattern;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBanner;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.DyeUtils;

/** 1.12.2's dynamic banner recipes: adding a pattern to a banner and copying a banner's patterns onto a plain one. */
public class RecipesBanners {
    private static boolean isBanner(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.BANNER;
    }

    /** A banner, one dye and the pattern's shape in dye (or the pattern's item) add that pattern, at most six. */
    public static class RecipeAddPattern extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            boolean banner = false;
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (isBanner(stack)) {
                    if (banner || TileEntityBanner.getPatterns(stack) >= 6) return false;
                    banner = true;
                }
            }
            return banner && matchPatterns(inv) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack result = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (isBanner(stack)) {
                    result = stack.copy();
                    result.setCount(1);
                    break;
                }
            }
            BannerPattern pattern = matchPatterns(inv);
            if (pattern != null && !result.isEmpty()) {
                int color = 0;
                for (int j = 0; j < inv.getSizeInventory(); ++j) {
                    ItemStack stack = inv.getStackInSlot(j);
                    if (DyeUtils.isDye(stack)) {
                        color = DyeUtils.colorFromStack(stack).map(c -> c.getDyeDamage()).orElse(0);
                        break;
                    }
                }
                NBTTagCompound be = result.getOrCreateSubCompound("BlockEntityTag");
                NBTTagList list;
                if (be.hasKey("Patterns", 9)) {
                    list = be.getTagList("Patterns", 10);
                } else {
                    list = new NBTTagList();
                    be.setTag("Patterns", list);
                }
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Pattern", pattern.getHashname());
                entry.setInteger("Color", color);
                list.appendTag(entry);
            }
            return result;
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
                if (!stack.isEmpty()) list.set(i, stack.getItem().getContainerItem(stack));
            }
            return list;
        }

        /** The pattern the grid makes, or null. */
        private BannerPattern matchPatterns(InventoryCrafting inv) {
            for (BannerPattern pattern : BannerPattern.values()) {
                if (!pattern.hasPattern()) continue;
                boolean ok = true;
                if (pattern.hasPatternItem()) {
                    boolean item = false;
                    boolean dye = false;
                    for (int i = 0; i < inv.getSizeInventory() && ok; ++i) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (stack.isEmpty() || isBanner(stack)) continue;
                        if (DyeUtils.isDye(stack)) {
                            if (dye) ok = false;
                            dye = true;
                        } else {
                            if (item || !stack.isItemEqual(pattern.getPatternItem())) ok = false;
                            item = true;
                        }
                    }
                    if (!item || !dye) ok = false;
                } else if (inv.getSizeInventory() == pattern.getPatterns().length * pattern.getPatterns()[0].length()) {
                    int color = -1;
                    for (int k = 0; k < inv.getSizeInventory() && ok; ++k) {
                        int row = k / 3;
                        int column = k % 3;
                        ItemStack stack = inv.getStackInSlot(k);
                        char c = pattern.getPatterns()[row].charAt(column);
                        if (!stack.isEmpty() && !isBanner(stack)) {
                            if (!DyeUtils.isDye(stack)) {
                                ok = false;
                                break;
                            }
                            int here = DyeUtils.rawDyeDamageFromStack(stack);
                            if (color != -1 && color != here || c == ' ') {
                                ok = false;
                                break;
                            }
                            color = here;
                        } else if (c != ' ') {
                            ok = false;
                        }
                    }
                } else {
                    ok = false;
                }
                if (ok) return pattern;
            }
            return null;
        }

        @Override
        public boolean isDynamic() {
            return true;
        }

        @Override
        public boolean canFit(int width, int height) {
            return width >= 3 && height >= 3;
        }
    }

    /** A banner with patterns and a plain banner of the same colour: the plain one gets the patterns, the original is kept. */
    public static class RecipeDuplicatePattern extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            ItemStack patterned = ItemStack.EMPTY;
            ItemStack plain = ItemStack.EMPTY;
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!isBanner(stack)) return false;
                if (TileEntityBanner.getPatterns(stack) > 0) {
                    if (!patterned.isEmpty()) return false;
                    patterned = stack;
                } else {
                    if (!plain.isEmpty()) return false;
                    plain = stack;
                }
            }
            return !patterned.isEmpty() && !plain.isEmpty() && TileEntityBanner.getColor(patterned) == TileEntityBanner.getColor(plain);
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && TileEntityBanner.getPatterns(stack) > 0) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    return copy;
                }
            }
            return ItemStack.EMPTY;
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
                if (stack.isEmpty()) continue;
                if (stack.getItem().hasContainerItem(stack)) {
                    list.set(i, stack.getItem().getContainerItem(stack));
                } else if (stack.hasTagCompound() && TileEntityBanner.getPatterns(stack) > 0) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    list.set(i, copy);
                }
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
