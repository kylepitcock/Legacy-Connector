package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * Migrates AlienEvo's missing-model catch from GeckoLib's removed
 * GeckoLibException to the RuntimeException now thrown by GeckoLib 4.9.3.
 *
 * The catch type is also embedded in StackMapTable frames and local-variable
 * debug metadata. All of those references must be updated together or the
 * class can still fail verification/loading even after its exception table
 * has been changed.
 */
final class AlienEvoGeckoException1201 implements Opcodes {
    static final String TARGET = "net/habb/alienevo/compat/geckolib/renderlayer/AlienGeckoRenderLayer";

    private static final String METHOD_NAME = "safelyGetBakedModel";
    private static final String METHOD_DESC =
            "(Lnet/habb/alienevo/compat/geckolib/renderlayer/AlienLayerState;)"
                    + "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;";
    private static final String LEGACY_INTERNAL = "software/bernie/geckolib/GeckoLibException";
    private static final String CURRENT_INTERNAL = "java/lang/RuntimeException";
    private static final String LEGACY_DESC = "L" + LEGACY_INTERNAL + ";";
    private static final String CURRENT_DESC = "L" + CURRENT_INTERNAL + ";";

    private AlienEvoGeckoException1201() {}

    /** Returns the number of catch handlers migrated. */
    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode target = null;
        int matchingMethods = 0;
        for (MethodNode method : node.methods) {
            if (METHOD_NAME.equals(method.name)) {
                target = method;
                matchingMethods++;
            }
        }
        if (matchingMethods != 1 || target == null || !METHOD_DESC.equals(target.desc)) {
            throw new IllegalStateException("AlienEvo AlienGeckoRenderLayer has an unexpected "
                    + "safelyGetBakedModel method shape");
        }

        int oldCatchTypes = 0;
        int currentCatchTypes = 0;
        for (TryCatchBlockNode block : target.tryCatchBlocks) {
            if (LEGACY_INTERNAL.equals(block.type)) oldCatchTypes++;
            else if (CURRENT_INTERNAL.equals(block.type)) currentCatchTypes++;
        }

        int oldFrameTypes = 0;
        int currentFrameTypes = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction instanceof FrameNode frame) {
                int[] counts = replaceFrameTypes(frame);
                oldFrameTypes += counts[0];
                currentFrameTypes += counts[1];
            }
        }

        int oldLocalTypes = 0;
        int currentLocalTypes = 0;
        if (target.localVariables != null) {
            for (LocalVariableNode local : target.localVariables) {
                if (LEGACY_DESC.equals(local.desc)) {
                    oldLocalTypes++;
                } else if (CURRENT_DESC.equals(local.desc)) {
                    currentLocalTypes++;
                }
                if (local.signature != null) {
                    local.signature = local.signature.replace(LEGACY_DESC, CURRENT_DESC);
                }
            }
        }

        // A second pass is idempotent only when the exception table, stack-map
        // frame and local-variable entry all consistently use RuntimeException.
        if (oldCatchTypes == 0 && currentCatchTypes == 1
                && oldFrameTypes == 0 && currentFrameTypes >= 1
                && oldLocalTypes == 0 && currentLocalTypes == 1) {
            return 0;
        }

        if (oldCatchTypes != 1 || currentCatchTypes != 0
                || oldFrameTypes < 1 || oldLocalTypes != 1 || currentLocalTypes != 0) {
            throw new IllegalStateException("AlienEvo AlienGeckoRenderLayer has an unexpected "
                    + "GeckoLibException catch shape (catch=" + oldCatchTypes + "/" + currentCatchTypes
                    + ", frames=" + oldFrameTypes + "/" + currentFrameTypes
                    + ", locals=" + oldLocalTypes + "/" + currentLocalTypes + ")");
        }

        for (TryCatchBlockNode block : target.tryCatchBlocks) {
            if (LEGACY_INTERNAL.equals(block.type)) block.type = CURRENT_INTERNAL;
        }
        for (LocalVariableNode local : target.localVariables) {
            if (LEGACY_DESC.equals(local.desc)) local.desc = CURRENT_DESC;
            if (local.signature != null) local.signature = local.signature.replace(LEGACY_DESC, CURRENT_DESC);
        }

        // method.exceptions is the Exceptions attribute, not the catch table,
        // but keep it consistent if a future build also declares this type.
        if (target.exceptions != null) {
            for (int i = 0; i < target.exceptions.size(); i++) {
                if (LEGACY_INTERNAL.equals(target.exceptions.get(i))) {
                    target.exceptions.set(i, CURRENT_INTERNAL);
                }
            }
        }
        return 1;
    }

    private static int[] replaceFrameTypes(FrameNode frame) {
        int oldTypes = 0;
        int currentTypes = 0;
        if (frame.local != null) {
            for (int i = 0; i < frame.local.size(); i++) {
                Object value = frame.local.get(i);
                if (LEGACY_INTERNAL.equals(value)) {
                    frame.local.set(i, CURRENT_INTERNAL);
                    oldTypes++;
                } else if (CURRENT_INTERNAL.equals(value)) {
                    currentTypes++;
                }
            }
        }
        if (frame.stack != null) {
            for (int i = 0; i < frame.stack.size(); i++) {
                Object value = frame.stack.get(i);
                if (LEGACY_INTERNAL.equals(value)) {
                    frame.stack.set(i, CURRENT_INTERNAL);
                    oldTypes++;
                } else if (CURRENT_INTERNAL.equals(value)) {
                    currentTypes++;
                }
            }
        }
        return new int[] {oldTypes, currentTypes};
    }
}
