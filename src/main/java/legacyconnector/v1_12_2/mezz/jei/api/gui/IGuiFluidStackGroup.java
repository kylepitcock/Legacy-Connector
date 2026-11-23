package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: the fluid slots. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IGuiFluidStackGroup extends IGuiIngredientGroup<legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack> {
    /** JEI 4 redeclared these with the fluid type, so mods call them by that descriptor. */
    @Override
    void set(int slotIndex, legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack fluidStack);

    @Override
    void set(int slotIndex, java.util.List<legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack> fluidStacks);
    void init(int slotIndex, boolean input, int xPosition, int yPosition, int width, int height, int capacityMb, boolean showCapacity, IDrawable overlay);
}
