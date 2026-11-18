package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.common.base.Splitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Client-side helpers of gap round 11 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv} and {@code mc-fields.tsv}. */
public final class Round11ClientCompat {
    private Round11ClientCompat() {}

    /** {@code ConfirmLinkScreen.confirmLinkNow(url, lastScreen, trusted)}: the screen comes first in 1.21.1. */
    public static void confirmLinkNow(String url, Screen lastScreen, boolean trusted) {
        ConfirmLinkScreen.confirmLinkNow(lastScreen, url, trusted);
    }

    public static void confirmLinkNow(String url, Screen lastScreen) {
        ConfirmLinkScreen.confirmLinkNow(lastScreen, url, true);
    }

    public static Button.OnPress confirmLink(String url, Screen lastScreen, boolean trusted) {
        return ConfirmLinkScreen.confirmLink(lastScreen, url, trusted);
    }

    public static Button.OnPress confirmLink(String url, Screen lastScreen) {
        return ConfirmLinkScreen.confirmLink(lastScreen, url, true);
    }

    /** A delta tracker answering one fixed partial tick (1.21.1's {@code Gui.renderSlot} takes the tracker, 1.20.1's the float). */
    private static DeltaTracker tracker(float partial) {
        return new DeltaTracker() {
            @Override
            public float getGameTimeDeltaTicks() {
                return partial;
            }

            @Override
            public float getGameTimeDeltaPartialTick(boolean runsNormally) {
                return partial;
            }

            @Override
            public float getRealtimeDeltaTicks() {
                return partial;
            }
        };
    }

    /** {@code Gui.renderSlot(graphics, x, y, partialTick, player, stack, seed)}: private in 1.21.1 (access transformer) with a delta tracker. */
    public static void guiRenderSlot(Gui gui, GuiGraphics graphics, int x, int y, float partialTick, Player player, ItemStack stack, int seed) {
        gui.renderSlot(graphics, x, y, tracker(partialTick), player, stack, seed);
    }

    /** A formatted text as one component (styles kept), for the label builder that takes components only. */
    private static Component component(FormattedText text) {
        if (text instanceof Component c) return c;
        MutableComponent out = Component.empty();
        text.visit((style, s) -> {
            out.append(Component.literal(s).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** {@code MultiLineLabel.create(font, text, maxWidth, maxRows)}. */
    public static MultiLineLabel multiLineLabel(Font font, FormattedText text, int maxWidth, int maxRows) {
        return MultiLineLabel.create(font, maxWidth, maxRows, component(text));
    }

    /** {@code ModelPart.Cube.compile(pose, buffer, light, overlay, r, g, b, a)}: one packed colour in 1.21.1. */
    public static void cubeCompile(ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        cube.compile(pose, buffer, light, overlay, FastColor.ARGB32.colorFromFloat(a, r, g, b));
    }

    /** {@code EditBox.moveCursorTo(pos)}: keeps the selection while shift is held, as the 1.20.1 key handling did. */
    public static void moveCursorTo(EditBox box, int pos) {
        box.moveCursorTo(pos, Screen.hasShiftDown());
    }

    private static AdvancementNode node(AdvancementHolder holder) {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getAdvancements().getTree().get(holder);
    }

    /** {@code AdvancementsScreen.getAdvancementWidget(advancement)}: the screen is keyed by tree nodes now; null when the advancement is not in the client tree. */
    public static AdvancementWidget advancementWidget(AdvancementsScreen screen, AdvancementHolder holder) {
        AdvancementNode n = node(holder);
        return n == null ? null : screen.getAdvancementWidget(n);
    }

    /** {@code ClientAdvancements.Listener.onUpdateAdvancementProgress(advancement, progress)}. */
    public static void advancementProgress(ClientAdvancements.Listener listener, AdvancementHolder holder, AdvancementProgress progress) {
        AdvancementNode n = node(holder);
        if (n != null) listener.onUpdateAdvancementProgress(n, progress);
    }

    /** {@code FilePackResources.SPLITTER}: the path splitter of the 1.20.1 class (a private constant that 1.21.1 no longer has). */
    public static Splitter filePackSplitter() {
        return Splitter.on('/').omitEmptyStrings().limit(3);
    }
}
