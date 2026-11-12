package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Data generation code is emptied; the rest of a jar stays as it was. */
class Datagen1201Test implements Opcodes {
    private static ClassNode cls(String name, String superName) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = superName;
        return n;
    }

    private static MethodNode method(ClassNode n, int access, String name, String desc, String owner, String call) {
        MethodNode m = new MethodNode(access, name, desc, null, null);
        if (owner != null) m.instructions.add(new MethodInsnNode(INVOKESTATIC, owner, call, "()V", false));
        m.instructions.add(new InsnNode(RETURN));
        m.maxStack = 1;
        m.maxLocals = 4;
        n.methods.add(m);
        return m;
    }

    @Test
    void providerClassBecomesShellKeepingStaticConstants() {
        ClassNode provider = cls("m/data/MyRecipes", "net/minecraft/data/recipes/RecipeProvider");
        provider.fields.add(new FieldNode(ACC_PRIVATE, "output", "Ljava/lang/Object;", null, null));
        provider.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "TAG", "Ljava/lang/String;", null, null));
        method(provider, ACC_PUBLIC, "<init>", "(Lnet/minecraft/data/PackOutput;)V", "net/minecraft/data/recipes/RecipeProvider", "<init>");
        method(provider, ACC_PUBLIC, "<init>", "()V", "net/minecraft/data/recipes/RecipeProvider", "<init>");
        method(provider, ACC_PUBLIC, "buildRecipes", "(Ljava/util/function/Consumer;)V", "net/minecraft/data/recipes/ShapedRecipeBuilder", "shaped");
        method(provider, ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);

        Datagen1201.Stats stats = Datagen1201.apply(List.of(provider));

        assertEquals(1, stats.classes);
        assertEquals("java/lang/Object", provider.superName);
        assertEquals(1, provider.fields.size());
        assertEquals("TAG", provider.fields.get(0).name);
        List<String> names = new ArrayList<>();
        for (MethodNode m : provider.methods) names.add(m.name);
        assertEquals(List.of("<init>", "<clinit>"), names); // the constructor taking a PackOutput is gone, the plain one throws
        assertEquals(ATHROW, provider.methods.get(0).instructions.getLast().getOpcode());
    }

    @Test
    void gatherDataHandlerAndDatagenOnlyHelpersReturnImmediately() {
        ClassNode mod = cls("m/Mod", "java/lang/Object");
        MethodNode gather = method(mod, ACC_PUBLIC | ACC_STATIC, "gather", "(Lnet/neoforged/neoforge/data/event/GatherDataEvent;)V", "m/Foo", "bar");
        MethodNode helper = method(mod, ACC_PUBLIC | ACC_STATIC, "helper", "()V", "net/minecraft/data/recipes/ShapelessRecipeBuilder", "shapeless");
        MethodNode normal = method(mod, ACC_PUBLIC | ACC_STATIC, "normal", "()V", "m/Foo", "bar");

        Datagen1201.Stats stats = Datagen1201.apply(List.of(mod));

        assertEquals(0, stats.classes);
        assertEquals(2, stats.methods);
        assertEquals(1, gather.instructions.size());
        assertEquals(1, helper.instructions.size());
        assertEquals(2, normal.instructions.size());
        assertTrue(mod.methods.contains(normal));
    }

    @Test
    void helpersUsingLootDataIdAreEmptiedWithDatagenOnlyMethods() {
        ClassNode mod = cls("m/PalladiumLootDataHelper", "java/lang/Object");
        MethodNode helper = method(mod, ACC_PUBLIC | ACC_STATIC, "validateLoot",
                "(Ljava/lang/Object;)V", "net/minecraft/world/level/storage/loot/LootDataId", "toString");
        MethodNode normal = method(mod, ACC_PUBLIC | ACC_STATIC, "normal", "()V", "m/Foo", "bar");

        Datagen1201.Stats stats = Datagen1201.apply(List.of(mod));

        assertEquals(0, stats.classes);
        assertEquals(1, stats.methods);
        assertEquals(1, helper.instructions.size());
        assertEquals(RETURN, helper.instructions.getFirst().getOpcode());
        assertEquals(2, normal.instructions.size());
    }

    @Test
    void nestedClassesOfAShellAreShellsToo() {
        ClassNode outer = cls("m/gen/LootProvider", "net/minecraft/data/loot/LootTableProvider");
        ClassNode inner = cls("m/gen/LootProvider$Blocks", "java/lang/Object");
        inner.interfaces = new ArrayList<>(List.of("java/lang/Runnable"));

        Datagen1201.Stats stats = Datagen1201.apply(List.of(outer, inner));

        assertEquals(2, stats.classes);
        assertTrue(inner.interfaces.isEmpty());
    }
}
