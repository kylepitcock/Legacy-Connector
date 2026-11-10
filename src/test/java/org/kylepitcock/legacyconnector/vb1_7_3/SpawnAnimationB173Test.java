package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class SpawnAnimationB173Test {
    record Puff(String name,double x,double y,double z,double vx,double vy,double vz){
        double[] values(){return new double[]{x,y,z,vx,vy,vz};}
    }
    static class ParticleWorld extends World {
        final List<Puff> puffs=new ArrayList<>();
        ParticleWorld(){super();}
        @Override public void addParticle(String name,double x,double y,double z,double vx,double vy,double vz){puffs.add(new Puff(name,x,y,z,vx,vy,vz));}
    }
    static class Actor extends LivingEntity {
        Actor(World world){super(world);}
        void seed(long value){random.setSeed(value);}
        long nextLong(){return random.nextLong();}
        double nextGaussian(){return random.nextGaussian();}
    }
    static class CustomActor extends Actor {
        int calls;
        CustomActor(World world){super(world);}
        @Override public void animateSpawn(){calls++;}
    }
    @Test void fixedSeedMatchesBetaPuffCoordinatesAndPreservesFollowingRandomStream(){
        var world=new ParticleWorld();var actor=new Actor(world);actor.seed(20261110L);
        actor.width=.6F;actor.height=1.8F;actor.setPosition(3.25,70.5,-9.75);
        actor.animateSpawn();
        assertEquals(20,world.puffs.size());assertTrue(world.puffs.stream().allMatch(p->p.name().equals("explode")));
        // Independent fixed-seed Beta reference: three Gaussian velocities before three float positions per puff.
        assertArrayEquals(new double[]{3.6854128060029705,71.24162491890401,-10.139337194511443,.003796358731520569,-.02143454227110726,.002176625783160331},world.puffs.getFirst().values(),1E-13);
        assertArrayEquals(new double[]{3.4890223644942515,71.79973971307032,-9.938244678318101,.024767745507056797,.0062847262027326545,.002964714938362208},world.puffs.getLast().values(),1E-13);
        assertEquals(5275583169586722449L,actor.nextLong());
        assertEquals(-.44942187880693457,actor.nextGaussian(),1E-14);
    }
    @Test void originalVirtualOverrideOwnsAnimationWithoutExtraHostPuffsOrRandomDraws(){
        var world=new ParticleWorld();var actor=new CustomActor(world);actor.seed(42);
        LivingEntity original=actor;original.animateSpawn();
        assertEquals(1,actor.calls);assertTrue(world.puffs.isEmpty());
        assertEquals(new Random(42).nextLong(),actor.nextLong());
    }
}
