package legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes;

public interface IAttribute {
    String getAttributeUnlocalizedName();

    double clampValue(double value);

    double getDefaultValue();

    boolean getShouldWatch();

    /** Stand-in for a vanilla attribute constant only the synthesized API declares (by "owner.field" name). */
    static IAttribute mcc$synthDefault(String field) {
        if (field.endsWith("EntityZombie.field_110186_bp")) {
            return new RangedAttribute("zombie.spawnReinforcements", 0.0, 0.0, 1.0).setDescription("Spawn Reinforcements Chance");
        }
        if (field.endsWith("EntityHorse.field_110271_bv")) {
            return new RangedAttribute("horse.jumpStrength", 0.7, 0.0, 2.0).setDescription("Jump Strength").setShouldWatch(true);
        }
        return new RangedAttribute(field, 0.0, -Double.MAX_VALUE, Double.MAX_VALUE);
    }
}
