package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.util.IRegistry;
import legacyconnector.v1_7_10.net.minecraft.util.RegistryDefaulted;

/**
 * The 1.7.10 dispenser: the shell plus the behaviour registry mods add their item behaviours to. Unregistered items
 * answer the default behaviour object (the synthesized {@code BehaviorDefaultDispenseItem}), as in vanilla, so
 * {@code getObject(...).getClass()} checks work. Registered behaviours also become modern dispenser behaviours.
 */
public class BlockDispenser extends legacyconnector.v1_7_10.net.minecraft.block.BlockContainer {
    /** Behaviours mods register are handed to the modern dispenser as well. */
    public static final IRegistry dispenseBehaviorRegistry = new RegistryDefaulted(defaultBehavior()) {
        @Override
        public void putObject(Object key, Object value) {
            super.putObject(key, value);
            org.kylepitcock.legacyconnector.v1_7_10.bridge.DispenserBridge.register(key, value);
        }
    };

    private static Object defaultBehavior() {
        for (String prefix : new String[] {"legacyconnector.v1_7_10.", "legacyconnector_synth.v1_7_10."}) {
            try {
                return Class.forName(prefix + "net.minecraft.dispenser.BehaviorDefaultDispenseItem").getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError e) {
                // try the next package
            }
        }
        return new Object();
    }

    public BlockDispenser(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDispenser(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockDispenser() {
        this(legacyconnector.v1_7_10.net.minecraft.block.material.Material.rock);
    }

    /** The direction a dispenser (metadata) faces. */
    public static legacyconnector.v1_7_10.net.minecraft.util.EnumFacing getFacing(int metadata) {
        return legacyconnector.v1_7_10.net.minecraft.util.EnumFacing.getFront(metadata & 7);
    }

    /** The point just in front of the dispenser where items leave it. */
    public static legacyconnector.v1_7_10.net.minecraft.dispenser.IPosition getIPositionFromBlockSource(legacyconnector.v1_7_10.net.minecraft.dispenser.IBlockSource source) {
        var facing = getFacing(source.getBlockMetadata());
        return new legacyconnector.v1_7_10.net.minecraft.dispenser.PositionImpl(source.getX() + 0.7D * facing.getFrontOffsetX(),
                source.getY() + 0.7D * facing.getFrontOffsetY(), source.getZ() + 0.7D * facing.getFrontOffsetZ());
    }
}
