package legacyconnector.v1_12_2.net.minecraftforge.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.gui.Gui;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiMainMenu;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Forge's update dot on a custom main menu's "Mods" button: the version checker's verdict over the loaded mods (an outdated mod first),
 * drawn from NeoForge's version-check icons at the button's right edge.
 */
public class NotificationModUpdateScreen extends GuiScreen {
    private static final ResourceLocation VERSION_CHECK_ICONS = new ResourceLocation("neoforge", "textures/gui/version_check_icons.png");
    private final GuiButton modButton;
    private net.neoforged.fml.VersionChecker.Status showNotification;

    public NotificationModUpdateScreen(GuiButton modButton) {
        this.modButton = modButton;
    }

    public static NotificationModUpdateScreen init(GuiMainMenu guiMainMenu, GuiButton modButton) {
        NotificationModUpdateScreen screen = new NotificationModUpdateScreen(modButton);
        if (guiMainMenu != null) screen.setGuiSize(guiMainMenu.width, guiMainMenu.height);
        screen.initGui();
        return screen;
    }

    @Override
    public void initGui() {
        if (showNotification != null) return;
        net.neoforged.fml.VersionChecker.Status found = null;
        try {
            for (var info : net.neoforged.fml.ModList.get().getMods()) {
                var status = net.neoforged.fml.VersionChecker.getResult(info).status();
                if (status == null || !status.shouldDraw()) continue;
                if (found == null || status == net.neoforged.fml.VersionChecker.Status.OUTDATED || status == net.neoforged.fml.VersionChecker.Status.BETA_OUTDATED) {
                    found = status;
                    if (status == net.neoforged.fml.VersionChecker.Status.OUTDATED) break;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            found = null;
        }
        showNotification = found;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (showNotification == null || !showNotification.shouldDraw() || modButton == null || !modButton.visible) return;
        mc().getTextureManager().bindTexture(VERSION_CHECK_ICONS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = modButton.x, y = modButton.y, w = modButton.width, h = modButton.height;
        boolean blink = showNotification.isAnimated() && (System.currentTimeMillis() / 800L & 1L) == 1L;
        Gui.drawModalRectWithCustomSizedTexture(x + w - (h / 2 + 4), y + (h / 2 - 4), showNotification.getSheetOffset() * 8, blink ? 8 : 0, 8, 8, 64, 16);
    }

    private static legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc() {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
    }
}
