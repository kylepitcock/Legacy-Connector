package org.kylepitcock.legacyconnector.loader.transform;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core shaders of a 1.20.1 mod on 1.21.1. A mod's shader sources import Minecraft's include files ({@code #moj_import <fog.glsl>}); the import resolves to
 * the 1.21.1 file, whose functions changed in 1.20.5: {@code fog_distance(mat4 modelViewMat, vec3 pos, int shape)} became
 * {@code fog_distance(vec3 pos, int shape)}. A source still calling the old form does not compile ("'fog_distance' : no matching overloaded function
 * found"), the shader registration throws, the client's first resource reload fails and the game stays on the loading screen.
 *
 * <p>The old overload is added behind the import, guarded so that a source and its own include files can both carry it. The {@code IViewRotMat} uniform
 * such shaders declare is no longer set by the game and keeps its identity default, which is right: 1.21.1 hands positions over without the view rotation.
 */
final class Shaders1201 {
    private Shaders1201() {}

    private static final Pattern SHADER_SOURCE = Pattern.compile("assets/[^/]+/shaders/.+\\.(vsh|fsh|glsl)");
    private static final Pattern FOG_IMPORT = Pattern.compile("(?m)^[ \\t]*#moj_import[ \\t]*<(?:minecraft:)?fog\\.glsl>[ \\t]*\\r?$");
    private static final Pattern OLD_FOG_CALL = Pattern.compile("\\bfog_distance\\s*\\(");

    /** 1.20.1's function, word for word. */
    static final String FOG_DISTANCE_1201 = """
            #ifndef LEGACYCONNECTOR_FOG_DISTANCE_1201
            #define LEGACYCONNECTOR_FOG_DISTANCE_1201
            // Legacy Connector: Minecraft 1.20.1's fog_distance(modelViewMat, pos, shape), removed in 1.20.5
            float fog_distance(mat4 modelViewMat, vec3 pos, int shape) {
                if (shape == 0) {
                    return length((modelViewMat * vec4(pos, 1.0)).xyz);
                } else {
                    float distXZ = length((modelViewMat * vec4(pos.x, 0.0, pos.z, 1.0)).xyz);
                    float distY = length((modelViewMat * vec4(0.0, pos.y, 0.0, 1.0)).xyz);
                    return max(distXZ, distY);
                }
            }
            #endif
            """;

    /** The resources with every shader source that imports fog.glsl and calls fog_distance given the 1.20.1 overload; the count goes to {@code changed[0]}. */
    static Map<String, byte[]> convertAll(Map<String, byte[]> resources, int[] changed) {
        Map<String, byte[]> out = null;
        for (Map.Entry<String, byte[]> e : resources.entrySet()) {
            if (!SHADER_SOURCE.matcher(e.getKey()).matches()) continue;
            String converted = convert(new String(e.getValue(), StandardCharsets.UTF_8));
            if (converted == null) continue;
            if (out == null) out = new LinkedHashMap<>(resources);
            out.put(e.getKey(), converted.getBytes(StandardCharsets.UTF_8));
            changed[0]++;
        }
        return out == null ? resources : out;
    }

    /** The source with the old overload behind its fog.glsl import, or null when it needs none. */
    static String convert(String source) {
        if (source.contains("LEGACYCONNECTOR_FOG_DISTANCE_1201") || !OLD_FOG_CALL.matcher(source).find()) return null;
        Matcher m = FOG_IMPORT.matcher(source);
        if (!m.find()) return null;
        int end = m.end();
        return source.substring(0, end) + "\n" + FOG_DISTANCE_1201 + source.substring(end);
    }
}
