package org.kylepitcock.legacyconnector.loader.transform;

/**
 * Read-only access to the 1.20.1 -> 1.21.1 API migration catalog for static compatibility audits.
 *
 * <p>This facade keeps the package-private transformation tables encapsulated. It does not
 * transform bytecode or claim that a recorded migration is behaviorally equivalent.</p>
 */
public final class Mc1201ApiMappings {
    private Mc1201ApiMappings() {}

    public record StaticFieldMapping(
            String targetName,
            String targetDescriptor,
            String postAdapter) {}

    /** Returns the mapped 1.21.1 class name, or the source name when no class move is recorded. */
    public static String targetClassName(String sourceInternalName) {
        if (sourceInternalName == null || sourceInternalName.isBlank()) {
            throw new IllegalArgumentException("A source class name is required");
        }
        return Mc1201Tables.get().classes.getOrDefault(sourceInternalName, sourceInternalName);
    }

    /** Returns the mapped target member name using the same owner/name/descriptor keys as the remapper. */
    public static String targetMemberName(String sourceOwner, String sourceName, String sourceDescriptor) {
        if (sourceOwner == null || sourceName == null || sourceDescriptor == null) {
            throw new IllegalArgumentException("A source owner, member name, and descriptor are required");
        }
        return Mc1201Tables.get().renamedMember(sourceOwner, sourceName, sourceDescriptor);
    }

    /**
     * Returns a known static-field shape migration. These rows are consumed by the bytecode
     * migrator for GETSTATIC instructions only; callers must verify the observed opcode before
     * treating the mapping as applicable.
     */
    public static StaticFieldMapping staticFieldMapping(
            String sourceOwner, String sourceName, String sourceDescriptor) {
        if (sourceOwner == null || sourceName == null || sourceDescriptor == null) {
            throw new IllegalArgumentException("A source owner, field name, and descriptor are required");
        }
        Mc1201Tables.Field field = Mc1201Tables.get().fields.get(
                sourceOwner + "." + sourceName + ":" + sourceDescriptor);
        if (field == null) return null;
        String targetName = field.newName().equals("-") ? sourceName : field.newName();
        String targetDescriptor = field.newDesc().equals("-") ? sourceDescriptor : field.newDesc();
        return new StaticFieldMapping(targetName, targetDescriptor, field.post());
    }
}
