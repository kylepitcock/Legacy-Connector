package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies that Palladium custom ammo is created through the 1.21.1 crossbow hook. */
class PalladiumCrossbowItem1201Test implements Opcodes {
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    private static final String PROJECTILE = "net/minecraft/world/entity/projectile/Projectile";
    private static final String ADDON_PROJECTILE_ITEM = "net/threetag/palladium/item/AddonProjectileItem";
    private static final String CIR =
            "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";

    private static ClassNode mixin() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = PalladiumCrossbowItem1201.MIXIN;
        node.superName = "java/lang/Object";

        MethodNode handler = new MethodNode(ACC_PRIVATE | ACC_STATIC, "shootProjectile",
                "(L" + LEVEL + ";L" + LIVING_ENTITY + ";Lnet/minecraft/world/InteractionHand;"
                        + "L" + ITEM_STACK + ";L" + ITEM_STACK + ";FZFFF"
                        + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                null, null);
        handler.visibleAnnotations = new ArrayList<>();
        AnnotationNode inject = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Inject;");
        AnnotationNode at = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
        at.values = new ArrayList<>(List.of("value", "HEAD"));
        inject.values = new ArrayList<>(List.of(
                "method", new ArrayList<>(List.of("shootProjectile")),
                "at", new ArrayList<>(List.of(at))));
        handler.visibleAnnotations.add(inject);
        handler.instructions.add(new InsnNode(RETURN));
        handler.maxLocals = 11;
        handler.maxStack = 1;
        node.methods.add(handler);
        return node;
    }

    @Test
    void customAmmoIsCreatedByTheNewCrossbowProjectileFactory() {
        ClassNode node = mixin();

        assertEquals(1, PalladiumCrossbowItem1201.apply(node));
        MethodNode handler = find(node, "palladium$createAddonProjectile", PalladiumCrossbowItem1201.HANDLER_DESC);
        assertNotNull(handler);
        assertFalse((handler.access & ACC_STATIC) != 0,
                "the 1.21.1 createProjectile hook is an instance method");
        assertEquals(8, handler.maxLocals);
        assertEquals(4, handler.maxStack);

        assertTrue(hasField(handler, GETFIELD, LEVEL, "isClientSide", "Z"),
                "the custom factory must not cast a client Level to ServerLevel");
        assertTrue(hasType(handler, INSTANCEOF, ADDON_PROJECTILE_ITEM));
        assertTrue(hasType(handler, CHECKCAST, ADDON_PROJECTILE_ITEM));
        assertTrue(hasCall(handler, INVOKEVIRTUAL, ADDON_PROJECTILE_ITEM, "createProjectile",
                "(L" + LEVEL + ";L" + ITEM_STACK + ";L" + LIVING_ENTITY + ";)L" + PROJECTILE + ";"));
        assertTrue(hasCall(handler, INVOKEVIRTUAL, "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
                "setReturnValue", "(Ljava/lang/Object;)V"));

        AnnotationNode inject = findInject(handler);
        assertNotNull(inject);
        assertEquals(List.of("createProjectile"), annotationValue(inject, "method"));
        assertEquals(Boolean.TRUE, annotationValue(inject, "cancellable"));
        List<?> atList = (List<?>) annotationValue(inject, "at");
        assertEquals("HEAD", annotationValue((AnnotationNode) atList.get(0), "value"));

        assertEquals(0, PalladiumCrossbowItem1201.apply(node),
                "the crossbow handler migration must be idempotent");
        assertClassCanBeSerialized(node);
    }

    @Test
    void unrelatedMixinsAreNotModified() {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.name = "example/UnrelatedMixin";
        node.superName = "java/lang/Object";
        assertEquals(0, PalladiumCrossbowItem1201.apply(node));
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(desc)) return method;
        return null;
    }

    private static AnnotationNode findInject(MethodNode method) {
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) return annotation;
        }
        return null;
    }

    private static Object annotationValue(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static boolean hasField(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == opcode
                    && field.owner.equals(owner) && field.name.equals(name) && field.desc.equals(desc)) return true;
        }
        return false;
    }

    private static boolean hasType(MethodNode method, int opcode, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == opcode && type.desc.equals(desc)) return true;
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return true;
        }
        return false;
    }

    private static void assertClassCanBeSerialized(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }
}
