package legacyconnector.v1_12_2.net.minecraft.util.text.event;

import java.util.Map;

/** What clicking a text does: open a link or file, run or suggest a command, turn a book page. */
public class ClickEvent {
    private final Action action;
    private final String value;

    public ClickEvent(Action theAction, String theValue) {
        this.action = theAction;
        this.value = theValue;
    }

    public Action getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClickEvent c && action == c.action && java.util.Objects.equals(value, c.value);
    }

    @Override
    public int hashCode() {
        return 31 * action.hashCode() + (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return "ClickEvent{action=" + action + ", value='" + value + "'}";
    }

    public net.minecraft.network.chat.ClickEvent mcc$modern() {
        net.minecraft.network.chat.ClickEvent.Action a = switch (action) {
            case OPEN_URL -> net.minecraft.network.chat.ClickEvent.Action.OPEN_URL;
            case OPEN_FILE -> net.minecraft.network.chat.ClickEvent.Action.OPEN_FILE;
            case RUN_COMMAND -> net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND;
            case SUGGEST_COMMAND -> net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND;
            case CHANGE_PAGE -> net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE;
        };
        return new net.minecraft.network.chat.ClickEvent(a, value == null ? "" : value);
    }

    public enum Action {
        OPEN_URL("open_url", true),
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        SUGGEST_COMMAND("suggest_command", true),
        CHANGE_PAGE("change_page", true);

        private static final Map<String, Action> NAME_MAPPING = new java.util.HashMap<>();
        private final boolean allowedInChat;
        private final String canonicalName;

        static {
            for (Action a : values()) NAME_MAPPING.put(a.canonicalName, a);
        }

        Action(String canonicalNameIn, boolean allowedInChatIn) {
            this.canonicalName = canonicalNameIn;
            this.allowedInChat = allowedInChatIn;
        }

        public boolean shouldAllowInChat() {
            return allowedInChat;
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public static Action getValueByCanonicalName(String canonicalNameIn) {
            return NAME_MAPPING.get(canonicalNameIn);
        }
    }
}
