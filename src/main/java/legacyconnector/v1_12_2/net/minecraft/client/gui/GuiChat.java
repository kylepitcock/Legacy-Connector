package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The chat input; displaying it shows the modern chat screen with the default text. */
public class GuiChat extends GuiScreen {
    protected GuiTextField inputField;
    private String defaultInputFieldText = "";

    public GuiChat() {
    }

    public GuiChat(String defaultText) {
        this.defaultInputFieldText = defaultText == null ? "" : defaultText;
    }

    public String mcc$defaultText() {
        return defaultInputFieldText;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** The chat's Tab completion; the block the player looks at is the target of block-position arguments. */
    public static class ChatTabCompleter extends legacyconnector.v1_12_2.net.minecraft.util.TabCompleter {
        private final legacyconnector.v1_12_2.net.minecraft.client.Minecraft client = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();

        public ChatTabCompleter(GuiTextField p_i46749_1_) {
            super(p_i46749_1_, false);
        }

        @Override
        public void complete() {
            super.complete();
            if (completions.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (String s : completions) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(s);
                }
                net.minecraft.client.Minecraft.getInstance().gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(sb.toString()));
            }
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getTargetBlockPos() {
            var hit = net.minecraft.client.Minecraft.getInstance().hitResult;
            if (hit instanceof net.minecraft.world.phys.BlockHitResult b && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(b.getBlockPos());
            }
            return null;
        }
    }
}
