package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A storage block (iron, gold, diamond, emerald, lapis, coal...): an iron-material cube with its own map colour. */
public class BlockCompressed extends Block {
    private final MapColor field_150202_a;

    public BlockCompressed(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150202_a = null;
    }

    public BlockCompressed(MapColor color) {
        super(Material.iron);
        this.field_150202_a = color;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    public MapColor getMapColor(int meta) {
        return field_150202_a != null ? field_150202_a : super.getMapColor(meta);
    }
}
