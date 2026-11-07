package org.kylepitcock.legacyconnector.v1_12_2.advancement;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.advancements.Advancement;
import legacyconnector.v1_12_2.net.minecraft.advancements.CriteriaTriggers;
import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionInstance;
import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.server.PlayerAdvancements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The modern trigger {@code legacyconnector:legacy} of the criteria that use a trigger a 1.12.2 mod registered itself
 * ({@code CriteriaTriggers.register(new MyTrigger())}). The criterion carries the trigger's id and the original {@code conditions} JSON; when a
 * player's advancements start listening, the mod's {@link ICriterionTrigger#deserializeInstance} parses the conditions and the mod's trigger
 * gets the legacy listener ({@code addListener}). The mod keeps firing its trigger from its own code, as in 1.12; the legacy listener's grant
 * awards the criterion of the modern advancement. A trigger the mod never registered does nothing (logged once).
 */
public final class LegacyTrigger1122 implements CriterionTrigger<LegacyTrigger1122.Instance> {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Advancements1122");
    public static final LegacyTrigger1122 INSTANCE = new LegacyTrigger1122();
    private static final Gson GSON = new Gson();
    private static final JsonDeserializationContext CONTEXT = new JsonDeserializationContext() {
        @Override
        public <T> T deserialize(JsonElement json, Type typeOfT) {
            return GSON.fromJson(json, typeOfT);
        }
    };
    private static final Codec<JsonElement> JSON = Codec.PASSTHROUGH.xmap(d -> d.convert(JsonOps.INSTANCE).getValue(), j -> new Dynamic<>(JsonOps.INSTANCE, j));
    private static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("trigger").forGetter(Instance::trigger),
            JSON.optionalFieldOf("conditions", new JsonObject()).forGetter(Instance::conditions)).apply(i, Instance::new));
    private static final Set<String> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The modern listener to the legacy one handed to the mod's trigger (so it can be removed again). */
    private final Map<PlayerAdvancements, Map<Listener<Instance>, ICriterionTrigger.Listener<ICriterionInstance>>> registered = new java.util.WeakHashMap<>();

    private LegacyTrigger1122() {}

    public static final class Instance implements CriterionTriggerInstance {
        private final String trigger;
        private final JsonElement conditions;

        public Instance(String trigger, JsonElement conditions) {
            this.trigger = trigger;
            this.conditions = conditions;
        }

        public String trigger() {
            return trigger;
        }

        public JsonElement conditions() {
            return conditions;
        }

        @Override
        public void validate(CriterionValidator validator) {
        }
    }

    @Override
    public void addPlayerListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        Instance instance = listener.trigger();
        ICriterionTrigger<ICriterionInstance> legacy = CriteriaTriggers.get(new ResourceLocation(instance.trigger()));
        if (legacy == null) {
            if (REPORTED.add(instance.trigger())) LOG.warn("The advancement trigger {} was never registered by a 1.12.2 mod; its criteria can not be met", instance.trigger());
            return;
        }
        var legacyAdvancements = legacyAdvancements(advancements);
        if (legacyAdvancements == null) return;
        ICriterionInstance parsed;
        try {
            JsonObject json = instance.conditions().isJsonObject() ? instance.conditions().getAsJsonObject() : new JsonObject();
            parsed = legacy.deserializeInstance(json, CONTEXT);
        } catch (RuntimeException | LinkageError e) {
            if (REPORTED.add(instance.trigger() + "!")) LOG.warn("Cannot read the conditions of the advancement trigger {}: {}", instance.trigger(), e.toString());
            return;
        }
        if (parsed == null) return;
        var legacyListener = new ICriterionTrigger.Listener<ICriterionInstance>(parsed, Advancement.mcc$of(listener.advancement()), listener.criterion());
        legacyListener.mcc$grant = () -> listener.run(advancements);
        synchronized (registered) {
            registered.computeIfAbsent(advancements, k -> new IdentityHashMap<>()).put(listener, legacyListener);
        }
        try {
            legacy.addListener(legacyAdvancements, legacyListener);
        } catch (RuntimeException | LinkageError e) {
            if (REPORTED.add(instance.trigger() + "?")) LOG.warn("The advancement trigger {} failed to add a listener: {}", instance.trigger(), e.toString());
        }
    }

    @Override
    public void removePlayerListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        ICriterionTrigger.Listener<ICriterionInstance> legacyListener;
        synchronized (registered) {
            var perPlayer = registered.get(advancements);
            legacyListener = perPlayer == null ? null : perPlayer.remove(listener);
        }
        if (legacyListener == null) return;
        ICriterionTrigger<ICriterionInstance> legacy = CriteriaTriggers.get(new ResourceLocation(listener.trigger().trigger()));
        var legacyAdvancements = legacyAdvancements(advancements);
        if (legacy != null && legacyAdvancements != null) legacy.removeListener(legacyAdvancements, legacyListener);
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements advancements) {
        var legacyAdvancements = legacyAdvancements(advancements);
        if (legacyAdvancements == null) return;
        for (ICriterionTrigger<?> legacy : CriteriaTriggers.getAll()) {
            if (legacy.getId() != null && !"minecraft".equals(legacy.getId().getNamespace())) legacy.removeAllListeners(legacyAdvancements);
        }
        synchronized (registered) {
            registered.remove(advancements);
        }
    }

    @Override
    public Codec<Instance> codec() {
        return CODEC;
    }

    private static legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements legacyAdvancements(PlayerAdvancements advancements) {
        net.minecraft.server.level.ServerPlayer sp = advancements.player;
        if (sp == null) return null;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(sp) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP mp
                ? new legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements(mp, advancements) : null;
    }
}
