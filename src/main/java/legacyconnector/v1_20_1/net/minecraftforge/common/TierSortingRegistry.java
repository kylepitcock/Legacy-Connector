package legacyconnector.v1_20_1.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forge 1.20.1's tier ordering registry. NeoForge 1.21.1 replaced the ordering by an incorrect-blocks tag per tier ({@code Tier.getIncorrectBlocksForDrops}), so this
 * keeps the registrations (names and order) for the mods that read them and answers {@code isCorrectTierForDrops} from the tag.
 */
public final class TierSortingRegistry {
    private static final Map<ResourceLocation, Tier> BY_NAME = new LinkedHashMap<>();
    private static final Map<Tier, ResourceLocation> NAMES = new java.util.IdentityHashMap<>();
    private static final List<Tier> SORTED = new ArrayList<>();

    static {
        for (Tiers t : Tiers.values()) {
            ResourceLocation name = ResourceLocation.withDefaultNamespace(t.name().toLowerCase(java.util.Locale.ROOT));
            BY_NAME.put(name, t);
            NAMES.put(t, name);
            SORTED.add(t);
        }
    }

    private TierSortingRegistry() {}

    public static synchronized Tier registerTier(Tier tier, ResourceLocation name, List<Object> after, List<Object> before) {
        if (BY_NAME.containsKey(name)) return BY_NAME.get(name);
        BY_NAME.put(name, tier);
        NAMES.put(tier, name);
        int index = SORTED.size();
        for (Object b : before) {
            Tier t = resolve(b);
            if (t != null && SORTED.contains(t)) index = Math.min(index, SORTED.indexOf(t));
        }
        for (Object a : after) {
            Tier t = resolve(a);
            if (t != null && SORTED.contains(t)) index = Math.max(index, SORTED.indexOf(t) + 1);
        }
        SORTED.add(Math.min(index, SORTED.size()), tier);
        return tier;
    }

    @Nullable
    private static Tier resolve(Object o) {
        if (o instanceof Tier t) return t;
        if (o instanceof ResourceLocation rl) return BY_NAME.get(rl);
        return null;
    }

    public static synchronized List<Tier> getSortedTiers() {
        return List.copyOf(SORTED);
    }

    @Nullable
    public static synchronized Tier byName(ResourceLocation name) {
        return BY_NAME.get(name);
    }

    @Nullable
    public static synchronized ResourceLocation getName(Tier tier) {
        return NAMES.get(tier);
    }

    public static synchronized List<Tier> getTiersLowerThan(Tier tier) {
        int at = SORTED.indexOf(tier);
        return at <= 0 ? List.of() : List.copyOf(SORTED.subList(0, at));
    }

    public static boolean isTierSorted(Tier tier) {
        return NAMES.containsKey(tier);
    }

    public static boolean isCorrectTierForDrops(Tier tier, BlockState state) {
        return !state.is(tier.getIncorrectBlocksForDrops());
    }

    public static boolean isCorrectTierForDrops(BlockState state, Tier tier) {
        return isCorrectTierForDrops(tier, state);
    }
}
