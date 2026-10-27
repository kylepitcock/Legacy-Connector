package legacyconnector.v1_12_2.net.minecraft.util.text.event;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/**
 * What hovering a text shows: a text, an item or an entity. 1.12 kept items and entities as their NBT written into the text
 * value; they are turned into the modern item and entity tooltips.
 */
public class HoverEvent {
    private final Action action;
    private final ITextComponent value;

    public HoverEvent(Action actionIn, ITextComponent valueIn) {
        this.action = actionIn;
        this.value = valueIn;
    }

    public Action getAction() {
        return action;
    }

    public ITextComponent getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HoverEvent h && action == h.action && java.util.Objects.equals(value, h.value);
    }

    @Override
    public int hashCode() {
        return 31 * action.hashCode() + (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return "HoverEvent{action=" + action + ", value='" + value + "'}";
    }

    public net.minecraft.network.chat.HoverEvent mcc$modern() {
        String text = value == null ? "" : value.getUnformattedText();
        try {
            if (action == Action.SHOW_ITEM) {
                var tag = net.minecraft.nbt.TagParser.parseTag(text);
                var legacy = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(tag));
                var stack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(legacy);
                return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM,
                        new net.minecraft.network.chat.HoverEvent.ItemStackInfo(stack));
            }
            if (action == Action.SHOW_ENTITY) {
                var tag = net.minecraft.nbt.TagParser.parseTag(text);
                var type = net.minecraft.world.entity.EntityType.byString(tag.getString("type")).orElse(net.minecraft.world.entity.EntityType.PIG);
                java.util.UUID id;
                try {
                    id = java.util.UUID.fromString(tag.getString("id"));
                } catch (IllegalArgumentException e) {
                    id = new java.util.UUID(0, 0);
                }
                net.minecraft.network.chat.Component name = tag.contains("name") ? net.minecraft.network.chat.Component.literal(tag.getString("name")) : null;
                return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY,
                        new net.minecraft.network.chat.HoverEvent.EntityTooltipInfo(type, id, name));
            }
        } catch (Exception | LinkageError e) {
            // shown as text below
        }
        return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                value == null ? net.minecraft.network.chat.Component.empty() : value.mcc$modern());
    }

    public enum Action {
        SHOW_TEXT("show_text", true),
        SHOW_ACHIEVEMENT("show_achievement", true),
        SHOW_ITEM("show_item", true),
        SHOW_ENTITY("show_entity", true);

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
