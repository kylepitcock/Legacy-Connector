package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;

/** How an advancement shows (title, description, icon, frame, position): a view over the modern display info. */
public class DisplayInfo {
    private final net.minecraft.advancements.DisplayInfo info;

    public DisplayInfo(net.minecraft.advancements.DisplayInfo info) {
        this.info = info;
    }

    public ITextComponent getTitle() {
        return new TextComponentString(info.getTitle().getString());
    }

    public ITextComponent getDescription() {
        return new TextComponentString(info.getDescription().getString());
    }

    public ItemStack getIcon() {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(info.getIcon());
    }

    public ResourceLocation getBackground() {
        return info.getBackground().map(rl -> new ResourceLocation(rl.getNamespace(), rl.getPath())).orElse(null);
    }

    public FrameType getFrame() {
        return FrameType.mcc$of(info.getType());
    }

    public float getX() {
        return info.getX();
    }

    public float getY() {
        return info.getY();
    }

    public boolean shouldShowToast() {
        return info.shouldShowToast();
    }

    public boolean shouldAnnounceToChat() {
        return info.shouldAnnounceChat();
    }

    public boolean isHidden() {
        return info.isHidden();
    }
}
