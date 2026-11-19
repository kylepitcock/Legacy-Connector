package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Narrow migrations for native AlienEvo Forge event handlers whose event types
 * or callback semantics changed between Forge 1.20.1 and NeoForge 1.21.1.
 *
 * <p>Generic class-name relocation is not sufficient for these APIs: the old
 * phase-based tick events became dedicated Pre/Post event classes, and the old
 * GUI overlay event exposes named overlay wrappers that became ResourceLocation
 * layer identifiers. These rewrites preserve the old END/START timing by
 * selecting the matching new event class.
 */
final class AlienEvoForgeEvents1201 implements Opcodes {
    static final String PLAYER_TICK_POST =
            "net/neoforged/neoforge/event/tick/PlayerTickEvent$Post";
    static final String SERVER_TICK_POST =
            "net/neoforged/neoforge/event/tick/ServerTickEvent$Post";
    static final String RENDER_FRAME_PRE =
            "net/neoforged/neoforge/client/event/RenderFrameEvent$Pre";
    static final String RENDER_GUI_LAYER =
            "net/neoforged/neoforge/client/event/RenderGuiLayerEvent";
    static final String RENDER_GUI_LAYER_PRE = RENDER_GUI_LAYER + "$Pre";
    static final String RENDER_GUI_LAYER_POST = RENDER_GUI_LAYER + "$Post";
    static final String VANILLA_GUI_LAYERS =
            "net/neoforged/neoforge/client/gui/VanillaGuiLayers";
    static final String NAMED_GUI_OVERLAY =
            "net/neoforged/neoforge/client/gui/overlay/NamedGuiOverlay";
    static final String PLAYER_EVENT =
            "net/neoforged/neoforge/event/entity/player/PlayerEvent";
    static final String CANCELLABLE_EVENT = "net/neoforged/bus/api/ICancellableEvent";
    static final String RESOURCE_LOCATION = "Lnet/minecraft/resources/ResourceLocation;";
    static final String PLAYER_DESC = "Lnet/minecraft/world/entity/player/Player;";
    static final String MINECRAFT_SERVER_DESC = "Lnet/minecraft/server/MinecraftServer;";

    private static final String SCALE_REACH =
            "net/habb/alienevo/forge/ScaleReachForgeHandler";
    private static final String RIDE_HANDLER =
            "net/habb/alienevo/forge/event/ForgeRideAbilityHandler";
    private static final String ENTITY_EVENTS =
            "net/habb/alienevo/forge/event/ForgeAlienEntityEvents";
    private static final String CLIENT_EVENTS =
            "net/habb/alienevo/forge/client/ForgeClientEvents";
    private static final String FOV_ANIMATOR =
            "net/habb/alienevo/client/model/animation/FirstPersonFovAnimator";

    private static final String PLAYER_TICK_POST_DESC = "(L" + PLAYER_TICK_POST + ";)V";
    private static final String SERVER_TICK_POST_DESC = "(L" + SERVER_TICK_POST + ";)V";
    private static final String RENDER_FRAME_PRE_DESC = "(L" + RENDER_FRAME_PRE + ";)V";
    private static final String RENDER_GUI_LAYER_PRE_DESC = "(L" + RENDER_GUI_LAYER_PRE + ";)V";
    private static final String RENDER_GUI_LAYER_POST_DESC = "(L" + RENDER_GUI_LAYER_POST + ";)V";
    private static final String NAMED_OVERLAY_DESC = "()L" + NAMED_GUI_OVERLAY + ";";
    private static final String LAYER_GET_NAME_DESC = "()" + RESOURCE_LOCATION;
    private static final String LAYER_FIELD_DESC = "L" + VANILLA_GUI_LAYERS + ";";

    private AlienEvoForgeEvents1201() {}

    /** Applies only to the inspected event-handler classes; returns changed bytecode sites. */
    static int apply(ClassNode node) {
        if (SCALE_REACH.equals(node.name)) return rewritePlayerTick(node, 5);
        if (RIDE_HANDLER.equals(node.name)) return rewritePlayerTick(node, 2);
        if (ENTITY_EVENTS.equals(node.name)) return rewriteServerTick(node);
        if (CLIENT_EVENTS.equals(node.name)) return rewriteClientEvents(node);
        return 0;
    }

