package org.kylepitcock.legacyconnector.loader;

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
    /**
     * Forge 1.5.2 (7.8.x): the 1.6.4 model one step back. A forward version like {@link #V1_6_4}: it links against the 1.7.10 shim, shares the numeric-id facade
     * classes of 1.6.4 (its {@link #shimPrefix()} is the 1.6.4 one), takes the 1.6.4 forward tables as a fallback ({@link #forwardTableIds()}) and has its own API
     * tables ({@code legacy/1.5.2}) and synthesized classes. Differences from 1.6.4: {@code @PreInit/@Init/@PostInit} only (no {@code @EventHandler}),
     * {@code EntityLiving} is the base of every living entity (no {@code EntityLivingBase}), no attributes, textures are string paths
     * ({@code RenderEngine.bindTexture(String)}), no {@code ResourceLocation}.
     */
    V1_5_2("1.5.2", "legacyconnector/v1_6_4/", "legacyconnector_synth/v1_5_2/"),
    /**
     * Forge 1.6.4: {@code cpw.mods.fml}, {@code Block.blocksList[id]}, {@code Icon}. A forward version: its jars are linked against the 1.7.10 shim
     * ({@link #forward()}), after the class renames of {@code legacyconnector/forward/1.6.4}; the classes of its own prefixes are the numeric-id facade
     * and what the synthesizer generates from the 1.6.4 API data.
     */
    V1_6_4("1.6.4", "legacyconnector/v1_6_4/", "legacyconnector_synth/v1_6_4/"),
    /** Forge 1.7.2 - 1.7.10: {@code cpw.mods.fml}, block names + metadata, IIcon rendering. */
    V1_7_10("1.7.10", "legacyconnector/v1_7_10/", "legacyconnector_synth/v1_7_10/"),
    /** Forge 1.8 - 1.12.2: {@code net.minecraftforge.fml}, IBlockState, JSON models, registry events. */
    V1_12_2("1.12.2", "legacyconnector/v1_12_2/", "legacyconnector_synth/v1_12_2/"),
    /**
     * Forge 1.20.1: the same class model as 1.21.1, so nothing is relocated. The jar is remapped in place (SRG to Mojang names, Forge to NeoForge
     * packages) and runs as a native NeoForge mod; {@link #shimPrefix()} only holds the small facade of Forge API that 1.21.1 no longer has
     * ({@code FMLJavaModLoadingContext}, {@code DistExecutor}, ...). See {@code Forge1201Transformer}.
     */
    V1_20_1("1.20.1", "legacyconnector/v1_20_1/", "legacyconnector_synth/v1_20_1/");

    /**
     * Package roots (internal names) that are owned by Minecraft/Forge in legacy versions and must be
     * redirected into the shim. Anything else a mod references is either its own code, a library or the JDK.
     */
    public static final List<String> RELOCATED_ROOTS = List.of(
            "net/minecraft/",
            "net/minecraftforge/",
            "cpw/mods/fml/",
            // Realms' copy of the chat formatting codes, which 1.8-1.12 mods used (the shim has it for 1.12.2)
            "com/mojang/realmsclient/",
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
            "org/lwjgl/opengl/OpenGLException",
            "org/lwjgl/LWJGLException",
            // LWJGL 2's OpenAL context holder and exception (LWJGL 3 has neither); AL10 and the rest are LWJGL 3's own classes
            "org/lwjgl/openal/AL",
            "org/lwjgl/openal/OpenALException",
            "org/lwjgl/Sys",
            // The sound library 1.7.10 bundled (mods configure it directly); the modern game has its own engine.
            "paulscode/sound/");

    private final String id;
    private final String shimPrefix;
    private final String synthPrefix;

    LegacyVersion(String id, String shimPrefix, String synthPrefix) {
        this.id = id;
        this.shimPrefix = shimPrefix;
        this.synthPrefix = synthPrefix;
    }

    /** The family whose shim this version's jars link against (1.5.2 and 1.6.4 use the 1.7.10 one), or null when it has its own shim. */
    public LegacyVersion forward() {
        return this == V1_5_2 || this == V1_6_4 ? V1_7_10 : null;
    }

    /** True for the versions with numeric block and item ids (1.5.2 and 1.6.4): they share the id facade and the notch-name handling. */
    public boolean numericIds() {
        return this == V1_5_2 || this == V1_6_4;
    }

    /**
     * Ids of the forward table folders ({@code legacyconnector/forward/<id>/}) that apply to this version, oldest first: later entries win. 1.5.2 starts from the
     * 1.6.4 tables (same renames to the 1.7.10 shim) and overrides them with its own.
     */
    public List<String> forwardTableIds() {
        return this == V1_5_2 ? List.of(V1_6_4.id, id) : List.of(id);
    }

    /** The family of the runtime platform and hand-written shim that runs this version's mods. */
    public LegacyVersion family() {
        LegacyVersion f = forward();
        return f != null ? f : this;
    }

    /** Internal-name prefix of the hand-written shim classes this version's jars link against ({@link #shimPrefix()} unless forwarded). */
    public String linkPrefix() {
        return family().shimPrefix;
    }

    /** The canonical Minecraft version this family's shim emulates. Also names the mapping data folder. */
    public String id() {
        return id;
    }

    /** Internal-name prefix all relocated classes are moved under, e.g. {@code legacyconnector/v1_7_10/}. */
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
        if (v.startsWith("1.5")) return V1_5_2;
        if (v.startsWith("1.6")) return V1_6_4;
        if (v.startsWith("1.7")) return V1_7_10;
        if (v.startsWith("1.8") || v.startsWith("1.9") || v.startsWith("1.10") || v.startsWith("1.11") || v.startsWith("1.12")) {
            return V1_12_2;
        }
        return null;
    }
}
