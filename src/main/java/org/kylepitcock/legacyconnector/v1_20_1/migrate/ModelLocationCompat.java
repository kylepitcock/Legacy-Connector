package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Map;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The boundary between 1.20.1's model locations and 1.21.1's.
 *
 * <p>In Forge 1.20.1 {@code ModelResourceLocation extends ResourceLocation} (path plus variant); in 1.21.1 it is an unrelated record ({@code id},
 * {@code variant}). Translated code sees {@link LegacyModelResourceLocation}, a ResourceLocation subclass that carries the variant (the loader maps the class
 * name), so it keeps compiling-shape: model locations passed as ResourceLocations, kept in {@code Map<ResourceLocation, ...>}, cast back and forth. Only calls into
 * the modern game convert (the call rewrite rows in {@code mc-calls.tsv} / {@code forge-call-rewrites.tsv} / {@code mc-fields.tsv} lead here).
 *
 * <p>Mapping, both directions:
 * <ul>
 *   <li>legacy key with a variant {@code ns:path#variant}  &lt;-&gt;  record {@code (ns:path, variant)};</li>
 *   <li>plain {@link ResourceLocation} key  &lt;-&gt;  record {@code (id, "standalone")}: 1.21.1 registers models through
 *       {@code ModelEvent.RegisterAdditional} as {@code ModelResourceLocation.standalone(id)}; in 1.20.1 those were plain locations in the same map. A plain
 *       key that finds no standalone model falls back to the {@code "inventory"} variant for lookups (never for writes).</li>
 * </ul>
 * Maps handed out by the baking events are live views ({@link LegacyModelMap}): reads convert keys, writes go through to the real map.
 */
public final class ModelLocationCompat {
    private ModelLocationCompat() {}

    /** The record for a key a legacy mod uses (a legacy model location, a plain location = standalone model, or already a record). */
    public static ModelResourceLocation modern(Object key) {
        if (key instanceof LegacyModelResourceLocation l) return new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(l.getNamespace(), l.getPath()), l.getVariant());
        if (key instanceof ModelResourceLocation m) return m;
        if (key instanceof ResourceLocation r) return ModelResourceLocation.standalone(plain(r));
        return null;
    }

    private static ResourceLocation plain(ResourceLocation r) {
        return r.getClass() == ResourceLocation.class ? r : ResourceLocation.fromNamespaceAndPath(r.getNamespace(), r.getPath());
    }

    /** The key legacy code sees for a record: a plain location for a standalone model, a legacy model location otherwise. */
    public static ResourceLocation legacy(ModelResourceLocation m) {
        if (m == null) return null;
        if (ModelResourceLocation.STANDALONE_VARIANT.equals(m.variant())) return m.id();
        return new LegacyModelResourceLocation(m.id().getNamespace(), m.id().getPath(), m.variant());
    }

    /** The model location of a record in the legacy class (always one, also for standalone ones: it is what a call that returns a ModelResourceLocation hands out). */
    public static LegacyModelResourceLocation legacyLocation(ModelResourceLocation m) {
        return m == null ? null : new LegacyModelResourceLocation(m.id().getNamespace(), m.id().getPath(), m.variant());
    }

    // ---- calls into the modern game -------------------------------------------------------------------------------------------------------

    /** {@code ModelEvent.ModifyBakingResult.getModels()} and {@code BakingCompleted.getModels()}. */
    public static Map<ResourceLocation, BakedModel> models(ModelEvent.ModifyBakingResult event) {
        return new LegacyModelMap(event.getModels());
    }

    public static Map<ResourceLocation, BakedModel> models(ModelEvent.BakingCompleted event) {
        return new LegacyModelMap(event.getModels());
    }

    /** {@code ModelEvent.RegisterAdditional.register(ResourceLocation)}. */
    public static void registerAdditional(ModelEvent.RegisterAdditional event, ResourceLocation model) {
        event.register(modern(model));
    }

    /** {@code ModelManager.getModel(ModelResourceLocation)}. */
    public static BakedModel getModel(ModelManager manager, LegacyModelResourceLocation location) {
        return manager.getModel(modern(location));
    }

    /** {@code ModelManager.getModel(ResourceLocation)} (Forge): a standalone model, else the inventory variant of the location. */
    public static BakedModel getModel(ModelManager manager, ResourceLocation id) {
        if (id instanceof LegacyModelResourceLocation l) return manager.getModel(modern(l));
        ResourceLocation plain = plain(id);
        BakedModel standalone = manager.getModel(ModelResourceLocation.standalone(plain));
        if (standalone != null && standalone != manager.getMissingModel()) return standalone;
        return manager.getModel(ModelResourceLocation.inventory(plain));
    }

    public static LegacyModelResourceLocation stateToModelLocation(BlockState state) {
        return legacyLocation(BlockModelShaper.stateToModelLocation(state));
    }

    public static LegacyModelResourceLocation stateToModelLocation(ResourceLocation id, BlockState state) {
        return legacyLocation(BlockModelShaper.stateToModelLocation(plain(id), state));
    }

    /** {@code ItemModelShaper.register(Item, ModelResourceLocation)}. */
    public static void registerItemModel(ItemModelShaper shaper, Item item, LegacyModelResourceLocation location) {
        shaper.register(item, modern(location));
    }

    /** Reads of the public static model-location constants of ItemRenderer ({@code TRIDENT_IN_HAND_MODEL}...): post step of the field rows. */
    public static LegacyModelResourceLocation constant(ModelResourceLocation m) {
        return legacyLocation(m);
    }
}