    private static int rewritePlayerTick(ClassNode node, int expectedPlayerReads) {
        MethodNode method = requireMethod(node, "onPlayerTick", PLAYER_TICK_POST_DESC);
        boolean phaseRemoved = removeLeadingPhaseGuard(method, 0, "END");
        int playerFields = rewritePlayerFieldReads(method);
        int entityCalls = countMethodCalls(method, PLAYER_EVENT, "getEntity", "()" + PLAYER_DESC);

        if (phaseRemoved && playerFields == expectedPlayerReads && entityCalls == expectedPlayerReads) {
            removeStaleTickEventInnerClasses(node);
            return 1 + playerFields;
        }
        if (!phaseRemoved && playerFields == 0 && entityCalls == expectedPlayerReads) {
            removeStaleTickEventInnerClasses(node);
            return 0;
        }
        throw new IllegalStateException(
                node.name + ".onPlayerTick has an unexpected PlayerTickEvent migration shape: "
                        + "phaseRemoved=" + phaseRemoved + ", playerFields=" + playerFields
                        + ", getEntityCalls=" + entityCalls + ", expected=" + expectedPlayerReads);
    }

    private static int rewritePlayerFieldReads(MethodNode method) {
        int rewritten = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETFIELD
                    && field.name.equals("player")
                    && field.desc.equals(PLAYER_DESC)) {
                method.instructions.set(field,
                        new MethodInsnNode(INVOKEVIRTUAL, PLAYER_EVENT, "getEntity", "()" + PLAYER_DESC, false));
                rewritten++;
            }
        }
        return rewritten;
    }

    private static int rewriteServerTick(ClassNode node) {
        MethodNode method = requireMethod(node, "onServerTick", SERVER_TICK_POST_DESC);
        boolean phaseRemoved = removeLeadingPhaseGuard(method, 1, "END");
        int getServerCalls = countMethodCalls(method, SERVER_TICK_POST, "getServer", "()" + MINECRAFT_SERVER_DESC);
        if (phaseRemoved && getServerCalls == 6) {
            removeStaleTickEventInnerClasses(node);
            return 1;
        }
        if (!phaseRemoved && getServerCalls == 6) {
            removeStaleTickEventInnerClasses(node);
            return 0;
        }
        throw new IllegalStateException(
                node.name + ".onServerTick has an unexpected ServerTickEvent migration shape: "
                        + "phaseRemoved=" + phaseRemoved + ", getServerCalls=" + getServerCalls);
    }

    private static int rewriteClientEvents(ClassNode node) {
        int changed = rewriteRenderFramePre(node);
        changed += rewriteOverlayPre(node);
        changed += rewriteOverlayPost(node);
        removeStaleTickEventInnerClasses(node);
        return changed;
    }

    private static int rewriteRenderFramePre(ClassNode node) {
        MethodNode method = requireMethod(node, "onRenderTick", RENDER_FRAME_PRE_DESC);
        if (hasOnlyRenderFrameStartBody(method)) return 0;

        int phaseReads = countFieldAccesses(method, GETFIELD, "phase");
        int startReads = countFieldAccesses(method, GETSTATIC, "START");
        int callbackCalls = countMethodCalls(method, FOV_ANIMATOR, "onFrameStart", "()V");
        if (phaseReads != 1 || startReads != 1 || callbackCalls != 1) {
            throw new IllegalStateException(
                    node.name + ".onRenderTick has an unexpected RenderFrameEvent migration shape: "
                            + "phaseReads=" + phaseReads + ", startReads=" + startReads
                            + ", callbackCalls=" + callbackCalls);
        }

        method.instructions.clear();
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, FOV_ANIMATOR, "onFrameStart", "()V", false));
        method.instructions.add(new InsnNode(RETURN));
        if (method.tryCatchBlocks != null) method.tryCatchBlocks.clear();
        if (method.localVariables != null) method.localVariables.clear();
        method.maxStack = 0;
        method.maxLocals = 0;
        return 1;
    }

    private static boolean hasOnlyRenderFrameStartBody(MethodNode method) {
        List<AbstractInsnNode> executable = executableInstructions(method);
        return executable.size() == 2
                && executable.get(0) instanceof MethodInsnNode call
                && call.getOpcode() == INVOKESTATIC
                && call.owner.equals(FOV_ANIMATOR)
                && call.name.equals("onFrameStart")
                && call.desc.equals("()V")
                && executable.get(1).getOpcode() == RETURN;
    }

    private static int rewriteOverlayPre(ClassNode node) {
        MethodNode method = requireMethod(node, "onRenderOverlayPre", RENDER_GUI_LAYER_PRE_DESC);
        return rewriteOverlayMethod(method, true, 6, 5, 5, 5, 1);
    }

    private static int rewriteOverlayPost(ClassNode node) {
        MethodNode method = requireMethod(node, "onRenderOverlayPost", RENDER_GUI_LAYER_POST_DESC);
        return rewriteOverlayMethod(method, false, 3, 3, 3, 0, 0);
    }

    private static int rewriteOverlayMethod(
            MethodNode method,
            boolean cancellable,
            int expectedOverlayReads,
            int expectedTypeCalls,
            int expectedLayerFields,
            int expectedCancelCalls,
            int expectedOverlayIdCalls) {
        int overlayReads = countMethodCalls(method, null, "getOverlay", NAMED_OVERLAY_DESC);
        int typeCalls = countMethodCalls(method, VANILLA_GUI_LAYERS, "type", NAMED_OVERLAY_DESC);
        int layerFields = countVanillaLayerFields(method, LAYER_FIELD_DESC);
        int cancelCalls = countCallsByName(method, "setCanceled", "(Z)V", cancellable ? RENDER_GUI_LAYER_PRE : null);
        int overlayIdCalls = countMethodCalls(method, NAMED_GUI_OVERLAY, "id", "()" + RESOURCE_LOCATION);
        int getNameCalls = countMethodCalls(method, RENDER_GUI_LAYER, "getName", LAYER_GET_NAME_DESC);
        int resourceLocationFields = countVanillaLayerFields(method, RESOURCE_LOCATION);
        int interfaceCancelCalls = countMethodCalls(method, CANCELLABLE_EVENT, "setCanceled", "(Z)V");

        if (overlayReads == 0 && typeCalls == 0 && overlayIdCalls == 0
                && getNameCalls == expectedOverlayReads
                && resourceLocationFields == expectedLayerFields
                && cancelCalls == 0
                && interfaceCancelCalls == expectedCancelCalls) {
            return 0;
        }

        if (overlayReads != expectedOverlayReads
                || typeCalls != expectedTypeCalls
                || layerFields != expectedLayerFields
                || cancelCalls != expectedCancelCalls
                || overlayIdCalls != expectedOverlayIdCalls) {
            throw new IllegalStateException(
                    method.name + " has an unexpected RenderGuiLayerEvent migration shape: "
                            + "getOverlay=" + overlayReads + ", type=" + typeCalls
                            + ", layerFields=" + layerFields + ", setCanceled=" + cancelCalls
                            + ", overlayId=" + overlayIdCalls + ", getName=" + getNameCalls
                            + ", ResourceLocation fields=" + resourceLocationFields
                            + ", ICancellableEvent calls=" + interfaceCancelCalls);
        }

        int changed = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.name.equals("getOverlay") && call.desc.equals(NAMED_OVERLAY_DESC)) {
                call.setOpcode(INVOKEVIRTUAL);
                call.owner = RENDER_GUI_LAYER;
                call.name = "getName";
                call.desc = LAYER_GET_NAME_DESC;
                call.itf = false;
                changed++;
            } else if (call.owner.equals(VANILLA_GUI_LAYERS)
                    && call.name.equals("type")
                    && call.desc.equals(NAMED_OVERLAY_DESC)) {
                method.instructions.remove(call);
                changed++;
            } else if (call.owner.equals(NAMED_GUI_OVERLAY)
                    && call.name.equals("id")
                    && call.desc.equals("()" + RESOURCE_LOCATION)) {
                method.instructions.remove(call);
                changed++;
            } else if (cancellable && call.owner.equals(RENDER_GUI_LAYER_PRE)
                    && call.name.equals("setCanceled") && call.desc.equals("(Z)V")) {
                call.setOpcode(INVOKEINTERFACE);
                call.owner = CANCELLABLE_EVENT;
                call.itf = true;
                changed++;
            }
        }

        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETSTATIC
                    && field.owner.equals(VANILLA_GUI_LAYERS)
                    && field.desc.equals(LAYER_FIELD_DESC)) {
                if (field.name.equals("MOUNT_HEALTH")) field.name = "VEHICLE_HEALTH";
                field.desc = RESOURCE_LOCATION;
                changed++;
            }
        }

        int migratedGetNames = countMethodCalls(method, RENDER_GUI_LAYER, "getName", LAYER_GET_NAME_DESC);
        int migratedResourceFields = countVanillaLayerFields(method, RESOURCE_LOCATION);
        int migratedInterfaceCalls = countMethodCalls(method, CANCELLABLE_EVENT, "setCanceled", "(Z)V");
        if (migratedGetNames != expectedOverlayReads
                || migratedResourceFields != expectedLayerFields
                || migratedInterfaceCalls != expectedCancelCalls
                || countMethodCalls(method, VANILLA_GUI_LAYERS, "type", NAMED_OVERLAY_DESC) != 0
                || countMethodCalls(method, NAMED_GUI_OVERLAY, "id", "()" + RESOURCE_LOCATION) != 0) {
            throw new IllegalStateException(method.name + " did not reach the expected ResourceLocation GUI-layer form");
        }
        return changed;
    }

    /**
     * Removes Forge's leading event.phase == START/END gate after the method
     * descriptor has been remapped to the corresponding NeoForge Pre/Post type.
     * The selected event class itself guarantees the required phase.
     */
    private static boolean removeLeadingPhaseGuard(MethodNode method, int eventSlot, String expectedPhase) {
        List<AbstractInsnNode> first = executableInstructions(method);
        if (first.size() < 4) {
            throw new IllegalStateException(method.name + " is too short for its expected tick-phase guard");
        }
        AbstractInsnNode load = first.get(0);
        AbstractInsnNode phase = first.get(1);
        AbstractInsnNode expected = first.get(2);
        AbstractInsnNode branch = first.get(3);
        if (load instanceof VarInsnNode variable && variable.getOpcode() == ALOAD && variable.var == eventSlot
                && phase instanceof FieldInsnNode phaseField
                && phaseField.getOpcode() == GETFIELD && phaseField.name.equals("phase")
                && expected instanceof FieldInsnNode phaseValue
                && phaseValue.getOpcode() == GETSTATIC && phaseValue.name.equals(expectedPhase)
                && branch instanceof JumpInsnNode jump && jump.getOpcode() == IF_ACMPNE) {
            method.instructions.remove(load);
            method.instructions.remove(phase);
            method.instructions.remove(expected);
            method.instructions.remove(branch);
            return true;
        }
        int phaseFields = countFieldAccesses(method, GETFIELD, "phase");
        if (phaseFields == 0) return false;
        throw new IllegalStateException(method.name + " has an unexpected leading " + expectedPhase + " phase guard");
    }

    private static void removeStaleTickEventInnerClasses(ClassNode node) {
        if (node.innerClasses == null) return;
        node.innerClasses.removeIf(inner ->
                (inner.name != null && inner.name.startsWith("net/neoforged/neoforge/event/TickEvent$"))
                        || "net/neoforged/neoforge/event/TickEvent".equals(inner.outerName));
    }

    private static MethodNode requireMethod(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!method.name.equals(name)) continue;
            if (!method.desc.equals(descriptor)) continue;
            if (found != null) throw new IllegalStateException(node.name + " contains duplicate " + name + descriptor);
            found = method;
        }
        if (found == null) {
            throw new IllegalStateException(node.name + " is missing expected method " + name + descriptor);
        }
        return found;
    }

    private static int countFieldAccesses(MethodNode method, int opcode, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == opcode && field.name.equals(name)) count++;
        }
        return count;
    }

    private static int countMethodCalls(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && (owner == null || call.owner.equals(owner))
                    && call.name.equals(name) && call.desc.equals(descriptor)) count++;
        }
        return count;
    }

    private static int countCallsByName(MethodNode method, String name, String descriptor, String owner) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && (owner == null || call.owner.equals(owner))
                    && call.name.equals(name) && call.desc.equals(descriptor)) count++;
        }
        return count;
    }

    private static int countVanillaLayerFields(MethodNode method, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == GETSTATIC
                    && field.owner.equals(VANILLA_GUI_LAYERS)
                    && field.desc.equals(descriptor)) count++;
        }
        return count;
    }

    private static List<AbstractInsnNode> executableInstructions(MethodNode method) {
        List<AbstractInsnNode> instructions = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() >= 0) instructions.add(instruction);
        }
        return instructions;
    }
}
