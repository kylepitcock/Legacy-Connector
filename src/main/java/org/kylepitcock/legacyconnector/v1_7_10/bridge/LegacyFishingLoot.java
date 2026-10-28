package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraftforge.common.FishingHooks;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.LoggerFactory;

/**
 * Fish, junk and treasure that 1.7.10 mods added through {@code FishingHooks}: modern fishing rolls its own loot table,
 * then this modifier tells the category of the catch from the item and, with the weight share the mods' entries had in
 * that 1.7.10 category, replaces it with one of them. Other catches (modern-only fish, other mods' loot) stay.
 */
public final class LegacyFishingLoot extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "legacyconnector");
    public static final MapCodec<LegacyFishingLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, LegacyFishingLoot::new));
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<LegacyFishingLoot>> SERIALIZER =
            REGISTER.register("legacy_fishing", () -> CODEC);
    /** The data files that switch the modifier on (served by the generated data pack). */
    public static final String MODIFIER_JSON = "{\"type\":\"legacyconnector:legacy_fishing\",\"conditions\":[]}";
    public static final String LIST_JSON = "{\"replace\":false,\"entries\":[\"legacyconnector:legacy_fishing\"]}";

    public LegacyFishingLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (generatedLoot.size() != 1 || !BuiltInLootTables.FISHING.location().equals(context.getQueriedLootTableId())) return generatedLoot;
        ItemStack caught = generatedLoot.get(0);
        try {
            var legacy = FishingHooks.mcc$rollAdded(category(caught), new Random(context.getRandom().nextLong()));
            if (legacy == null) return generatedLoot;
            ItemStack modern = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(legacy);
            if (modern == null || modern.isEmpty()) return generatedLoot;
            generatedLoot.set(0, modern);
        } catch (RuntimeException | LinkageError e) {
            LoggerFactory.getLogger("LegacyConnector/Fishing").warn("A legacy fishing entry failed; the vanilla catch stays", e);
        }
        return generatedLoot;
    }

    private static FishingHooks.FishableCategory category(ItemStack caught) {
        if (caught.is(ItemTags.FISHES)) return FishingHooks.FishableCategory.FISH;
        if (caught.isEnchanted() || caught.is(Items.ENCHANTED_BOOK) || caught.is(Items.NAME_TAG) || caught.is(Items.SADDLE)
                || caught.is(Items.NAUTILUS_SHELL) || caught.is(Items.BOW)) {
            return FishingHooks.FishableCategory.TREASURE;
        }
        return FishingHooks.FishableCategory.JUNK;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
