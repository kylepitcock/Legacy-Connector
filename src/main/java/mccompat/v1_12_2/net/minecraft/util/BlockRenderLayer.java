package mccompat.v1_12_2.net.minecraft.util;

public enum BlockRenderLayer {
    SOLID("Solid"), CUTOUT_MIPPED("Mipped Cutout"), CUTOUT("Cutout"), TRANSLUCENT("Translucent");

    private final String layerName;

    BlockRenderLayer(String layerName) {
        this.layerName = layerName;
    }

    @Override
    public String toString() {
        return layerName;
    }
}
