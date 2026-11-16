package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x's armour absorption record: the shim's own class (fields {@code Priority}, {@code AbsorbMax}, {@code AbsorbRatio}, {@code Slot}) under the 3.x name. */
public class ArmorProperties extends legacyconnector.v1_7_10.net.minecraftforge.common.ISpecialArmor.ArmorProperties {
    public ArmorProperties(int priority, double ratio, int max) {
        super(priority, ratio, max);
    }

    @Override
    public ArmorProperties copy() {
        ArmorProperties c = new ArmorProperties(Priority, AbsorbRatio, AbsorbMax);
        c.Armor = Armor;
        c.Toughness = Toughness;
        c.Slot = Slot;
        return c;
    }

    @Override
    public String toString() {
        return String.format("%d, %d, %f, %d", Priority, AbsorbMax, AbsorbRatio, Slot);
    }
}
