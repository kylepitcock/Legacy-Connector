package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

class BedsNativeWakeB173Test {
    // Test runtime supplies ASM 9; the legacy compile facade has older Opcodes constants.
    private static final int ASM_API=9<<16;
    private static class OriginalBed extends Block {
        private OriginalBed(){super(250,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.WOOL);}
        @Override public int getRenderType(){return 14;}
    }
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}
    private static void field(Class<?> owner,Object target,String name,Object value) throws Exception {
        var field=owner.getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    private static LegacyB173Block metadataOnlyHost(Block original) throws Exception {
        // Block constructors allocate intrusive registry holders, forbidden after Bootstrap freezes vanilla.
        // Build a detached real host/state graph instead; do not thaw or register anything in the global registry.
        var host=(LegacyB173Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(LegacyB173Block.class.getName());
        field(LegacyB173Block.class,host,"legacy",original);
        field(BlockBehaviour.class,host,"properties",BlockBehaviour.Properties.of());
        var definition=new net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.state.BlockState>(host)
                .add(LegacyB173Block.META).create(net.minecraft.world.level.block.Block::defaultBlockState,net.minecraft.world.level.block.state.BlockState::new);
        field(net.minecraft.world.level.block.Block.class,host,"stateDefinition",definition);
        field(net.minecraft.world.level.block.Block.class,host,"defaultBlockState",definition.any().setValue(LegacyB173Block.META,0));
        return host;
    }
    @Test void metadataOnlyNativeBedWakesWithoutReadingMissingVanillaFacing() throws Exception {
        var original=(Block)org.kylepitcock.legacyconnector.core.synth.Synth.blank(OriginalBed.class.getName());
        var host=metadataOnlyHost(original);
        for(int direction=0;direction<4;direction++){
            var state=host.defaultBlockState().setValue(LegacyB173Block.META,direction|12);
            assertFalse(state.hasProperty(BedBlock.FACING));
            assertThrows(IllegalArgumentException.class,()->state.getValue(BedBlock.FACING));
            var steps=new ArrayList<String>();var destination=new Vec3(direction,71,3);
            assertTrue(BedsB173.prepareWake(state,()->{
                assertEquals(java.util.List.of("free"),steps);
                assertEquals(BedsB173.direction(state.getValue(LegacyB173Block.META)),state.getBedDirection(null,BlockPos.ZERO));
                steps.add("find");return destination;
            },()->steps.add("free"),p->{assertSame(destination,p);steps.add("place");},()->steps.add("clear")));
            assertEquals(java.util.List.of("free","find","place","clear"),steps);
        }
    }
    @Test void nativeVanillaWakeKeepsItsEntireOriginalPlacementPath(){
        assertFalse(BedsB173.prepareWake(net.minecraft.world.level.block.Blocks.RED_BED.defaultBlockState(),
                ()->{throw new AssertionError();},()->fail(),p->fail(),()->fail()));
        assertFalse(BedsB173.prepareWake(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                ()->{throw new AssertionError();},()->fail(),p->fail(),()->fail()));
    }
    @Test void nativeWakeEventRunsBeforeTheIncompatibleLivingEntityPlacement() throws Exception {
        var calls=new ArrayList<String>();
        try(var in=net.minecraft.world.entity.player.Player.class.getResourceAsStream("Player.class")){
            assertNotNull(in);
            new ClassReader(in).accept(new ClassVisitor(ASM_API){
                @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                    if(!name.equals("stopSleepInBed")||!descriptor.equals("(ZZ)V"))return null;
                    return new MethodVisitor(ASM_API){
                        @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean itf){
                            if(owner.equals("net/neoforged/neoforge/event/EventHooks")&&name.equals("onPlayerWakeup"))calls.add("event");
                            if(owner.equals("net/minecraft/world/entity/LivingEntity")&&name.equals("stopSleeping"))calls.add("placement");
                        }
                    };
                }
            },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        assertEquals(java.util.List.of("event","placement"),calls);
    }
}
