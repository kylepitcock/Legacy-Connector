package legacyconnector.v1_7_10.net.minecraft.client.gui.achievement;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiOptionButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.gui.IProgressMeter;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;
import legacyconnector.v1_7_10.net.minecraft.stats.AchievementList;
import legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraftforge.common.AchievementPage;
import legacyconnector.v1_7_10.org.lwjgl.input.Mouse;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's achievement map with Forge's pages: the achievements of the page laid out on their grid over a terrain background,
 * lines to their parents, dimmed by how far they are from being unlockable, dragged to pan and zoomed with the wheel; hovering
 * one shows its name, description and requirement. The window texture of 1.7.10 is gone, so the frame is drawn plainly and the
 * frames of the achievements are the modern advancement frames. The achievements are the player's legacy ones (StatFileWriter).
 */
public class GuiAchievements extends GuiScreen implements IProgressMeter {
    protected static final int field_146572_y = AchievementList.minDisplayColumn * 24 - 112;
    protected static final int field_146571_z = AchievementList.minDisplayRow * 24 - 112;
    protected static final int field_146559_A = AchievementList.maxDisplayColumn * 24 - 77;
    protected static final int field_146560_B = AchievementList.maxDisplayRow * 24 - 77;
    protected static final ResourceLocation field_146561_C = new ResourceLocation("textures/gui/achievement/achievement_background.png");
    protected GuiScreen parentScreen;
    protected int field_146555_f = 256;
    protected int field_146557_g = 202;
    protected int field_146563_h;
    protected int field_146564_i;
    protected float field_146570_r = 1.0F;
    protected double field_146569_s;
    protected double field_146568_t;
    protected double field_146567_u;
    protected double field_146566_v;
    protected double field_146565_w;
    protected double field_146573_x;
    protected int field_146554_D;
    protected StatFileWriter statFileWriter;
    protected boolean loadingAchievements = false;
    /** Forge: the page shown (-1 is Minecraft's), the button cycling it and the achievements no page claims. */
    private int currentPage = -1;
    private GuiButton button;
    private final LinkedList<Achievement> minecraftAchievements = new LinkedList<>();

    public GuiAchievements(GuiScreen parent, StatFileWriter writer) {
        this.parentScreen = parent;
        this.statFileWriter = writer;
        int w = 141;
        int h = 141;
        this.field_146569_s = this.field_146567_u = this.field_146565_w = AchievementList.openInventory.displayColumn * 24 - w / 2 - 12;
        this.field_146568_t = this.field_146566_v = this.field_146573_x = AchievementList.openInventory.displayRow * 24 - h / 2;
        for (Achievement a : AchievementList.achievementList) {
            if (!AchievementPage.isAchievementInPages(a)) minecraftAchievements.add(a);
        }
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiOptionButton(1, width / 2 + 24, height / 2 + 74, 80, 20, I18n.format("gui.done")));
        buttonList.add(button = new GuiButton(2, (width - field_146555_f) / 2 + 24, height / 2 + 74, 125, 20, AchievementPage.getTitle(currentPage)));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (loadingAchievements) return;
        if (b.id == 1) {
            mc.displayGuiScreen(parentScreen);
        } else if (b.id == 2) {
            currentPage++;
            if (currentPage >= AchievementPage.getAchievementPages().size()) currentPage = -1;
            button.displayString = AchievementPage.getTitle(currentPage);
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        var inventoryKey = net.minecraft.client.Minecraft.getInstance().options.keyInventory;
        if (keyCode != 0 && org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.toGlfw(keyCode) == inventoryKey.getKey().getValue()) {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        } else {
            super.keyTyped(c, keyCode);
        }
    }

