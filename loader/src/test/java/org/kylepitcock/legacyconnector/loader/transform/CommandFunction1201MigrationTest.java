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

    @Test
    void commandFunctionGenericErasureAndQueuedExecutionCallsUseCompatibilityHelpers() {
        assertTrue(McMembers1201.hasClass(MODERN_FUNCTION));

        String commandSourceStack = "net/minecraft/commands/CommandSourceStack";
        String resourceLocation = "net/minecraft/resources/ResourceLocation";
        String dispatcher = "com/mojang/brigadier/CommandDispatcher";
        String manager = "net/minecraft/server/ServerFunctionManager";
        String fromLinesDesc = "(L" + resourceLocation + ";L" + dispatcher + ";L" + commandSourceStack
                + ";Ljava/util/List;)L" + MODERN_FUNCTION + ";";
        String executeDesc = "(L" + MODERN_FUNCTION + ";L" + commandSourceStack + ";)I";
        String fromLinesHelperDesc = fromLinesDesc;
        String executeHelperDesc = "(L" + manager + ";L" + MODERN_FUNCTION + ";L" + commandSourceStack + ";)V";
        String helperOwner = "org/kylepitcock/legacyconnector/v1_20_1/migrate/CommandFunctionCompat";

        ClassNode parser = cls("example/LegacyFunctionParser");
        MethodNode parse = new MethodNode(ACC_PUBLIC | ACC_STATIC, "parse", "()L" + MODERN_FUNCTION + ";", null, null);
        parse.instructions.add(new InsnNode(ACONST_NULL));
        parse.instructions.add(new InsnNode(ACONST_NULL));
        parse.instructions.add(new InsnNode(ACONST_NULL));
        parse.instructions.add(new InsnNode(ACONST_NULL));
        parse.instructions.add(new MethodInsnNode(INVOKESTATIC, MODERN_FUNCTION, "fromLines", fromLinesDesc, false));
        parse.instructions.add(new InsnNode(ARETURN));
        parser.methods.add(parse);

        ClassNode caller = cls("example/LegacyFunctionCaller");
        MethodNode discardedResult = new MethodNode(ACC_PUBLIC | ACC_STATIC, "executeAndIgnoreCount", "()V", null, null);
        discardedResult.instructions.add(new InsnNode(ACONST_NULL));
        discardedResult.instructions.add(new InsnNode(ACONST_NULL));
        discardedResult.instructions.add(new InsnNode(ACONST_NULL));
        discardedResult.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, manager, "execute", executeDesc, false));
        discardedResult.instructions.add(new InsnNode(POP));
        discardedResult.instructions.add(new InsnNode(RETURN));
        caller.methods.add(discardedResult);

        MethodNode observedResult = new MethodNode(ACC_PUBLIC | ACC_STATIC, "executeAndObserveCount", "()I", null, null);
        observedResult.instructions.add(new InsnNode(ACONST_NULL));
        observedResult.instructions.add(new InsnNode(ACONST_NULL));
        observedResult.instructions.add(new InsnNode(ACONST_NULL));
        observedResult.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, manager, "execute", executeDesc, false));
        observedResult.instructions.add(new InsnNode(IRETURN));
        caller.methods.add(observedResult);

        Mc1201Hierarchy hierarchy = new Mc1201Hierarchy();
        hierarchy.add(parser);
        hierarchy.add(caller);
        Mc1201Migration.Counts counts = new Mc1201Migration.Counts();
        counts.add(Mc1201Migration.apply(parser, hierarchy));
        counts.add(Mc1201Migration.apply(caller, hierarchy));

        assertEquals(2, counts.calls, "fromLines and the discarded execute result should be migrated");

        MethodInsnNode fromLines = null;
        for (AbstractInsnNode instruction : parse.instructions) {
            if (instruction instanceof MethodInsnNode method) fromLines = method;
        }
        assertNotNull(fromLines);
        assertEquals(Opcodes.INVOKESTATIC, fromLines.getOpcode());
        assertEquals(helperOwner, fromLines.owner);
        assertEquals("fromLines", fromLines.name);
        assertEquals(fromLinesHelperDesc, fromLines.desc);

        MethodInsnNode execute = null;
        for (AbstractInsnNode instruction : discardedResult.instructions) {
            if (instruction instanceof MethodInsnNode method) execute = method;
            assertTrue(instruction.getOpcode() != Opcodes.POP, "the obsolete int-result POP must be removed");
        }
        assertNotNull(execute);
        assertEquals(Opcodes.INVOKESTATIC, execute.getOpcode());
        assertEquals(helperOwner, execute.owner);
        assertEquals("execute", execute.name);
        assertEquals(executeHelperDesc, execute.desc);

        MethodInsnNode stillObserved = null;
        for (AbstractInsnNode instruction : observedResult.instructions) {
            if (instruction instanceof MethodInsnNode method) stillObserved = method;
        }
        assertNotNull(stillObserved);
        assertEquals(manager, stillObserved.owner,
                "a caller that observes the old synchronous command count must not receive a fabricated value");
        assertEquals(executeDesc, stillObserved.desc);
        assertEquals(Opcodes.IRETURN, stillObserved.getNext().getOpcode());
    }

    private static MethodNode firstMethod(ClassNode node) {
        return node.methods.stream().filter(method -> method.name.equals("firstTick")).findFirst().orElseThrow();
    }
}
