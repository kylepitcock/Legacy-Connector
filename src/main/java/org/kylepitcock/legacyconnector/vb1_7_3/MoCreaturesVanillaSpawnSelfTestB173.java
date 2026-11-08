package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.src.SettingInt;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Reads actual original initialization and effective native biome lists; private jars remain runtime-only. */
public final class MoCreaturesVanillaSpawnSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173VanillaSpawnTest");
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 VANILLA SPAWN TEST failed",error);}});}
    public static void run(net.minecraft.server.level.ServerLevel level)throws ReflectiveOperationException {
        var registration=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(registration.isEmpty())return;
        var mod=registration.get().mod().instance;int frequency=((Number)((SettingInt)mod.getClass().getField("squidfreq").get(null)).get()).intValue();
        var requests=SpawnRulesB173.changes().stream().filter(entry->SpawnRulesB173.vanillaType(entry.type())==EntityType.SQUID&&entry.group().mcc$modern()==MobCategory.WATER_CREATURE).toList();
        require(requests.size()==1&&requests.getFirst().weight()==(frequency>0?frequency:-1),"actual MoC constructor retains final vanilla squid add/remove request for configured frequency "+frequency);
        var overworld=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("is_overworld"));int checked=0;
        for(var biome:level.registryAccess().registryOrThrow(Registries.BIOME).holders().toList())if(biome.is(overworld)){
            var entries=biome.value().getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().stream().filter(entry->entry.type==EntityType.SQUID).toList();
            require(entries.size()==(frequency>0?1:0),"effective native squid list obeys actual original replacement/removal: "+biome.unwrapKey()+" count="+entries.size());
            if(frequency>0)require(entries.getFirst().getWeight().asInt()==frequency&&entries.getFirst().minCount==1&&entries.getFirst().maxCount==4,"effective native squid spawn uses original configured weight and Beta group policy");checked++;
        }
        require(checked>0,"actual vanilla override checked bound overworld biomes");
        LOG.info("B1.7.3 VANILLA SPAWN TEST: actual MoC frequency {}, original constructor request and authoritative native squid policy passed in {} overworld biomes",frequency,checked);
    }
}
