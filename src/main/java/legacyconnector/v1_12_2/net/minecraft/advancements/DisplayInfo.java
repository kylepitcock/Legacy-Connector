package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** How an advancement shows: title, description, icon, frame, tab background, position in the tree and visibility. */
public class DisplayInfo {
    private final ITextComponent title;
    private final ITextComponent description;
    private final ItemStack icon;
    private final ResourceLocation background;
    private final FrameType frame;
    private final boolean showToast;
    private final boolean announceToChat;
    private final boolean hidden;
    private float x;
    private float y;

    public DisplayInfo(ItemStack icon, ITextComponent title, ITextComponent description, ResourceLocation background, FrameType frame, boolean showToast,
            boolean announceToChat, boolean hidden) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.background = background;
        this.frame = frame;
        this.showToast = showToast;
        this.announceToChat = announceToChat;
        this.hidden = hidden;
    }

    /** The legacy view of the game's display info (not a 1.12 method). */
    public static DisplayInfo mcc$of(net.minecraft.advancements.DisplayInfo d) {
        if (d == null) return null;
        var bg = d.getBackground().orElse(null);
        DisplayInfo out = new DisplayInfo(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(d.getIcon()),
                text(d.getTitle()), text(d.getDescription()), bg == null ? null : new ResourceLocation(bg.getNamespace(), bg.getPath()),
                FrameType.mcc$of(d.getType()), d.shouldShowToast(), d.shouldAnnounceChat(), d.isHidden());
        out.setPosition(d.getX(), d.getY());
        return out;
    }

    private static ITextComponent text(net.minecraft.network.chat.Component c) {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(c == null ? "" : c.getString());
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public ITextComponent getTitle() {
        return title;
    }

    public ITextComponent getDescription() {
        return description;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public ResourceLocation getBackground() {
        return background;
    }

    public FrameType getFrame() {
        return frame;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean shouldShowToast() {
        return showToast;
    }

    public boolean shouldAnnounceToChat() {
        return announceToChat;
    }

    public boolean isHidden() {
        return hidden;
    }
}
