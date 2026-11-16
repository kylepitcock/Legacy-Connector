package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * A mod class that extends Forge 1.20.1's {@code ModConfig} (to move its server config file out of the world folder through its own
 * {@code ConfigFileTypeHandler}) cannot link on NeoForge 1.21.1, where {@code ModConfig} is final. Such a class moves to
 * {@code LegacyModConfig}, which holds the constructor arguments and registers them with FML when the mod adds the object to its container:
 * <ul>
 *   <li>{@code super(...)} calls and calls on {@code this} that name ModConfig go to {@code LegacyModConfig};</li>
 *   <li>{@code ModContainer.addConfig(x)} becomes {@code LegacyModConfig.addTo(container, x)};</li>
 *   <li>{@code instanceof X} / {@code checkcast X} first map the value through {@code LegacyModConfig.wrapperOf}, so the real config of a
 *       {@code ModConfigEvent} finds the mod's object.</li>
 * </ul>
 * Runs on the names after the Forge to NeoForge renames.
 */
final class ModConfigSubclass1201 {
    private ModConfigSubclass1201() {}

    private static final String MOD_CONFIG = "net/neoforged/fml/config/ModConfig";
    private static final String LEGACY = "legacyconnector/v1_20_1/net/minecraftforge/fml/config/LegacyModConfig";
    private static final String CONTAINER = "net/neoforged/fml/ModContainer";
    private static final String ADD_CONFIG_HELPER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/RoundCompat";

    /** Returns the number of classes moved. */
    static int apply(List<ClassNode> nodes) {
        Set<String> subclasses = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ClassNode n : nodes) {
                if (subclasses.contains(n.name)) continue;
                if (MOD_CONFIG.equals(n.superName) || subclasses.contains(n.superName)) {
                    subclasses.add(n.name);
                    changed = true;
                }
            }
        }
        boolean addConfigCalls = false;
        for (ClassNode n : nodes) {
            boolean sub = subclasses.contains(n.name);
            if (sub && MOD_CONFIG.equals(n.superName)) n.superName = LEGACY;
            for (MethodNode m : n.methods) {
                if (m.instructions == null) continue;
                for (AbstractInsnNode in : m.instructions.toArray()) {
                    if (in instanceof MethodInsnNode mi) {
                        if (sub && mi.owner.equals(MOD_CONFIG) && (mi.getOpcode() == Opcodes.INVOKESPECIAL || mi.getOpcode() == Opcodes.INVOKEVIRTUAL)) {
                            mi.owner = LEGACY;
                        } else if (mi.name.equals("addConfig") // also without a subclass in this jar: a config class of another jar (Ars Nouveau ANModConfig) may be a facade
                                && (mi.owner.equals(CONTAINER) && mi.desc.equals("(L" + MOD_CONFIG + ";)V")
                                        // the call table already moved it to a no-op helper (the config of a real ModConfig is tracked when it is made)
                                        || mi.owner.equals(ADD_CONFIG_HELPER) && mi.desc.equals("(L" + CONTAINER + ";L" + MOD_CONFIG + ";)V"))) {
                            m.instructions.set(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, LEGACY, "addTo", "(L" + CONTAINER + ";Ljava/lang/Object;)V", false));
                            addConfigCalls = true;
                        }
                    } else if (in instanceof TypeInsnNode t && (t.getOpcode() == Opcodes.INSTANCEOF || t.getOpcode() == Opcodes.CHECKCAST) && subclasses.contains(t.desc)) {
                        m.instructions.insertBefore(t, new MethodInsnNode(Opcodes.INVOKESTATIC, LEGACY, "wrapperOf", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                    }
                }
            }
        }
        return subclasses.size();
    }
}
