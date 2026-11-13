package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A small typed model of stringified NBT (the {@code {Potion:"minecraft:water",Damage:3s}} that 1.20.1 data files carry in recipe results, loot
 * functions and predicates), with a parser and a writer. The structured item migration ({@link ItemComponents1201}) works on it instead of on
 * regular expressions over the text.
 *
 * <p>Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux (the idea of a typed tag model for
 * the migration of item NBT; the parser and writer here are our own code).
 */
final class Snbt1201 {
    private Snbt1201() {}

    sealed interface Tag permits Compound, ListTag, Str, Num, ArrayTag {}

    record Str(String value) implements Tag {}

    /** A number with its NBT type: {@code b} byte, {@code s} short, {@code i} int, {@code l} long, {@code f} float, {@code d} double. */
    record Num(char type, BigDecimal value) implements Tag {
        boolean integral() {
            return type == 'b' || type == 's' || type == 'i' || type == 'l';
        }

        /** The value as a long when it is integral (or a whole floating point number), else null. */
        Long asLong() {
            try {
                return value.longValueExact();
            } catch (ArithmeticException e) {
                return null;
            }
        }
    }

    record ListTag(List<Tag> items) implements Tag {}

    /** {@code [B;...]}, {@code [I;...]} or {@code [L;...]}. */
    record ArrayTag(char kind, List<Num> values) implements Tag {}

    record Compound(LinkedHashMap<String, Tag> entries) implements Tag {
        Tag get(String key) {
            return entries.get(key);
        }

        String string(String key) {
            return entries.get(key) instanceof Str s ? s.value() : null;
        }
    }

    // ---- parsing ------------------------------------------------------------------------------------------------------------------

    private static final Pattern INT = Pattern.compile("[-+]?\\d+");
    private static final Pattern FLOAT = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");

    /** The compound a 1.20.1 data file's NBT field holds (an SNBT string, or a JSON object, which SNBT parses as well), or null when it is not one. */
    static Compound parseField(JsonElement nbt) {
        if (nbt == null) return null;
        String text = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
        return parseCompound(text);
    }

