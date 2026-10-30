package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2 unbaked JSON block/item model: parent, texture variables, elements, display transforms, item overrides and the ambient occlusion
 * and gui3d flags, read with {@link #deserialize}. Texture variables resolve through the parent chain ({@link #resolveTextureName}).
 */
public class ModelBlock {
    private static final Logger LOGGER = LogManager.getLogger();
    /** Kept for mods that read it; {@link #deserialize} parses the JSON itself. */
    public static final Gson SERIALIZER = new Gson();
    private final List<BlockPart> elements;
    private final boolean gui3d;
    private final boolean ambientOcclusion;
    private final ItemCameraTransforms cameraTransforms;
    private final List<ItemOverride> overrides;
    public String name = "";
    protected final Map<String, String> textures;
    protected ModelBlock parent;
    protected ResourceLocation parentLocation;

    public static ModelBlock deserialize(Reader readerIn) {
        JsonObject json = JsonParser.parseReader(readerIn).getAsJsonObject();
        return fromJson(json);
    }

    public static ModelBlock deserialize(String jsonString) {
        return deserialize(new StringReader(jsonString));
    }

    public ModelBlock(ResourceLocation parentLocationIn, List<BlockPart> elementsIn, Map<String, String> texturesIn, boolean ambientOcclusionIn, boolean gui3dIn,
            ItemCameraTransforms cameraTransformsIn, List<ItemOverride> overridesIn) {
        this.elements = elementsIn;
        this.ambientOcclusion = ambientOcclusionIn;
        this.gui3d = gui3dIn;
        this.textures = texturesIn;
        this.parentLocation = parentLocationIn;
        this.cameraTransforms = cameraTransformsIn;
        this.overrides = overridesIn;
    }

    public List<BlockPart> getElements() {
        return elements.isEmpty() && hasParent() ? parent.getElements() : elements;
    }

    private boolean hasParent() {
        return parent != null;
    }

    public boolean isAmbientOcclusion() {
        return hasParent() ? parent.isAmbientOcclusion() : ambientOcclusion;
    }

    public boolean isGui3d() {
        return gui3d;
    }

    public boolean isResolved() {
        return parentLocation == null || parent != null && parent.isResolved();
    }

    public void getParentFromMap(Map<ResourceLocation, ModelBlock> p_178299_1_) {
        if (parentLocation != null) parent = p_178299_1_.get(parentLocation);
    }

    public List<ItemOverride> getOverrides() {
        return overrides;
    }

    public ItemOverrideList createOverrides() {
        return overrides.isEmpty() ? ItemOverrideList.NONE : new ItemOverrideList(overrides);
    }

    public boolean isTexturePresent(String textureName) {
        return !"missingno".equals(resolveTextureName(textureName));
    }

    public String resolveTextureName(String textureName) {
        if (!startsWithHash(textureName)) textureName = '#' + textureName;
        return resolveTextureName(textureName, new Bookkeep(this));
    }

    private String resolveTextureName(String textureName, Bookkeep bookkeep) {
        if (startsWithHash(textureName)) {
            if (this == bookkeep.modelExt) {
                LOGGER.warn("Unable to resolve texture due to upward reference: {} in {}", textureName, name);
                return "missingno";
            }
            String s = textures.get(textureName.substring(1));
            if (s == null && hasParent()) s = parent.resolveTextureName(textureName, bookkeep);
            bookkeep.modelExt = this;
            if (s != null && startsWithHash(s)) s = bookkeep.model.resolveTextureName(s, bookkeep);
            return s != null && !startsWithHash(s) ? s : "missingno";
        }
        return textureName;
    }

    private boolean startsWithHash(String hash) {
        return hash.charAt(0) == '#';
    }

    public ResourceLocation getParentLocation() {
        return parentLocation;
    }

    public ModelBlock getRootModel() {
        return hasParent() ? parent.getRootModel() : this;
    }

    public ItemCameraTransforms getAllTransforms() {
        return new ItemCameraTransforms(getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND),
                getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND), getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND),
                getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND), getTransform(ItemCameraTransforms.TransformType.HEAD),
                getTransform(ItemCameraTransforms.TransformType.GUI), getTransform(ItemCameraTransforms.TransformType.GROUND),
                getTransform(ItemCameraTransforms.TransformType.FIXED));
    }

    private ItemTransformVec3f getTransform(ItemCameraTransforms.TransformType type) {
        return parent != null && !cameraTransforms.hasCustomTransform(type) ? parent.getTransform(type) : cameraTransforms.getTransform(type);
    }

    public static void checkModelHierarchy(Map<ResourceLocation, ModelBlock> p_178312_0_) {
    }

    /** The model being resolved and the last model visited (detects texture variables that point back up). */
    public static final class Bookkeep {
        public final ModelBlock model;
        public ModelBlock modelExt;

        private Bookkeep(ModelBlock modelIn) {
            this.model = modelIn;
        }
    }

    // ------------------------------------------------------------------ JSON

    private static ModelBlock fromJson(JsonObject json) {
        List<BlockPart> elements = Lists.newArrayList();
        if (json.has("elements")) for (JsonElement e : json.getAsJsonArray("elements")) elements.add(part(e.getAsJsonObject()));
        Map<String, String> textures = Maps.newHashMap();
        if (json.has("textures")) {
            for (var e : json.getAsJsonObject("textures").entrySet()) textures.put(e.getKey(), e.getValue().getAsString());
        }
        ResourceLocation parent = json.has("parent") ? new ResourceLocation(json.get("parent").getAsString()) : null;
        boolean ao = !json.has("ambientocclusion") || json.get("ambientocclusion").getAsBoolean();
        ItemCameraTransforms transforms = ItemCameraTransforms.DEFAULT;
        if (json.has("display")) transforms = transforms(json.getAsJsonObject("display"));
        List<ItemOverride> overrides = Lists.newArrayList();
        if (json.has("overrides")) {
            for (JsonElement e : json.getAsJsonArray("overrides")) {
                JsonObject o = e.getAsJsonObject();
                Map<ResourceLocation, Float> predicate = Maps.newLinkedHashMap();
                if (o.has("predicate")) for (var p : o.getAsJsonObject("predicate").entrySet()) predicate.put(new ResourceLocation(p.getKey()), p.getValue().getAsFloat());
                overrides.add(new ItemOverride(new ResourceLocation(o.get("model").getAsString()), predicate));
            }
        }
        boolean gui3d = !json.has("gui3d") || json.get("gui3d").getAsBoolean();
        return new ModelBlock(parent, elements, textures, ao, gui3d, transforms, overrides);
    }

    private static ItemCameraTransforms transforms(JsonObject display) {
        return new ItemCameraTransforms(transform(display, "thirdperson_lefthand", "thirdperson_righthand", true),
                transform(display, "thirdperson_righthand", "thirdperson", false), transform(display, "firstperson_lefthand", "firstperson_righthand", true),
                transform(display, "firstperson_righthand", "firstperson", false), transform(display, "head", null, false), transform(display, "gui", null, false),
                transform(display, "ground", null, false), transform(display, "fixed", null, false));
    }

    /** A display transform; a missing left-hand transform mirrors the right-hand one, as 1.12 did. */
    private static ItemTransformVec3f transform(JsonObject display, String key, String fallback, boolean mirror) {
        JsonObject t = display.has(key) ? display.getAsJsonObject(key) : fallback != null && display.has(fallback) ? display.getAsJsonObject(fallback) : null;
        if (t == null) return ItemTransformVec3f.DEFAULT;
        float[] r = vec(t, "rotation", 0.0F), tr = vec(t, "translation", 0.0F), s = vec(t, "scale", 1.0F);
        for (int i = 0; i < 3; i++) tr[i] = Math.max(-5.0F, Math.min(5.0F, tr[i] * 0.0625F));
        for (int i = 0; i < 3; i++) s[i] = Math.max(-4.0F, Math.min(4.0F, s[i]));
        if (mirror && !display.has(key)) {
            r[1] = -r[1];
            r[2] = -r[2];
            tr[0] = -tr[0];
        }
        return new ItemTransformVec3f(new Vector3f(r[0], r[1], r[2]), new Vector3f(tr[0], tr[1], tr[2]), new Vector3f(s[0], s[1], s[2]));
    }

    private static float[] vec(JsonObject o, String key, float def) {
        if (!o.has(key)) return new float[] {def, def, def};
        JsonArray a = o.getAsJsonArray(key);
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static BlockPart part(JsonObject o) {
        float[] from = vec(o, "from", 0.0F), to = vec(o, "to", 16.0F);
        BlockPartRotation rotation = null;
        if (o.has("rotation")) {
            JsonObject r = o.getAsJsonObject("rotation");
            float[] origin = vec(r, "origin", 8.0F);
            EnumFacing.Axis axis = EnumFacing.Axis.byName(r.get("axis").getAsString().toLowerCase(java.util.Locale.ROOT));
            rotation = new BlockPartRotation(new Vector3f(origin[0], origin[1], origin[2]), axis, r.get("angle").getAsFloat(),
                    r.has("rescale") && r.get("rescale").getAsBoolean());
        }
        Map<EnumFacing, BlockPartFace> faces = new EnumMap<>(EnumFacing.class);
        if (o.has("faces")) {
            for (var e : o.getAsJsonObject("faces").entrySet()) {
                EnumFacing facing = EnumFacing.byName(e.getKey());
                if (facing == null) continue;
                JsonObject f = e.getValue().getAsJsonObject();
                EnumFacing cull = f.has("cullface") ? EnumFacing.byName(f.get("cullface").getAsString()) : null;
                int tint = f.has("tintindex") ? f.get("tintindex").getAsInt() : -1;
                String texture = f.get("texture").getAsString();
                float[] uv = null;
                if (f.has("uv")) {
                    JsonArray a = f.getAsJsonArray("uv");
                    uv = new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat()};
                }
                int rot = f.has("rotation") ? f.get("rotation").getAsInt() : 0;
                faces.put(facing, new BlockPartFace(cull, tint, texture, new BlockFaceUV(uv, rot)));
            }
        }
        boolean shade = !o.has("shade") || o.get("shade").getAsBoolean();
        return new BlockPart(new Vector3f(from[0], from[1], from[2]), new Vector3f(to[0], to[1], to[2]), faces, rotation, shade);
    }

    /** Gson adapter for model json (mods register it on their own Gson to read models). */
    public static class Deserializer implements com.google.gson.JsonDeserializer<ModelBlock> {
        @Override
        public ModelBlock deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            if (!json.isJsonObject()) throw new com.google.gson.JsonParseException("Model is not a json object");
            return fromJson(json.getAsJsonObject());
        }
    }
}
