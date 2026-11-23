package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * GeckoLib animation files ({@code assets/<ns>/animations/*.animation.json}) of 1.20.1 mods whose vectors hold an easing name where a
 * number belongs ({@code "vector": ["linear", "linear", "linear"]}, a Blockbench export mistake, also {@code "scale": "linear"}).
 * GeckoLib 4.4 on 1.20.1 read such a component as an unknown Molang variable, which is 0; GeckoLib 4.9 on 1.21.1 fails the expression
 * ("Failed to parse expression 'linear'") and the animation does not load. The component becomes 0, which is what the old library computed.
 */
final class GeckoAnimations1201 {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Pattern EASING = Pattern.compile("linear|constant|step|catmullrom|bezier|ease(?:In|Out|InOut)[A-Z][A-Za-z]*");
    private static final String[] CHANNELS = {"rotation", "position", "scale"};

    private GeckoAnimations1201() {}

    /** The fixed file, or null when nothing in it needed a change (or it is no animation file). */
    static byte[] fix(byte[] data) {
        JsonElement root;
        try {
            String text = new String(data, StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) text = text.substring(1);
            if (!text.contains("\"animations\"")) return null;
            JsonReader reader = new JsonReader(new StringReader(text));
            reader.setLenient(true);
            root = JsonParser.parseReader(reader);
        } catch (RuntimeException e) {
            return null;
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("animations") || !root.getAsJsonObject().get("animations").isJsonObject()) return null;
        boolean changed = false;
        for (Map.Entry<String, JsonElement> an : root.getAsJsonObject().getAsJsonObject("animations").entrySet()) {
            if (!an.getValue().isJsonObject() || !an.getValue().getAsJsonObject().has("bones")
                    || !an.getValue().getAsJsonObject().get("bones").isJsonObject()) continue;
            for (Map.Entry<String, JsonElement> bone : an.getValue().getAsJsonObject().getAsJsonObject("bones").entrySet()) {
                if (!bone.getValue().isJsonObject()) continue;
                JsonObject b = bone.getValue().getAsJsonObject();
                for (String ch : CHANNELS) {
                    JsonElement v = b.get(ch);
                    if (v == null) continue;
                    if (isEasingName(v)) {
                        b.add(ch, new JsonPrimitive(0));
                        changed = true;
                    } else {
                        changed |= fixIn(v);
                    }
                }
            }
        }
        return changed ? GSON.toJson(root).getBytes(StandardCharsets.UTF_8) : null;
    }

    private static boolean isEasingName(JsonElement e) {
        return e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && EASING.matcher(e.getAsString().trim()).matches();
    }

    /** Replaces the easing names inside a channel value (vectors, keyframes, pre/post parts); the easing keys keep their names. */
    private static boolean fixIn(JsonElement e) {
        boolean changed = false;
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                JsonElement x = a.get(i);
                if (isEasingName(x)) {
                    a.set(i, new JsonPrimitive(0));
                    changed = true;
                } else {
                    changed |= fixIn(x);
                }
            }
        } else if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                String k = en.getKey();
                if (k.equals("easing") || k.equals("lerp_mode") || k.equals("easingArgs")) continue;
                if (isEasingName(en.getValue())) {
                    en.setValue(new JsonPrimitive(0));
                    changed = true;
                } else {
                    changed |= fixIn(en.getValue());
                }
            }
        }
        return changed;
    }
}
