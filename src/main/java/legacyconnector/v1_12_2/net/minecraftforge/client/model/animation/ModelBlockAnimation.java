package legacyconnector.v1_12_2.net.minecraftforge.client.model.animation;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResource;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Forge's armature of a vanilla json model ({@code armatures/...json}): which model elements each joint moves, with their weights. A
 * model without an armature file has the empty one. Animation clips of the file are not read: Forge's animated model rendering has no
 * bridge, so models draw in their rest pose.
 */
public class ModelBlockAnimation {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Animation");
    public static final ModelBlockAnimation defaultModelBlockAnimation = new ModelBlockAnimation(ImmutableMap.of(), ImmutableMap.of());

    private final ImmutableMap<String, ImmutableMap<String, float[]>> joints;
    private final ImmutableMap<String, Object> clips;

    public ModelBlockAnimation(ImmutableMap<String, ImmutableMap<String, float[]>> joints, ImmutableMap<String, ?> clips) {
        this.joints = joints;
        this.clips = ImmutableMap.copyOf(clips);
    }

    public ImmutableMap<String, ?> getClips() {
        return clips;
    }

    /** The joints by name: model element index to weights. */
    public ImmutableMap<String, ImmutableMap<String, float[]>> mcc$joints() {
        return joints;
    }

    /** Reads the armature file; a missing or broken one gives {@link #defaultModelBlockAnimation}. */
    public static ModelBlockAnimation loadVanillaAnimation(IResourceManager manager, ResourceLocation armatureLocation) {
        if (manager == null || armatureLocation == null) return defaultModelBlockAnimation;
        try {
            IResource resource;
            try {
                resource = manager.getResource(armatureLocation);
            } catch (java.io.FileNotFoundException e) {
                return defaultModelBlockAnimation;
            }
            JsonElement json;
            try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader);
            }
            if (!json.isJsonObject()) return defaultModelBlockAnimation;
            JsonObject obj = json.getAsJsonObject();
            ImmutableMap.Builder<String, ImmutableMap<String, float[]>> joints = ImmutableMap.builder();
            if (obj.has("joints")) {
                for (Map.Entry<String, JsonElement> joint : obj.getAsJsonObject("joints").entrySet()) {
                    ImmutableMap.Builder<String, float[]> weights = ImmutableMap.builder();
                    for (Map.Entry<String, JsonElement> e : joint.getValue().getAsJsonObject().entrySet()) {
                        var array = e.getValue().getAsJsonArray();
                        float[] w = new float[array.size()];
                        for (int i = 0; i < w.length; i++) w[i] = array.get(i).getAsFloat();
                        weights.put(e.getKey(), w);
                    }
                    joints.put(joint.getKey(), weights.build());
                }
            }
            return new ModelBlockAnimation(joints.build(), ImmutableMap.of());
        } catch (java.io.IOException | RuntimeException e) {
            LOGGER.debug("Could not read armature {}: {}", armatureLocation, e.toString());
            return defaultModelBlockAnimation;
        }
    }
}
