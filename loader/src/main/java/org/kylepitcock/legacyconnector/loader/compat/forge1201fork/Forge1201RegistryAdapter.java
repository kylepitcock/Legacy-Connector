package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

/**
 * Rewrites the audited Forge registry call patterns to their NeoForge 1.21.1 equivalents.
 *
 * <p>This is intentionally narrow and descriptor-exact. Unknown Forge registry fields,
 * methods, and overloads are left untouched so that the API audit can continue to report
 * them rather than silently guessing a replacement.</p>
 */
public final class Forge1201RegistryAdapter {
    private static final String FORGE_REGISTRIES = "net/minecraftforge/registries/ForgeRegistries";
    private static final String FORGE_REGISTRY_OBJECT = "net/minecraftforge/registries/RegistryObject";
    private static final String FORGE_DEFERRED_REGISTER = "net/minecraftforge/registries/DeferredRegister";
    private static final String FORGE_REGISTRY = "net/minecraftforge/registries/IForgeRegistry";

    private static final String BUILT_IN_REGISTRIES = "net/minecraft/core/registries/BuiltInRegistries";
    private static final String REGISTRY = "net/minecraft/core/Registry";
    private static final String DEFAULTED_REGISTRY = "net/minecraft/core/DefaultedRegistry";
    private static final String FORGE_CONTAINER_FACTORY = "net/minecraftforge/network/IContainerFactory";
    private static final String FRIENDLY_BYTE_BUF_DESC = "Lnet/minecraft/network/FriendlyByteBuf;";
    private static final String REGISTRY_FRIENDLY_BYTE_BUF_DESC =
            "Lnet/minecraft/network/RegistryFriendlyByteBuf;";
    private static final String DEFERRED_REGISTER = "net/neoforged/neoforge/registries/DeferredRegister";
    private static final String DEFERRED_HOLDER = "net/neoforged/neoforge/registries/DeferredHolder";
    private static final String SUPPLIER = "java/util/function/Supplier";

    private static final String FORGE_REGISTRY_DESC = "L" + FORGE_REGISTRY + ";";
    private static final String REGISTRY_DESC = "L" + REGISTRY + ";";
    private static final String DEFERRED_REGISTER_DESC = "L" + FORGE_DEFERRED_REGISTER + ";";
    private static final String TARGET_DEFERRED_REGISTER_DESC = "L" + DEFERRED_REGISTER + ";";
    private static final String REGISTRY_OBJECT_DESC = "L" + FORGE_REGISTRY_OBJECT + ";";
    private static final String DEFERRED_HOLDER_DESC = "L" + DEFERRED_HOLDER + ";";
    private static final String SUPPLIER_DESC = "Ljava/util/function/Supplier;";
    private static final String RESOURCE_LOCATION_DESC = "Lnet/minecraft/resources/ResourceLocation;";
    private static final String RESOURCE_KEY_DESC = "Lnet/minecraft/resources/ResourceKey;";
    private static final String OBJECT_DESC = "Ljava/lang/Object;";
    private static final String STRING_DESC = "Ljava/lang/String;";

    private static final String DEFERRED_REGISTER_CREATE_SOURCE =
            "(" + FORGE_REGISTRY_DESC + STRING_DESC + ")" + DEFERRED_REGISTER_DESC;
    private static final String DEFERRED_REGISTER_CREATE_TARGET =
            "(" + REGISTRY_DESC + STRING_DESC + ")" + TARGET_DEFERRED_REGISTER_DESC;
    private static final String DEFERRED_REGISTER_REGISTER_SOURCE =
            "(" + STRING_DESC + SUPPLIER_DESC + ")" + REGISTRY_OBJECT_DESC;
    private static final String DEFERRED_REGISTER_REGISTER_TARGET =
            "(" + STRING_DESC + SUPPLIER_DESC + ")" + DEFERRED_HOLDER_DESC;

