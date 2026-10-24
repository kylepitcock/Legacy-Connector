package legacyconnector.v1_7_10.net.minecraft.event;

/** What clicking a chat component does. */
public class ClickEvent {
    public enum Action {
        OPEN_URL("open_url", true),
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        TWITCH_USER_INFO("twitch_user_info", false),
        SUGGEST_COMMAND("suggest_command", true);

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
    private final String value;

    public ClickEvent(Action action, String value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }

    public net.minecraft.network.chat.ClickEvent mcc$modern() {
        return switch (action) {
            case OPEN_URL -> new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, value);
            case RUN_COMMAND -> new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, value);
            case SUGGEST_COMMAND -> new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, value);
            default -> null;
        };
    }
}
