package org.kylepitcock.legacyconnector.compat.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IModIngredientRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * Ingredient types a 1.12.2 plugin registers for its own objects (gases, essentia, ...): each becomes a modern JEI ingredient type whose
 * helper and renderer call the legacy ones, so the ingredients show in recipe slots, tooltips and the ingredient list.
 */
final class ModIngredientTypes {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/JEI1122");

    private ModIngredientTypes() {}

    /** A bridged type: the modern type and renderer of a legacy ingredient class. */
    record Bridged<V>(Class<V> cls, IIngredientType<V> type, IIngredientRenderer<V> renderer) {}

    private static final Map<Class<?>, Bridged<?>> BRIDGED = new ConcurrentHashMap<>();

    static Bridged<?> of(Class<?> cls) {
        return cls == null ? null : BRIDGED.get(cls);
    }

    static void clear() {
        BRIDGED.clear();
    }

    record LegacyType<V>(Class<V> cls) implements IIngredientType<V> {
        @Override
        public Class<? extends V> getIngredientClass() {
            return cls;
        }
    }

    private static String sanitize(String s) {
        String out = s == null ? "" : s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return out.isEmpty() ? "unknown" : out;
    }

    @SuppressWarnings("unchecked")
    static <V> void register(IModIngredientRegistration registration, Class<?> rawClass,
            legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientHelper<V> helper,
            legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer<V> renderer, Collection<V> all) {
        Class<V> cls = (Class<V>) rawClass;
        var type = new LegacyType<>(cls);
        var modernHelper = new Helper<>(type, helper);
        var modernRenderer = new Renderer<>(renderer);
        List<V> list = all == null ? new ArrayList<>() : new ArrayList<>(all);
        Codec<V> codec = Codec.STRING.comapFlatMap(id -> {
            for (V v : list) if (id.equals(helper.getUniqueId(v))) return DataResult.success(v);
            return DataResult.error(() -> "Unknown " + cls.getSimpleName() + " " + id);
        }, helper::getUniqueId);
        try {
            registration.register(type, list, modernHelper, modernRenderer, codec);
            BRIDGED.put(cls, new Bridged<>(cls, type, modernRenderer));
            LOG.info("1.12.2 JEI ingredient type {}: {} ingredients", cls.getName(), list.size());
        } catch (RuntimeException e) {
            LOG.warn("JEI refused the 1.12.2 ingredient type {}: {}", cls.getName(), e.toString());
        }
    }

    static final class Helper<V> implements IIngredientHelper<V> {
        private final IIngredientType<V> type;
        private final legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientHelper<V> legacy;

        Helper(IIngredientType<V> type, legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientHelper<V> legacy) {
            this.type = type;
            this.legacy = legacy;
        }

        @Override
        public IIngredientType<V> getIngredientType() {
            return type;
        }

        @Override
        public String getDisplayName(V ingredient) {
            return legacy.getDisplayName(ingredient);
        }

        @Override
        public String getUniqueId(V ingredient, UidContext context) {
            return legacy.getUniqueId(ingredient);
        }

        @Override
        public String getWildcardId(V ingredient) {
            return legacy.getWildcardId(ingredient);
        }

        @Override
        public String getDisplayModId(V ingredient) {
            return legacy.getModId(ingredient);
        }

        @Override
        public ResourceLocation getResourceLocation(V ingredient) {
            return ResourceLocation.fromNamespaceAndPath(sanitize(legacy.getModId(ingredient)), sanitize(legacy.getResourceId(ingredient)));
        }

        @Override
        public V copyIngredient(V ingredient) {
            return legacy.copyIngredient(ingredient);
        }

        @Override
        public boolean isValidIngredient(V ingredient) {
            return legacy.isValidIngredient(ingredient);
        }

        @Override
        public String getErrorInfo(V ingredient) {
            return ingredient == null ? "null" : legacy.getErrorInfo(ingredient);
        }
    }

    static final class Renderer<V> implements IIngredientRenderer<V> {
        private final legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer<V> legacy;
        private final int width, height;

        Renderer(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer<V> legacy) {
            this(legacy, 16, 16);
        }

        Renderer(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer<V> legacy, int width, int height) {
            this.legacy = legacy;
            this.width = width;
            this.height = height;
        }

        /** The same renderer for a slot of another size (a legacy slot says how large its ingredient is drawn). */
        Renderer<V> sized(int w, int h) {
            return new Renderer<>(legacy, Math.max(1, w), Math.max(1, h));
        }

        @Override
        public void render(GuiGraphics graphics, V ingredient) {
            RenderContext.beginGui(graphics);
            try {
                legacy.render(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft(), 0, 0, ingredient);
            } catch (RuntimeException | LinkageError e) {
                if (FAILED.add(legacy.getClass())) LOG.debug("1.12.2 JEI renderer {} failed: {}", legacy.getClass().getName(), e.toString());
            } finally {
                RenderContext.end();
            }
        }

        @Override
        public List<Component> getTooltip(V ingredient, TooltipFlag flag) {
            List<Component> out = new ArrayList<>();
            try {
                var lines = legacy.getTooltip(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft(), ingredient,
                        legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag.mcc$of(flag.isAdvanced()));
                if (lines != null) for (String l : lines) out.add(Component.literal(l));
            } catch (RuntimeException | LinkageError e) {
                if (FAILED.add(legacy.getClass())) LOG.debug("1.12.2 JEI tooltip of {} failed: {}", legacy.getClass().getName(), e.toString());
            }
            return out;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private static final java.util.Set<Class<?>> FAILED = ConcurrentHashMap.newKeySet();
}
