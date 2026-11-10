package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.particle.*;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

class VanillaParticleBasesB173Test {
    private static List<Tessellator.Vertex> render(Particle particle,float partial){
        Tessellator.beginCapture();
        List<Tessellator.Vertex> vertices;
        try{particle.render(Tessellator.INSTANCE,partial,1,1,0,0,0);}
        finally{vertices=Tessellator.endCapture();}
        return vertices;
    }
    @Test void portalTrajectoryMatchesVanillaGoldenAndPreservesOriginalLifetime(){
        var particle=new PortalParticle(null,1,2,3,2,-.5,4);
        assertTrue(particle.noClip);assertTrue(particle.maxParticleAge>=40&&particle.maxParticleAge<50);
        assertTrue(particle.textureId>=0&&particle.textureId<8);
        particle.maxParticleAge=40;particle.particleAge=10;particle.tick();
        // Captured from the named vanilla Beta class with the same public state in an isolated JShell.
        assertEquals(3.25,particle.x);assertEquals(2.1875,particle.y);assertEquals(7.5,particle.z);
        assertEquals(1,particle.prevX);assertEquals(2,particle.prevY);assertEquals(3,particle.prevZ);assertEquals(11,particle.particleAge);
        particle.particleAge=39;particle.tick();assertFalse(particle.dead);assertEquals(40,particle.particleAge);
        particle.tick();assertTrue(particle.dead);assertEquals(1,particle.x);assertEquals(2,particle.y);assertEquals(3,particle.z);
    }
    @Test void portalGrowthBrightnessAndPrivateTrajectoryStateSurviveTransport(){
        var particle=new PortalParticle(null,1,2,3,2,-.5,4);float initialScale=particle.scale;
        particle.maxParticleAge=40;particle.particleAge=10;
        var before=render(particle,0);assertEquals(4,before.size());
        assertEquals(initialScale*.4375F,particle.scale);assertEquals(.00390625F,particle.getBrightnessAtEyes(0));
        var copy=(PortalParticle)ParticlesB173.restore(ParticlesB173.decode(ParticlesB173.encode(ParticlesB173.capture(particle))),null);
        assertTrue(copy.noClip);assertEquals(before,render(copy,0));
        particle.tick();copy.tick();assertEquals(particle.x,copy.x);assertEquals(particle.y,copy.y);assertEquals(particle.z,copy.z);
    }
    @Test void smokeAndExplosionTickMatchVanillaFloatDampingGoldens(){
        var smoke=new FireSmokeParticle(null,1,2,3,0,0,0);
        var explosion=new ExplosionParticle(null,1,2,3,0,0,0);
        for(var particle:List.of(smoke,explosion)){
            particle.noClip=true;particle.maxParticleAge=16;particle.particleAge=3;
            particle.velocityX=.25;particle.velocityY=.5;particle.velocityZ=-.75;particle.tick();
            assertEquals(1.25,particle.x);assertEquals(2.504,particle.y);assertEquals(2.25,particle.z);
            assertEquals(5,particle.textureId);assertEquals(4,particle.particleAge);
        }
        assertArrayEquals(new double[]{.23999999463558197,.4838399891853333,-.7199999839067459},
                new double[]{smoke.velocityX,smoke.velocityY,smoke.velocityZ});
        assertArrayEquals(new double[]{.22499999403953552,.4535999879837036,-.6749999821186066},
                new double[]{explosion.velocityX,explosion.velocityY,explosion.velocityZ});
        var restored=ParticlesB173.restore(ParticlesB173.capture(smoke),null);assertEquals(FireSmokeParticle.class,restored.getClass());assertTrue(restored.noClip);
    }
    @Test void smokeUsesNativeCollisionServiceAndRetainsItsCeilingResponse(){
        var smoke=new FireSmokeParticle(null,0,0,0,0,0,0);smoke.maxParticleAge=20;
        smoke.velocityX=1;smoke.velocityY=.5;smoke.velocityZ=2;
        smoke.mcc$particleHost=(particle,x,y,z)->{particle.setPosition(particle.x+x,particle.y,particle.z+z);particle.onGround=true;};
        smoke.tick();assertEquals(1,smoke.x);assertEquals(0,smoke.y);assertEquals(2,smoke.z);
        assertEquals(1.1*(double).96F*(double).7F,smoke.velocityX);
        assertEquals(2*1.1*(double).96F*(double).7F,smoke.velocityZ);
    }
    @Test void itemFragmentsUseQuarterCellUvsAndSdkRestorationRemainsFinite()throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var item=(Item)((sun.misc.Unsafe)field.get(null)).allocateInstance(Item.class);item.setTextureId(50);
        var particle=new ItemParticle(null,1,2,3,item);particle.prevU=2;particle.prevV=1;particle.scale=1;
        assertEquals(2,particle.getGroup());assertEquals(50,particle.textureId);
        var vertices=render(particle,0);assertEquals(4,vertices.size());
        assertEquals(.15625,vertices.getFirst().u());assertEquals((double)(.203125F+.015609375F),vertices.getFirst().v());
        assertEquals((double)(.15625F+.015609375F),vertices.get(2).u());assertEquals(.203125,vertices.get(2).v());
        var restored=ParticlesB173.restore(ParticlesB173.capture(particle),null);assertEquals(ItemParticle.class,restored.getClass());assertEquals(vertices,render(restored,0));
        class UnownedParticle extends PortalParticle {UnownedParticle(){super(null,0,0,0,0,0,0);}}
        assertThrows(IllegalArgumentException.class,()->ParticlesB173.restore(ParticlesB173.capture(new UnownedParticle()),null));
    }
}
