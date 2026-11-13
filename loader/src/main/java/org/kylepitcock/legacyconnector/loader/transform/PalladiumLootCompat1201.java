package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Migrates Palladium 4.5.x's custom loot-pool JSON reader from Forge's
 * removed Deserializers Gson factory to NeoForge 1.21.1's loot-pool codec.
 *
 * The ordinary Gson instance remains in place for SimpleJsonResourceReloadListener
 * to parse resource files into JsonElements. Only the Gson.fromJson(..., LootPool.class)
 * operation is redirected to the runtime codec bridge.
 */
final class PalladiumLootCompat1201 implements Opcodes {
    static final String MANAGER = "net/threetag/palladium/loot/LootTableModificationManager";

    private static final String DESERIALIZERS =
            "net/minecraft/world/level/storage/loot/Deserializers";
    private static final String GSON_BUILDER = "com/google/gson/GsonBuilder";
    private static final String GSON = "com/google/gson/Gson";
    private static final String JSON_ELEMENT = "com/google/gson/JsonElement";
    private static final String LOOT_POOL = "net/minecraft/world/level/storage/loot/LootPool";
    private static final String MODIFICATION =
            "net/threetag/palladium/loot/LootTableModificationManager$Modification";
    private static final String HELPER = "legacyconnector/v1_20_1/PalladiumLootCompat";

    private static final String FACTORY_DESC = "()L" + GSON_BUILDER + ";";
    private static final String FROM_JSON_DESC =
            "(L" + JSON_ELEMENT + ";Ljava/lang/Class;)Ljava/lang/Object;";
    private static final String HELPER_DESC =
            "(L" + GSON + ";L" + JSON_ELEMENT + ";Ljava/lang/Class;)L" + LOOT_POOL + ";";
    private static final String LAMBDA_DESC =
            "(Ljava/util/concurrent/atomic/AtomicInteger;"
                    + "Lnet/minecraft/resources/ResourceLocation;"
                    + "L" + MODIFICATION + ";L" + JSON_ELEMENT + ";)V";

    private PalladiumLootCompat1201() {
    }

    /** Returns one when Palladium's loot manager is successfully adapted. */
    static int apply(ClassNode node) {
        if (!MANAGER.equals(node.name)) return 0;

        MethodNode initializer = find(node, "<clinit>", "()V");
        MethodNode poolConsumer = find(node, "lambda$apply$0", LAMBDA_DESC);
        if (initializer == null || poolConsumer == null) {
            throw new IllegalStateException("Palladium LootTableModificationManager no longer has the inspected 4.5.x bytecode shape");
        }

        MethodInsnNode oldFactory = null;
        for (AbstractInsnNode instruction : initializer.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && DESERIALIZERS.equals(call.owner)
                    && ("m_78800_".equals(call.name) || "createLootTableSerializer".equals(call.name))
                    && FACTORY_DESC.equals(call.desc)) {
                if (oldFactory != null) {
                    throw new IllegalStateException("Palladium loot manager invokes the removed Gson factory more than once");
                }
                oldFactory = call;
            }
        }
        if (oldFactory == null) {
            throw new IllegalStateException("Palladium loot manager no longer calls Deserializers.createLootTableSerializer()");
        }

        InsnList gsonBuilder = new InsnList();
        gsonBuilder.add(new TypeInsnNode(NEW, GSON_BUILDER));
        gsonBuilder.add(new InsnNode(DUP));
        gsonBuilder.add(new MethodInsnNode(INVOKESPECIAL, GSON_BUILDER, "<init>", "()V", false));
        initializer.instructions.insertBefore(oldFactory, gsonBuilder);
        initializer.instructions.remove(oldFactory);
        initializer.maxStack = Math.max(initializer.maxStack, 2);

        MethodInsnNode oldPoolParse = null;
        for (AbstractInsnNode instruction : poolConsumer.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && GSON.equals(call.owner)
                    && "fromJson".equals(call.name)
                    && FROM_JSON_DESC.equals(call.desc)) {
                if (oldPoolParse != null) {
                    throw new IllegalStateException("Palladium loot consumer has more than one Gson.fromJson call");
                }
                oldPoolParse = call;
            }
        }
        if (oldPoolParse == null) {
            throw new IllegalStateException("Palladium loot consumer no longer deserializes a JsonElement through Gson.fromJson");
        }

        AbstractInsnNode cast = nextMeaningful(oldPoolParse);
        if (!(cast instanceof TypeInsnNode typeCast)
                || typeCast.getOpcode() != CHECKCAST
                || !LOOT_POOL.equals(typeCast.desc)) {
            throw new IllegalStateException("Palladium loot-pool Gson parse is no longer followed by the expected LootPool cast");
        }

        MethodInsnNode addPool = null;
        for (AbstractInsnNode instruction : poolConsumer.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEVIRTUAL
                    && MODIFICATION.equals(call.owner)
                    && "addPool".equals(call.name)
                    && ("(L" + LOOT_POOL + ";)L" + MODIFICATION + ";").equals(call.desc)) {
                if (addPool != null) {
                    throw new IllegalStateException("Palladium loot consumer invokes Modification.addPool more than once");
                }
                addPool = call;
            }
        }
        if (addPool == null || nextMeaningful(typeCast) != addPool) {
            throw new IllegalStateException("Palladium loot consumer no longer passes the decoded pool directly to Modification.addPool");
        }

        // Keep the Gson receiver and Class literal on the stack, but redirect
        // only this exact LootPool deserialization to the 1.21.1 codec bridge.
        oldPoolParse.setOpcode(INVOKESTATIC);
        oldPoolParse.owner = HELPER;
        oldPoolParse.name = "decodeLootPool";
        oldPoolParse.desc = HELPER_DESC;
        oldPoolParse.itf = false;

        // NeoForge conditional loot pools can decode to an empty list. In that
        // case skip addPool entirely instead of inserting null into Palladium's list.
        AbstractInsnNode afterAddPool = nextMeaningful(addPool);
        if (afterAddPool == null || afterAddPool.getOpcode() != POP) {
            throw new IllegalStateException("Palladium loot consumer no longer discards the fluent addPool return value");
        }
        LabelNode hasPool = new LabelNode();
        LabelNode afterAdd = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new InsnNode(DUP));
        guard.add(new JumpInsnNode(IFNONNULL, hasPool));
        guard.add(new InsnNode(POP)); // decoded pool is null
        guard.add(new InsnNode(POP)); // Modification receiver
        guard.add(new JumpInsnNode(GOTO, afterAdd));
        guard.add(hasPool);
        poolConsumer.instructions.insertBefore(addPool, guard);
        poolConsumer.instructions.insert(afterAddPool, afterAdd);
        poolConsumer.maxStack = Math.max(poolConsumer.maxStack, 3);
        return 1;
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && (next instanceof LabelNode || next instanceof LineNumberNode || next instanceof FrameNode)) {
            next = next.getNext();
        }
        return next;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }
}
