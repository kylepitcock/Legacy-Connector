package org.kylepitcock.legacyconnector.loader.transform;

/**
 * Authlib's 1.21.1 Property is a record. The 1.20.1 class exposed the same
 * String components through JavaBean getters; remap those names at the
 * bytecode mapper boundary so ordinary calls and method handles are both
 * translated by ASM's ClassRemapper.
 */
final class AuthlibProperty1201 {
    static final String OWNER = "com/mojang/authlib/properties/Property";
    private static final String STRING_GETTER = "()Ljava/lang/String;";

    private AuthlibProperty1201() {
    }

    static String mapMethodName(String owner, String name, String descriptor) {
        if (!OWNER.equals(owner) || !STRING_GETTER.equals(descriptor)) {
            return name;
        }
        return switch (name) {
            case "getName" -> "name";
            case "getValue" -> "value";
            case "getSignature" -> "signature";
            default -> name;
        };
    }
}
