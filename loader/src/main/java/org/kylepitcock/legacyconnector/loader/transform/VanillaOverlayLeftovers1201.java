package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Forge's {@code VanillaGuiOverlay} is an enum ({@code HOTBAR}, {@code id()}, {@code type()}); the curated alias maps it to NeoForge's
 * {@code VanillaGuiLayers}, an interface of {@code ResourceLocation} constants. Code that only compares overlays ({@code event.getOverlay() ==
 * VanillaGuiOverlay.HOTBAR.type()}, {@code .id()}) keeps the enum's shape after the rename and references members the native class does not have (the
 * constants typed as the enum, {@code id()}, {@code type()}). What is still typed as the enum after the dedicated adapters ran is pointed back at the shim's
 * enum, whose constants carry the NeoForge layer ids and whose {@code type()} is what {@code RenderGuiLayerEvent.getOverlay()} answers (see
 * {@code ForgeApiTailClient1201.layerOverlay}). Uses the adapters already turned into {@code ResourceLocation} layer ids are untouched.
 */
final class VanillaOverlayLeftovers1201 {
    private VanillaOverlayLeftovers1201() {}

    private static final String NATIVE = "net/neoforged/neoforge/client/gui/VanillaGuiLayers";
    private static final String SHIM = "legacyconnector/v1_20_1/net/minecraftforge/client/gui/overlay/VanillaGuiOverlay";
    private static final String NATIVE_DESC = "L" + NATIVE + ";";
    private static final String SHIM_DESC = "L" + SHIM + ";";

    /** @return the number of references pointed at the shim enum */
    static int apply(ClassNode node) {
        int n = 0;
        for (FieldNode f : node.fields) {
            if (f.desc.contains(NATIVE_DESC)) {
                f.desc = f.desc.replace(NATIVE_DESC, SHIM_DESC);
                n++;
            }
        }
        for (MethodNode m : node.methods) {
            if (m.desc.contains(NATIVE_DESC)) {
                m.desc = m.desc.replace(NATIVE_DESC, SHIM_DESC);
                n++;
            }
            if (m.instructions == null) continue;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = in.getNext()) {
                if (in instanceof FieldInsnNode f) {
                    // HOTBAR typed as the enum (the native constants are ResourceLocations and stay)
                    boolean constant = f.owner.equals(NATIVE) && f.desc.equals(NATIVE_DESC);
                    if (constant) f.owner = SHIM;
                    if (f.desc.contains(NATIVE_DESC)) {
                        f.desc = f.desc.replace(NATIVE_DESC, SHIM_DESC);
                        n++;
                    } else if (constant) {
                        n++;
                    }
                } else if (in instanceof MethodInsnNode c) {
                    boolean touched = false;
                    if (c.owner.equals(NATIVE) && (c.getOpcode() != Opcodes.INVOKESTATIC || c.name.equals("values") || c.name.equals("valueOf"))) {
                        c.owner = SHIM;
                        touched = true;
                    }
                    if (c.desc.contains(NATIVE_DESC)) {
                        c.desc = c.desc.replace(NATIVE_DESC, SHIM_DESC);
                        touched = true;
                    }
                    if (touched) n++;
                } else if (in instanceof TypeInsnNode t && t.desc.equals(NATIVE)) {
                    t.desc = SHIM;
                    n++;
                }
            }
        }
        return n;
    }
}
