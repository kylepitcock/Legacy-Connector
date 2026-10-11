package org.kylepitcock.mccompat.loader;

import java.util.List;

/**
 * A family of legacy Forge versions that share one shim API.
 *
 * <p>Every legacy mod is assigned exactly one family. All references the mod makes to Minecraft, Forge
 * or FML classes are relocated under {@link #shimPrefix()}, where the runtime half of MC Compatibility
 * provides re-implementations of the legacy API on top of NeoForge 1.21.1. Relocation (instead of
 * reusing the original package names) is required because the Java module system forbids two modules
 * from sharing a package, and packages such as {@code net.minecraft.client} exist in both worlds.</p>
 *
 * <p>Minor versions are folded into the nearest family: SRG member ids are stable across versions, so a
 * 1.10.2 mod links against the 1.12.2 shim for everything that did not change in between.</p>
 */
public enum LegacyVersion {
    /** Forge 1.6.4 - 1.7.10: {@code cpw.mods.fml}, numeric block ids + metadata, IIcon rendering. */
    V1_7_10("1.7.10", "mccompat/v1_7_10/", "mccompat_synth/v1_7_10/"),
    /** Forge 1.8 - 1.12.2: {@code net.minecraftforge.fml}, IBlockState, JSON models, registry events. */
    V1_12_2("1.12.2", "mccompat/v1_12_2/", "mccompat_synth/v1_12_2/");

    /**
     * Package roots (internal names) that are owned by Minecraft/Forge in legacy versions and must be
     * redirected into the shim. Anything else a mod references is either its own code, a library or the JDK.
     */
    public static final List<String> RELOCATED_ROOTS = List.of(
            "net/minecraft/",
            "net/minecraftforge/",
            "cpw/mods/fml/",
            // LWJGL 2 classes that have no LWJGL 3 equivalent. GL11 is relocated so the shim can emulate the
            // fixed-function pipeline that the 1.21 core profile no longer provides.
            "org/lwjgl/input/",
            "org/lwjgl/util/",
            "org/lwjgl/opengl/Display",
            "org/lwjgl/opengl/DisplayMode",
            "org/lwjgl/opengl/GL11",
            "org/lwjgl/opengl/GL15",
            "org/lwjgl/opengl/PixelFormat",
            "org/lwjgl/opengl/ContextCapabilities",
            "org/lwjgl/opengl/GLContext",
            "org/lwjgl/LWJGLException",
            "org/lwjgl/Sys");

    private final String id;
    private final String shimPrefix;
    private final String synthPrefix;

    LegacyVersion(String id, String shimPrefix, String synthPrefix) {
        this.id = id;
        this.shimPrefix = shimPrefix;
        this.synthPrefix = synthPrefix;
    }

    /** The canonical Minecraft version this family's shim emulates. Also names the mapping data folder. */
    public String id() {
        return id;
    }

    /** Internal-name prefix all relocated classes are moved under, e.g. {@code mccompat/v1_7_10/}. */
    public String shimPrefix() {
        return shimPrefix;
    }

    /**
     * Where legacy API classes the hand-written shim does not provide are synthesized (see
     * {@code synth.SynthJarBuilder}). A separate root because JPMS forbids splitting a package between the
     * runtime mod and the generated jar.
     */
    public String synthPrefix() {
        return synthPrefix;
    }

    /** API packages of bridged legacy libraries (see {@link LibraryBridges}); relocated into the shim like Forge. */
    private static volatile List<String> bridgedRoots = List.of();

    public static void setBridgedRoots(List<String> roots) {
        bridgedRoots = List.copyOf(roots);
    }

    public static boolean isRelocatedRoot(String internalName) {
        for (String root : bridgedRoots) if (internalName.startsWith(root)) return true;
        for (String root : RELOCATED_ROOTS) {
            if (internalName.startsWith(root)) {
                // Exact-class roots (no trailing slash) must not swallow e.g. "org/lwjgl/opengl/GL11Ext".
                if (root.endsWith("/") || internalName.length() == root.length() || internalName.charAt(root.length()) == '$') {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Picks the family for a Minecraft version string taken from mcmod.info or {@code @Mod} attributes.
     * Returns null when the string does not identify a supported version.
     */
    public static LegacyVersion fromMinecraftVersion(String mcVersion) {
        if (mcVersion == null) return null;
        String v = mcVersion.replaceAll("[\\[\\]()\\s]", "");
        if (v.startsWith("1.6") || v.startsWith("1.7")) return V1_7_10;
        if (v.startsWith("1.8") || v.startsWith("1.9") || v.startsWith("1.10") || v.startsWith("1.11") || v.startsWith("1.12")) {
            return V1_12_2;
        }
        return null;
    }
}
