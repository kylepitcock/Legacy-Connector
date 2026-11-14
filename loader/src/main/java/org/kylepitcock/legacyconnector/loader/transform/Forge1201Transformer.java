package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyJarInspector;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.Provenance;
import org.kylepitcock.legacyconnector.loader.SideBySide;
import org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.resource.DataConverter1201;
import org.kylepitcock.legacyconnector.loader.resource.FunctionConverter1201;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Forge 1.20.1 to NeoForge 1.21.1, in place. Unlike 1.7.10 and 1.12.2 nothing is relocated into a shim: the mod keeps calling the real
 * Minecraft classes. What changes:
 * <ul>
 *   <li>SRG ids ({@code m_46859_}, {@code f_46441_}) become Mojang names ({@link SrgMojangTable}), also in reflection strings;</li>
 *   <li>Forge classes become NeoForge's (tables in {@code legacyconnector/forward/1.20.1/*.tsv}, then the package moves); the few Forge API classes
 *       1.21.1 no longer has are served by a facade ({@code legacyconnector/v1_20_1/...} in the shim index) and take precedence;</li>
 *   <li>{@code mods.toml} becomes {@code neoforge.mods.toml}, the access transformer and mixin configs are made Mojang-named.</li>
 * </ul>
 * The result is a plain NeoForge mod that FML loads and constructs itself; no legacy lifecycle runs.
 */
public final class Forge1201Transformer {
    private static final Pattern SRG = Pattern.compile("^[mf]_\\d+_$");
    // KubeJS 7 moved ConsoleJS from the KubeJS 6 util package into script.
    // Palladium 4.5.x was compiled against the old owner; its ConsoleJS method
    // names and descriptors remain compatible after this owner/type relocation.
    private static final String KUBEJS6_CONSOLE = "dev/latvian/mods/kubejs/util/ConsoleJS";
    private static final String KUBEJS7_CONSOLE = "dev/latvian/mods/kubejs/script/ConsoleJS";

    private static final String[][] PACKAGE_MOVES = {
            {"net/minecraftforge/fml/", "net/neoforged/fml/"},
            {"net/minecraftforge/eventbus/", "net/neoforged/bus/"},
            {"net/minecraftforge/api/", "net/neoforged/api/"},
            {"net/minecraftforge/forgespi/", "net/neoforged/neoforgespi/"},
            {"net/minecraftforge/", "net/neoforged/neoforge/"},
    };

    private final ShimIndex shim;
    private final Map<String, String> srg;
    private final Map<String, String> classRenames;

    private static final LegacyVersion VER = LegacyVersion.V1_20_1;
    /** Isolated library copies (side-by-side libraries): their packages are relocated, their mod ids replaced. */
    private SideBySide plan = SideBySide.current();
    /** Classes (original internal names) and lower-case mod ids of the jar being translated. */
    private Set<String> ownClasses = Set.of();
    private Set<String> ownIds = Set.of();

    public Forge1201Transformer sideBySide(SideBySide plan) {
        this.plan = plan == null ? SideBySide.NONE : plan;
        return this;
    }

    public Forge1201Transformer(ShimIndex shim) {
        this.shim = shim;
        this.srg = SrgMojangTable.load();
        this.classRenames = loadRenames();
    }

    private static volatile Map<String, String> renamesCache;

