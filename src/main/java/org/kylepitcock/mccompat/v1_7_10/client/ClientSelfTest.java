package org.kylepitcock.mccompat.v1_7_10.client;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.mccompat.v1_7_10.Assets1710;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client smoke test ({@code -Dretrofit.clientSelfTest}): once the title screen is shown (resources and models loaded),
 * reports per mod the textures generated models reference that no resource provides, and legacy items/blocks whose
 * baked model shows the missing texture; then quits. Without the property it does nothing.
 */
public final class ClientSelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/ClientTest");
    private static boolean done;

    private ClientSelfTest() {}

    public static void register() {
        if (System.getProperty("retrofit.clientSelfTest") == null) return;
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (done || !(e.getNewScreen() instanceof TitleScreen)) return;
            done = true;
            Minecraft.getInstance().tell(() -> {
                try {
                    run();
                } catch (RuntimeException ex) {
                    LOG.error("Client self test failed", ex);
                }
                Minecraft.getInstance().stop();
            });
        });
    }

    private static void run() {
        Minecraft mc = Minecraft.getInstance();
        var resources = mc.getResourceManager();
        Map<String, Set<String>> missingFiles = new TreeMap<>();
        for (var e : Assets1710.TEXTURE_REFS.entrySet()) {
            for (String ref : e.getValue()) {
                ResourceLocation tex = ResourceLocation.tryParse(ref);
                if (tex == null) continue;
                ResourceLocation file = tex.withPath(p -> "textures/" + p + ".png");
                if (resources.getResource(file).isEmpty()) missingFiles.computeIfAbsent(e.getKey(), k -> new TreeSet<>()).add(ref);
            }
        }
        var missing = MissingTextureAtlasSprite.getLocation();
        Map<String, int[]> perMod = new TreeMap<>(); // items total, items missing, blocks total, blocks missing
        Map<String, Set<String>> examples = new TreeMap<>();
        RandomSource random = RandomSource.create(0);
        for (Item item : ItemBridge.modItems()) {
            for (var modern : item.mcc$variants.values()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(modern);
                int[] c = perMod.computeIfAbsent(id.getNamespace(), k -> new int[4]);
                c[0]++;
                var model = mc.getItemRenderer().getModel(new net.minecraft.world.item.ItemStack(modern), null, null, 0);
                if (model.isCustomRenderer()) continue;
                if (shows(model.getQuads(null, null, random), missing) || model.getParticleIcon().contents().name().equals(missing)) {
                    c[1]++;
                    examples.computeIfAbsent(id.getNamespace(), k -> new TreeSet<>()).add(id.getPath());
                }
            }
        }
        // Spawn eggs (modern items, not legacy ones): the model must be the egg template with two layers, tinted.
        StringBuilder eggs = new StringBuilder();
        for (ResourceLocation id : org.kylepitcock.mccompat.v1_7_10.entity.LegacySpawnEggs.ids()) {
            var egg = BuiltInRegistries.ITEM.get(id);
            var stack = new net.minecraft.world.item.ItemStack(egg);
            var model = mc.getItemRenderer().getModel(stack, null, null, 0);
            var quads = model.getQuads(null, null, random);
            int c0 = mc.getItemColors().getColor(stack, 0), c1 = mc.getItemColors().getColor(stack, 1);
            boolean air = egg == net.minecraft.world.item.Items.AIR;
            if (air || quads.isEmpty() || shows(quads, missing) || (c0 & 0xFFFFFF) == 0xFFFFFF && (c1 & 0xFFFFFF) == 0xFFFFFF) {
                eggs.append(String.format("%n      egg %s: %s quads=%d colours=%08x/%08x", id, air ? "NOT IN REGISTRY" : "model", quads.size(), c0, c1));
            }
        }
        for (Block b : BlockBridge.modBlocks()) {
            if (b.mcc$modern == null) continue;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b.mcc$modern);
            int[] c = perMod.computeIfAbsent(id.getNamespace(), k -> new int[4]);
            c[2]++;
            var model = mc.getBlockRenderer().getBlockModel(b.mcc$modern.defaultBlockState());
            boolean bad = model.getParticleIcon().contents().name().equals(missing);
            for (Direction d : Direction.values()) bad |= shows(model.getQuads(b.mcc$modern.defaultBlockState(), d, random), missing);
            if (bad) {
                c[3]++;
                examples.computeIfAbsent(id.getNamespace(), k -> new TreeSet<>()).add("block:" + id.getPath());
            }
        }
        StringBuilder sb = new StringBuilder();
        int items = 0, itemsBad = 0, blocks = 0, blocksBad = 0, files = 0;
        for (var e : perMod.entrySet()) {
            int[] c = e.getValue();
            items += c[0];
            itemsBad += c[1];
            blocks += c[2];
            blocksBad += c[3];
            Set<String> mf = missingFiles.getOrDefault(e.getKey(), Set.of());
            files += mf.size();
            if (c[1] == 0 && c[3] == 0 && mf.isEmpty()) continue;
            sb.append(String.format("%n  %-26s items %4d/%-4d blocks %4d/%-4d missing texture files %d", e.getKey(), c[1], c[0], c[3], c[2], mf.size()));
            if (!mf.isEmpty()) sb.append("\n      files: ").append(mf.stream().limit(6).toList());
            Set<String> ex = examples.get(e.getKey());
            if (ex != null) sb.append("\n      e.g. ").append(ex.stream().limit(6).toList());
        }
        LOG.info("CLIENT SELF TEST: {} of {} items and {} of {} blocks show the missing texture; {} referenced texture files missing{}",
                itemsBad, items, blocksBad, blocks, files, sb.append(eggs));
    }

    private static boolean shows(java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads, ResourceLocation missing) {
        for (var q : quads) if (q.getSprite().contents().name().equals(missing)) return true;
        return false;
    }
}
