package example.versionprobe;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.kylepitcock.legacyconnector.api.LegacyAddon;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyBlocks;
import org.kylepitcock.legacyconnector.api.LegacyCapabilities;
import org.kylepitcock.legacyconnector.api.LegacyContentHooks;
import org.kylepitcock.legacyconnector.api.LegacyItems;
import org.kylepitcock.legacyconnector.api.LegacyMod;
import org.kylepitcock.legacyconnector.api.LegacyPhase;
import org.kylepitcock.legacyconnector.api.LegacySupport;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs what the addon API sees: every legacy mod with its Minecraft version, and per mod a few of its items and blocks pushed
 * through the conversions (modern -> legacy -> modern), the content keys, numeric ids and the legacy version of a block state.
 * Lines start with {@code PROBE}; the last line of each phase is a {@code PROBE SUMMARY} with the ok/bad counters per version.
 */
public final class VersionProbeAddon implements LegacyAddon {
    private static final Logger LOG = LoggerFactory.getLogger("VersionProbe");
    private static final int SAMPLES_PER_MOD = 4;

    private final Map<LegacyVersion, int[]> counters = new EnumMap<>(LegacyVersion.class);
    private int capabilityRules;

    @Override
    public void onLegacyModsDiscovered() {
        LOG.info("PROBE phase DISCOVERED api={} mods={}", LegacyApi.API_VERSION, LegacyApi.mods().size());
        for (LegacyMod m : LegacyApi.mods()) {
            LOG.info("PROBE mod id={} legacyId={} version={} mc={} failed={}", m.id(), m.legacyId(), m.version(), m.minecraftVersion().id(), m.failed());
        }
        for (LegacyVersion v : LegacyVersion.values()) {
            LOG.info("PROBE count mc={} mods={}", v.id(), LegacyApi.mods(v).size());
        }
        // the support matrix as data: one line per version
        for (LegacyVersion v : LegacySupport.versions()) {
            StringBuilder sb = new StringBuilder();
            for (LegacySupport.Feature f : LegacySupport.Feature.values()) sb.append(f.name()).append('=').append(LegacySupport.of(f, v).level()).append(' ');
            LOG.info("PROBE support mc={} {}", v.id(), sb.toString().trim());
        }
    }

    @Override
    public void onLegacyModFailed(LegacyMod mod, String phase) {
        LOG.info("PROBE failed mod={} mc={} phase={} failedPhase={} missingDependencies={}", mod.id(), mod.minecraftVersion().id(), phase,
                mod.failedPhase().orElse("-"), mod.missingDependencies());
    }

    @Override
    public void onLegacyRegistered() {
        LOG.info("PROBE phase REGISTERED reached={}", LegacyApi.phase());
    }

    @Override
    public void registerCapabilities(LegacyCapabilities caps) {
        caps.forAnyTile(Capabilities.ItemHandler.BLOCK, (tile, side, host) -> {
            if (capabilityRules++ < 3) {
                LOG.info("PROBE capability adapter asked for tile {} (version {})", tile.getClass().getName(), LegacyBlocks.tileVersion(host).map(LegacyVersion::id).orElse("?"));
            }
            return null;
        });
        LOG.info("PROBE registerCapabilities called");
    }

    @Override
    public void onLegacyLoadComplete() {
        LOG.info("PROBE phase LOAD_COMPLETE reached={}", LegacyApi.phase());
        for (LegacyVersion v : LegacyVersion.values()) {
            // a modern item no legacy version had: the stand-in has a stable identity
            ResourceLocation copper = ResourceLocation.withDefaultNamespace("copper_ingot");
            var key = LegacyContentHooks.itemKey(copper, v);
            var key2 = LegacyContentHooks.itemKey(copper, v);
            var stack = LegacyContentHooks.legacyStack(copper, 3, v);
            boolean stable = key.isPresent() && key2.isPresent() && key.get().legacyName().equals(key2.get().legacyName()) && key.get().damage() == key2.get().damage();
            LOG.info("PROBE standin mc={} key={} damage={} stable={} legacyStack={}", v.id(), key.map(k -> k.legacyName()).orElse("-"), key.map(k -> k.damage()).orElse(-1),
                    stable, stack.map(s -> s.getClass().getSimpleName()).orElse("-"));
            count(v, stable && stack.isPresent());
            if (stack.isPresent()) {
                ItemStack back = LegacyItems.fromLegacy(stack.get());
                boolean ok = back.getItem() == BuiltInRegistries.ITEM.get(copper) && back.getCount() == 3;
                LOG.info("PROBE roundtrip mc={} copper_ingot x3 -> {} x{} {}", v.id(), BuiltInRegistries.ITEM.getKey(back.getItem()), back.getCount(), ok ? "ok" : "BAD");
                count(v, ok);
            }
            if (v.usesNumericIds() || v.isBeta()) {
                var id = key.flatMap(k -> LegacyContentHooks.numericItemId(k.legacyItem(), v).stream().boxed().findFirst());
                LOG.info("PROBE numericId mc={} copper_ingot={} back={}", v.id(), id.orElse(-1),
                        id.flatMap(i -> LegacyContentHooks.legacyItemById(i, v)).map(o -> o == key.get().legacyItem() ? "same" : "OTHER").orElse("-"));
            }
        }
        for (LegacyMod mod : LegacyApi.mods()) probe(mod);
        for (var e : counters.entrySet()) {
            LOG.info("PROBE SUMMARY mc={} ok={} bad={}", e.getKey().id(), e.getValue()[0], e.getValue()[1]);
        }
        LOG.info("PROBE DONE");
    }

