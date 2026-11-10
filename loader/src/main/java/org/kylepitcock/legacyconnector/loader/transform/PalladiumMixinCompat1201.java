package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Palladium 4.5.x mixin handlers whose target APIs changed between Minecraft
 * 1.20.1 and 1.21.1. This is deliberately scoped to Palladium's own mixins.
 */
final class PalladiumMixinCompat1201 implements Opcodes {
    private static final String CALLBACK = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String CALLBACK_RETURN = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";

    private static final String ITEM_MIXIN = "net/threetag/palladium/mixin/ItemMixin";
    private static final String ARMOR_ITEM_MIXIN = "net/threetag/palladium/mixin/ArmorItemMixin";
    private static final String ARMOR_STAND_MIXIN = "net/threetag/palladium/mixin/ArmorStandMixin";
    private static final String SCOREBOARD_MIXIN = "net/threetag/palladium/mixin/ServerScoreboardMixin";
    private static final String PLAYER_RENDERER_INVOKER = "net/threetag/palladium/mixin/client/PlayerRendererInvoker";
    private static final String HUMANOID_ARMOR_MIXIN = "net/threetag/palladium/mixin/client/HumanoidArmorLayerMixin";
    private static final String GEO_RENDERER_MIXIN = "net/threetag/palladium/mixin/client/GeoRendererMixin";
    private static final String LEVEL_RENDERER_MIXIN = "net/threetag/palladium/mixin/client/LevelRendererMixin";
    private static final String MODEL_PART_INVOKER = "net/threetag/palladium/mixin/client/ModelPartInvoker";

    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";
    private static final String VERTEX_CONSUMER = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String MULTI_BUFFER = "net/minecraft/client/renderer/MultiBufferSource";
    private static final String HUMANOID_MODEL = "net/minecraft/client/model/HumanoidModel";
    private static final String DISPLAY_SLOT = "net/minecraft/world/scores/DisplaySlot";
    private static final String OBJECTIVE = "net/minecraft/world/scores/Objective";
    private static final String SCOREBOARD = "net/minecraft/world/scores/Scoreboard";
    private static final String SET_DISPLAY_PACKET = "net/minecraft/network/protocol/game/ClientboundSetDisplayObjectivePacket";
    private static final String GEO_QUAD = "software/bernie/geckolib/cache/object/GeoQuad";
    private static final String GEO_VERTEX = "software/bernie/geckolib/cache/object/GeoVertex";
    private static final String VECTOR3F = "org/joml/Vector3f";
    private static final String VECTOR4F = "org/joml/Vector4f";
    private static final String MATRIX4F = "org/joml/Matrix4f";

    private PalladiumMixinCompat1201() {}

    /** Returns the number of Palladium mixin classes adapted. */
    static int apply(ClassNode node) {
        return switch (node.name) {
            case ITEM_MIXIN -> adaptItemMixin(node) ? 1 : 0;
            case ARMOR_ITEM_MIXIN -> adaptArmorItemMixin(node) ? 1 : 0;
            case ARMOR_STAND_MIXIN -> adaptArmorStandMixin(node) ? 1 : 0;
            case SCOREBOARD_MIXIN -> adaptScoreboardMixin(node) ? 1 : 0;
            case PLAYER_RENDERER_INVOKER -> adaptPlayerRendererInvoker(node) ? 1 : 0;
            case HUMANOID_ARMOR_MIXIN -> adaptHumanoidArmorMixin(node) ? 1 : 0;
            case GEO_RENDERER_MIXIN -> adaptGeoRendererMixin(node) ? 1 : 0;
            case LEVEL_RENDERER_MIXIN -> adaptLevelRendererMixin(node) ? 1 : 0;
            case MODEL_PART_INVOKER -> adaptModelPartInvoker(node) ? 1 : 0;
            default -> 0;
        };
    }

