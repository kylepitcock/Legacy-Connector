package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A pressure plate: pressed by everything, by mobs, or by players only. */
public class BlockPressurePlate extends Block {
    public enum Sensitivity {
        everything, mobs, players
    }

    private final Sensitivity sensitivity;

    public BlockPressurePlate(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.sensitivity = Sensitivity.everything;
    }

    public BlockPressurePlate(String texture, Material material, Sensitivity sensitivity) {
        super(material);
        this.sensitivity = sensitivity;
    }

    public Sensitivity mcc$sensitivity() {
        return sensitivity;
    }
}
