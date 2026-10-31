package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockItemAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy block subtypes as blocks of their own, like the 1.13 flattening ("wool:3" became "light_blue_wool").
 *
 * <p>A legacy block is one modern block with its metadata as the {@code meta} property, and each subtype item is its own modern
 * item ({@code mod:name}, {@code mod:name_3}, see {@link ItemBridge}). Modern code takes a block item's block and its default state
 * (display stands per planks item, framed and copycat blocks, Cyclops Core's helpers): every subtype gave metadata 0. So each
 * metadata value an item subtype places ({@code ItemBlock.getMetadata(damage)}, not 0) gets a <em>member</em> block
 * {@code mod:name_N} whose default state is that metadata, and the subtype items point at it ({@code BlockItem.getBlock()},
 * {@code Item.byBlock}, {@code Block.asItem}).</p>
 *
 * <ul>
 *     <li>A member is the same legacy block (an {@link LegacyBlockAdapter} sharing every behaviour) and owns its key metadata plus the
 *     metadata values the legacy block drops as that subtype ({@code damageDropped}: a birch log's axis metas 5, 9, 13 go with 1), so
 *     rotating a log stays in its block. Metadata without an item of its own stays in the base block, which keeps its id and all 16
 *     values (saved worlds hold {@code mod:name[meta=N]} states).</li>
 *     <li>Legacy code reads and writes legacy block and metadata as before ({@link BlockBridge#toModern} picks the member); a metadata
 *     change that moves a state to another member of the same legacy block keeps the tile entity and calls no breakBlock/onBlockAdded
 *     (the adapter's onRemove/onPlace, the host type accepts every member, two mixins make scheduled ticks, block events and the client's
 *     block entity check accept any member).</li>
 *     <li>Saved worlds: states of the base block with a metadata a member now owns are moved into the member when their chunk loads
 *     (and back when {@code blocks.flattenSubtypes} is false; the members are registered either way, so no saved block is lost).</li>
 *     <li>Cost: at most 16 extra block states per legacy block (members split the 16 metadata values), one block per placed subtype.</li>
 * </ul>
 */
public final class BlockFlattening1710 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Blocks");
    private static boolean enabled;
    private static boolean listening;
    private static int throttledFailures;

    private BlockFlattening1710() {}

    /** Whether subtype blocks are in use (members are registered either way). */
    public static boolean enabled() {
        return enabled;
    }

    /**
     * Registers the subtype blocks of every legacy mod block once all legacy mods have loaded and every subtype item exists (called by
     * Platform1710 before the host block entity type is built, which must accept them). Returns the new blocks.
     */
    public static synchronized List<LegacyBlockAdapter> register() {
        enabled = LegacyConfig.flattenBlockSubtypes();
        List<LegacyBlockAdapter> all = new ArrayList<>();
        int families = 0, states = 0;
        for (Block b : BlockBridge.modBlocks()) {
            if (!(b.mcc$modern instanceof LegacyBlockAdapter base) || base.isDeepVariant() || base.isFlattenedMember() || !base.members().isEmpty()) continue;
            try {
                List<LegacyBlockAdapter> made = flatten(b, base);
                if (made.isEmpty()) continue;
                families++;
                all.addAll(made);
                for (LegacyBlockAdapter m : made) states += m.ownMetas().length;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Subtypes of {} stay in one block: {}", b, e.toString());
            }
        }
        if (!all.isEmpty()) listen();
        LOG.info("Flattened block subtypes: {} subtype block(s) of {} legacy block(s), {} block state(s){}", all.size(), families, states,
                enabled ? "" : " (blocks.flattenSubtypes = false: registered for saved worlds, not used)");
        return all;
    }

    private static List<LegacyBlockAdapter> flatten(Block block, LegacyBlockAdapter base) {
        Item item = ItemBridge.fromBlock(block);
        if (!(item instanceof ItemBlock ib) || item.mcc$variants.size() < 2) return List.of();
        // the block metadata each subtype item places -> those item subtypes (in order)
        Map<Integer, List<Integer>> itemsByMeta = new TreeMap<>();
        for (var e : item.mcc$variants.entrySet()) {
            if (!(e.getValue() instanceof LegacyBlockItemAdapter)) continue;
            int placed = placedMeta(ib, e.getKey());
            if (placed > 0) itemsByMeta.computeIfAbsent(placed, k -> new ArrayList<>()).add(e.getKey());
        }
        if (itemsByMeta.isEmpty()) return List.of();
        // which subtype each metadata value is: its own if an item places it, else the subtype it drops as, else the base
        int[] owner = new int[16];
        for (int m = 1; m < 16; m++) {
            if (itemsByMeta.containsKey(m)) {
                owner[m] = m;
                continue;
            }
            int dropped;
            try {
                dropped = placedMeta(ib, block.damageDropped(m));
            } catch (RuntimeException | LinkageError e) {
                dropped = 0;
            }
            owner[m] = dropped > 0 && itemsByMeta.containsKey(dropped) ? dropped : 0;
        }
        ResourceLocation baseId = BuiltInRegistries.BLOCK.getKey(base);
        LegacyBlockAdapter[] byMeta = new LegacyBlockAdapter[16];
        List<LegacyBlockAdapter> members = new ArrayList<>();
        for (var e : itemsByMeta.entrySet()) {
            int key = e.getKey();
            int[] metas = java.util.stream.IntStream.range(1, 16).filter(m -> owner[m] == key).toArray();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "_" + key);
            if (BuiltInRegistries.BLOCK.containsKey(id)) {
                LOG.debug("{} is taken: subtype {} of {} stays in the base block", id, key, baseId);
                continue;
            }
            LegacyBlockAdapter member = base.createMember(metas, key);
            LegacyRegistrar.register(BuiltInRegistries.BLOCK, id, (net.minecraft.world.level.block.Block) member);
            for (int m : metas) byMeta[m] = member;
            members.add(member);
            // the item of this metadata: the subtype whose damage is the metadata, else the first that places it
            List<Integer> subtypes = e.getValue();
            int named = subtypes.contains(key) ? key : subtypes.get(0);
            member.setMemberDescription(description(ib, named));
            net.minecraft.world.item.Item.BY_BLOCK.put(member, item.mcc$variants.get(named));
            if (enabled) {
                for (int d : subtypes) ((LegacyBlockItemAdapter) item.mcc$variants.get(d)).setFlattenedBlock(member);
            }
        }
        base.linkMembers(members, enabled ? byMeta : null);
        return members;
    }

    /** The metadata a subtype item places ({@code ItemBlock.getMetadata}, 4 bits as the 1.7.10 chunk stores it), -1 when it throws. */
    private static int placedMeta(ItemBlock ib, int damage) {
        try {
            return ib.getMetadata(damage) & 15;
        } catch (RuntimeException | LinkageError e) {
            return -1;
        }
    }

    /** The subtype's name key (1.7.10 named a placed block's subtype by its ItemBlock), or null for the block's own. */
    private static String description(ItemBlock ib, int damage) {
        try {
            String name = ib.getUnlocalizedName(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(ib, 1, damage));
            return name == null || name.isEmpty() ? null : name + ".name";
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ saved worlds

    /** A state that is not where legacy code would put it now (a base state whose metadata a member owns, or the reverse when off). */
    private static final Predicate<BlockState> MISPLACED = s -> s.getBlock() instanceof LegacyBlockAdapter a && hasFamily(a) && a.canonical(s) != s;

    private static boolean hasFamily(LegacyBlockAdapter a) {
        return !a.familyBase().members().isEmpty();
    }

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel && e.getChunk() instanceof LevelChunk chunk) normalize(chunk);
        });
    }

    /**
     * Moves the chunk's states of flattened legacy blocks into the block that holds them now (worlds saved before the flattening, or
     * with it switched the other way). Runs on the server thread as the chunk becomes a level chunk, before anything ticks it or a
     * client sees it; the section is written directly (same legacy block and metadata: no updates, light or shape change) and the
     * block entity is kept. Sections whose palette holds no such state are skipped.
     */
    static void normalize(LevelChunk chunk) {
        try {
            LevelChunkSection[] sections = chunk.getSections();
            int changed = 0;
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (section.hasOnlyAir() || !section.maybeHas(MISPLACED)) continue;
                int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState s = section.getBlockState(x, y, z);
                            if (!(s.getBlock() instanceof LegacyBlockAdapter a) || !hasFamily(a)) continue;
                            BlockState c = a.canonical(s);
                            if (c == s) continue;
                            section.setBlockState(x, y, z, c, false);
                            BlockEntity be = chunk.getBlockEntities().get(new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z));
                            if (be != null) be.setBlockState(c);
                            changed++;
                        }
                    }
                }
            }
            if (changed > 0) {
                chunk.setUnsaved(true);
                LOG.debug("Chunk {}: {} legacy block state(s) moved to their subtype blocks", chunk.getPos(), changed);
            }
        } catch (RuntimeException | LinkageError e) {
            if (throttledFailures++ < 5) LOG.warn("Subtype blocks of chunk {} not converted", chunk.getPos(), e);
        }
    }
}
