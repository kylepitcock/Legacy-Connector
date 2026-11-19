package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;

/** Regression coverage for explicit NeoForge aliases that overlap legacy shim facade classes. */
class Forge1201ClassMappingTest {
    private static final String SHIM_PREFIX = "legacyconnector/v1_20_1/";

    private static ShimIndex shimWithFacades(String... names) {
        Map<String, ClassInfo> classes = new HashMap<>();
        for (String name : names) {
            classes.put(SHIM_PREFIX + name, new ClassInfo(
                    SHIM_PREFIX + name, "java/lang/Object", List.of(), 0));
        }
        return new ShimIndex(classes);
    }

    @Test
    void explicitTickEventAliasesTakePrecedenceOverLegacyFacades() {
        Forge1201Transformer transformer = new Forge1201Transformer(shimWithFacades(
                "net/minecraftforge/event/TickEvent$PlayerTickEvent",
                "net/minecraftforge/event/TickEvent$ServerTickEvent",
                "net/minecraftforge/event/TickEvent$RenderTickEvent",
                "net/minecraftforge/event/TickEvent$ClientTickEvent"));

        assertEquals(
                "net/neoforged/neoforge/event/tick/PlayerTickEvent$Post",
                transformer.mapClass("net/minecraftforge/event/TickEvent$PlayerTickEvent"));
        assertEquals(
                "net/neoforged/neoforge/event/tick/ServerTickEvent$Post",
                transformer.mapClass("net/minecraftforge/event/TickEvent$ServerTickEvent"));
        assertEquals(
                "net/neoforged/neoforge/client/event/RenderFrameEvent$Pre",
                transformer.mapClass("net/minecraftforge/event/TickEvent$RenderTickEvent"));

        // No curated alias exists for ClientTickEvent, so its legacy facade remains in use.
        assertEquals(
                SHIM_PREFIX + "net/minecraftforge/event/TickEvent$ClientTickEvent",
                transformer.mapClass("net/minecraftforge/event/TickEvent$ClientTickEvent"));
    }

    @Test
    void explicitOuterEventAliasIsAppliedToNestedClassesBeforeFacadeFallback() {
        Forge1201Transformer transformer = new Forge1201Transformer(shimWithFacades(
                "net/minecraftforge/client/event/RenderGuiOverlayEvent$Pre",
                "net/minecraftforge/client/event/RenderGuiOverlayEvent$Post"));

        assertEquals(
                "net/neoforged/neoforge/client/event/RenderGuiLayerEvent$Pre",
                transformer.mapClass("net/minecraftforge/client/event/RenderGuiOverlayEvent$Pre"));
        assertEquals(
                "net/neoforged/neoforge/client/event/RenderGuiLayerEvent$Post",
                transformer.mapClass("net/minecraftforge/client/event/RenderGuiOverlayEvent$Post"));
    }

    @Test
    void registrationOverlayEventKeepsTheLegacyFacadeForPalladiumCore() {
        Forge1201Transformer transformer = new Forge1201Transformer(shimWithFacades(
                "net/minecraftforge/client/event/RegisterGuiOverlaysEvent"));

        assertEquals(
                SHIM_PREFIX + "net/minecraftforge/client/event/RegisterGuiOverlaysEvent",
                transformer.mapClass("net/minecraftforge/client/event/RegisterGuiOverlaysEvent"));
    }

    @Test
    void vanillaOverlayIdentifiersUseNeoForgeLayersButNamedWrappersStayShimBacked() {
        Forge1201Transformer transformer = new Forge1201Transformer(shimWithFacades(
                "net/minecraftforge/client/gui/overlay/VanillaGuiOverlay",
                "net/minecraftforge/client/gui/overlay/NamedGuiOverlay"));

        assertEquals(
                "net/neoforged/neoforge/client/gui/VanillaGuiLayers",
                transformer.mapClass("net/minecraftforge/client/gui/overlay/VanillaGuiOverlay"));
        assertEquals(
                SHIM_PREFIX + "net/minecraftforge/client/gui/overlay/NamedGuiOverlay",
                transformer.mapClass("net/minecraftforge/client/gui/overlay/NamedGuiOverlay"));
    }
}
