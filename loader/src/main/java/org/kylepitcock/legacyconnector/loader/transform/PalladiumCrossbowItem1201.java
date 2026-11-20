package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Moves Palladium's custom crossbow-ammo interception from the removed 1.20.1
 * shootProjectile(Level, ..., ItemStack ammo, ...) hook to the 1.21.1
 * createProjectile(Level, LivingEntity, ItemStack weapon, ItemStack ammo, boolean)
 * hook. Vanilla's new shootProjectile path then handles launch direction,
 * velocity, mob targeting, durability and sound for the returned projectile.
 */
final class PalladiumCrossbowItem1201 implements Opcodes {
    static final String MIXIN = "net/threetag/palladium/mixin/CrossbowItemMixin";
    private static final String OLD_DESC =
            "(Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/InteractionHand;"
            + "Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemStack;FZFFF"
            + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
    static final String HANDLER_DESC =
            "(Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemStack;Z"
            + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    private static final String PROJECTILE = "net/minecraft/world/entity/projectile/Projectile";
    private static final String ADDON_PROJECTILE_ITEM = "net/threetag/palladium/item/AddonProjectileItem";
    private static final String CALLBACK_RETURN =
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";

    private PalladiumCrossbowItem1201() {}

    /** Returns the number of migrated mixin classes. */
    static int apply(ClassNode node) {
        if (!MIXIN.equals(node.name)) return 0;

        MethodNode handler = find(node, "palladium$createAddonProjectile", HANDLER_DESC);
        if (handler != null) return 0; // already migrated

        handler = find(node, "shootProjectile", OLD_DESC);
        if (handler == null) {
            throw new IllegalStateException("Palladium CrossbowItemMixin no longer matches the inspected 1.20.1 handler");
        }

        AnnotationNode inject = findInject(handler);
        if (inject == null) {
            throw new IllegalStateException("Palladium CrossbowItemMixin.shootProjectile is missing its @Inject annotation");
        }

        rewriteInjection(inject);
        handler.name = "palladium$createAddonProjectile";
        handler.access &= ~ACC_STATIC;
        handler.desc = HANDLER_DESC;
        handler.signature = null;
        handler.parameters = null;
        handler.visibleParameterAnnotations = null;
        handler.invisibleParameterAnnotations = null;
        handler.visibleAnnotableParameterCount = 0;
        handler.invisibleAnnotableParameterCount = 0;
        handler.localVariables = null;
        handler.instructions = createProjectileHandler();
        handler.tryCatchBlocks.clear();
        handler.maxLocals = 8;
        handler.maxStack = 4;

        // The old lambda only existed for the removed ItemStack damage call;
        // new CrossbowItem.performShooting owns durability and sound.
        node.methods.removeIf(method -> method.name.equals("lambda$shootProjectile$0"));
        return 1;
    }

    private static AnnotationNode findInject(MethodNode method) {
        for (List<AnnotationNode> annotations : List.of(
                method.visibleAnnotations == null ? List.of() : method.visibleAnnotations,
                method.invisibleAnnotations == null ? List.of() : method.invisibleAnnotations)) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) return annotation;
            }
        }
        return null;
    }

    private static void rewriteInjection(AnnotationNode inject) {
        if (inject.values == null) inject.values = new ArrayList<>();
        boolean methodFound = false;
        boolean cancellableFound = false;
        for (int i = 0; i + 1 < inject.values.size(); i += 2) {
            String key = (String) inject.values.get(i);
            if (key.equals("method")) {
                inject.values.set(i + 1, new ArrayList<>(List.of("createProjectile")));
                methodFound = true;
            } else if (key.equals("cancellable")) {
                inject.values.set(i + 1, Boolean.TRUE);
                cancellableFound = true;
            }
        }
        if (!methodFound) {
            throw new IllegalStateException("Palladium CrossbowItemMixin @Inject has no method selector");
        }
        if (!cancellableFound) {
            inject.values.add("cancellable");
            inject.values.add(Boolean.TRUE);
        }
    }

    private static InsnList createProjectileHandler() {
        InsnList code = new InsnList();
        LabelNode returnOriginal = new LabelNode();

        // AddonProjectileItem.createProjectile casts to ServerLevel. Keep the
        // vanilla client-side path untouched; the server-created entity will sync.
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new FieldInsnNode(GETFIELD, LEVEL, "isClientSide", "Z"));
        code.add(new JumpInsnNode(IFNE, returnOriginal));

        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, ITEM_STACK, "getItem",
                "()Lnet/minecraft/world/item/Item;", false));
        code.add(new TypeInsnNode(INSTANCEOF, ADDON_PROJECTILE_ITEM));
        code.add(new JumpInsnNode(IFEQ, returnOriginal));

        // return ((AddonProjectileItem) ammo.getItem())
        //     .createProjectile(level, ammo, shooter);
        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, ITEM_STACK, "getItem",
                "()Lnet/minecraft/world/item/Item;", false));
        code.add(new TypeInsnNode(CHECKCAST, ADDON_PROJECTILE_ITEM));
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new VarInsnNode(ALOAD, 4));
        code.add(new VarInsnNode(ALOAD, 2));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, ADDON_PROJECTILE_ITEM, "createProjectile",
                "(L" + LEVEL + ";L" + ITEM_STACK + ";L" + LIVING_ENTITY + ";)L" + PROJECTILE + ";",
                false));
        code.add(new VarInsnNode(ASTORE, 7));
        code.add(new VarInsnNode(ALOAD, 6));
        code.add(new VarInsnNode(ALOAD, 7));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, CALLBACK_RETURN, "setReturnValue",
                "(Ljava/lang/Object;)V", false));

        code.add(returnOriginal);
        code.add(new InsnNode(RETURN));
        return code;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        return null;
    }
}
