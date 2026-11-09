package org.kylepitcock.legacyconnector.core;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.Test;

class LegacyModsIdentityTest {
    private static LegacyModRecord mod(String version,String id,String dependencies){var meta=new JsonObject();meta.addProperty("modernId",id.toLowerCase(Locale.ROOT));meta.addProperty("dependencies",dependencies);return new LegacyModRecord(version,id,"example.Main",null,null,meta);}
    private static Field field(String name)throws Exception{var f=LegacyMods.class.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static void withMods(List<LegacyModRecord> mods,Runnable proof)throws Exception{
        var all=(List<LegacyModRecord>)field("MODS").get(null);var byId=(Map<String,LegacyModRecord>)field("BY_LEGACY_ID").get(null);var previous=new ArrayList<>(all);var ids=new HashMap<>(byId);
        try{all.clear();byId.clear();for(var mod:mods)LegacyMods.add(mod);proof.run();}finally{all.clear();all.addAll(previous);byId.clear();byId.putAll(ids);}
    }
    @Test void bothCopiesRunAndDependenciesResolveWithinTheirOwnVersion()throws Exception{
        var beta=mod("b1.7.3","Shared","");var forge=mod("1.7.10","shared","");var betaConsumer=mod("b1.7.3","aaa","after:shared");var forgeConsumer=mod("1.7.10","AAA","required-after:Shared");
        withMods(List.of(betaConsumer,forgeConsumer,beta,forge),()->{var ordered=LegacyMods.ordered();assertEquals(List.of(beta,betaConsumer,forge,forgeConsumer),ordered);assertSame(beta,LegacyMods.byLegacyId("SHARED","b1.7.3"));assertSame(forge,LegacyMods.byLegacyId("Shared","1.7.10"));assertNull(LegacyMods.byLegacyId("shared","1.12.2"));});
    }
    @Test void exactForwardedVersionWinsBeforeCompatibleFamily()throws Exception{
        var old=mod("1.6.4","Shared","");var current=mod("1.7.10","shared","");
        withMods(List.of(old,current),()->{assertEquals(2,LegacyMods.ordered().size());assertSame(old,LegacyMods.byLegacyId("shared","1.6.4"));assertSame(current,LegacyMods.byLegacyId("shared","1.7.10"));assertSame(current,LegacyMods.byLegacyId("shared","1.5.2"));});
    }
    @Test void existingSameVersionDuplicateAndDependencySemanticsStayStable()throws Exception{
        var previous=mod("1.7.10","shared","");var latest=mod("1.7.10","SHARED","");var user=mod("1.7.10","aaa","after:shared@1.0");
        withMods(List.of(previous,latest,user),()->assertEquals(List.of(latest,user),LegacyMods.ordered()));
    }
}
