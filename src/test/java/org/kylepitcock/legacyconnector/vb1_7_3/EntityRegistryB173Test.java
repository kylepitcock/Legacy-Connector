package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.EntityRegistry;
import legacyconnector.vb1_7_3.net.minecraft.src.ModLoader;
import org.junit.jupiter.api.Test;

class EntityRegistryB173Test {
    static class ReplacementCow extends legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity {
        ReplacementCow(legacyconnector.vb1_7_3.net.minecraft.world.World world){super(world);}
    }
    @Test void originalReflectionAliasesExposeTheSameLiveNameMap() throws Exception {
        var map=EntityRegistry.stringToClassMapping;
        assertSame(map,ModLoader.getPrivateValue(EntityRegistry.class,null,"a"));
        assertSame(map,ModLoader.getPrivateValue(EntityRegistry.class,null,"stringToClassMapping"));
        assertSame(map,ModLoader.getPrivateValue(EntityRegistry.class,null,"field_1611_a"));
        for(var registration:ContentB173.entities())assertEquals(registration.type(),map.get(registration.name()));
        assertNull(map.get("absent_registry_probe"));
    }
    @Test void mapCapturedBeforeRegistrationSeesLaterOriginalEntityNames() throws Exception {
        var map=EntityRegistry.stringToClassMapping;
        var field=ContentB173.class.getDeclaredField("ENTITIES");field.setAccessible(true);
        @SuppressWarnings("unchecked") var registrations=(java.util.List<ContentB173.Registration>)field.get(null);
        int size=registrations.size();
        try {
            ContentB173.registerEntity(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,"RegistryLateProbe",31944);
            assertEquals(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,map.get("RegistryLateProbe"));
            assertTrue(map.entrySet().stream().anyMatch(entry->entry.getKey().equals("RegistryLateProbe")));
        } finally {registrations.subList(size,registrations.size()).clear();}
    }
    @Test void namedVanillaRemovalBeforeReplacementKeepsOriginalIdentityAndLatestRegistrationWins()throws Exception {
        var field=ContentB173.class.getDeclaredField("ENTITIES");field.setAccessible(true);
        @SuppressWarnings("unchecked") var registrations=(java.util.List<ContentB173.Registration>)field.get(null);
        var previous=new java.util.ArrayList<>(registrations);
        try {
            registrations.clear();
            var vanilla=legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity.class;
            assertSame(vanilla,SpawnRulesB173.namedEntity("Cow"));assertSame(vanilla,EntityRegistry.stringToClassMapping.get("Cow"));
            assertSame(legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity.class,SpawnRulesB173.namedEntity("Pig"));
            assertNull(SpawnRulesB173.namedEntity("cow"));assertNull(SpawnRulesB173.namedEntity("UnknownNativeCreature"));
            var rules=new SpawnRulesB173.Registry();
            rules.remove(SpawnRulesB173.namedEntity("Cow"),legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup.CREATURE,null);
            ContentB173.registerEntity(ReplacementCow.class,"Cow",92);
            assertSame(ReplacementCow.class,SpawnRulesB173.namedEntity("Cow"));
            rules.add(SpawnRulesB173.namedEntity("Cow"),8,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup.CREATURE,null);
            assertTrue(rules.changes().stream().anyMatch(e->e.type()==vanilla&&e.weight()==-1));
            assertTrue(rules.changes().stream().anyMatch(e->e.type()==ReplacementCow.class&&e.weight()==8));
            ContentB173.registerEntity(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,"Cow",31943);
            assertSame(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,SpawnRulesB173.namedEntity("Cow"));
            assertEquals(1,EntityRegistry.stringToClassMapping.entrySet().stream().filter(e->e.getKey().equals("Cow")).count());
        }finally{registrations.clear();registrations.addAll(previous);}
    }

