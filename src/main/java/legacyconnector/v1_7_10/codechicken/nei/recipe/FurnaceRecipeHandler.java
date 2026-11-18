package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.TreeSet;
import legacyconnector.v1_7_10.codechicken.nei.ItemList;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFurnace;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;

/**
 * NEI's smelting handler. Mods subclass it for furnace-like machines (Traincraft's distillation tower and open hearth furnace) and
 * fill {@link #arecipes} themselves; the vanilla smelting list is JEI's own.
 */
public class FurnaceRecipeHandler extends TemplateRecipeHandler {
    /** A fuel of the furnace: its stack (at the fuel slot of the furnace recipe) and the ticks it burns. */
    public static class FuelPair {
        public PositionedStack stack;
        public int burnTime;

        public FuelPair(ItemStack ingred, int burnTime) {
            this.stack = new PositionedStack(ingred, 51, 42, false);
            this.burnTime = burnTime;
        }
    }

    /** NEI: every fuel of the item list (handlers of furnace-like machines list them as their fuel choices); filled when the NEI plugins load. */
    public static ArrayList<FuelPair> afuels = new ArrayList<>();
    /** NEI: item ids a plugin excludes from {@link #afuels}. */
    public static TreeSet<Integer> efuels = new TreeSet<>();

    /** Fills {@link #afuels} from the item list: each stack the furnace burns. */
    public static void mcc$findFuels() {
        ArrayList<FuelPair> found = new ArrayList<>();
        for (ItemStack stack : ItemList.items) {
            try {
                int burn = TileEntityFurnace.getItemBurnTime(stack);
                if (burn > 0) found.add(new FuelPair(stack.copy(), burn));
            } catch (RuntimeException | LinkageError e) {
                // an item whose fuel value cannot be asked
            }
        }
        afuels.clear();
        afuels.addAll(found);
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(50, 23, 18, 18), "fuel"));
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "smelting"));
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return null;
    }

    @Override
    public String getRecipeName() {
        return "Smelting";
    }

    @Override
    public String getGuiTexture() {
        return "textures/gui/container/furnace.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "smelting";
    }
}
