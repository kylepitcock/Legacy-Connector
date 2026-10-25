package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;

public class BlockFluidClassic extends BlockFluidBase {
    public BlockFluidClassic(Fluid fluid, Material material) {
        super(fluid, material);
    }

    public BlockFluidClassic(Fluid fluid, Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor mapColor) {
        super(fluid, material, mapColor);
    }
}
