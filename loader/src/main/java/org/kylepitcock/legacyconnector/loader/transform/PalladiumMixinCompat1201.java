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
    private static final String PLAYER_LIST_MIXIN = "net/threetag/palladium/mixin/PlayerListMixin";
    private static final String ENTITY_SELECTOR_PARSER_MIXIN = "net/threetag/palladium/mixin/EntitySelectorParserMixin";
    private static final String PLAYER_RENDERER_INVOKER = "net/threetag/palladium/mixin/client/PlayerRendererInvoker";
    private static final String BODY_PART = "net/threetag/palladium/entity/BodyPart";
    private static final String MIXIN_SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String HUMANOID_ARMOR_MIXIN = "net/threetag/palladium/mixin/client/HumanoidArmorLayerMixin";
    private static final String GEO_RENDERER_MIXIN = "net/threetag/palladium/mixin/client/GeoRendererMixin";
    private static final String LEVEL_RENDERER_MIXIN = "net/threetag/palladium/mixin/client/LevelRendererMixin";
    private static final String MODEL_PART_INVOKER = "net/threetag/palladium/mixin/client/ModelPartInvoker";
    private static final String VIBRATION_LISTENER_MIXIN =
            "net/threetag/palladium/mixin/VibrationListenerMixin";
    private static final String HOLDER = "net/minecraft/core/Holder";
    private static final String GAME_EVENT = "net/minecraft/world/level/gameevent/GameEvent";

    private static final String POSE_STACK = "com/mojang/blaze3d/vertex/PoseStack";
    private static final String VERTEX_CONSUMER = "com/mojang/blaze3d/vertex/VertexConsumer";
    private static final String MULTI_BUFFER = "net/minecraft/client/renderer/MultiBufferSource";
    private static final String HUMANOID_MODEL = "net/minecraft/client/model/HumanoidModel";
    private static final String DISPLAY_SLOT = "net/minecraft/world/scores/DisplaySlot";
    private static final String OBJECTIVE = "net/minecraft/world/scores/Objective";
    private static final String SCOREBOARD = "net/minecraft/world/scores/Scoreboard";
    private static final String SERVER_SCOREBOARD = "net/minecraft/server/ServerScoreboard";
    private static final String PLAYER_SCORE_ENTRY = "net/minecraft/world/scores/PlayerScoreEntry";
    // Mc1201Tables maps the removed 1.20.1 Score class to this stand-in before Palladium mixin adaptation runs.
    private static final String LEGACY_SCORE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyScore";
    private static final String NUMBER_FORMAT = "net/minecraft/network/chat/numbers/NumberFormat";
    private static final String COMPONENT = "net/minecraft/network/chat/Component";
    private static final String SET_SCORE_PACKET = "net/minecraft/network/protocol/game/ClientboundSetScorePacket";
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
            case PLAYER_LIST_MIXIN -> adaptPlayerListMixin(node) ? 1 : 0;
            case ENTITY_SELECTOR_PARSER_MIXIN -> adaptEntitySelectorParserMixin(node) ? 1 : 0;
            case PLAYER_RENDERER_INVOKER -> adaptPlayerRendererInvoker(node) ? 1 : 0;
            case BODY_PART -> adaptBodyPartRotationCall(node) ? 1 : 0;
            case HUMANOID_ARMOR_MIXIN -> adaptHumanoidArmorMixin(node) ? 1 : 0;
            case GEO_RENDERER_MIXIN -> adaptGeoRendererMixin(node) ? 1 : 0;
            case LEVEL_RENDERER_MIXIN -> adaptLevelRendererMixin(node) ? 1 : 0;
            case MODEL_PART_INVOKER -> adaptModelPartInvoker(node) ? 1 : 0;
            case VIBRATION_LISTENER_MIXIN -> adaptVibrationListenerMixin(node) ? 1 : 0;
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

    /**
     * Minecraft 1.20.1's EntitySelectorParser exposed a single "predicate"
     * field. In 1.21.1 it owns a list named "predicates" and the old field no
     * longer exists. Palladium's mixin only declared that field as a @Shadow;
     * its power-id extension uses its own @Unique field instead. Remove only
     * the unused stale shadow, and reject any bytecode that actually depends
     * on it rather than silently changing selector behavior.
     */
    /**
     * Palladium 4.5.x's PlayerList mixin replays tracked objectives to a
     * joining player. Minecraft 1.21.1 replaced Scoreboard.getPlayerScores
     * (Collection<Score>) with listPlayerScores (Collection<PlayerScoreEntry>)
     * and changed ClientboundSetScorePacket from a four-argument mode packet
     * to a record carrying optional display/number-format values.
     *
     * <p>Keep the feature: adapt its one targeted mixin method to consume the
     * new score-entry record and forward its display metadata to the new packet.
     * This is intentionally scoped to Palladium's exact PlayerListMixin shape.</p>
     */
    private static boolean adaptPlayerListMixin(ClassNode node) {
        String objective = "L" + OBJECTIVE + ";";
        String callback = CALLBACK;
        String handlerDesc = "(L" + SERVER_SCOREBOARD + ";"
                + "Lnet/minecraft/server/level/ServerPlayer;" + callback + ")V";
        MethodNode handler = find(node, "updateEntireScoreboard", handlerDesc);
        if (handler == null) {
            return false;
        }

        String oldScoresDesc = "(" + objective + ")Ljava/util/Collection;";
        String newPacketDesc = "(Ljava/lang/String;Ljava/lang/String;ILjava/util/Optional;Ljava/util/Optional;)V";
        String oldPacketDesc = "(L" + SERVER_SCOREBOARD + "$Method;Ljava/lang/String;Ljava/lang/String;I)V";

        MethodInsnNode listScores = null;
        MethodInsnNode packetConstructor = null;
        TypeInsnNode legacyScoreCast = null;
        int oldListCalls = 0;
        int newListCalls = 0;
        int oldPacketCalls = 0;
        int newPacketCalls = 0;
        for (AbstractInsnNode instruction : handler.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals(SERVER_SCOREBOARD)
                        && call.name.equals("getPlayerScores")
                        && call.desc.equals(oldScoresDesc)) {
                    listScores = call;
                    oldListCalls++;
                } else if (call.owner.equals(SCOREBOARD)
                        && call.name.equals("listPlayerScores")
                        && call.desc.equals(oldScoresDesc)) {
                    newListCalls++;
                }
                if (call.owner.equals(SET_SCORE_PACKET) && call.name.equals("<init>")) {
                    if (call.desc.equals(oldPacketDesc)) {
                        packetConstructor = call;
                        oldPacketCalls++;
                    } else if (call.desc.equals(newPacketDesc)) {
                        newPacketCalls++;
                    }
                }
            } else if (instruction instanceof TypeInsnNode type
                    && type.getOpcode() == CHECKCAST
                    && type.desc.equals(LEGACY_SCORE)) {
                legacyScoreCast = type;
            }
        }

        // An already-adapted class is a no-op. This also makes repeated
        // transformation safe if the loader sees a cached translated artifact.
        if (oldListCalls == 0 && newListCalls == 1
                && oldPacketCalls == 0 && newPacketCalls == 1
                && legacyScoreCast == null) {
            return false;
        }
        if (oldListCalls != 1 || listScores == null
                || oldPacketCalls != 1 || packetConstructor == null
                || newListCalls != 0 || newPacketCalls != 0
                || legacyScoreCast == null) {
            throw new IllegalStateException("Palladium PlayerListMixin.updateEntireScoreboard no longer matches the inspected 1.20.1 scoreboard replay shape");
        }

        // The new method is declared on Scoreboard (the receiver remains a
        // ServerScoreboard instance, which is a Scoreboard subclass).
        listScores.owner = SCOREBOARD;
        listScores.name = "listPlayerScores";
        listScores.itf = false;
        legacyScoreCast.desc = PLAYER_SCORE_ENTRY;

        // Locate the exact old packet argument sequence immediately before
        // ClientboundSetScorePacket.<init>:
        //   CHANGE, objective name, score owner, score value.
        AbstractInsnNode scoreValueCall = previousReal(packetConstructor);
        AbstractInsnNode scoreValueReceiver = previousReal(scoreValueCall);
        AbstractInsnNode scoreOwnerCall = previousReal(scoreValueReceiver);
        AbstractInsnNode scoreOwnerReceiver = previousReal(scoreOwnerCall);
        AbstractInsnNode objectiveNameCall = previousReal(scoreOwnerReceiver);
        AbstractInsnNode objectiveReceiver = previousReal(objectiveNameCall);
        AbstractInsnNode updateMode = previousReal(objectiveReceiver);
        AbstractInsnNode duplicate = previousReal(updateMode);
        AbstractInsnNode allocation = previousReal(duplicate);

        if (!(scoreValueCall instanceof MethodInsnNode getScore)
                || getScore.getOpcode() != INVOKEVIRTUAL
                || !getScore.owner.equals(LEGACY_SCORE)
                || !getScore.name.equals("getScore")
                || !getScore.desc.equals("()I")
                || !(scoreValueReceiver instanceof VarInsnNode scoreValueLoad)
                || scoreValueLoad.getOpcode() != ALOAD || scoreValueLoad.var != 8
                || !(scoreOwnerCall instanceof MethodInsnNode getOwner)
                || getOwner.getOpcode() != INVOKEVIRTUAL
                || !getOwner.owner.equals(LEGACY_SCORE)
                || !getOwner.name.equals("getOwner")
                || !getOwner.desc.equals("()Ljava/lang/String;")
                || !(scoreOwnerReceiver instanceof VarInsnNode scoreOwnerLoad)
                || scoreOwnerLoad.getOpcode() != ALOAD || scoreOwnerLoad.var != 8
                || !(objectiveNameCall instanceof MethodInsnNode getObjectiveName)
                || getObjectiveName.getOpcode() != INVOKEVIRTUAL
                || !getObjectiveName.owner.equals(OBJECTIVE)
                || !getObjectiveName.name.equals("getName")
                || !getObjectiveName.desc.equals("()Ljava/lang/String;")
                || !(objectiveReceiver instanceof VarInsnNode objectiveLoad)
                || objectiveLoad.getOpcode() != ALOAD || objectiveLoad.var != 6
                || !(updateMode instanceof FieldInsnNode mode)
                || mode.getOpcode() != GETSTATIC
                || !mode.owner.equals(SERVER_SCOREBOARD + "$Method")
                || !mode.name.equals("CHANGE")
                || !mode.desc.equals("L" + SERVER_SCOREBOARD + "$Method;")
                || !(duplicate instanceof InsnNode dup) || dup.getOpcode() != DUP
                || !(allocation instanceof TypeInsnNode packetNew)
                || packetNew.getOpcode() != NEW || !packetNew.desc.equals(SET_SCORE_PACKET)
                || !packetConstructor.owner.equals(SET_SCORE_PACKET)
                || packetConstructor.getOpcode() != INVOKESPECIAL) {
            throw new IllegalStateException("Palladium PlayerListMixin score packet argument sequence changed; refusing a partial scoreboard migration");
        }

        // Discard the old mode/objective/Score argument instructions. Keep NEW
        // and DUP, then build the new packet from PlayerScoreEntry accessors.
        for (AbstractInsnNode old : new AbstractInsnNode[] {
                mode, objectiveLoad, getObjectiveName, scoreOwnerLoad, getOwner, scoreValueLoad, getScore
        }) {
            handler.instructions.remove(old);
        }

        InsnList packetArguments = new InsnList();
        packetArguments.add(new VarInsnNode(ALOAD, 8));
        packetArguments.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SCORE_ENTRY,
                "owner", "()Ljava/lang/String;", false));

        packetArguments.add(new VarInsnNode(ALOAD, 6));
        packetArguments.add(new MethodInsnNode(INVOKEVIRTUAL, OBJECTIVE,
                "getName", "()Ljava/lang/String;", false));

        packetArguments.add(new VarInsnNode(ALOAD, 8));
        packetArguments.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SCORE_ENTRY,
                "value", "()I", false));

        packetArguments.add(new VarInsnNode(ALOAD, 8));
        packetArguments.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SCORE_ENTRY,
                "display", "()L" + COMPONENT + ";", false));
        packetArguments.add(new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "ofNullable", "(Ljava/lang/Object;)Ljava/util/Optional;", false));

        packetArguments.add(new VarInsnNode(ALOAD, 8));
        packetArguments.add(new MethodInsnNode(INVOKEVIRTUAL, PLAYER_SCORE_ENTRY,
                "numberFormatOverride", "()L" + NUMBER_FORMAT + ";", false));
        packetArguments.add(new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "ofNullable", "(Ljava/lang/Object;)Ljava/util/Optional;", false));

        handler.instructions.insert(dup, packetArguments);
        packetConstructor.desc = newPacketDesc;
        handler.maxStack = Math.max(handler.maxStack, 7);
        return true;
    }

    private static boolean adaptEntitySelectorParserMixin(ClassNode node) {
        FieldNode obsolete = null;
        for (FieldNode field : node.fields) {
            if (!"Ljava/util/function/Predicate;".equals(field.desc) || !hasAnnotation(field.visibleAnnotations, MIXIN_SHADOW)) {
                continue;
            }
            if (obsolete != null) {
                throw new IllegalStateException("Palladium EntitySelectorParserMixin has multiple obsolete Predicate @Shadow fields");
            }
            obsolete = field;
        }
        if (obsolete == null) return false;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode access
                        && access.owner.equals(node.name)
                        && access.name.equals(obsolete.name)
                        && access.desc.equals(obsolete.desc)) {
                    throw new IllegalStateException("Palladium EntitySelectorParserMixin uses the obsolete predicate @Shadow field in "
                            + method.name + method.desc);
                }
            }
        }

        node.fields.remove(obsolete);
        return true;
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) return false;
        return annotations.stream().anyMatch(annotation -> descriptor.equals(annotation.desc));
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

    /**
     * Minecraft 1.21.1 added the entity scale as the final parameter to
     * LivingEntityRenderer.setupRotations. Keep Palladium's BodyPart call in
     * sync with the adapted PlayerRendererInvoker and pass the actual player
     * scale instead of allowing it to resolve to a throwing soft link.
     *
     * BodyPart.getTransformationMatrix stores its AbstractClientPlayer in
     * local 4; the call's three legacy float arguments are already on the
     * operand stack, so appending player.getScale() supplies the fourth.
     */
    private static boolean adaptBodyPartRotationCall(ClassNode node) {
        String oldDesc = "(Lnet/minecraft/client/player/AbstractClientPlayer;L" + POSE_STACK + ";FFF)V";
        String newDesc = "(Lnet/minecraft/client/player/AbstractClientPlayer;L" + POSE_STACK + ";FFFF)V";
        String livingEntity = "net/minecraft/world/entity/LivingEntity";
        MethodInsnNode oldCall = null;
        MethodInsnNode newCall = null;
        int oldCount = 0;
        int newCount = 0;

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !call.owner.equals(PLAYER_RENDERER_INVOKER)
                        || !call.name.equals("invokeSetupRotations")) continue;
                if (call.getOpcode() != INVOKEINTERFACE || !call.itf) {
                    throw new IllegalStateException("Palladium BodyPart PlayerRendererInvoker call is not INVOKEINTERFACE");
                }
                if (call.desc.equals(oldDesc)) {
                    oldCall = call;
                    oldCount++;
                } else if (call.desc.equals(newDesc)) {
                    newCall = call;
                    newCount++;
                }
            }
        }

        if (oldCount == 0 && newCount == 1 && newCall != null) {
            AbstractInsnNode scaleCallNode = previousReal(newCall);
            AbstractInsnNode playerLoadNode = previousReal(scaleCallNode);
            if (scaleCallNode instanceof MethodInsnNode scaleCall
                    && scaleCall.getOpcode() == INVOKEVIRTUAL
                    && scaleCall.owner.equals(livingEntity)
                    && scaleCall.name.equals("getScale")
                    && scaleCall.desc.equals("()F")
                    && playerLoadNode instanceof VarInsnNode playerLoad
                    && playerLoad.getOpcode() == ALOAD
                    && playerLoad.var == 4) {
                return false;
            }
            throw new IllegalStateException("Palladium BodyPart already-migrated rotation call is missing player.getScale()");
        }
        if (oldCount != 1 || newCount != 0 || oldCall == null) {
            throw new IllegalStateException("Palladium BodyPart PlayerRendererInvoker call shape changed: expected one legacy call, found "
                    + oldCount + " legacy and " + newCount + " migrated");
        }

        InsnList scaleArgument = new InsnList();
        scaleArgument.add(new VarInsnNode(ALOAD, 4));
        scaleArgument.add(new MethodInsnNode(INVOKEVIRTUAL, livingEntity, "getScale", "()F", false));
        MethodNode containingMethod = null;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction == oldCall) {
                    containingMethod = method;
                    break;
                }
            }
            if (containingMethod != null) break;
        }
        if (containingMethod == null) {
            throw new IllegalStateException("Palladium BodyPart rotation call is detached from its method");
        }

        containingMethod.instructions.insertBefore(oldCall, scaleArgument);
        oldCall.desc = newDesc;
        containingMethod.maxStack = Math.max(containingMethod.maxStack + 1, 7);
        return true;
    }

    private static boolean adaptHumanoidArmorMixin(ClassNode node) {
        String oldArmorPieceDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";"
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                + "L" + HUMANOID_MODEL + ";" + CALLBACK + ")V";
        String newArmorPieceDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";"
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                + "L" + HUMANOID_MODEL + ";FFFFFF" + CALLBACK + ")V";
        String targetArmorPieceDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";"
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                + "L" + HUMANOID_MODEL + ";FFFFFF)V";
        MethodNode armorPiece = find(node, "renderArmorPiece", oldArmorPieceDesc);
        boolean changed = false;
        if (armorPiece != null) {
            // In 1.21.1 HumanoidArmorLayer.render() calls the six-float overload.
            // Palladium's old refmap maps the bare selector to the no-float
            // overload, so changing only the handler descriptor still binds
            // the wrong target. Use an exact named selector for the active
            // overload and bypass that stale 1.20.1 refmap entry.
            shiftLocals(armorPiece, 7, 6);
            armorPiece.desc = newArmorPieceDesc;
            armorPiece.signature = null;
            armorPiece.parameters = null;
            clearParameterAnnotations(armorPiece);
            setInjectMethodSelector(armorPiece, "renderArmorPiece",
                    "renderArmorPiece" + targetArmorPieceDesc);
            changed = true;
        }

        String oldDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";I"
                + "Lnet/minecraft/world/item/ArmorItem;L" + HUMANOID_MODEL + ";ZFFF"
                + "Ljava/lang/String;" + CALLBACK + ")V";
        String newDesc = "(L" + POSE_STACK + ";L" + MULTI_BUFFER + ";I"
                + "L" + HUMANOID_MODEL + ";ILnet/minecraft/resources/ResourceLocation;"
                + CALLBACK + ")V";
        MethodNode method = find(node, "renderModel", oldDesc);
        if (method != null) {
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.parameters = null;
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
            changed = true;
        }
        return changed;
    }

    /**
     * Palladium's 1.20.1 refmap maps the bare selector to the old no-float
     * HumanoidArmorLayer method. Point this one injection at the exact 1.21.1
     * overload and disable the obsolete refmap lookup for that selector only.
     */
    private static void setInjectMethodSelector(MethodNode handler, String oldSelector, String newSelector) {
        if (replaceInjectMethodSelector(handler.visibleAnnotations, oldSelector, newSelector)) return;
        if (replaceInjectMethodSelector(handler.invisibleAnnotations, oldSelector, newSelector)) return;
        throw new IllegalStateException("Palladium HumanoidArmorLayerMixin is missing @Inject(method=[\""
                + oldSelector + "\"])");
    }

    private static boolean replaceInjectMethodSelector(List<AnnotationNode> annotations,
                                                       String oldSelector, String newSelector) {
        if (annotations == null) return false;
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                    || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (!"method".equals(annotation.values.get(i))
                        || !(annotation.values.get(i + 1) instanceof List<?> methods)) continue;
                List<Object> replacement = new java.util.ArrayList<>(methods);
                int index = replacement.indexOf(oldSelector);
                if (index < 0) continue;
                replacement.set(index, newSelector);
                annotation.values.set(i + 1, replacement);

                boolean remapPresent = false;
                for (int j = 0; j + 1 < annotation.values.size(); j += 2) {
                    if ("remap".equals(annotation.values.get(j))) {
                        annotation.values.set(j + 1, Boolean.FALSE);
                        remapPresent = true;
                        break;
                    }
                }
                if (!remapPresent) {
                    annotation.values.add("remap");
                    annotation.values.add(Boolean.FALSE);
                }
                return true;
            }
        }
        return false;
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

    /**
     * Minecraft 1.21.1 passes a Holder<GameEvent> to vibration listeners.
     * Palladium's handler compares the incoming event by identity with two
     * GameEvent constants, so unwrap Holder.value() before those comparisons.
     */
    private static boolean adaptVibrationListenerMixin(ClassNode node) {
        String oldDesc = "(Lnet/minecraft/server/level/ServerLevel;L" + GAME_EVENT
                + ";L" + GAME_EVENT + "$Context;Lnet/minecraft/world/phys/Vec3;"
                + CALLBACK_RETURN + ")V";
        String newDesc = "(Lnet/minecraft/server/level/ServerLevel;L" + HOLDER
                + ";L" + GAME_EVENT + "$Context;Lnet/minecraft/world/phys/Vec3;"
                + CALLBACK_RETURN + ")V";
        MethodNode handler = find(node, "handleGameEvent", oldDesc);
        if (handler == null) return false;

        int unwraps = 0;
        for (AbstractInsnNode instruction : handler.instructions.toArray()) {
            if (instruction instanceof VarInsnNode load
                    && load.getOpcode() == ALOAD
                    && load.var == 2) {
                InsnList unbox = new InsnList();
                unbox.add(new MethodInsnNode(INVOKEINTERFACE, HOLDER,
                        "value", "()Ljava/lang/Object;", true));
                unbox.add(new TypeInsnNode(CHECKCAST, GAME_EVENT));
                handler.instructions.insert(load, unbox);
                unwraps++;
            }
        }
        if (unwraps != 2) {
            throw new IllegalStateException(
                    "Palladium VibrationListenerMixin.handleGameEvent expected two Holder.value() unwraps, found "
                            + unwraps);
        }
        handler.desc = newDesc;
        handler.signature = null;
        return true;
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

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }
}