    static class UnknownCow extends ReplacementCow {UnknownCow(){super(null);}}
    @Test void getIdUsesExactClassLatestRegistrationAndOriginalCase()throws Exception{
        var field=ContentB173.class.getDeclaredField("ENTITIES");field.setAccessible(true);
        @SuppressWarnings("unchecked") var registrations=(java.util.List<ContentB173.Registration>)field.get(null);
        var previous=new java.util.ArrayList<>(registrations);
        try{
            registrations.clear();
            assertEquals("Cow",EntityRegistry.getId(new legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity(null)));
            assertNull(EntityRegistry.getId(new ReplacementCow(null)));assertNull(EntityRegistry.getId(new UnknownCow()));
            ContentB173.registerEntity(ReplacementCow.class,"PhygCaseFixture",31945);
            assertEquals("PhygCaseFixture",EntityRegistry.getId(new ReplacementCow(null)));
            assertNull(EntityRegistry.getId(new UnknownCow()),"Subclass must not borrow a registered parent's identity");
            ContentB173.registerEntity(ReplacementCow.class,"PhygCaseFixtureLatest",31946);
            assertEquals("PhygCaseFixtureLatest",EntityRegistry.getId(new ReplacementCow(null)));
            assertThrows(NullPointerException.class,()->EntityRegistry.getId(null),"Original getId dereferences the supplied entity");
        }finally{registrations.clear();registrations.addAll(previous);}
    }
    @Test void genericNativePigViewResolvesCanonicalPigWithoutNamingUnknownOriginalSubclasses()throws Exception{
        NativeStackComponentsB173Test.bootstrap();
        var nativePig=(net.minecraft.world.entity.animal.Pig)org.kylepitcock.legacyconnector.core.synth.Synth.blank(net.minecraft.world.entity.animal.Pig.class.getName());
        var type=net.minecraft.world.entity.Entity.class.getDeclaredField("type");type.setAccessible(true);type.set(nativePig,net.minecraft.world.entity.EntityType.PIG);
        var factory=EntityViewsB173.class.getDeclaredMethod("create",net.minecraft.world.entity.Entity.class);factory.setAccessible(true);
        var ordinary=(legacyconnector.vb1_7_3.net.minecraft.entity.Entity)factory.invoke(null,nativePig);ordinary.mcc$modern=nativePig;
        assertSame(EntityViewsB173.class,ordinary.getClass().getEnclosingClass());assertEquals("Pig",EntityRegistry.getId(ordinary));
        var unknown=new UnknownCow();unknown.mcc$modern=nativePig;assertNull(EntityRegistry.getId(unknown));
        var registrationsField=ContentB173.class.getDeclaredField("ENTITIES");registrationsField.setAccessible(true);
        @SuppressWarnings("unchecked") var registrations=(java.util.List<ContentB173.Registration>)registrationsField.get(null);
        int size=registrations.size();
        try{
            ContentB173.registerEntity(ReplacementCow.class,"HostedOriginalName",31947);
            var hosted=new ReplacementCow(null);hosted.mcc$modern=nativePig;
            assertEquals("HostedOriginalName",EntityRegistry.getId(hosted),"Registered original identity wins over attached native family");
        }finally{registrations.subList(size,registrations.size()).clear();}
        assertSame(legacyconnector.vb1_7_3.net.minecraft.entity.mob.PigZombieEntity.class,EntityRegistry.stringToClassMapping.get("PigZombie"));
        assertEquals("PigZombie",NativeEntityNamesB173.alias("minecraft:zombified_piglin"));
        assertEquals("PrimedTnt",NativeEntityNamesB173.alias("minecraft:tnt"));
        assertEquals("FallingSand",NativeEntityNamesB173.alias("minecraft:falling_block"));
        assertEquals("Minecart",NativeEntityNamesB173.alias("minecraft:chest_minecart"));
        assertNull(NativeEntityNamesB173.alias("fixture:pig"));assertNull(NativeEntityNamesB173.alias("minecraft:hoglin"));
        assertNull(NativeEntityNamesB173.alias("minecraft:unknown"));
    }
}
