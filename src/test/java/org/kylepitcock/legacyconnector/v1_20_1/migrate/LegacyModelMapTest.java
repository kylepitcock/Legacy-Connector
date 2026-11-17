package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LegacyModelMapTest {
    private static BakedModel model() {
        return (BakedModel) Proxy.newProxyInstance(BakedModel.class.getClassLoader(), new Class<?>[] {BakedModel.class}, (p, m, a) -> m.getName().equals("hashCode") ? System.identityHashCode(p) : m.getName().equals("equals") ? p == a[0] : null);
    }

    private final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mod", "thing");
    private final Map<ModelResourceLocation, BakedModel> real = new HashMap<>();
    private final BakedModel inventory = model();
    private final BakedModel standalone = model();
    private final BakedModel variant = model();
    private final Map<ResourceLocation, BakedModel> view = new LegacyModelMap(real);

    private void fill() {
        real.put(ModelResourceLocation.inventory(id), inventory);
        real.put(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("mod", "extra")), standalone);
        real.put(new ModelResourceLocation(id, "facing=north"), variant);
    }

    @Test
    void legacyClassIsAResourceLocationWithVariantSemantics() {
        LegacyModelResourceLocation a = new LegacyModelResourceLocation("mod", "thing", "Inventory");
        assertTrue(((Object) a) instanceof ResourceLocation);
        assertEquals("inventory", a.getVariant());
        assertEquals("mod:thing#inventory", a.toString());
        assertEquals("thing", a.getPath());
        assertEquals(a, new LegacyModelResourceLocation(id, "inventory"));
        assertEquals(a, new LegacyModelResourceLocation("mod:thing#inventory"));
        assertEquals(a.hashCode(), new LegacyModelResourceLocation("mod:thing#inventory").hashCode());
        assertNotEquals(a, new LegacyModelResourceLocation(id, "normal"));
        assertEquals("minecraft:stone#x", LegacyModelResourceLocation.vanilla("stone", "x").toString());
        assertEquals("", new LegacyModelResourceLocation("mod:thing").getVariant());
    }

    @Test
    void iterationYieldsLegacyKeys() {
        fill();
        Set<ResourceLocation> keys = new HashSet<>(view.keySet());
        assertEquals(3, keys.size());
        assertTrue(keys.contains(new LegacyModelResourceLocation(id, "inventory")));
        assertTrue(keys.contains(new LegacyModelResourceLocation(id, "facing=north")));
        // a standalone model shows as a plain location (1.20.1 had those as plain keys)
        ResourceLocation extra = ResourceLocation.fromNamespaceAndPath("mod", "extra");
        assertTrue(keys.contains(extra));
        for (ResourceLocation k : view.keySet()) {
            if (k.equals(extra)) assertFalse(k instanceof LegacyModelResourceLocation);
        }
    }

    @Test
    void getByLegacyAndPlainKeys() {
        fill();
        assertSame(inventory, view.get(new LegacyModelResourceLocation(id, "inventory")));
        assertSame(variant, view.get(new LegacyModelResourceLocation(id, "facing=north")));
        assertNull(view.get(new LegacyModelResourceLocation(id, "facing=south")));
        assertSame(standalone, view.get(ResourceLocation.fromNamespaceAndPath("mod", "extra")));
        // a plain location without a standalone model falls back to the inventory variant (lookups only)
        assertSame(inventory, view.get(id));
        assertTrue(view.containsKey(id));
        assertFalse(view.containsKey(ResourceLocation.fromNamespaceAndPath("mod", "absent")));
    }

    @Test
    void putReplacesTheRealEntryAndPlainKeyWritesStandalone() {
        fill();
        BakedModel wrapped = model();
        assertSame(inventory, view.put(new LegacyModelResourceLocation(id, "inventory"), wrapped));
        assertSame(wrapped, real.get(ModelResourceLocation.inventory(id)));
        assertEquals(3, real.size());
        BakedModel added = model();
        view.put(ResourceLocation.fromNamespaceAndPath("mod", "fresh"), added);
        assertSame(added, real.get(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("mod", "fresh"))));
        // computeIfPresent / replaceAll write through as well
        BakedModel again = model();
        view.computeIfPresent(new LegacyModelResourceLocation(id, "facing=north"), (k, v) -> again);
        assertSame(again, real.get(new ModelResourceLocation(id, "facing=north")));
        BakedModel all = model();
        view.replaceAll((k, v) -> all);
        for (BakedModel m : real.values()) assertSame(all, m);
        assertSame(all, view.remove(new LegacyModelResourceLocation(id, "facing=north")));
        assertFalse(real.containsKey(new ModelResourceLocation(id, "facing=north")));
    }

    @Test
    void conversionRoundTrip() {
        ModelResourceLocation m = new ModelResourceLocation(id, "inventory");
        assertEquals(m, ModelLocationCompat.modern(ModelLocationCompat.legacy(m)));
        assertTrue(ModelLocationCompat.legacy(ModelResourceLocation.standalone(id)).getClass() == ResourceLocation.class);
        assertEquals(ModelResourceLocation.standalone(id), ModelLocationCompat.modern(id));
        assertEquals(m, ModelLocationCompat.modern(new LegacyModelResourceLocation(id, "inventory")));
    }
}