    private static final String REGISTRY_OBJECT_GET_DESC = "()" + OBJECT_DESC;
    private static final String REGISTRY_GET_KEY_DESC = "(" + OBJECT_DESC + ")" + RESOURCE_LOCATION_DESC;
    private static final String REGISTRY_GET_VALUE_SOURCE_DESC = "(" + RESOURCE_LOCATION_DESC + ")" + OBJECT_DESC;
    private static final String REGISTRY_GET_VALUE_TARGET_NAME = "get";
    private static final String REGISTRY_GET_RESOURCE_KEY_SOURCE_NAME = "m_6246_";
    private static final String REGISTRY_GET_RESOURCE_KEY_TARGET_NAME = "get";
    private static final String REGISTRY_GET_RESOURCE_KEY_SOURCE_DESC =
            "(" + RESOURCE_KEY_DESC + ")" + OBJECT_DESC;
    private static final String MENU_FACTORY_SAM_SOURCE_DESC =
            "(ILnet/minecraft/world/entity/player/Inventory;" + FRIENDLY_BYTE_BUF_DESC
                    + ")Lnet/minecraft/world/inventory/AbstractContainerMenu;";
    private static final String MENU_FACTORY_SAM_TARGET_DESC =
            "(ILnet/minecraft/world/entity/player/Inventory;" + REGISTRY_FRIENDLY_BYTE_BUF_DESC
                    + ")Lnet/minecraft/world/inventory/AbstractContainerMenu;";
    private static final String MENU_FACTORY_INSTANTIATED_SOURCE_PREFIX =
            "(ILnet/minecraft/world/entity/player/Inventory;" + FRIENDLY_BYTE_BUF_DESC + ")";
    private static final String MENU_FACTORY_INSTANTIATED_TARGET_PREFIX =
            "(ILnet/minecraft/world/entity/player/Inventory;" + REGISTRY_FRIENDLY_BYTE_BUF_DESC + ")";
    private static final String MENU_FACTORY_IMPLEMENTATION_DESC =
            "(ILnet/minecraft/world/entity/player/Inventory;" + FRIENDLY_BYTE_BUF_DESC + ")V";
    private static final String LAMBDA_METAFACTORY_OWNER = "java/lang/invoke/LambdaMetafactory";
    private Forge1201RegistryAdapter() {}

    public record Stats(
            int registryObjectGetCalls,
            int deferredRegisterCreateCalls,
            int deferredRegisterRegisterCalls,
            int builtInRegistryFieldReads,
            int registryLookupCalls,
            int registryResourceKeyLookupCalls,
            int menuFactoryLambdaAdaptations) {
        public static Stats empty() {
            return new Stats(0, 0, 0, 0, 0, 0, 0);
        }

        public Stats plus(Stats other) {
            if (other == null) throw new IllegalArgumentException("Other registry adapter stats are required");
            return new Stats(
                    registryObjectGetCalls + other.registryObjectGetCalls,
                    deferredRegisterCreateCalls + other.deferredRegisterCreateCalls,
                    deferredRegisterRegisterCalls + other.deferredRegisterRegisterCalls,
                    builtInRegistryFieldReads + other.builtInRegistryFieldReads,
                    registryLookupCalls + other.registryLookupCalls,
                    registryResourceKeyLookupCalls + other.registryResourceKeyLookupCalls,
                    menuFactoryLambdaAdaptations + other.menuFactoryLambdaAdaptations);
        }
    }