    private void probe(LegacyMod mod) {
        LegacyVersion v = mod.minecraftVersion();
        int items = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(mod.id())) continue;
            if (items++ >= SAMPLES_PER_MOD) break;
            try {
                var key = LegacyContentHooks.itemKey(id, v);
                Object legacy = LegacyItems.toLegacy(new ItemStack(item, 2), v);
                ItemStack back = LegacyItems.fromLegacy(legacy);
                boolean versionOk = LegacyApi.versionOf(new ItemStack(item)).map(x -> x == v).orElse(false);
                boolean ok = key.isPresent() && legacy != null && back.getItem() == item && back.getCount() == 2 && versionOk;
                String nid = (v.usesNumericIds() || v.isBeta()) && legacy != null ? String.valueOf(LegacyContentHooks.numericItemId(legacy, v).orElse(-1)) : "-";
                LOG.info("PROBE item mod={} mc={} {} key={}:{} numeric={} -> {} {}", mod.id(), v.id(), id, key.map(k -> k.legacyName()).orElse("-"),
                        key.map(k -> k.damage()).orElse(-1), nid, BuiltInRegistries.ITEM.getKey(back.getItem()), ok ? "ok" : "BAD");
                count(v, ok);
            } catch (Throwable t) {
                LOG.info("PROBE item mod={} {} FAILED {}", mod.id(), id, t.toString());
                count(v, false);
            }
        }
        int blocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(mod.id())) continue;
            if (blocks++ >= SAMPLES_PER_MOD) break;
            try {
                var state = block.defaultBlockState();
                var owner = LegacyBlocks.versionOf(state);
                Object legacy = LegacyBlocks.legacyBlock(state, v).orElse(null);
                var meta = LegacyBlocks.legacyMeta(state, v);
                var back = legacy == null ? java.util.Optional.<net.minecraft.world.level.block.state.BlockState>empty() : LegacyBlocks.fromLegacy(legacy, meta.orElse(0));
                boolean ok = owner.isPresent() && owner.get() == v && LegacyApi.versionOf(state).map(x -> x == v).orElse(false) && legacy != null && back.isPresent() && back.get().getBlock() == block;
                String nid = (v.usesNumericIds() || v.isBeta()) && legacy != null ? String.valueOf(LegacyContentHooks.numericBlockId(legacy, v).orElse(-1)) : "-";
                LOG.info("PROBE block mod={} mc={} {} versionOf={} meta={} numeric={} -> {} {}", mod.id(), v.id(), id, owner.map(LegacyVersion::id).orElse("-"),
                        meta.isPresent() ? meta.getAsInt() : "-", nid, back.map(s -> BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()).orElse("-"), ok ? "ok" : "BAD");
                count(v, ok);
            } catch (Throwable t) {
                LOG.info("PROBE block mod={} {} FAILED {}", mod.id(), id, t.toString());
                count(v, false);
            }
        }
        // a class of the legacy mod resolved through the API (the loader of the mod's own classes)
        var viaApi = LegacyContentHooks.itemKey(BuiltInRegistries.ITEM.keySet().stream().filter(k -> k.getNamespace().equals(mod.id())).findFirst().orElse(ResourceLocation.withDefaultNamespace("stick")), v)
                .flatMap(k -> mod.classOf(k.legacyItem().getClass().getName()));
        LOG.info("PROBE classOf mod={} mc={} itemClassResolved={}", mod.id(), v.id(), viaApi.isPresent());
        var cls = mod.instance().map(o -> o.getClass().getName()).orElse("-");
        LOG.info("PROBE modclass mod={} mc={} instance={} classLoader={}", mod.id(), v.id(), cls, mod.classLoader() == null ? "-" : mod.classLoader().getClass().getSimpleName());
    }

    private void count(LegacyVersion v, boolean ok) {
        counters.computeIfAbsent(v, k -> new int[2])[ok ? 0 : 1]++;
    }
}
