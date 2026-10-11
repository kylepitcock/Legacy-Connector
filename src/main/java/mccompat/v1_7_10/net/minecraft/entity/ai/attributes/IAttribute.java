package mccompat.v1_7_10.net.minecraft.entity.ai.attributes;

public interface IAttribute {
    String getAttributeUnlocalizedName();

    double clampValue(double value);

    double getDefaultValue();

    boolean getShouldWatch();
}