    private static boolean adaptItemMixin(ClassNode node) {
        String oldDesc = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;"
                + "Lnet/minecraft/world/item/TooltipFlag;" + CALLBACK + ")V";
        String newDesc = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;"
                + "Lnet/minecraft/world/item/TooltipFlag;" + CALLBACK + ")V";
        MethodNode method = find(node, "appendHoverText", oldDesc);
        if (method == null) return false;

        // Preserve the old handler body, which expected a Level in local 2.
        // Kube's 1.21.1 Item.TooltipContext exposes that Level through level().
        // Allocate the extracted Level after all existing locals; local 6 may
        // already hold the old method's first temporary.
        int levelLocal = Math.max(method.maxLocals, 6);
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof VarInsnNode var && var.getOpcode() == ALOAD && var.var == 2) {
                var.var = levelLocal;
            }
        }
        InsnList head = new InsnList();
        head.add(new VarInsnNode(ALOAD, 2));
        head.add(new MethodInsnNode(INVOKEINTERFACE, "net/minecraft/world/item/Item$TooltipContext",
                "level", "()Lnet/minecraft/world/level/Level;", true));
        head.add(new VarInsnNode(ASTORE, levelLocal));
        method.instructions.insert(head);
        method.desc = newDesc;
        method.signature = null;
        method.maxLocals = levelLocal + 1;
        method.maxStack = Math.max(method.maxStack, 2);
        return true;
    }

    /**
     * In 1.20.1 Mob.getEquipmentSlotForItem(ItemStack) was static. In 1.21.1
     * it is an instance method on LivingEntity; this handler's Player parameter
     * is the receiver required by the new API.
     */
    private static boolean adaptArmorItemMixin(ClassNode node) {
        String handlerDesc = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;"
                + "Lnet/minecraft/world/InteractionHand;" + CALLBACK_RETURN + ")V";
        String itemStack = "Lnet/minecraft/world/item/ItemStack;";
        String equipmentSlot = "Lnet/minecraft/world/entity/EquipmentSlot;";
        String callDesc = "(" + itemStack + ")" + equipmentSlot;
        String mob = "net/minecraft/world/entity/Mob";
        String livingEntity = "net/minecraft/world/entity/LivingEntity";
        MethodNode handler = find(node, "use", handlerDesc);
        if (handler == null) return false;

        MethodInsnNode target = null;
        for (AbstractInsnNode instruction : handler.instructions) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !call.name.equals("getEquipmentSlotForItem")
                    || !call.desc.equals(callDesc)) continue;
            if (target != null) {
                throw new IllegalStateException("Palladium ArmorItemMixin.use has multiple equipment-slot lookups");
            }
            target = call;
        }
        if (target == null) return false;

        if (target.getOpcode() == INVOKEVIRTUAL && target.owner.equals(livingEntity)) {
            return false; // already in the 1.21.1 form
        }
        if (target.getOpcode() != INVOKESTATIC || !(target.owner.equals(mob) || target.owner.equals(livingEntity))) {
            throw new IllegalStateException("Palladium ArmorItemMixin.getEquipmentSlotForItem call has an unexpected owner/opcode: "
                    + target.owner + "." + target.name + target.desc + " opcode=" + target.getOpcode());
        }

        // Before: [ItemStack]. After: [Player, ItemStack], ready for invokevirtual.
        InsnList receiver = new InsnList();
        receiver.add(new VarInsnNode(ALOAD, 2));
        receiver.add(new InsnNode(SWAP));
        handler.instructions.insertBefore(target, receiver);
        target.setOpcode(INVOKEVIRTUAL);
        target.owner = livingEntity;
        target.itf = false;
        handler.maxStack = Math.max(handler.maxStack, 2);
        return true;
    }

    private static boolean adaptArmorStandMixin(ClassNode node) {
        String oldDesc = "(Lnet/minecraft/world/damagesource/DamageSource;" + CALLBACK + ")V";
        String newDesc = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;"
                + CALLBACK + ")V";
        MethodNode method = find(node, "brokenByPlayer", oldDesc);
        if (method == null) return false;

        shiftLocals(method, 1, 1);
        method.desc = newDesc;
        method.signature = null;
        return true;
    }

    private static boolean adaptScoreboardMixin(ClassNode node) {
        boolean changed = false;
        String oldSetDesc = "(IL" + OBJECTIVE + ";" + CALLBACK + ")V";
        String newSetDesc = "(L" + DISPLAY_SLOT + ";L" + OBJECTIVE + ";" + CALLBACK + ")V";
        MethodNode setDisplay = find(node, "setDisplayObjective", oldSetDesc);
        if (setDisplay != null) {
            setDisplay.desc = newSetDesc;
            setDisplay.signature = null;
            for (AbstractInsnNode instruction : setDisplay.instructions.toArray()) {
                if (instruction instanceof VarInsnNode var && var.getOpcode() == ILOAD && var.var == 1) {
                    var.setOpcode(ALOAD);
                }
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals(SCOREBOARD)
                        && call.name.equals("getDisplayObjective")
                        && call.desc.equals("(I)L" + OBJECTIVE + ";")) {
                    call.desc = "(L" + DISPLAY_SLOT + ";)L" + OBJECTIVE + ";";
                    changed = true;
                } else if (instruction instanceof MethodInsnNode call
                        && call.owner.equals(SET_DISPLAY_PACKET)
                        && call.name.equals("<init>")
                        && call.desc.equals("(IL" + OBJECTIVE + ";)V")) {
                    call.desc = "(L" + DISPLAY_SLOT + ";L" + OBJECTIVE + ";)V";
                    changed = true;
                }
            }
            changed = true;
        }

        String stopDesc = "(L" + OBJECTIVE + ";" + CALLBACK_RETURN + ")V";
        MethodNode stopTracking = find(node, "getStopTrackingPackets", stopDesc);
        if (stopTracking != null) {
            rewriteScoreboardStopTracking(stopTracking);
            changed = true;
        }
        return changed;
    }

    /**
     * 1.21.1 replaced the numeric objective slot with DisplaySlot. Enumerate
     * the enum directly rather than assuming that the old 0..18 range persists.
     */
    private static void rewriteScoreboardStopTracking(MethodNode method) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;

        String trackedManager = "net/threetag/palladium/world/TrackedScoresManager";
        String displayArray = "[L" + DISPLAY_SLOT + ";";
        LabelNode notTracked = new LabelNode();
        LabelNode loop = new LabelNode();
        LabelNode done = new LabelNode();

        InsnList code = method.instructions;
        code.add(new FieldInsnNode(GETSTATIC, trackedManager, "INSTANCE", "L" + trackedManager + ";"));
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, OBJECTIVE, "getName", "()Ljava/lang/String;", false));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, trackedManager, "isTracked", "(Ljava/lang/String;)Z", false));
        code.add(new JumpInsnNode(IFEQ, notTracked));

        code.add(new TypeInsnNode(NEW, "java/util/ArrayList"));
        code.add(new InsnNode(DUP));
        code.add(new MethodInsnNode(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        code.add(new VarInsnNode(ASTORE, 3));

        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new TypeInsnNode(CHECKCAST, SCOREBOARD));
        code.add(new VarInsnNode(ASTORE, 4));

        code.add(new MethodInsnNode(INVOKESTATIC, DISPLAY_SLOT, "values", "()" + displayArray, false));
        code.add(new VarInsnNode(ASTORE, 5));
        code.add(new InsnNode(ICONST_0));
        code.add(new VarInsnNode(ISTORE, 6));

        code.add(loop);
        code.add(new VarInsnNode(ILOAD, 6));
        code.add(new VarInsnNode(ALOAD, 5));
        code.add(new InsnNode(ARRAYLENGTH));
        code.add(new JumpInsnNode(IF_ICMPGE, done));

        code.add(new VarInsnNode(ALOAD, 5));
        code.add(new VarInsnNode(ILOAD, 6));
        code.add(new InsnNode(AALOAD));
        code.add(new VarInsnNode(ASTORE, 7));

        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new VarInsnNode(ALOAD, 7));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, SCOREBOARD, "getDisplayObjective",
                "(L" + DISPLAY_SLOT + ";)L" + OBJECTIVE + ";", false));
        code.add(new VarInsnNode(ALOAD, 1));
        LabelNode next = new LabelNode();
        code.add(new JumpInsnNode(IF_ACMPNE, next));

        code.add(new VarInsnNode(ALOAD, 3));
        code.add(new TypeInsnNode(NEW, SET_DISPLAY_PACKET));
        code.add(new InsnNode(DUP));
        code.add(new VarInsnNode(ALOAD, 7));
        code.add(new InsnNode(ACONST_NULL));
        code.add(new MethodInsnNode(INVOKESPECIAL, SET_DISPLAY_PACKET, "<init>",
                "(L" + DISPLAY_SLOT + ";L" + OBJECTIVE + ";)V", false));
        code.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
        code.add(new InsnNode(POP));

        code.add(next);
        code.add(new IincInsnNode(6, 1));
        code.add(new JumpInsnNode(GOTO, loop));

        code.add(done);
        code.add(new VarInsnNode(ALOAD, 2));
        code.add(new VarInsnNode(ALOAD, 3));
        code.add(new MethodInsnNode(INVOKEVIRTUAL,
                "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
                "setReturnValue", "(Ljava/lang/Object;)V", false));
        code.add(new InsnNode(RETURN));

        code.add(notTracked);
        code.add(new InsnNode(RETURN));
        method.maxStack = 5;
        method.maxLocals = 8;
    }

    private static boolean adaptPlayerRendererInvoker(ClassNode node) {
        String oldDesc = "(Lnet/minecraft/client/player/AbstractClientPlayer;L" + POSE_STACK + ";FFF)V";
        String newDesc = "(Lnet/minecraft/client/player/AbstractClientPlayer;L" + POSE_STACK + ";FFFF)V";
        MethodNode method = find(node, "invokeSetupRotations", oldDesc);
        if (method == null) return false;
        method.desc = newDesc;
        method.signature = null;
        return true;
    }

    private static boolean adaptHumanoidArmorMixin(ClassNode node) {
        String oldDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";I"
                + "Lnet/minecraft/world/item/ArmorItem;L" + HUMANOID_MODEL + ";ZFFF"
                + "Ljava/lang/String;" + CALLBACK + ")V";
        String newDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";I"
                + "L" + HUMANOID_MODEL + ";ILnet/minecraft/resources/ResourceLocation;"
                + CALLBACK + ")V";
        MethodNode method = find(node, "renderModel", oldDesc);
        if (method == null) return false;

        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        clearParameterAnnotations(method);
        LabelNode returnLabel = new LabelNode();
        InsnList code = method.instructions;
        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new TypeInsnNode(INSTANCEOF,
                "net/threetag/palladium/compat/geckolib/armor/CancelGeckoArmorBuffer"));
        code.add(new JumpInsnNode(IFEQ, returnLabel));
        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new InsnNode(ACONST_NULL));
        code.add(new VarInsnNode(ILOAD, 3));
        code.add(new VarInsnNode(ILOAD, 5));
        code.add(new InsnNode(ICONST_M1));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, HUMANOID_MODEL, "renderToBuffer",
                "(L" + POSE_STACK + ";L" + VERTEX_CONSUMER + ";III)V", false));
        code.add(new VarInsnNode(ALOAD, 7));
        code.add(new MethodInsnNode(INVOKEVIRTUAL,
                "org/spongepowered/asm/mixin/injection/callback/CallbackInfo", "cancel", "()V", false));
        code.add(returnLabel);
        code.add(new InsnNode(RETURN));
        method.desc = newDesc;
        method.signature = null;
        method.maxStack = 6;
        method.maxLocals = 8;
        return true;
    }

    /**
     * GeckoLib 4.9.x uses an ARGB color int instead of four float tint values.
     * Keep Palladium's global alpha multiplier while targeting that new method.
     */
    private static boolean adaptGeoRendererMixin(ClassNode node) {
        String oldDesc = "(L" + GEO_QUAD + ";L" + MATRIX4F + ";L" + VECTOR3F + ";L" + VERTEX_CONSUMER
                + ";I I F F F F)V".replace(" ", "");
        String newDesc = "(L" + GEO_QUAD + ";L" + MATRIX4F + ";L" + VECTOR3F + ";L" + VERTEX_CONSUMER + ";III)V";
        MethodNode method = find(node, "createVerticesOfQuad", oldDesc);
        if (method == null) return false;

        method.desc = newDesc;
        method.signature = null;
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        clearParameterAnnotations(method);

        String vertexArray = "[L" + GEO_VERTEX + ";";
        LabelNode loop = new LabelNode();
        LabelNode done = new LabelNode();
        InsnList code = method.instructions;

        // Preserve the RGB channels and scale only the existing alpha channel.
        code.add(new VarInsnNode(ILOAD, 7));
        code.add(new LdcInsnNode(0x00FFFFFF));
        code.add(new InsnNode(IAND));
        code.add(new VarInsnNode(ILOAD, 7));
        code.add(new IntInsnNode(BIPUSH, 24));
        code.add(new InsnNode(IUSHR));
        code.add(new IntInsnNode(SIPUSH, 255));
        code.add(new InsnNode(IAND));
        code.add(new InsnNode(I2F));
        code.add(new FieldInsnNode(GETSTATIC,
                "net/threetag/palladium/client/renderer/entity/HumanoidRendererModifications",
                "ALPHA_MULTIPLIER", "F"));
        code.add(new InsnNode(FMUL));
        code.add(new InsnNode(F2I));
        code.add(new InsnNode(ICONST_0));
        code.add(new IntInsnNode(SIPUSH, 255));
        code.add(new MethodInsnNode(INVOKESTATIC, "net/minecraft/util/Mth", "clamp", "(III)I", false));
        code.add(new IntInsnNode(BIPUSH, 24));
        code.add(new InsnNode(ISHL));
        code.add(new InsnNode(IOR));
        code.add(new VarInsnNode(ISTORE, 13));

        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, GEO_QUAD, "vertices", "()" + vertexArray, false));
        code.add(new VarInsnNode(ASTORE, 8));
        code.add(new InsnNode(ICONST_0));
        code.add(new VarInsnNode(ISTORE, 9));

        code.add(loop);
        code.add(new VarInsnNode(ILOAD, 9));
        code.add(new VarInsnNode(ALOAD, 8));
        code.add(new InsnNode(ARRAYLENGTH));
        code.add(new JumpInsnNode(IF_ICMPGE, done));

        code.add(new VarInsnNode(ALOAD, 8));
        code.add(new VarInsnNode(ILOAD, 9));
        code.add(new InsnNode(AALOAD));
        code.add(new VarInsnNode(ASTORE, 10));
        code.add(new VarInsnNode(ALOAD, 10));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, GEO_VERTEX, "position", "()L" + VECTOR3F + ";", false));
        code.add(new VarInsnNode(ASTORE, 11));

        code.add(new VarInsnNode(ALOAD, 2));
        code.add(new TypeInsnNode(NEW, VECTOR4F));
        code.add(new InsnNode(DUP));
        code.add(new VarInsnNode(ALOAD, 11));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR3F, "x", "()F", false));
        code.add(new VarInsnNode(ALOAD, 11));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR3F, "y", "()F", false));
        code.add(new VarInsnNode(ALOAD, 11));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR3F, "z", "()F", false));
        code.add(new InsnNode(FCONST_1));
        code.add(new MethodInsnNode(INVOKESPECIAL, VECTOR4F, "<init>", "(FFFF)V", false));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, MATRIX4F, "transform",
                "(L" + VECTOR4F + ";)L" + VECTOR4F + ";", false));
        code.add(new VarInsnNode(ASTORE, 12));

        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new VarInsnNode(ALOAD, 12));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR4F, "x", "()F", false));
        code.add(new VarInsnNode(ALOAD, 12));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR4F, "y", "()F", false));
        code.add(new VarInsnNode(ALOAD, 12));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR4F, "z", "()F", false));
        code.add(new VarInsnNode(ILOAD, 13));
        code.add(new VarInsnNode(ALOAD, 10));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, GEO_VERTEX, "texU", "()F", false));
        code.add(new VarInsnNode(ALOAD, 10));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, GEO_VERTEX, "texV", "()F", false));
        code.add(new VarInsnNode(ILOAD, 6));
        code.add(new VarInsnNode(ILOAD, 5));
        code.add(new VarInsnNode(ALOAD, 3));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR3F, "x", "()F", false));
        code.add(new VarInsnNode(ALOAD, 3));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR3F, "y", "()F", false));
        code.add(new VarInsnNode(ALOAD, 3));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, VECTOR3F, "z", "()F", false));
        code.add(new MethodInsnNode(INVOKEINTERFACE, VERTEX_CONSUMER, "addVertex",
                "(FFFIFFIIFFF)V", true));
        code.add(new IincInsnNode(9, 1));
        code.add(new JumpInsnNode(GOTO, loop));
        code.add(done);
        code.add(new InsnNode(RETURN));

        method.maxStack = 12;
        method.maxLocals = 14;
        return true;
    }

    private static boolean adaptLevelRendererMixin(ClassNode node) {
        String oldSkyDesc = "(L" + POSE_STACK + ";L" + MATRIX4F + ";FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;"
                + CALLBACK + ")V";
        String newSkyDesc = "(L" + MATRIX4F + ";L" + MATRIX4F + ";FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;"
                + CALLBACK + ")V";
        MethodNode skipSky = find(node, "skipSkyRenderingForPhasingBlindness", oldSkyDesc);
        boolean changed = false;
        if (skipSky != null) {
            skipSky.desc = newSkyDesc;
            skipSky.signature = null;
            changed = true;
        }

        // renderSky no longer exposes a PoseStack/popPose point. Drop only this optional
        // cosmetic Watcher overlay hook; the phasing-blindness sky suppression remains.
        MethodNode watcher = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("renderWatcher") && method.desc.equals(oldSkyDesc)) {
                watcher = method;
                break;
            }
        }
        if (watcher != null) {
            node.methods.remove(watcher);
            changed = true;
        }
        return changed;
    }

    private static boolean adaptModelPartInvoker(ClassNode node) {
        String oldDesc = "(L" + POSE_STACK + "$Pose;L" + VERTEX_CONSUMER + ";IIFFFF)V";
        String newDesc = "(L" + POSE_STACK + "$Pose;L" + VERTEX_CONSUMER + ";III)V";
        MethodNode method = find(node, "invokeCompile", oldDesc);
        if (method == null) return false;
        method.desc = newDesc;
        method.signature = null;
        clearParameterAnnotations(method);
        return true;
    }

    /**
     * The rewritten handlers use a different parameter layout from the original
     * 1.20.1 methods. Parameter annotations are indexed against that old layout,
     * so retaining them can make ASM emit an annotation index outside the new
     * descriptor's argument count. Keep method-level Mixin annotations untouched.
     */
    private static void clearParameterAnnotations(MethodNode method) {
        method.visibleParameterAnnotations = null;
        method.invisibleParameterAnnotations = null;
        method.visibleAnnotableParameterCount = 0;
        method.invisibleAnnotableParameterCount = 0;
    }

    private static void shiftLocals(MethodNode method, int first, int amount) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof VarInsnNode var && var.var >= first) var.var += amount;
            else if (instruction instanceof IincInsnNode increment && increment.var >= first) increment.var += amount;
        }
        if (method.localVariables != null) {
            for (LocalVariableNode local : method.localVariables) {
                if (local.index >= first) local.index += amount;
            }
        }
        method.maxLocals += amount;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }
}
