package legacyconnector.v1_7_10.net.minecraft.block;

/** The enchanting table block; custom tables subclass it (their tile entity and GUI are the mod's own). */
public class BlockEnchantmentTable extends BlockContainer {
    public BlockEnchantmentTable(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockEnchantmentTable(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockEnchantmentTable() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.rock);
    }
}
