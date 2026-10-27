package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2: something with a (possibly custom) name: inventories, nameable tile entities. */
public interface IWorldNameable {
    String getName();

    boolean hasCustomName();

    legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName();
}