    private List<Achievement> pageAchievements() {
        return currentPage == -1 ? minecraftAchievements : AchievementPage.getAchievementPage(currentPage).getAchievements();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (loadingAchievements) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, I18n.format("multiplayer.downloadingStats"), width / 2, height / 2, 16777215);
            drawCenteredString(fontRendererObj, lanSearchStates[(int) (Minecraft.getSystemTime() / 150L % lanSearchStates.length)], width / 2,
                    height / 2 + fontRendererObj.FONT_HEIGHT * 2, 16777215);
            return;
        }
        if (Mouse.isButtonDown(0)) {
            int left = (width - field_146555_f) / 2 + 8;
            int top = (height - field_146557_g) / 2 + 17;
            if ((field_146554_D == 0 || field_146554_D == 1) && mouseX >= left && mouseX < left + 224 && mouseY >= top && mouseY < top + 155) {
                if (field_146554_D == 0) {
                    field_146554_D = 1;
                } else {
                    field_146567_u -= (mouseX - field_146563_h) * field_146570_r;
                    field_146566_v -= (mouseY - field_146564_i) * field_146570_r;
                    field_146565_w = field_146569_s = field_146567_u;
                    field_146573_x = field_146568_t = field_146566_v;
                }
                field_146563_h = mouseX;
                field_146564_i = mouseY;
            }
        } else {
            field_146554_D = 0;
        }
        int wheel = GuiScreen.mcc$frameWheel();
        float oldZoom = field_146570_r;
        if (wheel < 0) field_146570_r += 0.25F;
        else if (wheel > 0) field_146570_r -= 0.25F;
        field_146570_r = MathHelper.clamp_float(field_146570_r, 1.0F, 2.0F);
        if (field_146570_r != oldZoom) {
            float oldW = oldZoom * field_146555_f;
            float oldH = oldZoom * field_146557_g;
            float newW = field_146570_r * field_146555_f;
            float newH = field_146570_r * field_146557_g;
            field_146567_u -= (newW - oldW) * 0.5F;
            field_146566_v -= (newH - oldH) * 0.5F;
            field_146565_w = field_146569_s = field_146567_u;
            field_146573_x = field_146568_t = field_146566_v;
        }
        if (field_146565_w < field_146572_y) field_146565_w = field_146572_y;
        if (field_146573_x < field_146571_z) field_146573_x = field_146571_z;
        if (field_146565_w >= field_146559_A) field_146565_w = field_146559_A - 1;
        if (field_146573_x >= field_146560_B) field_146573_x = field_146560_B - 1;
        drawDefaultBackground();
        drawAchievementScreen(mouseX, mouseY, partialTicks);
        drawTitle();
    }

    @Override
    public void doneLoading() {
        loadingAchievements = false;
    }

    @Override
    public void updateScreen() {
        if (loadingAchievements) return;
        field_146569_s = field_146567_u;
        field_146568_t = field_146566_v;
        double dx = field_146565_w - field_146567_u;
        double dy = field_146573_x - field_146566_v;
        if (dx * dx + dy * dy < 4.0D) {
            field_146567_u += dx;
            field_146566_v += dy;
        } else {
            field_146567_u += dx * 0.85D;
            field_146566_v += dy * 0.85D;
        }
    }

    protected void drawTitle() {
        int left = (width - field_146555_f) / 2;
        int top = (height - field_146557_g) / 2;
        fontRendererObj.drawString(I18n.format("gui.achievements"), left + 15, top + 5, 4210752);
    }

    private boolean unlocked(Achievement a) {
        return statFileWriter != null && statFileWriter.hasAchievementUnlocked(a);
    }

    private boolean canUnlock(Achievement a) {
        return statFileWriter == null ? a.parentAchievement == null : statFileWriter.canUnlockAchievement(a);
    }

    private int stepsAway(Achievement a) {
        if (statFileWriter != null) return statFileWriter.func_150874_c(a);
        int n = 0;
        for (Achievement p = a.parentAchievement; p != null; p = p.parentAchievement) n++;
        return n;
    }

    private static net.minecraft.client.renderer.texture.TextureAtlasSprite blockSprite(String name) {
        return net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/" + name));
    }

    protected void drawAchievementScreen(int mouseX, int mouseY, float partialTicks) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        int scrollX = MathHelper.floor_double(field_146569_s + (field_146567_u - field_146569_s) * partialTicks);
        int scrollY = MathHelper.floor_double(field_146568_t + (field_146566_v - field_146568_t) * partialTicks);
        if (scrollX < field_146572_y) scrollX = field_146572_y;
        if (scrollY < field_146571_z) scrollY = field_146571_z;
        if (scrollX >= field_146559_A) scrollX = field_146559_A - 1;
        if (scrollY >= field_146560_B) scrollY = field_146560_B - 1;
        int windowLeft = (width - field_146555_f) / 2;
        int windowTop = (height - field_146557_g) / 2;
        int mapLeft = windowLeft + 16;
        int mapTop = windowTop + 17;
        List<Achievement> achievements = pageAchievements();
        Achievement hovered = null;

        gui.enableScissor(mapLeft, mapTop, mapLeft + 224, mapTop + 155);
        gui.pose().pushPose();
        try {
            gui.pose().translate(mapLeft, mapTop, 0);
            gui.pose().scale(1.0F / field_146570_r, 1.0F / field_146570_r, 1.0F);
            // terrain: deeper rows get stone, ores and finally bedrock, seeded by the player so it stays put
            int tileX = scrollX + 288 >> 4;
            int tileY = scrollY + 288 >> 4;
            int offX = (scrollX + 288) % 16;
            int offY = (scrollY + 288) % 16;
            float tileW = 16.0F / field_146570_r;
            Random random = new Random();
            String player = mc.getSession() == null ? "" : String.valueOf(mc.getSession().getPlayerID());
            for (int row = 0; row * tileW - offY < 155.0F; row++) {
                float shade = 0.6F - (tileY + row) / 25.0F * 0.3F;
                gui.setColor(shade, shade, shade, 1.0F);
                for (int col = 0; col * tileW - offX < 224.0F; col++) {
                    random.setSeed(player.hashCode() + tileX + col + (tileY + row) * 16L);
                    int depth = random.nextInt(1 + tileY + row) + (tileY + row) / 2;
                    String block = "sand";
                    if (depth <= 37 && tileY + row != 35) {
                        if (depth == 22) block = random.nextInt(2) == 0 ? "diamond_ore" : "redstone_ore";
                        else if (depth == 10) block = "iron_ore";
                        else if (depth == 8) block = "coal_ore";
                        else if (depth > 4) block = "stone";
                        else if (depth > 0) block = "dirt";
                    } else {
                        block = "bedrock";
                    }
                    gui.blit(col * 16 - offX, row * 16 - offY, 0, 16, 16, blockSprite(block));
                }
            }
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            // lines to the parents: grey once unlocked, green when unlockable, black otherwise
            for (Achievement a : achievements) {
                if (a.parentAchievement == null || !achievements.contains(a.parentAchievement)) continue;
                int x = a.displayColumn * 24 - scrollX + 11;
                int y = a.displayRow * 24 - scrollY + 11;
                int px = a.parentAchievement.displayColumn * 24 - scrollX + 11;
                int py = a.parentAchievement.displayRow * 24 - scrollY + 11;
                if (stepsAway(a) > 4) continue;
                int color = unlocked(a) ? -6250336 : canUnlock(a) ? -16711936 : -16777216;
                drawHorizontalLine(x, px, y, color);
                drawVerticalLine(px, y, py, color);
            }
            float mapMouseX = (mouseX - mapLeft) * field_146570_r;
            float mapMouseY = (mouseY - mapTop) * field_146570_r;
            for (Achievement a : achievements) {
                int x = a.displayColumn * 24 - scrollX;
                int y = a.displayRow * 24 - scrollY;
                if (x < -24 || y < -24 || x > 224.0F * field_146570_r || y > 155.0F * field_146570_r) continue;
                int steps = stepsAway(a);
                float shade;
                if (unlocked(a)) shade = 0.75F;
                else if (canUnlock(a)) shade = 1.0F;
                else if (steps < 3) shade = 0.3F;
                else if (steps == 3) shade = 0.2F;
                else if (steps == 4) shade = 0.1F;
                else continue;
                boolean done = unlocked(a);
                String frame = "advancements/" + (a.getSpecial() ? "challenge" : "task") + "_frame_" + (done ? "obtained" : "unobtained");
                gui.setColor(shade, shade, shade, 1.0F);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace(frame), x - 2, y - 2, 26, 26);
                gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                if (a.theItemStack != null) {
                    gui.renderItem(a.theItemStack.mcc$modern(), x + 3, y + 3);
                    // 1.7.10 drew achievements that cannot be unlocked yet as dark silhouettes
                    if (!canUnlock(a)) gui.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), x + 3, y + 3, x + 19, y + 19, 400, 0xE0101010);
                }
                if (mapMouseX >= x && mapMouseX <= x + 22 && mapMouseY >= y && mapMouseY <= y + 22) hovered = a;
            }
        } finally {
            gui.pose().popPose();
            gui.disableScissor();
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        drawWindowFrame(gui, windowLeft, windowTop, mapLeft, mapTop);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hovered != null) drawAchievementTooltip(hovered, mouseX, mouseY);
    }

    /** The grey window around the map (1.7.10 drew it from achievement_background.png, which no longer exists). */
    private void drawWindowFrame(net.minecraft.client.gui.GuiGraphics gui, int left, int top, int mapLeft, int mapTop) {
        int right = left + field_146555_f;
        int bottom = top + field_146557_g;
        int panel = 0xFFC6C6C6;
        gui.fill(left, top, right, mapTop, panel);
        gui.fill(left, mapTop + 155, right, bottom, panel);
        gui.fill(left, mapTop, mapLeft, mapTop + 155, panel);
        gui.fill(mapLeft + 224, mapTop, right, mapTop + 155, panel);
        // light top-left and dark bottom-right edges outside, the reverse around the map
        gui.fill(left, top, right, top + 1, 0xFFFFFFFF);
        gui.fill(left, top, left + 1, bottom, 0xFFFFFFFF);
        gui.fill(left, bottom - 1, right, bottom, 0xFF555555);
        gui.fill(right - 1, top, right, bottom, 0xFF555555);
        gui.fill(mapLeft - 1, mapTop - 1, mapLeft + 225, mapTop, 0xFF373737);
        gui.fill(mapLeft - 1, mapTop - 1, mapLeft, mapTop + 156, 0xFF373737);
        gui.fill(mapLeft - 1, mapTop + 155, mapLeft + 225, mapTop + 156, 0xFFFFFFFF);
        gui.fill(mapLeft + 224, mapTop - 1, mapLeft + 225, mapTop + 156, 0xFFFFFFFF);
    }

    private void drawAchievementTooltip(Achievement a, int mouseX, int mouseY) {
        String name = a.getStatName() == null ? a.statId : a.getStatName().getUnformattedText();
        String desc = a.getDescription();
        int x = mouseX + 12;
        int y = mouseY - 4;
        int steps = stepsAway(a);
        if (!canUnlock(a)) {
            if (steps > 3) return;
            if (steps == 3) name = I18n.format("achievement.unknown");
            int w = Math.max(fontRendererObj.getStringWidth(name), 120);
            String requires = new ChatComponentTranslation("achievement.requires", a.parentAchievement == null ? "" : a.parentAchievement.getStatName())
                    .getUnformattedText();
            int h = fontRendererObj.splitStringWidth(requires, w);
            drawGradientRect(x - 3, y - 3, x + w + 3, y + h + 12 + 3, -1073741824, -1073741824);
            fontRendererObj.drawSplitString(requires, x, y + 12, w, -9416624);
        } else {
            int w = Math.max(fontRendererObj.getStringWidth(name), 120);
            int h = fontRendererObj.splitStringWidth(desc, w);
            if (unlocked(a)) h += 12;
            drawGradientRect(x - 3, y - 3, x + w + 3, y + h + 3 + 12, -1073741824, -1073741824);
            fontRendererObj.drawSplitString(desc, x, y + 12, w, -6250336);
            if (unlocked(a)) fontRendererObj.drawStringWithShadow(I18n.format("achievement.taken"), x, y + h + 4, -7302913);
        }
        fontRendererObj.drawStringWithShadow(name, x, y, canUnlock(a) ? (a.getSpecial() ? -128 : -1) : (a.getSpecial() ? -8355776 : -8355712));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !loadingAchievements;
    }
}
