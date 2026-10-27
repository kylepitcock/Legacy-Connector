package legacyconnector.v1_12_2.net.minecraft.util.text;

import java.util.List;

/** A chat/tooltip text: a component with a style and appended siblings (iterating gives it and all its parts). */
public interface ITextComponent extends Iterable<ITextComponent> {
    ITextComponent setStyle(Style style);

    Style getStyle();

    ITextComponent appendText(String text);

    ITextComponent appendSibling(ITextComponent component);

    String getUnformattedComponentText();

    String getUnformattedText();

    String getFormattedText();

    List<ITextComponent> getSiblings();

    ITextComponent createCopy();

    /** The modern component (literal or translatable text, its style and siblings). */
    default net.minecraft.network.chat.MutableComponent mcc$modern() {
        var out = net.minecraft.network.chat.Component.literal(getUnformattedComponentText());
        out.setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }

    /** Text components as the 1.12 JSON text format (chat packets, signs, books, tellraw). */
    class Serializer implements com.google.gson.JsonDeserializer<ITextComponent>, com.google.gson.JsonSerializer<ITextComponent> {
        private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

        @Override
        public ITextComponent deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext ctx) {
            return mcc$fromTree(json);
        }

        @Override
        public com.google.gson.JsonElement serialize(ITextComponent src, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext ctx) {
            return mcc$toTree(src);
        }

        public static String componentToJson(ITextComponent component) {
            return GSON.toJson(mcc$toTree(component));
        }

        public static ITextComponent jsonToComponent(String json) {
            return mcc$fromTree(com.google.gson.JsonParser.parseString(json));
        }

        public static ITextComponent fromJsonLenient(String json) {
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(json));
            reader.setLenient(true);
            return mcc$fromTree(com.google.gson.JsonParser.parseReader(reader));
        }

        public static com.google.gson.JsonElement mcc$toTree(ITextComponent c) {
            if (c == null) return com.google.gson.JsonNull.INSTANCE;
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            if (!c.getStyle().isEmpty()) c.getStyle().mcc$writeJson(o);
            if (!c.getSiblings().isEmpty()) {
                com.google.gson.JsonArray extra = new com.google.gson.JsonArray();
                for (ITextComponent s : c.getSiblings()) extra.add(mcc$toTree(s));
                o.add("extra", extra);
            }
            if (c instanceof TextComponentTranslation t) {
                o.addProperty("translate", t.getKey());
                if (t.getFormatArgs() != null && t.getFormatArgs().length > 0) {
                    com.google.gson.JsonArray with = new com.google.gson.JsonArray();
                    for (Object arg : t.getFormatArgs()) {
                        if (arg instanceof ITextComponent ac) with.add(mcc$toTree(ac));
                        else with.add(new com.google.gson.JsonPrimitive(String.valueOf(arg)));
                    }
                    o.add("with", with);
                }
            } else {
                o.addProperty("text", c.getUnformattedComponentText());
            }
            return o;
        }

        public static ITextComponent mcc$fromTree(com.google.gson.JsonElement json) {
            if (json == null || json.isJsonNull()) return null;
            if (json.isJsonPrimitive()) return new TextComponentString(json.getAsString());
            if (json.isJsonArray()) {
                ITextComponent out = null;
                for (com.google.gson.JsonElement e : json.getAsJsonArray()) {
                    ITextComponent part = mcc$fromTree(e);
                    if (part == null) continue;
                    if (out == null) out = part;
                    else out.appendSibling(part);
                }
                return out;
            }
            if (!json.isJsonObject()) throw new com.google.gson.JsonParseException("Don't know how to turn " + json + " into a Component");
            com.google.gson.JsonObject o = json.getAsJsonObject();
            ITextComponent out;
            if (o.has("text")) {
                out = new TextComponentString(o.get("text").getAsString());
            } else if (o.has("translate")) {
                String key = o.get("translate").getAsString();
                if (o.has("with")) {
                    com.google.gson.JsonArray with = o.getAsJsonArray("with");
                    Object[] args = new Object[with.size()];
                    for (int i = 0; i < args.length; i++) {
                        ITextComponent a = mcc$fromTree(with.get(i));
                        args[i] = a instanceof TextComponentString ts && ts.getStyle().isEmpty() && ts.getSiblings().isEmpty() ? ts.getText() : a;
                    }
                    out = new TextComponentTranslation(key, args);
                } else {
                    out = new TextComponentTranslation(key);
                }
            } else if (o.has("keybind")) {
                out = new TextComponentKeybind(o.get("keybind").getAsString());
            } else if (o.has("score") && o.get("score").isJsonObject()) {
                com.google.gson.JsonObject score = o.getAsJsonObject("score");
                out = new TextComponentString(score.has("value") ? score.get("value").getAsString() : "");
            } else if (o.has("selector")) {
                out = new TextComponentString(o.get("selector").getAsString());
            } else {
                throw new com.google.gson.JsonParseException("Don't know how to turn " + json + " into a Component");
            }
            if (o.has("extra")) {
                com.google.gson.JsonArray extra = o.getAsJsonArray("extra");
                if (extra.size() <= 0) throw new com.google.gson.JsonParseException("Unexpected empty array of components");
                for (com.google.gson.JsonElement e : extra) {
                    ITextComponent part = mcc$fromTree(e);
                    if (part != null) out.appendSibling(part);
                }
            }
            out.setStyle(Style.mcc$readJson(o));
            return out;
        }
    }
}
