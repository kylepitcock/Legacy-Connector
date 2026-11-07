package legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Forge 1.20.1's vanilla HUD overlays. NeoForge 1.21.1 has its own (differently cut) list of layers: each constant maps to the layer that draws it, so
 * {@code id()} is what the layer APIs take, and the render events carry {@code type()}.
 */
public enum VanillaGuiOverlay {
    VIGNETTE(VanillaGuiLayers.CAMERA_OVERLAYS),
    SPYGLASS(VanillaGuiLayers.CAMERA_OVERLAYS),
    HELMET(VanillaGuiLayers.CAMERA_OVERLAYS),
    FROSTBITE(VanillaGuiLayers.CAMERA_OVERLAYS),
    PORTAL(VanillaGuiLayers.CAMERA_OVERLAYS),
    HOTBAR(VanillaGuiLayers.HOTBAR),
    CROSSHAIR(VanillaGuiLayers.CROSSHAIR),
    BOSS_EVENT_PROGRESS(VanillaGuiLayers.BOSS_OVERLAY),
    PLAYER_HEALTH(VanillaGuiLayers.PLAYER_HEALTH),
    ARMOR_LEVEL(VanillaGuiLayers.ARMOR_LEVEL),
    FOOD_LEVEL(VanillaGuiLayers.FOOD_LEVEL),
    MOUNT_HEALTH(VanillaGuiLayers.VEHICLE_HEALTH),
    AIR_LEVEL(VanillaGuiLayers.AIR_LEVEL),
    JUMP_BAR(VanillaGuiLayers.JUMP_METER),
    EXPERIENCE_BAR(VanillaGuiLayers.EXPERIENCE_BAR),
    ITEM_NAME(VanillaGuiLayers.SELECTED_ITEM_NAME),
    DEBUG_TEXT(VanillaGuiLayers.DEBUG_OVERLAY),
    FPS_GRAPH(VanillaGuiLayers.DEBUG_OVERLAY),
    POTION_ICONS(VanillaGuiLayers.EFFECTS),
    SUBTITLES(VanillaGuiLayers.SUBTITLE_OVERLAY),
    TITLE_TEXT(VanillaGuiLayers.TITLE),
    SCOREBOARD(VanillaGuiLayers.SCOREBOARD_SIDEBAR),
    CHAT_PANEL(VanillaGuiLayers.CHAT),
    PLAYER_LIST(VanillaGuiLayers.TAB_LIST),
    RECORD_OVERLAY(VanillaGuiLayers.OVERLAY_MESSAGE),
    SLEEP_FADE(VanillaGuiLayers.SLEEP_OVERLAY);

    private final ResourceLocation id;
    private final NamedGuiOverlay type;

    VanillaGuiOverlay(ResourceLocation id) {
        this.id = id;
        this.type = new NamedGuiOverlay(id, (gui, graphics, partialTick, width, height) -> {
        });
    }

    public ResourceLocation id() {
        return id;
    }

    public NamedGuiOverlay type() {
        return type;
    }

    /** The overlay a NeoForge layer id stands for (the first constant mapped to it), or null for a layer of a mod. */
    public static VanillaGuiOverlay byLayer(ResourceLocation layer) {
        for (VanillaGuiOverlay o : values()) if (o.id.equals(layer)) return o;
        return null;
    }
}
