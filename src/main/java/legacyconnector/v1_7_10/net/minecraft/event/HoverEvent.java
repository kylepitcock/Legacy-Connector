package legacyconnector.v1_7_10.net.minecraft.event;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** What hovering a chat component shows. Only text tooltips are translated. */
public class HoverEvent {
    public enum Action {
        SHOW_TEXT("show_text", true),
        SHOW_ACHIEVEMENT("show_achievement", true),
        SHOW_ITEM("show_item", true);

        private final String name;
        private final boolean allowedInChat;

        Action(String name, boolean allowedInChat) {
            this.name = name;
            this.allowedInChat = allowedInChat;
        }

        public boolean shouldAllowInChat() {
            return allowedInChat;
        }

        public String getCanonicalName() {
            return name;
        }

        public static Action getValueByCanonicalName(String name) {
            for (Action a : values()) if (a.name.equals(name)) return a;
            return null;
        }
    }

    private final Action action;
    private final IChatComponent value;

    public HoverEvent(Action action, IChatComponent value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public IChatComponent getValue() {
        return value;
    }

    public net.minecraft.network.chat.HoverEvent mcc$modern() {
        if (action != Action.SHOW_TEXT || value == null) return null;
        return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, value.mcc$toModern());
    }
}
