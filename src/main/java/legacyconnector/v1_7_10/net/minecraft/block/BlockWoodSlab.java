package legacyconnector.v1_7_10.net.minecraft.block;

/** The wooden slab (half or double) of the six wood types; mods build slabs of their own woods on it. */
public class BlockWoodSlab extends BlockSlab {
    public BlockWoodSlab(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockWoodSlab(boolean doubleSlab) {
        super(doubleSlab, legacyconnector.v1_7_10.net.minecraft.block.material.Material.wood);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }
}
