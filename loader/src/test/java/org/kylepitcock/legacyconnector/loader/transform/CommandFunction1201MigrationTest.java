package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** The 1.20.1 CommandFunction class move used by Palladium's command abilities. */
class CommandFunction1201MigrationTest implements Opcodes {
    private static final String LEGACY_FUNCTION = "net/minecraft/commands/CommandFunction";
    private static final String MODERN_FUNCTION = "net/minecraft/commands/functions/CommandFunction";
    private static final String SERVER = "net/minecraft/server/MinecraftServer";
    private static final String PARSER = "net/threetag/palladium/util/property/CommandFunctionProperty$CommandFunctionParsing";

    private static ClassNode cls(String name) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static ClassNode remap(ClassNode source) {
        ClassNode result = new ClassNode();
        Remapper names = new Remapper() {
            @Override
            public String map(String internalName) {
                return Mc1201Tables.get().classes.getOrDefault(internalName, internalName);
            }
        };
        source.accept(new ClassRemapper(result, names));
        return result;
    }

    @Test
    void movedCommandFunctionReturnTypeDoesNotBecomeAThrowingSoftLink() {
        assertEquals(MODERN_FUNCTION, Mc1201Tables.get().classes.get(LEGACY_FUNCTION),
                "the 1.20.1 class must be renamed before soft-link analysis");
        assertTrue(McMembers1201.hasClass(MODERN_FUNCTION),
                "the 1.21.1 member index must recognize the replacement type");

        String legacyDesc = "(L" + SERVER + ";)L" + LEGACY_FUNCTION + ";";
        String modernDesc = "(L" + SERVER + ";)L" + MODERN_FUNCTION + ";";

        ClassNode parser = cls(PARSER);
        MethodNode resolve = new MethodNode(ACC_PUBLIC | ACC_STATIC, "getCommandFunction", legacyDesc, null, null);
        resolve.instructions.add(new InsnNode(ACONST_NULL));
        resolve.instructions.add(new InsnNode(ARETURN));
        parser.methods.add(resolve);

        ClassNode ability = cls("net/threetag/palladium/power/ability/CommandAbility");
        MethodNode firstTick = new MethodNode(ACC_PUBLIC, "firstTick", "(L" + SERVER + ";)V", null, null);
        firstTick.instructions.add(new VarInsnNode(ALOAD, 1));
        firstTick.instructions.add(new MethodInsnNode(INVOKESTATIC, PARSER, "getCommandFunction", legacyDesc, false));
        firstTick.instructions.add(new InsnNode(POP));
        firstTick.instructions.add(new InsnNode(RETURN));
        ability.methods.add(firstTick);

        ClassNode mappedParser = remap(parser);
        ClassNode mappedAbility = remap(ability);
        Mc1201Hierarchy hierarchy = new Mc1201Hierarchy();
        hierarchy.add(mappedParser);
        hierarchy.add(mappedAbility);

        SoftLink1201 softLink = new SoftLink1201(List.of(mappedParser, mappedAbility), hierarchy);
        assertEquals(0, softLink.apply(mappedParser));
        assertEquals(0, softLink.apply(mappedAbility));
        assertEquals(0, softLink.stats.symbols(),
                "the remapped return type and its internal call must link without a fail() stand-in");

        MethodInsnNode call = null;
        for (AbstractInsnNode instruction : firstMethod(mappedAbility).instructions) {
            if (instruction instanceof MethodInsnNode method && method.name.equals("getCommandFunction")) call = method;
        }
        assertNotNull(call);
        assertEquals(modernDesc, call.desc);
    }

    private static MethodNode firstMethod(ClassNode node) {
        return node.methods.stream().filter(method -> method.name.equals("firstTick")).findFirst().orElseThrow();
    }
}