    public static Stats apply(ClassNode classNode) {
        if (classNode == null) throw new IllegalArgumentException("Class node is required");

        int holderGets = 0;
        int registerCreates = 0;
        int registerEntries = 0;
        int builtInFields = 0;
        int registryLookups = 0;
        int registryResourceKeyLookups = 0;
        int menuFactoryLambdaAdaptations = 0;

        for (MethodNode method : classNode.methods) {
            // Replacing an invocation requires a new ASM node because instruction opcodes are immutable.
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals(FORGE_REGISTRIES)) {
                    RegistryField replacement = registryField(field.name);
                    if (replacement != null && field.desc.equals(FORGE_REGISTRY_DESC)) {
                        field.owner = BUILT_IN_REGISTRIES;
                        field.name = replacement.name();
                        field.desc = replacement.descriptor();
                        builtInFields++;
                    }
                    continue;
                }

                if (instruction instanceof InvokeDynamicInsnNode lambda
                        && adaptMenuFactoryLambda(lambda)) {
                    menuFactoryLambdaAdaptations++;
                    continue;
                }

                if (!(instruction instanceof MethodInsnNode call)) continue;

                if (call.owner.equals(FORGE_REGISTRY_OBJECT)
                        && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.name.equals("get")
                        && call.desc.equals(REGISTRY_OBJECT_GET_DESC)) {
                    method.instructions.set(call, new MethodInsnNode(
                            Opcodes.INVOKEINTERFACE, SUPPLIER, "get", REGISTRY_OBJECT_GET_DESC, true));
                    holderGets++;
                    continue;
                }

                if (call.owner.equals(FORGE_DEFERRED_REGISTER)
                        && call.getOpcode() == Opcodes.INVOKESTATIC
                        && call.name.equals("create")
                        && call.desc.equals(DEFERRED_REGISTER_CREATE_SOURCE)) {
                    call.desc = DEFERRED_REGISTER_CREATE_TARGET;
                    registerCreates++;
                    continue;
                }

                if (call.owner.equals(FORGE_DEFERRED_REGISTER)
                        && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.name.equals("register")
                        && call.desc.equals(DEFERRED_REGISTER_REGISTER_SOURCE)) {
                    call.desc = DEFERRED_REGISTER_REGISTER_TARGET;
                    registerEntries++;
                    continue;
                }

                if (call.owner.equals(FORGE_REGISTRY)
                        && call.getOpcode() == Opcodes.INVOKEINTERFACE
                        && call.name.equals("getKey")
                        && call.desc.equals(REGISTRY_GET_KEY_DESC)) {
                    call.owner = REGISTRY;
                    call.itf = true;
                    registryLookups++;
                    continue;
                }

                if (call.owner.equals(FORGE_REGISTRY)
                        && call.getOpcode() == Opcodes.INVOKEINTERFACE
                        && call.name.equals("getValue")
                        && call.desc.equals(REGISTRY_GET_VALUE_SOURCE_DESC)) {
                    call.owner = REGISTRY;
                    call.name = REGISTRY_GET_VALUE_TARGET_NAME;
                    call.itf = true;
                    registryLookups++;
                    continue;
                }

                // This 1.20.1 Registry#get(ResourceKey) call carries its SRG method name.
                // NeoForge 21.1.258 exposes the same exact descriptor as get(ResourceKey).
                if (call.owner.equals(REGISTRY)
                        && call.getOpcode() == Opcodes.INVOKEINTERFACE
                        && call.name.equals(REGISTRY_GET_RESOURCE_KEY_SOURCE_NAME)
                        && call.desc.equals(REGISTRY_GET_RESOURCE_KEY_SOURCE_DESC)) {
                    call.name = REGISTRY_GET_RESOURCE_KEY_TARGET_NAME;
                    registryResourceKeyLookups++;
                }
            }
        }

        return new Stats(holderGets, registerCreates, registerEntries, builtInFields,
                registryLookups, registryResourceKeyLookups, menuFactoryLambdaAdaptations);
    }

    /**
     * Migrates the audited Forge IContainerFactory lambda's functional signature to the
     * NeoForge 1.21.1 RegistryFriendlyByteBuf contract. Its implementation handle deliberately
     * keeps FriendlyByteBuf: RegistryFriendlyByteBuf extends FriendlyByteBuf, so the target
     * argument can be passed to the existing menu constructor without changing that method.
     */
    private static boolean adaptMenuFactoryLambda(InvokeDynamicInsnNode lambda) {
        if (!lambda.name.equals("create")
                || !lambda.desc.equals("()L" + FORGE_CONTAINER_FACTORY + ";")
                || !lambda.bsm.getOwner().equals(LAMBDA_METAFACTORY_OWNER)
                || !lambda.bsm.getName().equals("metafactory")
                || lambda.bsmArgs.length != 3
                || !(lambda.bsmArgs[0] instanceof Type samMethodType)
                || samMethodType.getSort() != Type.METHOD
                || !samMethodType.getDescriptor().equals(MENU_FACTORY_SAM_SOURCE_DESC)
                || !(lambda.bsmArgs[1] instanceof org.objectweb.asm.Handle implementation)
                || implementation.getTag() != Opcodes.H_NEWINVOKESPECIAL
                || !implementation.getName().equals("<init>")
                || !implementation.getDesc().equals(MENU_FACTORY_IMPLEMENTATION_DESC)
                || !(lambda.bsmArgs[2] instanceof Type instantiatedMethodType)
                || instantiatedMethodType.getSort() != Type.METHOD
                || !instantiatedMethodType.getDescriptor().startsWith(MENU_FACTORY_INSTANTIATED_SOURCE_PREFIX)) {
            return false;
        }

        String targetInstantiatedDescriptor = MENU_FACTORY_INSTANTIATED_TARGET_PREFIX
                + instantiatedMethodType.getReturnType().getDescriptor();
        lambda.bsmArgs[0] = Type.getMethodType(MENU_FACTORY_SAM_TARGET_DESC);
        lambda.bsmArgs[2] = Type.getMethodType(targetInstantiatedDescriptor);
        return true;
    }

    private static RegistryField registryField(String sourceName) {
        return switch (sourceName) {
            case "ITEMS" -> new RegistryField("ITEM", "L" + DEFAULTED_REGISTRY + ";");
            case "BLOCKS" -> new RegistryField("BLOCK", "L" + DEFAULTED_REGISTRY + ";");
            case "SOUND_EVENTS" -> new RegistryField("SOUND_EVENT", REGISTRY_DESC);
            case "BLOCK_ENTITY_TYPES" -> new RegistryField("BLOCK_ENTITY_TYPE", REGISTRY_DESC);
            case "ENTITY_TYPES" -> new RegistryField("ENTITY_TYPE", "L" + DEFAULTED_REGISTRY + ";");
            case "MENU_TYPES" -> new RegistryField("MENU", REGISTRY_DESC);
            default -> null;
        };
    }

    private record RegistryField(String name, String descriptor) {}
}
