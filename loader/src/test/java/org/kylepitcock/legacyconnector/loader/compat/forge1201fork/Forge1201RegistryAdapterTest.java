package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201MappingCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class Forge1201RegistryAdapterTest {
    private static final String FORGE_REGISTRY = "net/minecraftforge/registries/IForgeRegistry";
    private static final String FORGE_REGISTRY_DESC = "L" + FORGE_REGISTRY + ";";
    private static final String RESOURCE_LOCATION_DESC = "Lnet/minecraft/resources/ResourceLocation;";
    private static final String RESOURCE_KEY_DESC = "Lnet/minecraft/resources/ResourceKey;";
    private static final String OBJECT_DESC = "Ljava/lang/Object;";
    private static final String STRING_DESC = "Ljava/lang/String;";
    private static final String SUPPLIER_DESC = "Ljava/util/function/Supplier;";
    private static final String FORGE_DEFERRED_REGISTER =
            "net/minecraftforge/registries/DeferredRegister";
    private static final String FORGE_REGISTRY_OBJECT =
            "net/minecraftforge/registries/RegistryObject";

    @Test
    void rewritesAuditedHolderRegistrationAndVanillaRegistryCallPatterns() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/RegistryFixture";
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "exercise", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "ITEMS", FORGE_REGISTRY_DESC));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "BLOCKS", FORGE_REGISTRY_DESC));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "SOUND_EVENTS", FORGE_REGISTRY_DESC));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "BLOCK_ENTITY_TYPES", FORGE_REGISTRY_DESC));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "ENTITY_TYPES", FORGE_REGISTRY_DESC));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "MENU_TYPES", FORGE_REGISTRY_DESC));

        String createSource = "(" + FORGE_REGISTRY_DESC + STRING_DESC + ")L"
                + FORGE_DEFERRED_REGISTER + ";";
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                FORGE_DEFERRED_REGISTER, "create", createSource, false));

        String registerSource = "(" + STRING_DESC + SUPPLIER_DESC + ")L"
                + FORGE_REGISTRY_OBJECT + ";";
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                FORGE_DEFERRED_REGISTER, "register", registerSource, false));

        String holderGet = "()" + OBJECT_DESC;
        for (int i = 0; i < 3; i++) {
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                    FORGE_REGISTRY_OBJECT, "get", holderGet, false));
        }

        String getKey = "(" + OBJECT_DESC + ")" + RESOURCE_LOCATION_DESC;
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                FORGE_REGISTRY, "getKey", getKey, true));
        String getValue = "(" + RESOURCE_LOCATION_DESC + ")" + OBJECT_DESC;
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                FORGE_REGISTRY, "getValue", getValue, true));
        String getByResourceKey = "(" + RESOURCE_KEY_DESC + ")" + OBJECT_DESC;
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "net/minecraft/core/Registry", "m_6246_", getByResourceKey, true));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 0;
        node.methods = new ArrayList<>(List.of(method));

        Forge1201RegistryAdapter.Stats stats = Forge1201RegistryAdapter.apply(node);

        assertEquals(3, stats.registryObjectGetCalls());
        assertEquals(1, stats.deferredRegisterCreateCalls());
        assertEquals(1, stats.deferredRegisterRegisterCalls());
        assertEquals(6, stats.builtInRegistryFieldReads());
        assertEquals(2, stats.registryLookupCalls());
        assertEquals(1, stats.registryResourceKeyLookupCalls());

        List<FieldInsnNode> fields = findFields(method);
        assertEquals(List.of("ITEM", "BLOCK", "SOUND_EVENT", "BLOCK_ENTITY_TYPE", "ENTITY_TYPE", "MENU"),
                fields.stream().map(field -> field.name).toList());
        assertEquals(List.of(
                "Lnet/minecraft/core/DefaultedRegistry;",
                "Lnet/minecraft/core/DefaultedRegistry;",
                "Lnet/minecraft/core/Registry;",
                "Lnet/minecraft/core/Registry;",
                "Lnet/minecraft/core/DefaultedRegistry;",
                "Lnet/minecraft/core/Registry;"),
                fields.stream().map(field -> field.desc).toList());
        fields.forEach(field -> assertEquals(
                "net/minecraft/core/registries/BuiltInRegistries", field.owner));

        List<MethodInsnNode> calls = findCalls(method);
        MethodInsnNode create = calls.stream().filter(call -> call.name.equals("create"))
                .findFirst().orElseThrow();
        assertEquals(Opcodes.INVOKESTATIC, create.getOpcode());
        assertEquals("net/minecraftforge/registries/DeferredRegister", create.owner);
        assertEquals("(Lnet/minecraft/core/Registry;Ljava/lang/String;)"
                + "Lnet/neoforged/neoforge/registries/DeferredRegister;", create.desc);

        MethodInsnNode register = calls.stream().filter(call -> call.name.equals("register"))
                .findFirst().orElseThrow();
        assertEquals(Opcodes.INVOKEVIRTUAL, register.getOpcode());
        assertEquals("(Ljava/lang/String;Ljava/util/function/Supplier;)"
                + "Lnet/neoforged/neoforge/registries/DeferredHolder;", register.desc);

        List<MethodInsnNode> gets = calls.stream().filter(call ->
                call.owner.equals("java/util/function/Supplier") && call.name.equals("get")).toList();
        assertEquals(3, gets.size());
        gets.forEach(call -> {
            assertEquals(Opcodes.INVOKEINTERFACE, call.getOpcode());
            assertTrue(call.itf);
            assertEquals("()Ljava/lang/Object;", call.desc);
        });

        MethodInsnNode getKeyCall = calls.stream().filter(call -> call.name.equals("getKey"))
                .findFirst().orElseThrow();
        assertEquals("net/minecraft/core/Registry", getKeyCall.owner);
        assertEquals(Opcodes.INVOKEINTERFACE, getKeyCall.getOpcode());
        assertTrue(getKeyCall.itf);

        MethodInsnNode getValueCall = calls.stream().filter(call -> call.name.equals("get"))
                .filter(call -> call.owner.equals("net/minecraft/core/Registry"))
                .filter(call -> call.desc.equals("(" + RESOURCE_LOCATION_DESC + ")Ljava/lang/Object;"))
                .findFirst().orElseThrow();
        assertEquals(Opcodes.INVOKEINTERFACE, getValueCall.getOpcode());

        MethodInsnNode getResourceKeyCall = calls.stream()
                .filter(call -> call.owner.equals("net/minecraft/core/Registry"))
                .filter(call -> call.desc.equals("(" + RESOURCE_KEY_DESC + ")Ljava/lang/Object;"))
                .findFirst().orElseThrow();
        assertEquals("get", getResourceKeyCall.name);
        assertEquals(Opcodes.INVOKEINTERFACE, getResourceKeyCall.getOpcode());
        assertTrue(getResourceKeyCall.itf);
    }

    @Test
    void adaptsMenuRegistryAndMenuFactoryLambdaToNeoForge21() {
        String oldContainerFactory = "net/minecraftforge/network/IContainerFactory";
        String targetContainerFactory = "net/neoforged/neoforge/network/IContainerFactory";
        String oldMenuTypeExtension = "net/minecraftforge/common/extensions/IForgeMenuType";
        String targetMenuTypeExtension =
                "net/neoforged/neoforge/common/extensions/IMenuTypeExtension";
        String oldFriendlyBuffer = "Lnet/minecraft/network/FriendlyByteBuf;";
        String targetFriendlyBuffer = "Lnet/minecraft/network/RegistryFriendlyByteBuf;";
        String inventory = "Lnet/minecraft/world/entity/player/Inventory;";
        String abstractMenu = "Lnet/minecraft/world/inventory/AbstractContainerMenu;";
        String menuImplementation = "Lcom/scout/symbiote/menu/ArmSlotsMenu;";

        String sourceSamDescriptor = "(I" + inventory + oldFriendlyBuffer + ")" + abstractMenu;
        String targetSamDescriptor = "(I" + inventory + targetFriendlyBuffer + ")" + abstractMenu;
        String sourceInstantiatedDescriptor =
                "(I" + inventory + oldFriendlyBuffer + ")" + menuImplementation;
        String targetInstantiatedDescriptor =
                "(I" + inventory + targetFriendlyBuffer + ")" + menuImplementation;
        String implementationDescriptor = "(I" + inventory + oldFriendlyBuffer + ")V";

        ClassNode source = new ClassNode();
        source.version = Opcodes.V17;
        source.access = Opcodes.ACC_PUBLIC;
        source.name = "sample/MenuRegistryFixture";
        source.superName = "java/lang/Object";

        MethodNode method = new MethodNode(Opcodes.ASM9,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "registerMenu", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraftforge/registries/ForgeRegistries", "MENU_TYPES", FORGE_REGISTRY_DESC));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        method.instructions.add(new org.objectweb.asm.tree.InvokeDynamicInsnNode(
                "create",
                "()L" + oldContainerFactory + ";",
                new org.objectweb.asm.Handle(Opcodes.H_INVOKESTATIC,
                        "java/lang/invoke/LambdaMetafactory", "metafactory",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                                + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                                + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                                + "Ljava/lang/invoke/CallSite;",
                        false),
                org.objectweb.asm.Type.getMethodType(sourceSamDescriptor),
                new org.objectweb.asm.Handle(Opcodes.H_NEWINVOKESPECIAL,
                        "com/scout/symbiote/menu/ArmSlotsMenu", "<init>",
                        implementationDescriptor, false),
                org.objectweb.asm.Type.getMethodType(sourceInstantiatedDescriptor)));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                oldMenuTypeExtension, "create",
                "(L" + oldContainerFactory + ";)Lnet/minecraft/world/inventory/MenuType;",
                true));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        method.maxStack = 1;
        method.maxLocals = 0;
        source.methods.add(method);

        Forge1201RegistryAdapter.Stats stats = Forge1201RegistryAdapter.apply(source);
        assertEquals(1, stats.builtInRegistryFieldReads());
        assertEquals(1, stats.menuFactoryLambdaAdaptations());

        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        source.accept(new ClassRemapper(writer, new Remapper() {
            @Override
            public String map(String internalName) {
                var decision = Forge1201MappingCatalog.classify(internalName);
                return decision.kind() == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT
                        || decision.kind() == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                        ? decision.targetInternalName() : internalName;
            }
        }));

        ClassNode target = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(target, 0);
        FieldInsnNode targetRegistry = findFields(target.methods.get(0)).get(0);
        assertEquals("net/minecraft/core/registries/BuiltInRegistries", targetRegistry.owner);
        assertEquals("MENU", targetRegistry.name);
        assertEquals("Lnet/minecraft/core/Registry;", targetRegistry.desc);

        List<MethodInsnNode> targetCalls = findCalls(target.methods.get(0));
        MethodInsnNode targetFactoryCall = targetCalls.stream()
                .filter(call -> call.name.equals("create"))
                .findFirst().orElseThrow();
        assertEquals(targetMenuTypeExtension, targetFactoryCall.owner);
        assertEquals("(L" + targetContainerFactory + ";)Lnet/minecraft/world/inventory/MenuType;",
                targetFactoryCall.desc);
        assertEquals(Opcodes.INVOKESTATIC, targetFactoryCall.getOpcode());
        assertTrue(targetFactoryCall.itf);

        org.objectweb.asm.tree.InvokeDynamicInsnNode targetLambda = null;
        for (var instruction : target.methods.get(0).instructions) {
            if (instruction instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode dynamic) {
                targetLambda = dynamic;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(targetLambda);
        assertEquals("()L" + targetContainerFactory + ";", targetLambda.desc);
        assertEquals(targetSamDescriptor, ((org.objectweb.asm.Type) targetLambda.bsmArgs[0]).getDescriptor());
        assertEquals(targetInstantiatedDescriptor,
                ((org.objectweb.asm.Type) targetLambda.bsmArgs[2]).getDescriptor());
        org.objectweb.asm.Handle preservedConstructor =
                (org.objectweb.asm.Handle) targetLambda.bsmArgs[1];
        assertEquals("com/scout/symbiote/menu/ArmSlotsMenu", preservedConstructor.getOwner());
        assertEquals("<init>", preservedConstructor.getName());
        assertEquals(implementationDescriptor, preservedConstructor.getDesc());
    }

    @Test
    void remapsRegistryObjectFieldAndMethodGenericSignaturesToSupplier() {
        String registryObject = "net/minecraftforge/registries/RegistryObject";
        String supplier = "java/util/function/Supplier";
        String sourceDescriptor = "L" + registryObject + ";";
        String sourceSignature = "L" + registryObject + "<Lsample/Item;>;";
        String targetSignature = "L" + supplier + "<Lsample/Item;>;";

        ClassNode source = new ClassNode();
        source.version = Opcodes.V17;
        source.access = Opcodes.ACC_PUBLIC;
        source.name = "sample/HolderSignatureFixture";
        source.superName = "java/lang/Object";
        source.fields.add(new FieldNode(
                Opcodes.ACC_PUBLIC, "ITEM", sourceDescriptor, sourceSignature, null));

        MethodNode accessor = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "item", "()" + sourceDescriptor, "()" + sourceSignature, null);
        accessor.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ACONST_NULL));
        accessor.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ARETURN));
        accessor.maxStack = 1;
        accessor.maxLocals = 0;
        source.methods.add(accessor);

        MethodNode consumer = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "consume", "(" + sourceDescriptor + ")V",
                "(" + sourceSignature + ")V", null);
        consumer.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        consumer.maxStack = 0;
        consumer.maxLocals = 1;
        source.methods.add(consumer);

        ClassWriter writer = new ClassWriter(0);
        source.accept(new ClassRemapper(writer, new Remapper() {
            @Override
            public String map(String internalName) {
                var decision = Forge1201MappingCatalog.classify(internalName);
                return decision.kind() == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT
                        || decision.kind() == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                        ? decision.targetInternalName() : internalName;
            }
        }));

        ClassNode target = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(target, 0);

        assertEquals("L" + supplier + ";", target.fields.get(0).desc);
        assertEquals(targetSignature, target.fields.get(0).signature);
        MethodNode mappedAccessor = target.methods.stream()
                .filter(method -> method.name.equals("item"))
                .findFirst().orElseThrow();
        assertEquals("()L" + supplier + ";", mappedAccessor.desc);
        assertEquals("()" + targetSignature, mappedAccessor.signature);

        MethodNode mappedConsumer = target.methods.stream()
                .filter(method -> method.name.equals("consume"))
                .findFirst().orElseThrow();
        assertEquals("(L" + supplier + ";)V", mappedConsumer.desc);
        assertEquals("(" + targetSignature + ")V", mappedConsumer.signature);
    }

    @Test
    void preservesResourceKeyBasedDeferredRegisterCreateOverload() {
        String sourceOwner = "net/minecraftforge/registries/DeferredRegister";
        String targetOwner = "net/neoforged/neoforge/registries/DeferredRegister";
        String resourceKeyDescriptor = "(Lnet/minecraft/resources/ResourceKey;Ljava/lang/String;)L"
                + sourceOwner + ";";

        ClassNode source = new ClassNode();
        source.version = Opcodes.V17;
        source.access = Opcodes.ACC_PUBLIC;
        source.name = "sample/ResourceKeyRegistryFixture";
        source.superName = "java/lang/Object";
        MethodNode method = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "create", "()V", null, null);
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("symbiote"));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, sourceOwner, "create", resourceKeyDescriptor, false));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 0;
        source.methods.add(method);

        Forge1201RegistryAdapter.apply(source);
        MethodInsnNode untouched = findCalls(method).get(0);
        assertEquals(sourceOwner, untouched.owner);
        assertEquals(resourceKeyDescriptor, untouched.desc);

        ClassWriter writer = new ClassWriter(0);
        source.accept(new ClassRemapper(writer, new Remapper() {
            @Override
            public String map(String internalName) {
                var decision = Forge1201MappingCatalog.classify(internalName);
                return decision.kind() == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                        ? decision.targetInternalName() : internalName;
            }
        }));
        ClassNode target = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(target, 0);
        MethodInsnNode mapped = findCalls(target.methods.get(0)).get(0);

        assertEquals(targetOwner, mapped.owner);
        assertEquals("(Lnet/minecraft/resources/ResourceKey;Ljava/lang/String;)L" + targetOwner + ";",
                mapped.desc);
    }

    private static List<FieldInsnNode> findFields(MethodNode method) {
        List<FieldInsnNode> result = new ArrayList<>();
        for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field) result.add(field);
        }
        return result;
    }

    private static List<MethodInsnNode> findCalls(MethodNode method) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) result.add(call);
        }
        return result;
    }
}
