package org.kylepitcock.legacyconnector.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Model JSON files a mod reads itself (BuildCraft's variable-expression models: {@code "from": ["4 + y * 2", ...]} next to {@code "variables"},
 * {@code "rules"} or {@code "inlines"}) sit in {@code models/} beside the vanilla ones, and the game's model manager tries to load every file
 * there. Such a file is no model the game can use, and nothing in the game references it, so it loads as an empty model (the mod reads the file
 * through the resource manager itself) instead of logging a failed model load for each. Any other malformed model still fails as before.
 */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockModel$Deserializer")
public abstract class BlockExpressionModelMixin {
    private static final Logger LEGACYCONNECTOR$LOG = LoggerFactory.getLogger("Legacy Connector");

    @WrapMethod(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;")
    private BlockModel legacyconnector$leaveExpressionModels(JsonElement json, Type type, JsonDeserializationContext context, Operation<BlockModel> original) {
        try {
            return original.call(json, type, context);
        } catch (RuntimeException e) {
            if (json != null && json.isJsonObject()) {
                JsonObject o = json.getAsJsonObject();
                if (o.has("variables") || o.has("rules") || o.has("inlines")) {
                    LEGACYCONNECTOR$LOG.debug("Model JSON with expressions left to its mod: {}", e.toString());
                    return new BlockModel(null, List.of(), Map.of(), true, null, ItemTransforms.NO_TRANSFORMS, List.of());
                }
            }
            throw e;
        }
    }
}