    private static synchronized Map<String, String> loadRenames() {
        if (renamesCache != null) return renamesCache;
        Map<String, String> map = new HashMap<>();
        for (String f : List.of("forge-event-renames.tsv", "forge-fml-renames.tsv", "forge-curated-renames.tsv")) {
            try (InputStream in = Forge1201Transformer.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/" + f)) {
                if (in == null) continue;
                for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (l.startsWith("#")) continue;
                    String[] a = l.strip().split("\t");
                    if (a.length == 2) map.put(a[0], a[1]);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return renamesCache = map;
    }

    private static volatile Map<String, String> membersCache;

    /** forge-members.tsv: {@code owner<TAB>name<TAB>newName} (original Forge owner; fields and methods of that name, whatever the descriptor). */
    private static synchronized Map<String, String> forgeMembers() {
        if (membersCache != null) return membersCache;
        Map<String, String> map = new HashMap<>();
        try (InputStream in = Forge1201Transformer.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/forge-members.tsv")) {
            if (in != null) {
                for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (l.startsWith("#") || l.isBlank()) continue;
                    String[] a = l.strip().split("\t");
                    if (a.length == 3) map.put(a[0] + "." + a[1], a[2]);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return membersCache = map;
    }

    /** {@code experimental.forge1201.softlink} (default on; the loader sets the system property from the config). */
    static boolean softLinkEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("legacyconnector.forge1201.softlink", "true").trim());
    }

    // ---- names ----------------------------------------------------------------------------------------------------------------

    /** Maps one class name; names outside Forge are returned unchanged (Minecraft's own class names are the same in both runtimes). */
    String mapClass(String name) {
        // Also remap inner classes such as ConsoleJS$VarFunc, without moving
        // the rest of KubeJS's util package.
        if (name.equals(KUBEJS6_CONSOLE) || name.startsWith(KUBEJS6_CONSOLE + "$")) {
            return KUBEJS7_CONSOLE + name.substring(KUBEJS6_CONSOLE.length());
        }
        if (!name.startsWith("net/minecraftforge/")) return name;
        String facade = LegacyVersion.V1_20_1.shimPrefix() + name;
        if (shim.contains(facade)) return facade;
        String direct = classRenames.get(name);
        if (direct != null) return direct;
        int dollar = name.indexOf('$');
        if (dollar > 0) {
            String outer = mapClass(name.substring(0, dollar));
            if (!outer.equals(name.substring(0, dollar))) return outer + name.substring(dollar);
        }
        for (String[] move : PACKAGE_MOVES) if (name.startsWith(move[0])) return move[1] + name.substring(move[0].length());
        return name;
    }

    /** A class of an isolated library that this jar does not define: the library tables of the native build do not apply to it. */
    private boolean isForeignIsolated(String owner) {
        return owner != null && !plan.isEmpty() && !ownClasses.contains(owner) && plan.mapClass(VER, owner) != null;
    }

    private String srgOnly(String name) {
        if (name.length() > 3 && name.charAt(1) == '_') {
            String m = srg.get(name);
            if (m != null) return m;
        }
        return name;
    }

    private final class Mapper extends Remapper {
        @Override
        public String map(String internalName) {
            String moved = plan.isEmpty() ? null : plan.mapClass(VER, internalName);
            if (moved != null) return moved;
            String r = mapClass(internalName);
            // mc-classes.tsv is keyed by the name after the Forge renames (a NeoForge class that has no counterpart in 1.21.1 maps on to a stand-in)
            String mc = Mc1201Tables.get().classes.get(r);
            return mc != null ? mc : r;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if (isForeignIsolated(owner)) return srgOnly(name);
            if (owner != null) {
                String forge = forgeMembers().get(owner + "." + name);
                if (forge != null) return forge;
            }
            if (name.length() > 3 && name.charAt(1) == '_') {
                String m = srg.get(name);
                if (m != null) name = m;
            }
            return Mc1201Tables.get().renamedMember(owner, name, descriptor);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            if (isForeignIsolated(owner)) return srgOnly(name);
            String forge = forgeMembers().get(owner + "." + name);
            if (forge != null) return forge;
            if (name.length() > 3 && name.charAt(1) == '_') {
                String m = srg.get(name);
                if (m != null) name = m;
            }
            return Mc1201Tables.get().renamedMember(owner, name, ":" + descriptor);
        }

        @Override
        public String mapInvokeDynamicMethodName(String name, String descriptor) {
            // a lambda for a functional interface whose method was renamed (FriendlyByteBuf.Writer.accept is StreamEncoder.encode): the factory returns the interface
            String iface = Type.getReturnType(descriptor).getInternalName();
            String renamed = Mc1201Tables.get().renamedMember(iface, name, "*");
            if (!renamed.equals(name)) return renamed;
            return mapMethodName(null, name, descriptor);
        }
    }

    // ---- the jar --------------------------------------------------------------------------------------------------------------

    /**
     * Structure of a jar's classes for the translation of the OTHER jars of the set ({@link Mc1201Hierarchy#peers}): class names after the Forge renames,
     * supertypes, and the declared methods whose name a migration table mentions (the old-signature overrides a subclass in another jar may call or override).
     * Cheap: method bodies are skipped, everything else is dropped.
     */
    public List<org.kylepitcock.legacyconnector.loader.shim.ClassInfo> scan(LegacyJar jar) throws IOException {
        Set<String> watched = Mc1201Tables.get().watchedMethodNames();
        Mapper mapper = new Mapper();
        List<org.kylepitcock.legacyconnector.loader.shim.ClassInfo> out = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.existing().toFile())) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/")) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in.readAllBytes()).accept(new ClassRemapper(node, mapper), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    var info = new org.kylepitcock.legacyconnector.loader.shim.ClassInfo(node.name, node.superName, List.copyOf(node.interfaces), node.access);
                    for (MethodNode m : node.methods) if (watched.contains(m.name)) info.methods.put(m.name + m.desc, m.access);
                    out.add(info);
                } catch (RuntimeException ex) {
                    // reported when the jar itself is translated
                }
            }
        }
        return out;
    }

    public LegacyJarTransformer.Result transform(LegacyJar jar, Path output) throws IOException {
        return transform(jar, output, Map.of());
    }

    /** @param peers the classes of the other jars of the set (see {@link #scan}), by name */
    public LegacyJarTransformer.Result transform(LegacyJar jar, Path output, Map<String, org.kylepitcock.legacyconnector.loader.shim.ClassInfo> peers) throws IOException {
        LegacyJarTransformer.Result result = new LegacyJarTransformer.Result();
        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, byte[]> other = new LinkedHashMap<>();
        Manifest manifest = null;
        try (ZipFile zip = new ZipFile(jar.existing().toFile())) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                byte[] data;
                try (InputStream in = zip.getInputStream(e)) {
                    data = in.readAllBytes();
                }
                String name = e.getName();
                if (name.equals("META-INF/MANIFEST.MF")) manifest = new Manifest(new ByteArrayInputStream(data));
                else if (name.endsWith(".class") && !name.startsWith("META-INF/")) classes.put(name.substring(0, name.length() - 6), data);
                else other.put(name, data);
            }
        }
        ownClasses = new HashSet<>(classes.keySet());
        ownIds = new HashSet<>();
        for (var m : jar.mods) ownIds.add(m.modernId());
        boolean ownUnit = false;
        for (String id : ownIds) if (plan.unit(VER, id) != null) ownUnit = true;
        Mapper mapper = new Mapper();
        Map<String, byte[]> outClasses = new LinkedHashMap<>();
        int stringsMapped = 0;
        int rewrites = 0;
        List<ClassNode> nodes = new ArrayList<>();
        Mc1201Hierarchy hierarchy = new Mc1201Hierarchy().peers(peers);
        java.util.Set<String> serializable = SerialUids.serializable(classes);
        for (var e : classes.entrySet()) {
            ClassNode node = new ClassNode();
            try {
                new ClassReader(e.getValue()).accept(SerialUids.head(serializable, e.getValue(), new ClassRemapper(node, mapper)), 0);
            } catch (RuntimeException ex) {
                result.notes.add("Skipped unreadable class " + e.getKey() + ": " + ex);
                continue;
            }
            hierarchy.add(node);
            nodes.add(node);
        }
        Datagen1201.Stats datagen = Datagen1201.apply(nodes);
        if (datagen.classes + datagen.methods > 0) result.notes.add("Data generation code dropped (dead in a running game): " + datagen);
        Mc1201Migration.Counts mc = new Mc1201Migration.Counts();
        Mixin1201.Stats mixinStats = new Mixin1201.Stats();
        mixinStats.refmaps = org.kylepitcock.legacyconnector.loader.transform.mixin1201.Refmaps.parse(other);
        Set<String> jarClasses = new HashSet<>();
        Set<String> compoundSubclasses = new HashSet<>();
        for (ClassNode node : nodes) {
            jarClasses.add(node.name);
            if (CallRewrites1201.COMPOUND.equals(node.superName)) compoundSubclasses.add(node.name);
        }
        java.util.function.Predicate<String> modEvents = n -> jarClasses.contains(n) && hierarchy.isSubclassOf(n, "net/neoforged/bus/api/Event");
        Map<ClassNode, Integer> migrated = new java.util.IdentityHashMap<>();
        SoftLink1201 soft = softLinkEnabled() ? new SoftLink1201(nodes, hierarchy) : null;
        int palladiumKubejsChanges = 0;
        int palladiumKubejsClassesDropped = 0;
        int palladiumAddonPackChanges = 0;
        int palladiumWidgetButtonChanges = 0;
        int palladiumMixinChanges = 0;
        int palladiumLootChanges = 0;
        int palladiumAccessoryScreenChanges = 0;
        int palladiumAdvancementWidgetChanges = 0;
        int palladiumPowersScreenChanges = 0;
        int palladiumSelectionListChanges = 0;
        int palladiumSuitStandChanges = 0;
        int palladiumRendererRotationChanges = 0;
        int palladiumTailoringScreenChanges = 0;
        int palladiumSheepColorChanges = 0;
        int palladiumRenderLayerChanges = 0;
        int palladiumGeckoAnimationChanges = 0;
        int palladiumGeckoModelAnimationChanges = 0;
        int palladiumCommandFunctionChanges = 0;
        int palladiumPlayerSkinChanges = 0;
        int palladiumPlayerSkinFetcherChanges = 0;
        int palladiumFlightHandlerChanges = 0;
        int palladiumCustomProjectileChanges = 0;
        int palladiumCoreOverlayChanges = 0;
        int palladiumCoreLightningMixinChanges = 0;
        for (ClassNode node : nodes) {
            if (PalladiumKubeJS1201.dropLegacyKubejsClass(node.name)) {
                palladiumKubejsClassesDropped++;
                if (PalladiumKubeJS1201.SCRIPT_FILE_INFO_MIXIN.equals(node.name)) {
                    mixinStats.dropped.add(node.name + " (KubeJS 7 moved script metadata from ScriptFileInfo to ScriptFile)");
                    mixinStats.droppedClasses.add(node.name);
                }
                continue;
            }
            stringsMapped += mapSrgStrings(node);
            int viaCalls = CallRewrites1201.apply(node, compoundSubclasses);
            int palladiumGeckoAnimationChange = PalladiumGeckoRenderLayer1201.apply(node);
            palladiumGeckoAnimationChanges += palladiumGeckoAnimationChange;
            viaCalls += palladiumGeckoAnimationChange;
            int palladiumGeckoModelAnimationChange = PalladiumGeckoRenderLayerModel1201.apply(node);
            palladiumGeckoModelAnimationChanges += palladiumGeckoModelAnimationChange;
            viaCalls += palladiumGeckoModelAnimationChange;
            int palladiumSuitStandChange = PalladiumSuitStand1201.apply(node);
            palladiumSuitStandChanges += palladiumSuitStandChange;
            int palladiumRendererRotationChange = PalladiumRendererRotation1201.apply(node);
            palladiumRendererRotationChanges += palladiumRendererRotationChange;
            int palladiumTailoringScreenChange = PalladiumTailoringScreen1201.apply(node);
            palladiumTailoringScreenChanges += palladiumTailoringScreenChange;
            int palladiumSheepColorChange = PalladiumColor1201.apply(node);
            palladiumSheepColorChanges += palladiumSheepColorChange;
            int palladiumRenderLayerChange = PalladiumRenderLayer1201.apply(node);
            palladiumRenderLayerChanges += palladiumRenderLayerChange;
            int palladiumChanges = PalladiumKubeJS1201.apply(node);
            palladiumKubejsChanges += palladiumChanges;
            int palladiumAddonPackChange = PalladiumAddonPackManager1201.apply(node);
            palladiumAddonPackChanges += palladiumAddonPackChange;
            viaCalls += ForgeApiRewrites1201.apply(node, modEvents);
            if (modEvents.test(node.name) && ForgeApiRewrites1201.markCancelable(node)) viaCalls++;
            viaCalls += AttachGuards1201.apply(node, jarClasses);
            if (ForgeApiRewrites1201.markProviders(node)) viaCalls++;
            rewrites += viaCalls;
            // Replace the removed SkinTextureCallback API before the generic soft-link pass.
            int palladiumPlayerSkinFetcherChange = PalladiumPlayerSkinFetcher1201.apply(node);
            palladiumPlayerSkinFetcherChanges += palladiumPlayerSkinFetcherChange;
            // Adapt before Mc1201Migration redirects AddLayers.getSkin(String) to ClientCompat.getSkin.
            // The PalladiumCore adapter must see and retarget the original lookup before that generic pass.
            int palladiumPlayerSkinChange = PalladiumCorePlayerSkin1201.apply(node);
            palladiumPlayerSkinChanges += palladiumPlayerSkinChange;
            Mc1201Migration.Counts changed = Mc1201Migration.apply(node, hierarchy);
            mc.add(changed);
            int palladiumFlightHandlerChange = PalladiumFlightHandler1201.apply(node);
            palladiumFlightHandlerChanges += palladiumFlightHandlerChange;
            int palladiumCustomProjectileChange = PalladiumCustomProjectile1201.apply(node);
            palladiumCustomProjectileChanges += palladiumCustomProjectileChange;
            int palladiumCoreOverlayChange = PalladiumCoreOverlay1201.apply(node);
            palladiumCoreOverlayChanges += palladiumCoreOverlayChange;
            int palladiumCoreLightningMixinChange = PalladiumCoreLightningBoltMixin1201.apply(node);
            palladiumCoreLightningMixinChanges += palladiumCoreLightningMixinChange;
            // The generic rule above should cover this call shape. Palladium 4.5.10's command abilities
            // and energy-beam hit handlers are guarded explicitly as well, so a missed site cannot leave
            // its input/action path silently calling the removed synchronous API.
            int palladiumCommandFunctionChange = PalladiumCommandFunction1201.apply(node);
            palladiumCommandFunctionChanges += palladiumCommandFunctionChange;
            // Runs after GuiGraphics.blitNineSliced has been redirected, but before SoftLink1201
            // would replace Palladium's now-missing inherited Button.WIDGETS_LOCATION with null.
            int palladiumWidgetButtonChange = PalladiumWidgetButtons1201.apply(node);
            palladiumWidgetButtonChanges += palladiumWidgetButtonChange;
            int palladiumMixinChange = PalladiumMixinCompat1201.apply(node);
            palladiumMixinChanges += palladiumMixinChange;
            int palladiumLootChange = PalladiumLootCompat1201.apply(node);
            palladiumLootChanges += palladiumLootChange;
            int palladiumAccessoryScreenChange = PalladiumAccessoryScreen1201.apply(node);
            palladiumAccessoryScreenChanges += palladiumAccessoryScreenChange;
            int palladiumAdvancementWidgetChange = PalladiumAdvancementWidget1201.apply(node);
            palladiumAdvancementWidgetChanges += palladiumAdvancementWidgetChange;
            int palladiumPowersScreenChange = PalladiumPowersScreen1201.apply(node);
            palladiumPowersScreenChanges += palladiumPowersScreenChange;
            int palladiumSelectionListChange = PalladiumSelectionLists1201.apply(node);
            palladiumSelectionListChanges += palladiumSelectionListChange;
            int mixinResigned = Mixin1201.apply(node, srg, hierarchy, Mc1201Tables.get(), mixinStats);
            migrated.put(node, mixinResigned + viaCalls + changed.total() + palladiumChanges + palladiumAddonPackChange
                    + palladiumWidgetButtonChange + palladiumMixinChange + palladiumLootChange
                    + palladiumAccessoryScreenChange + palladiumAdvancementWidgetChange + palladiumPowersScreenChange
                    + palladiumSelectionListChange + palladiumSuitStandChange
                    + palladiumTailoringScreenChange + palladiumSheepColorChange
                    + palladiumRendererRotationChange + palladiumRenderLayerChange + palladiumPlayerSkinChange
                    + palladiumPlayerSkinFetcherChange + palladiumCommandFunctionChange
                    + palladiumFlightHandlerChange + palladiumCustomProjectileChange
                    + palladiumCoreOverlayChange + palladiumCoreLightningMixinChange);
        }
        if (palladiumKubejsChanges > 0 || palladiumKubejsClassesDropped > 0) {
            result.notes.add("Palladium KubeJS 6 integration adapted for KubeJS 7: "
                    + palladiumKubejsChanges + " bytecode rewrite(s), "
                    + palladiumKubejsClassesDropped + " obsolete KubeJS 6 class(es) removed");
        }
        if (palladiumAddonPackChanges > 0) {
            result.notes.add("Palladium addon-pack repository adapted: FolderRepositorySource uses Minecraft's symlink validator; the discovery callback rebuilds immutable 1.21.1 PackLocationInfo/PackSelectionConfig with the legacy addonpack: ID prefix and required-selection setting: "
                    + palladiumAddonPackChanges + " constructor call(s)");
        }
        if (palladiumWidgetButtonChanges > 0) {
            result.notes.add("Palladium default button backgrounds migrated to 1.21.1 GUI sprites: "
                    + palladiumWidgetButtonChanges + " button class(es)");
        }
        if (palladiumMixinChanges > 0) {
            result.notes.add("Palladium 1.21.1 compatibility adapters applied to "
                    + palladiumMixinChanges + " class(es); includes PlayerList scoreboard replay migration to PlayerScoreEntry and the 1.21.1 score packet, plus VibrationListener GameEvent Holder unwrapping; the obsolete Watcher render hook is omitted");
        }
        if (palladiumSuitStandChanges > 0) {
            result.notes.add("Palladium SuitStand migrated to the 1.21.1 SynchedEntityData builder lifecycle: "
                    + palladiumSuitStandChanges + " entity class(es)");
        }
        if (palladiumFlightHandlerChanges > 0) {
            result.notes.add("Palladium FlightHandler EntityDimensions.height field access migrated to the public 1.21.1 record accessor: "
                    + palladiumFlightHandlerChanges + " field read(s)");
        }
        if (palladiumCustomProjectileChanges > 0) {
            result.notes.add("Palladium CustomProjectile EntityDimensions.width field access migrated to the public 1.21.1 record accessor: "
                    + palladiumCustomProjectileChanges + " field read(s)");
        }
        if (palladiumCoreOverlayChanges > 0) {
            result.notes.add("PalladiumCore overlay registry verified against the Forge 1.20.1 RegisterGuiOverlaysEvent/ForgeGui shim, which forwards to NeoForge RegisterGuiLayersEvent/LayeredDraw.Layer: "
                    + palladiumCoreOverlayChanges + " overlay registry class(es)");
        }
        if (palladiumCoreLightningMixinChanges > 0) {
            result.notes.add("PalladiumCore LightningBoltMixin declares maxShiftBy=1 for its verified one-instruction "
                    + "INVOKE_ASSIGN shift after the second Level.getEntities call");
        }
        if (palladiumLootChanges > 0) {
            result.notes.add("Palladium loot-pool Gson deserialization migrated to NeoForge 1.21.1 loot codecs: "
                    + palladiumLootChanges + " manager class(es)");
        }
        if (ownUnit) {
            // an isolated library next to its native build registers the same ids as the native one: its deferred registers are not attached to the bus
            for (ClassNode node : nodes) {
                for (MethodNode m : node.methods) {
                    if (m.instructions == null) continue;
                    for (AbstractInsnNode in : m.instructions.toArray()) {
                        if (in instanceof org.objectweb.asm.tree.MethodInsnNode mi && mi.name.equals("register") && mi.desc.equals("(Lnet/neoforged/bus/api/IEventBus;)V")
                                && mi.owner.startsWith("net/neoforged/neoforge/registries/DeferredRegister") && mi.getOpcode() != org.objectweb.asm.Opcodes.INVOKESTATIC) {
                            m.instructions.set(mi, new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "legacyconnector/v1_20_1/ForgeApi1201",
                                    "skipIsolatedRegister", "(Lnet/neoforged/neoforge/registries/DeferredRegister;Lnet/neoforged/bus/api/IEventBus;)V", false));
                        }
                    }
                }
            }
        }
        int modCtors = ModConstructors1201.apply(nodes);
        if (modCtors > 0) result.notes.add("@Mod constructor taking FMLJavaModLoadingContext: a (IEventBus, ModContainer) constructor was generated for NeoForge");
        int subscribersMoved = Subscribers1201.apply(nodes);
        if (subscribersMoved > 0) result.notes.add("Event subscriber classes with instance handlers: " + subscribersMoved + " registered by the main class for their static handlers (Forge ignored the instance ones)");
        if (!plan.isEmpty()) for (ClassNode node : nodes) isolate(node);
        if (ownUnit) for (ClassNode node : nodes) renameInjectorHandlers(node, VER.id().replaceAll("[^a-z0-9]", ""));
        // the soft link runs when every class of the jar has been migrated: references to the mod's own members are judged by their final declarations
        for (ClassNode node : nodes) {
            if (PalladiumKubeJS1201.dropLegacyKubejsClass(node.name)) continue;
            int softened = soft == null ? 0 : soft.apply(node);
            byte[] bytes;
            if (migrated.get(node) > 0 || softened == 2) {
                // the stack at branch targets changed: frames are computed again
                try {
                    ClassWriter writer = hierarchy.frameWriter();
                    node.accept(writer);
                    bytes = writer.toByteArray();
                } catch (RuntimeException ex) {
                    result.notes.add("Frames of " + node.name + " could not be recomputed (" + ex + "); the class keeps the old ones and may fail to verify");
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    bytes = writer.toByteArray();
                }
            } else {
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                bytes = writer.toByteArray();
            }
            // a class of the jar that a table renames onto a class of the layer (Architectury's EventBuses): the layer's own copy is used, and a second one in this
            // jar would put the same package in two modules
            if (node.name.startsWith(Mc1201Tables.HELPER_PACKAGE.substring(0, Mc1201Tables.HELPER_PACKAGE.lastIndexOf("migrate/"))) && !ownClasses.contains(node.name)) continue;
            outClasses.put(node.name + ".class", bytes);
            result.classes++;
        }
        if (soft != null && soft.stats.symbols() > 0) {
            result.notes.add("Soft link: " + soft.stats.summary() + " that 1.21.1 lacks were replaced by stand-ins (the mod loads, only the features using them fail; experimental.forge1201.softlink=false turns this off)");
            result.notes.addAll(soft.stats.lines());
            result.softLinked = soft.stats.symbols();
            result.softSymbols.addAll(soft.stats.symbolList());
        }
        if (soft != null) for (String u : soft.unfixable) result.notes.add("Soft link: class " + u.replace('/', '.') + " does not exist in 1.21.1; a mod class extending it cannot be stood in for and will not load");
        if (mc.total() > 0) result.notes.add("Minecraft 1.20.1 to 1.21.1 migration: " + mc);
        if (mc.widgetNarrationOverrides > 0) {
            result.notes.add("Minecraft 1.20.1 AbstractWidget.updateNarration() overrides migrated to the 1.21.1 updateWidgetNarration() hook: "
                    + mc.widgetNarrationOverrides + " widget class(es)");
        }
        if (mixinStats.mixinClasses > 0) result.notes.add("Mixins: " + mixinStats.summary());
        for (String d : mixinStats.dropped) result.notes.add("Mixin dropped, its target no longer exists in Minecraft 1.21.1: " + d);
        result.notes.addAll(mixinStats.repair.notes);
        if (rewrites > 0) result.notes.add(rewrites + " call site(s) rewritten for changed 1.21.1 signatures (ResourceLocation factories, event bus post, PalladiumCore renderer lookup, inventory entity preview)");
        if (stringsMapped > 0) result.notes.add(stringsMapped + " reflection string(s) with SRG names now use Mojang names");

        Map<String, byte[]> outResources = new LinkedHashMap<>();
        Map<String, String> cfgRename = new HashMap<>();
        if (palladiumSuitStandChanges > 0) {
            result.notes.add("Palladium SuitStand migrated to the 1.21.1 SynchedEntityData.Builder lifecycle; DATA_CLIENT_FLAGS setup deferred until after the ArmorStand constructor: "
                    + palladiumSuitStandChanges + " entity class(es)");
        }
        if (palladiumRendererRotationChanges > 0) {
            result.notes.add("Palladium SuitStand/TrailSegment renderer setupRotations overrides and calls migrated to the 1.21.1 four-float signature with the legacy default scale: "
                    + palladiumRendererRotationChanges + " method/call site(s)");
        }
        if (palladiumAccessoryScreenChanges > 0) {
            result.notes.add("Palladium AccessoryScreen keeps its legacy lists/title/Done controls, skips the unused OptionsSubScreen body, passes Minecraft.options, suppresses the modern Screen background/blur hook while preserving super.render() widgets, and removes the legacy dirt-background layer: "
                    + palladiumAccessoryScreenChanges + " screen class(es)");
        }
        if (palladiumAdvancementWidgetChanges > 0) {
            result.notes.add("Palladium TreeAbilityWidget tooltip frame lookup migrated from removed AdvancementWidgetType#getIndex() to the preserved 1.21.1 enum order: "
                    + palladiumAdvancementWidgetChanges + " call site(s)");
        }
        if (palladiumPowersScreenChanges > 0) {
            result.notes.add("Palladium PowersScreen suppresses the modern Screen background/blur hook while preserving its custom contents: "
                    + palladiumPowersScreenChanges + " screen class(es)");
        }
        if (palladiumSelectionListChanges > 0) {
            result.notes.add("Palladium list widgets: " + palladiumSelectionListChanges
                    + " bytecode site(s) migrated (legacy x0 position fields to getX/setX, three legacy render preferences to 1.21.1 hooks, and verified no-op narration overrides)");
        }
        if (palladiumTailoringScreenChanges > 0) {
            result.notes.add("Palladium TailoringScreen removed its obsolete empty updateNarration() override, which conflicts with 1.21.1's final AbstractWidget implementation: "
                    + palladiumTailoringScreenChanges + " recipe-list class(es)");
        }
        if (palladiumSheepColorChanges > 0) {
            result.notes.add("Palladium SuitStand sheep-color calls migrated to 1.21.1 Sheep.getColor(DyeColor), preserving normalized RGB float arrays: "
                    + palladiumSheepColorChanges + " call site(s)");
        }
        if (palladiumRenderLayerChanges > 0) {
            result.notes.add("Palladium SuitStand RenderLayer color-copy call migrated from nine float arguments to the 1.21.1 six-float plus packed ARGB color overload: "
                    + palladiumRenderLayerChanges + " call site(s)");
        }
        if (palladiumGeckoAnimationChanges > 0) {
            result.notes.add("Palladium first-person GeckoRenderLayer adapted to GeckoLib's handleAnimations partial-tick API: "
                    + palladiumGeckoAnimationChanges + " call site(s)");
        }
        if (palladiumGeckoModelAnimationChanges > 0) {
            result.notes.add("Palladium GeckoRenderLayerModel inventory/player-entity preview adapted to GeckoLib's handleAnimations partial-tick API: "
                    + palladiumGeckoModelAnimationChanges + " call site(s)");
        }
        if (palladiumCommandFunctionChanges > 0) {
            result.notes.add("Palladium CommandAbility and EnergyBeamAbility command-function execution migrated to the 1.21.1 queued API: "
                    + palladiumCommandFunctionChanges + " call site(s)");
        }
        if (palladiumPlayerSkinChanges > 0) {
            result.notes.add("PalladiumCore player-skin lambda migrated in place from String to NeoForge PlayerSkin.Model; the original AddLayers callback and per-entity exception handling are preserved: "
                    + palladiumPlayerSkinChanges + " lambda(s)");
        }
        if (palladiumPlayerSkinFetcherChanges > 0) {
            result.notes.add("Palladium PlayerSkinFetcher migrated from the removed SkinTextureCallback to SkinManager.getOrLoad / CompletableFuture.whenComplete: "
                    + palladiumPlayerSkinFetcherChanges + " fetcher class(es)");
        }
        if (ownUnit) {
            String suffix = "_l" + VER.id().replaceAll("[^a-z0-9]", "");
            for (var e : other.entrySet()) {
                String n = e.getKey();
                if (n.indexOf('/') < 0 && n.endsWith(".json") && looksLikeMixinConfig(e.getValue())) cfgRename.put(n, SideBySide.mixinConfigName(n, suffix));
            }
        }
        for (var e : other.entrySet()) {
            String name = e.getKey();
            if (LegacyJarTransformer.isSignatureFile(name)) continue;
            // the config's refmap entry is removed (Mojang names); the file would be found by name for the native copy's config (same name) and break it
            if (ownUnit && name.indexOf('/') < 0 && name.endsWith(".refmap.json")) continue;
            // multi-loader jars: Sinytra Connector would load the same jar a second time as a Fabric mod
            if (name.equals("fabric.mod.json") || name.equals("quilt.mod.json")) continue;
            byte[] data = e.getValue();
            if (name.startsWith("META-INF/jarjar/") || (name.startsWith("META-INF/jars/") && name.endsWith(".jar"))) {   // the metadata may name any path (Artifacts: META-INF/jars/)
                if (name.endsWith(".jar")) {
                    byte[] nested = nestedJar(name, data, result);
                    if (nested == null) continue;
                    data = nested;
                } else if (name.equals("META-INF/jarjar/metadata.json")) {
                    data = jarJarMetadata(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                }
            }
            if (name.equals("META-INF/mods.toml")) {
                // a manifest may list a config that lives in ANOTHER jar (Cataclysm lists the Lionfish API one): registering it here would register it twice
                List<String> ownConfigs = new ArrayList<>(mixinConfigs(manifest));
                ownConfigs.removeIf(c -> !other.containsKey(c));
                String toml = modsToml(new String(data, StandardCharsets.UTF_8), ownConfigs);
                if (!plan.isEmpty()) {
                    for (var c : cfgRename.entrySet()) toml = toml.replace("\"" + c.getKey() + "\"", "\"" + c.getValue() + "\"");
                    // a dependent that lists the library's mixin config in its manifest (Alex's Caves: MixinConfigs=citadel.mixins.json,...) must not register it
                    // a second time under the library's name: configs that are not resources of this jar are not its own
                    Matcher cm = Pattern.compile("\n\\[\\[mixins\\]\\]\\s*\nconfig\\s*=\\s*\"([^\"]+)\"\\s*\n").matcher(toml);
                    StringBuilder cleaned = new StringBuilder();
                    while (cm.find()) {
                        String config = cm.group(1);
                        cm.appendReplacement(cleaned, other.containsKey(config) || cfgRename.containsValue(config) ? Matcher.quoteReplacement(cm.group()) : "\n");
                    }
                    cm.appendTail(cleaned);
                    toml = cleaned.toString();
                    toml = plan.rewriteToml(VER, toml, ownIds);
                }
                outResources.put("META-INF/neoforge.mods.toml", toml.getBytes(StandardCharsets.UTF_8));
                continue;
            }
            if (name.equals("META-INF/accesstransformer.cfg")) data = accessTransformer(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            else if (name.endsWith(".json") && looksLikeMixinConfig(data)) {
                String json = new String(data, StandardCharsets.UTF_8);
                if (!plan.isEmpty()) json = relocateMixinConfig(json, ownUnit);
                data = Mixin1201.config(json, mixinStats.droppedClasses, null).getBytes(StandardCharsets.UTF_8);
                name = cfgRename.getOrDefault(name, name);
            }
            outResources.put(name, data);
        }
        if (!plan.isEmpty()) {
            outResources = new LinkedHashMap<>(plan.rewriteResources(VER, outResources));
            if (ownUnit) dropDanglingProviders(outResources, outClasses.keySet());
            if (ownUnit) result.notes.add("Side-by-side library: mod id(s) " + ownIds + " load as separate unit(s), packages relocated under " + SideBySide.prefix(VER));
            else result.notes.add("Side-by-side: references to isolated libraries " + plan.units().stream().filter(u -> u.version() == VER).map(SideBySide.Unit::modId).toList() + " link to their relocated copies");
        }
        int alienEvoResourceChanges = PalladiumResourceCompat1201.apply(outResources);
        if (alienEvoResourceChanges > 0) {
            result.notes.add("AlienEvo resources migrate legacy ResourceLocation and SoundEvent script APIs, persistent NBT integer/string values, armor guards, Omnitrix tab comparisons and KubeJS 7 server/player access; normalize stat-ability modifier IDs, remove the obsolete EditBox.tick call, and repair inspected Diamond Shard/AFOMNI particle texture IDs: "
                    + alienEvoResourceChanges + " resource file(s)");
        }
        if (PalladiumKubeJS1201.addPluginDiscoveryResource(outResources, outClasses.keySet())) {
            result.notes.add("Palladium KubeJS 7 plugin discovery entry added to kubejs.plugins.txt");
        }
        if (AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(outResources, outClasses)) {
            result.notes.add("AlienEvo: removed the unreferenced hidden code_power_test resource; its generate_random/check_code types are not registered, while the real code system is handled by display_code");
        }
        if (AlienEvoResourceCompat1201.makeAdAstraDamageTypeOptional(outResources)) {
            result.notes.add("AlienEvo: made the Ad Astra oxygen damage-type tag optional; it is used when ad_astra:oxygen is registered and skipped when Ad Astra is absent");
        }
        if (AlienEvoResourceCompat1201.ensureAddonResourceRootMarker(outResources)) {
            result.notes.add("AlienEvo: added the 1.21.1 addon resource-root marker for its nonstandard addon/ asset tree");
        }
        // the data and resource packs: 1.21 folder names, recipe/loot/advancement/tag/model formats, forge: ids, pack.mcmeta (DataConverter1201)
        DataConverter1201.Stats dataStats = new DataConverter1201.Stats();
        outResources = new LinkedHashMap<>(DataConverter1201.convertAll(outResources, dataStats));
        if (dataStats.total() > 0) result.notes.add("Data and resources converted to the 1.21.1 formats: " + dataStats);
        FunctionConverter1201.Stats functionStats = new FunctionConverter1201.Stats();
        outResources = new LinkedHashMap<>(FunctionConverter1201.convertAll(outResources, functionStats));
        if (functionStats.changedFiles > 0) {
            String functionNote = "Legacy 1.20.1 command functions migrated: " + functionStats.changedFiles
                    + " function file(s), " + functionStats.rewrittenCommands + " command line(s)";
            if (functionStats.droppedIncompleteCommands > 0) {
                functionNote += "; " + functionStats.droppedIncompleteCommands + " incomplete actionless command line(s) replaced with explanatory comments";
            }
            result.notes.add(functionNote + ": " + functionStats.categories);
        }

        Manifest outManifest = LegacyJarTransformer.cleanManifest(manifest, jar);
        String originalSha = Provenance.sha256Hex(jar.existing());
        Provenance.addManifest(outManifest.getMainAttributes(), jar, originalSha);
        outResources.put(Provenance.RESOURCE, Provenance.json(jar, originalSha, LegacyJarTransformer.TRANSFORMER_VERSION));
        LegacyJarTransformer.write(output, outManifest, outClasses, outResources);
        return result;
    }

    // ---- side-by-side libraries ---------------------------------------------------------------------------------------------

    private static final String MOD_DESC = "Lnet/neoforged/fml/common/Mod;";
    private static final String SUBSCRIBER_DESC = "Lnet/neoforged/fml/common/Mod$EventBusSubscriber;";

    /** Relocates string constants and annotation values that name isolated classes, and swaps the mod id of an isolated library's own annotations. */
    private void isolate(ClassNode node) {
        for (MethodNode m : node.methods) {
            if (m.instructions != null) {
                for (AbstractInsnNode in : m.instructions) {
                    if (in instanceof LdcInsnNode l && l.cst instanceof String s) {
                        String moved = plan.mapString(VER, s);
                        if (!moved.equals(s)) {
                            l.cst = moved;
                            continue;
                        }
                        // ModList.isLoaded("citadel") in a dependent asks about the isolated copy it is linked to
                        SideBySide.Unit u = plan.unit(VER, s.toLowerCase(java.util.Locale.ROOT));
                        AbstractInsnNode next = l.getNext();
                        if (u != null && !ownIds.contains(u.modId()) && next instanceof org.objectweb.asm.tree.MethodInsnNode mi
                                && mi.owner.equals("net/neoforged/fml/ModList")) l.cst = u.newId();
                    }
                }
            }
            annotations(m.visibleAnnotations);
            annotations(m.invisibleAnnotations);
        }
        for (org.objectweb.asm.tree.FieldNode f : node.fields) {
            if (f.value instanceof String s) f.value = plan.mapString(VER, s);
            annotations(f.visibleAnnotations);
            annotations(f.invisibleAnnotations);
        }
        annotations(node.visibleAnnotations);
        annotations(node.invisibleAnnotations);
    }

    private void annotations(List<org.objectweb.asm.tree.AnnotationNode> list) {
        if (list == null) return;
        for (var a : list) {
            String idKey = MOD_DESC.equals(a.desc) ? "value" : SUBSCRIBER_DESC.equals(a.desc) ? "modid" : null;
            annotationValues(a.values, idKey);
        }
    }

    @SuppressWarnings("unchecked")
    private void annotationValues(List<Object> values, String idKey) {
        if (values == null) return;
        for (int i = 0; i + 1 < values.size(); i += 2) {
            Object v = values.get(i + 1);
            if (v instanceof String s) {
                String moved = plan.mapString(VER, s);
                if (idKey != null && idKey.equals(values.get(i)) && ownIds.contains(s.toLowerCase(java.util.Locale.ROOT))) {
                    SideBySide.Unit u = plan.unit(VER, s.toLowerCase(java.util.Locale.ROOT));
                    if (u != null) moved = u.newId();
                }
                values.set(i + 1, moved);
            } else if (v instanceof List<?> l) {
                List<Object> copy = (List<Object>) l;
                for (int k = 0; k < copy.size(); k++) {
                    Object x = copy.get(k);
                    if (x instanceof String s) copy.set(k, plan.mapString(VER, s));
                    else if (x instanceof org.objectweb.asm.tree.AnnotationNode an) annotationValues(an.values, null);
                }
            } else if (v instanceof org.objectweb.asm.tree.AnnotationNode an) {
                annotationValues(an.values, null);
            }
        }
    }

    /**
     * Service files of an isolated library naming providers the jar does not contain (Citadel lists net.sourceforge.jaad..., which only its native
     * build has): the module system adds a provider's package to the automatic module, and the native copy exports the same package.
     */
    private static void dropDanglingProviders(Map<String, byte[]> resources, Set<String> classEntries) {
        for (String name : new ArrayList<>(resources.keySet())) {
            if (!name.startsWith("META-INF/services/")) continue;
            StringBuilder kept = new StringBuilder();
            for (String line : new String(resources.get(name), StandardCharsets.UTF_8).split("\\R")) {
                String t = line.strip();
                int hash = t.indexOf('#');
                if (hash >= 0) t = t.substring(0, hash).strip();
                if (t.isEmpty()) continue;
                if (classEntries.contains(t.replace('.', '/') + ".class")) kept.append(t).append('\n');
            }
            if (kept.length() == 0) resources.remove(name);
            else resources.put(name, kept.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";

    /**
     * An isolated library's mixins merge into the same vanilla classes as the native copy's: two injector handlers with one name in one target fail the
     * class ("Critical injection failure"). The handlers (injector annotations from Mixin and MixinExtras) get a version suffix.
     */
    private static void renameInjectorHandlers(ClassNode node, String suffix) {
        if (node.invisibleAnnotations == null || node.invisibleAnnotations.stream().noneMatch(a -> MIXIN_DESC.equals(a.desc))) return;
        for (MethodNode m : node.methods) {
            if (m.name.startsWith("<") || !hasInjector(m.visibleAnnotations) && !hasInjector(m.invisibleAnnotations)) continue;
            m.name = m.name + "_l" + suffix;
        }
    }

    private static boolean hasInjector(List<org.objectweb.asm.tree.AnnotationNode> list) {
        if (list == null) return false;
        for (var a : list) {
            String d = a.desc;
            if (d.startsWith("Lorg/spongepowered/asm/mixin/injection/") && !d.contains("/At;") && !d.contains("/Slice;") && !d.contains("/Coerce;") && !d.contains("/Desc;")
                    && !d.contains("/Group;") && !d.contains("/Constant;")) return true;
            if (d.startsWith("Lcom/llamalad7/mixinextras/injector/")) return true;
        }
        return false;
    }

    /** A mixin config of this jar: its package and plugin class follow the relocation. */
    private String relocateMixinConfig(String json, boolean ownUnit) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            for (String key : new String[] {"package", "plugin"}) {
                if (root.has(key) && root.get(key).isJsonPrimitive()) root.addProperty(key, plan.mapString(VER, root.get(key).getAsString()));
            }
            // the native copy of the library injects into the same vanilla code: the isolated copy is applied after it (a lost injection is only logged)
            if (ownUnit) root.addProperty("priority", 2000);
            return root.toString();
        } catch (RuntimeException e) {
            return json;
        }
    }

    // ---- jar-in-jar ------------------------------------------------------------------------------------------------------------

    /** Nested libraries NeoForge 1.21.1 ships itself: a second (older) copy would split the module. */
    static boolean providedByHost(String path) {
        String n = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
        return n.startsWith("mixinextras");
    }

    /**
     * A jar inside {@code META-INF/jarjar}: dropped when the host provides it (MixinExtras), translated when it is a Forge 1.20.1 mod of its own (SpectreLib,
     * Registrate...), kept otherwise. Null drops it.
     */
    private byte[] nestedJar(String path, byte[] data, LegacyJarTransformer.Result result) throws IOException {
        if (providedByHost(path)) {
            result.notes.add("Dropped nested " + path.substring(path.lastIndexOf('/') + 1) + " (NeoForge provides it)");
            return null;
        }
        Path in = Files.createTempFile("lc1201-in-", ".jar");
        Path out = Files.createTempFile("lc1201-out-", ".jar");
        try {
            Files.write(in, data);
            LegacyJar nested = LegacyJarInspector.inspect(in);
            if (nested == null || nested.version != LegacyVersion.V1_20_1) return data;
            new Forge1201Transformer(shim).sideBySide(plan).transform(nested, out);
            result.notes.add("Translated nested " + path.substring(path.lastIndexOf('/') + 1));
            return Files.readAllBytes(out);
        } finally {
            Files.deleteIfExists(in);
            Files.deleteIfExists(out);
        }
    }

    /** {@code META-INF/jarjar/metadata.json}: the entries of dropped nested jars go. */
    static String jarJarMetadata(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray jars = root.getAsJsonArray("jars");
            if (jars == null) return json;
            JsonArray kept = new JsonArray();
            for (JsonElement el : jars) {
                String path = el.getAsJsonObject().has("path") ? el.getAsJsonObject().get("path").getAsString() : "";
                if (!providedByHost(path)) kept.add(el);
            }
            root.add("jars", kept);
            return root.toString();
        } catch (RuntimeException e) {
            return json;
        }
    }

    /** Reflection by SRG name ({@code ObfuscationReflectionHelper.findField(Foo.class, "f_1_")}): the runtime has Mojang names. */
    private int mapSrgStrings(ClassNode node) {
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode in : m.instructions) {
                if (in instanceof LdcInsnNode l && l.cst instanceof String s && s.length() > 3 && s.charAt(1) == '_' && SRG.matcher(s).matches()) {
                    String mojang = srg.get(s);
                    if (mojang != null) {
                        l.cst = mojang;
                        n++;
                    }
                }
            }
        }
        return n;
    }

    // ---- metadata and data -------------------------------------------------------------------------------------------------

    private static final Pattern KEY_LINE = Pattern.compile("^(\\s*)([A-Za-z_]+)(\\s*=\\s*)(.*)$");

    /** mods.toml to neoforge.mods.toml: same format; the loader, Minecraft and "forge" dependencies are retargeted, {@code mandatory} became {@code type}. */
    /** The mixin configs a Forge 1.20.1 jar names in its manifest ({@code MixinConfigs: a.mixins.json,b.mixins.json}); NeoForge wants them in the mods.toml. */
    static List<String> mixinConfigs(Manifest manifest) {
        String value = manifest == null ? null : manifest.getMainAttributes().getValue("MixinConfigs");
        List<String> out = new ArrayList<>();
        if (value != null) for (String c : value.split("[,\\s]+")) if (!c.isBlank()) out.add(c.strip());
        return out;
    }

    static String modsToml(String text) {
        return modsToml(text, List.of());
    }

    static String modsToml(String text, List<String> mixinConfigs) {
        // Repair dependency table owners for single-mod Forge jars that mistakenly name the owner "examplemod".
        String soleDeclaredModId = soleDeclaredModId(text);
        StringBuilder out = new StringBuilder("# Generated by Legacy Connector from the Forge 1.20.1 mods.toml\n");
        List<String> block = new ArrayList<>();
        boolean inDependency = false;
        boolean multi = false; // inside a ''' or """ string: its lines are text, whatever they look like
        for (String line : text.split("\\R", -1)) {
            boolean wasMulti = multi;
            if ((count(line, "'''") + count(line, "\"\"\"")) % 2 == 1) multi = !multi;
            if (wasMulti) {
                if (inDependency) block.add(line);
                else out.append(line).append('\n');
                continue;
            }
            String t = line.strip();
            if (t.startsWith("[")) {
                flushDependency(out, block, inDependency);
                block.clear();
                inDependency = t.startsWith("[[dependencies.");
                String header = line;
                if (inDependency && soleDeclaredModId != null) {
                    Matcher dependencyHeader = Pattern.compile("^(\\s*)\\[\\[dependencies\\.[^\\]]+\\]\\](\\s*(?:#.*)?)$").matcher(line);
                    if (dependencyHeader.matches()) {
                        header = dependencyHeader.group(1) + "[[dependencies." + soleDeclaredModId + "]]" + dependencyHeader.group(2);
                    }
                }
                out.append(inDependency ? "" : header).append(inDependency ? "" : "\n");
                if (inDependency) block.add(header);
                continue;
            }
            if (inDependency) {
                block.add(line);
                continue;
            }
            Matcher m = KEY_LINE.matcher(line);
            if (m.matches() && m.group(2).equals("displayTest")) continue; // removed in NeoForge
            if (m.matches() && m.group(2).equals("loaderVersion")) {
                out.append(m.group(1)).append("loaderVersion").append(m.group(3)).append("\"[1,)\"\n");
                continue;
            }
            out.append(line).append('\n');
        }
        flushDependency(out, block, inDependency);
        // a Forge mods.toml may already declare its [[mixins]] (NeoForge reads the table too): a second entry fails with "Decoration with key 'fabric-modId' already exists"
        Set<String> declared = new HashSet<>();
        Matcher declaredMixins = Pattern.compile("\\[\\[mixins\\]\\][^\\[]*?config\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.DOTALL).matcher(text);
        while (declaredMixins.find()) declared.add(declaredMixins.group(1));
        for (String config : mixinConfigs) {
            if (!declared.contains(config)) out.append("\n[[mixins]]\nconfig=\"").append(config).append("\"\n");
        }
        return out.toString();
    }

    private static String soleDeclaredModId(String text) {
        List<String> ids = new ArrayList<>();
        boolean inMod = false;
        boolean multi = false;
        for (String line : text.split("\\R", -1)) {
            boolean wasMulti = multi;
            if ((count(line, "'''") + count(line, "\"\"\"")) % 2 == 1) multi = !multi;
            if (wasMulti) continue;
            String t = line.strip();
            if (t.startsWith("[")) {
                inMod = t.equals("[[mods]]");
                continue;
            }
            if (!inMod) continue;
            Matcher m = KEY_LINE.matcher(line);
            if (m.matches() && m.group(2).equals("modId")) {
                String id = m.group(4).trim().replaceAll("^[\\\"']|[\\\"']\\s*(#.*)?$", "");
                if (!id.isBlank()) ids.add(id);
            }
        }
        return ids.size() == 1 ? ids.get(0) : null;
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }

    private static void flushDependency(StringBuilder out, List<String> block, boolean inDependency) {
        if (!inDependency || block.isEmpty()) return;
        String modId = null;
        for (String l : block) {
            Matcher m = KEY_LINE.matcher(l);
            if (m.matches() && m.group(2).equals("modId")) modId = m.group(4).replaceAll("^[\"']|[\"']\\s*(#.*)?$", "");
        }
        boolean wroteType = false;
        boolean hasType = block.stream().anyMatch(l -> {
            Matcher k = KEY_LINE.matcher(l);
            return k.matches() && k.group(2).equals("type");
        });
        for (String l : block) {
            Matcher m = KEY_LINE.matcher(l);
            if (!m.matches()) {
                out.append(l).append('\n');
                continue;
            }
            String key = m.group(2);
            if (key.equals("modId") && "forge".equals(modId)) out.append(m.group(1)).append("modId").append(m.group(3)).append("\"neoforge\"\n");
            else if (key.equals("versionRange") && "forge".equals(modId)) out.append(m.group(1)).append("versionRange").append(m.group(3)).append("\"[21.1,)\"\n");
            else if (key.equals("versionRange") && "minecraft".equals(modId)) out.append(m.group(1)).append("versionRange").append(m.group(3)).append("\"[1.21.1,1.22)\"\n");
            // other mods' 1.20.1 ranges ([1.20.1-x,1.21)) would reject their 1.21.1 versions: the dependency is kept, the range opened
            else if (key.equals("versionRange") && modId != null && !"minecraft".equals(modId) && !"forge".equals(modId)) out.append(m.group(1)).append("versionRange").append(m.group(3)).append("\"[0,)\"\n");
            else if (key.equals("mandatory") && hasType) continue; // both keys in one table: the file's own type stays (a second one is a TOML error)
            else if (key.equals("mandatory")) {
                out.append(m.group(1)).append("type").append(m.group(3)).append(m.group(4).strip().startsWith("true") ? "\"required\"" : "\"optional\"").append('\n');
                wroteType = true;
            } else out.append(l).append('\n');
        }
        if (!wroteType && block.stream().noneMatch(l -> l.strip().startsWith("type"))) out.append("type=\"required\"\n");
    }

    private static final Pattern AT_LINE = Pattern.compile("^(\\s*(?:public|protected|private|default)(?:[-+]f)?\\s+\\S+\\s+)([mf]_\\d+_)(.*)$");

    /** Forge access transformer: SRG member names to Mojang names (class names and descriptors are the same). */
    String accessTransformer(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\\R", -1)) {
            Matcher m = AT_LINE.matcher(line);
            if (m.matches() && srg.containsKey(m.group(2))) out.append(m.group(1)).append(srg.get(m.group(2))).append(m.group(3));
            else out.append(line);
            out.append('\n');
        }
        return out.toString();
    }

    private static boolean looksLikeMixinConfig(byte[] data) {
        if (data.length > 200_000) return false;
        String s = new String(data, StandardCharsets.UTF_8);
        return s.contains("\"package\"") && (s.contains("\"refmap\"") || s.contains("\"mixins\"") || s.contains("\"client\"") || s.contains("\"server\""));
    }

    /** Mixin refmaps translate Mojang names to SRG for production; 1.21.1 runs with Mojang names, which the annotations already use. */
    static String stripRefmap(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            root.remove("refmap");
            return root.toString();
        } catch (RuntimeException e) {
            return json.replaceAll("\"refmap\"\\s*:\\s*\"[^\"]*\"\\s*,?", "");
        }
    }
}