    static Compound parseCompound(String text) {
        try {
            Parser p = new Parser(text);
            Tag tag = p.value();
            p.ws();
            return p.pos == text.length() && tag instanceof Compound c ? c : null;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) {
            this.s = s;
        }

        void ws() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean eat(char c) {
            ws();
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        void expect(char c) {
            if (!eat(c)) throw new IllegalArgumentException("expected " + c + " at " + pos);
        }

        Tag value() {
            ws();
            if (pos >= s.length()) throw new IllegalArgumentException("end");
            char c = s.charAt(pos);
            if (c == '{') return compound();
            if (c == '[') return list();
            if (c == '"' || c == '\'') return new Str(quoted());
            return scalar(bare());
        }

        Compound compound() {
            expect('{');
            LinkedHashMap<String, Tag> map = new LinkedHashMap<>();
            if (eat('}')) return new Compound(map);
            do {
                ws();
                if (pos < s.length() && s.charAt(pos) == '}') break; // a trailing comma
                String key = pos < s.length() && (s.charAt(pos) == '"' || s.charAt(pos) == '\'') ? quoted() : bare();
                expect(':');
                map.put(key, value());
            } while (eat(','));
            expect('}');
            return new Compound(map);
        }

        Tag list() {
            expect('[');
            ws();
            if (pos + 1 < s.length() && s.charAt(pos + 1) == ';' && "BIL".indexOf(s.charAt(pos)) >= 0) {
                char kind = s.charAt(pos);
                pos += 2;
                List<Num> values = new ArrayList<>();
                if (!eat(']')) {
                    do {
                        ws();
                        if (pos < s.length() && s.charAt(pos) == ']') break;
                        if (!(scalar(bare()) instanceof Num n)) throw new IllegalArgumentException("number expected");
                        values.add(n);
                    } while (eat(','));
                    expect(']');
                }
                return new ArrayTag(kind, values);
            }
            List<Tag> items = new ArrayList<>();
            if (!eat(']')) {
                do {
                    ws();
                    if (pos < s.length() && s.charAt(pos) == ']') break;
                    items.add(value());
                } while (eat(','));
                expect(']');
            }
            return new ListTag(items);
        }

        String quoted() {
            char q = s.charAt(pos++);
            StringBuilder b = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\\' && pos < s.length()) {
                    char n = s.charAt(pos++);
                    b.append(n == 'n' ? '\n' : n == 't' ? '\t' : n);
                } else if (c == q) {
                    return b.toString();
                } else {
                    b.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        String bare() {
            ws();
            int start = pos;
            while (pos < s.length() && "{}[],:\"' \t\r\n".indexOf(s.charAt(pos)) < 0) pos++;
            if (pos == start) throw new IllegalArgumentException("token expected at " + pos);
            return s.substring(start, pos);
        }

        static Tag scalar(String t) {
            if (t.equalsIgnoreCase("true")) return new Num('b', BigDecimal.ONE);
            if (t.equalsIgnoreCase("false")) return new Num('b', BigDecimal.ZERO);
            char last = t.charAt(t.length() - 1);
            String body = t.substring(0, t.length() - 1);
            switch (Character.toLowerCase(last)) {
                case 'b' -> { if (INT.matcher(body).matches()) return new Num('b', new BigDecimal(body)); }
                case 's' -> { if (INT.matcher(body).matches()) return new Num('s', new BigDecimal(body)); }
                case 'l' -> { if (INT.matcher(body).matches()) return new Num('l', new BigDecimal(body)); }
                case 'f' -> { if (FLOAT.matcher(body).matches()) return new Num('f', new BigDecimal(body)); }
                case 'd' -> { if (FLOAT.matcher(body).matches()) return new Num('d', new BigDecimal(body)); }
                default -> { }
            }
            if (INT.matcher(t).matches()) return new Num('i', new BigDecimal(t));
            if (FLOAT.matcher(t).matches()) return new Num('d', new BigDecimal(t));
            return new Str(t);
        }
    }

    // ---- writing ------------------------------------------------------------------------------------------------------------------

    static String write(Tag tag) {
        StringBuilder b = new StringBuilder();
        write(tag, b);
        return b.toString();
    }

    private static void write(Tag tag, StringBuilder b) {
        switch (tag) {
            case Str s -> quote(s.value(), b);
            case Num n -> {
                String v = n.value().stripTrailingZeros().toPlainString();
                if (n.integral()) {
                    b.append(n.value().toBigInteger()).append(switch (n.type()) { case 'b' -> "b"; case 's' -> "s"; case 'l' -> "L"; default -> ""; });
                } else {
                    b.append(v.contains(".") ? v : v + ".0").append(n.type() == 'f' ? "f" : "d");
                }
            }
            case ListTag l -> {
                b.append('[');
                for (int i = 0; i < l.items().size(); i++) {
                    if (i > 0) b.append(',');
                    write(l.items().get(i), b);
                }
                b.append(']');
            }
            case ArrayTag a -> {
                b.append('[').append(a.kind()).append(';');
                for (int i = 0; i < a.values().size(); i++) {
                    if (i > 0) b.append(',');
                    write(a.values().get(i), b);
                }
                b.append(']');
            }
            case Compound c -> {
                b.append('{');
                boolean first = true;
                for (Map.Entry<String, Tag> e : c.entries().entrySet()) {
                    if (!first) b.append(',');
                    first = false;
                    if (e.getKey().matches("[A-Za-z0-9_.+-]+")) b.append(e.getKey());
                    else quote(e.getKey(), b);
                    b.append(':');
                    write(e.getValue(), b);
                }
                b.append('}');
            }
        }
    }

    private static void quote(String s, StringBuilder b) {
        b.append('"');
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        b.append('"');
    }
}
