package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockBreakParticlesB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        NativeStackComponentsB173Test.bootstrap();
        // Portable Bootstrap does not run NeoForgeRegistryCallbacks.BlockCallbacks.onBake.
        // Mirror its native registry-order state-ID population without changing already assigned IDs.
        for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            for(var state:block.getStateDefinition().getPossibleStates())
                if(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(state)<0)
                    net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.add(state);
    }
    @Test void originalGridHas64IndependentCellsWithOutwardVelocityAndNegativeCoordinates(){
        var position=new BlockPos(-3,17,9);var samples=new ArrayList<BlockBreakParticlesB173.Sample>();BlockBreakParticlesB173.samples(position,samples::add);
        assertEquals(64,samples.size());assertEquals(64,samples.stream().distinct().count());
        assertEquals(new BlockBreakParticlesB173.Sample(-2.875,17.125,9.125,-.375,-.375,-.375),samples.getFirst());
        assertEquals(new BlockBreakParticlesB173.Sample(-2.125,17.875,9.875,.375,.375,.375),samples.getLast());
        for(var sample:samples){assertEquals(sample.x()-position.getX()-.5,sample.velocityX());assertEquals(sample.y()-position.getY()-.5,sample.velocityY());assertEquals(sample.z()-position.getZ()-.5,sample.velocityZ());}
    }
    @Test void compactRequestRoundTripRetainsExplicitStatePropertiesAndRealm(){
        var state=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z);
        assertTrue(net.minecraft.world.level.block.Block.getId(state)>0,"Portable fixture must include native baked state IDs");
        assertSame(state,net.minecraft.world.level.block.Block.stateById(net.minecraft.world.level.block.Block.getId(state)));
        var original=new BlockBreakParticlesB173.Request(ResourceLocation.fromNamespaceAndPath("test","realm"),new BlockPos(21,64,-7),state);
        assertEquals(original,BlockBreakParticlesB173.decode(BlockBreakParticlesB173.encode(original)));
    }
    @Test void terminalInvalidFrameSuppressesOnlyItsWarningWithoutClampingOrChangingLifetime(){
        var particle=new Particle(null,0,0,0,0,0,0);particle.maxParticleAge=8;particle.particleAge=7;particle.textureId=-1;
        assertTrue(ParticlesB173.warnInvalidSprite(particle));particle.particleAge=8;assertFalse(ParticlesB173.warnInvalidSprite(particle));
        assertFalse(particle.dead);assertEquals(-1,particle.textureId);
        particle.textureId=4;assertTrue(ParticlesB173.warnInvalidSprite(particle));assertFalse(particle.dead);
        particle.textureId=-1;particle.maxParticleAge=0;assertTrue(ParticlesB173.warnInvalidSprite(particle));
    }
}
