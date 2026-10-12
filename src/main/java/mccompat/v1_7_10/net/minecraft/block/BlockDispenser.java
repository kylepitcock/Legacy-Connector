package mccompat.v1_7_10.net.minecraft.block;

import mccompat.v1_7_10.net.minecraft.util.IRegistry;
import mccompat.v1_7_10.net.minecraft.util.RegistryDefaulted;

/**
 * The 1.7.10 dispenser: the shell plus the behaviour registry mods add their item behaviours to. Unregistered items
 * answer the default behaviour object (the synthesized {@code BehaviorDefaultDispenseItem}), as in vanilla, so
 * {@code getObject(...).getClass()} checks work. The modern dispenser does not consult the registry yet.
 */
public class BlockDispenser extends mccompat.v1_7_10.net.minecraft.block.BlockContainer {
    public static final IRegistry dispenseBehaviorRegistry = new RegistryDefaulted(defaultBehavior());

    private static Object defaultBehavior() {
        for (String prefix : new String[] {"mccompat.v1_7_10.", "mccompat_synth.v1_7_10."}) {
            try {
                return Class.forName(prefix + "net.minecraft.dispenser.BehaviorDefaultDispenseItem").getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError e) {
                // try the next package
            }
        }
        return new Object();
    }

    public BlockDispenser(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDispenser(mccompat.v1_7_10.net.minecraft.block.material.Material material) {
        super(material);
    }

    public BlockDispenser() {
        this(mccompat.v1_7_10.net.minecraft.block.material.Material.rock);
    }
}
