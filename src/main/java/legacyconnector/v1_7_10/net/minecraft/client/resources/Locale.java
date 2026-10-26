package legacyconnector.v1_7_10.net.minecraft.client.resources;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * A translation table loaded from the {@code lang/<code>.lang} files of every resource domain, for a list of languages
 * (later ones override earlier ones). From the game's resource manager the tables also take the modern game's
 * {@code lang/<code>.json} files (vanilla and converted mod translations), read first.
 */
public class Locale {
    private static final Splitter splitter = Splitter.on('=').limit(2);
    private static final Pattern field_135031_c = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    Map field_135032_a = new HashMap();
    private boolean field_135029_d;

    public synchronized void loadLocaleDataFiles(IResourceManager manager, List languages) {
        field_135032_a.clear();
        for (Object o : languages) {
            String language = String.valueOf(o);
            if (manager instanceof org.kylepitcock.legacyconnector.v1_7_10.client.LegacySprites.ModResources) mcc$loadModern(language);
            String path = String.format("lang/%s.lang", language);
            Iterable<?> domains = manager.getResourceDomains();
            if (domains == null || !domains.iterator().hasNext()) domains = mcc$modernNamespaces();
            for (Object domain : domains) {
                try {
                    loadLocaleData(manager.getAllResources(new ResourceLocation(String.valueOf(domain), path)));
                } catch (IOException | RuntimeException ignored) {
                    // the domain has no such language
                }
            }
        }
        checkUnicode();
    }

    private static Iterable<?> mcc$modernNamespaces() {
        try {
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getNamespaces();
        } catch (RuntimeException | LinkageError e) {
            return List.of("minecraft");
        }
    }

    /** The modern game's translations for a language (all namespaces, every pack). */
    private void mcc$loadModern(String language) {
        try {
            var manager = net.minecraft.client.Minecraft.getInstance().getResourceManager();
            String file = "lang/" + language.toLowerCase(java.util.Locale.ROOT) + ".json";
            for (String namespace : manager.getNamespaces()) {
                for (var resource : manager.getResourceStack(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, file))) {
                    try (InputStream in = resource.open()) {
                        net.minecraft.locale.Language.loadFromJson(in, (key, value) -> field_135032_a.put(key, value));
                    } catch (IOException | RuntimeException ignored) {
                        // a broken file adds nothing
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // no client resources yet
        }
    }

    private void checkUnicode() {
        field_135029_d = false;
        int all = 0;
        int high = 0;
        for (Object o : field_135032_a.values()) {
            String s = (String) o;
            all += s.length();
            for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 256) high++;
        }
        field_135029_d = high / (float) Math.max(1, all) > 0.1F;
    }

    public boolean isUnicode() {
        return field_135029_d;
    }

    private void loadLocaleData(List resources) throws IOException {
        for (Object o : resources) {
            try (InputStream in = ((IResource) o).getInputStream()) {
                loadLocaleData(in);
            }
        }
    }

    private void loadLocaleData(InputStream in) throws IOException {
        for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\r?\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            Iterator<String> parts = splitter.split(line).iterator();
            String key = parts.next();
            if (!parts.hasNext()) continue;
            String value = field_135031_c.matcher(parts.next()).replaceAll("%$1s");
            field_135032_a.put(key, value);
        }
    }

    private String translateKeyPrivate(String key) {
        Object value = field_135032_a.get(key);
        return value == null ? key : (String) value;
    }

    public String formatMessage(String key, Object[] args) {
        String text = translateKeyPrivate(key);
        try {
            return String.format(text, args);
        } catch (IllegalFormatException e) {
            return "Format error: " + text;
        }
    }
}
