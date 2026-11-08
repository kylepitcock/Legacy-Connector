package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Calls of {@code BlockBehaviour} methods on another block ({@code Blocks.HOPPER.getInteractionShape(state, level, pos)}). They were public in 1.20.1 and are
 * protected in 1.21.1, where a subclass in another package may only call them on its own type: the verifier refuses the mod class. The call table sends such
 * calls (not {@code super} ones) here, and the method is invoked through a handle, with the same virtual dispatch.
 */
public final class BlockCalls1201 {
    private static final MethodHandle SHAPE = handle("getShape", VoxelShape.class, BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
    private static final MethodHandle COLLISION = handle("getCollisionShape", VoxelShape.class, BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
    private static final MethodHandle VISUAL = handle("getVisualShape", VoxelShape.class, BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
    private static final MethodHandle SUPPORT = handle("getBlockSupportShape", VoxelShape.class, BlockState.class, BlockGetter.class, BlockPos.class);
    private static final MethodHandle INTERACTION = handle("getInteractionShape", VoxelShape.class, BlockState.class, BlockGetter.class, BlockPos.class);
    private static final MethodHandle OCCLUSION = handle("getOcclusionShape", VoxelShape.class, BlockState.class, BlockGetter.class, BlockPos.class);
    private static final MethodHandle MENU = handle("getMenuProvider", MenuProvider.class, BlockState.class, Level.class, BlockPos.class);
    private static final MethodHandle RENDER = handle("getRenderShape", RenderShape.class, BlockState.class);

    private BlockCalls1201() {}

    private static MethodHandle handle(String name, Class<?> result, Class<?>... params) {
        try {
            return MethodHandles.privateLookupIn(BlockBehaviour.class, MethodHandles.lookup()).findVirtual(BlockBehaviour.class, name, MethodType.methodType(result, params));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException r) throw r;
        if (t instanceof Error e) throw e;
        return new IllegalStateException(t);
    }

    public static VoxelShape getShape(BlockBehaviour self, BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        try {
            return (VoxelShape) SHAPE.invokeExact(self, state, level, pos, ctx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static VoxelShape getCollisionShape(BlockBehaviour self, BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        try {
            return (VoxelShape) COLLISION.invokeExact(self, state, level, pos, ctx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static VoxelShape getVisualShape(BlockBehaviour self, BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        try {
            return (VoxelShape) VISUAL.invokeExact(self, state, level, pos, ctx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static VoxelShape getBlockSupportShape(BlockBehaviour self, BlockState state, BlockGetter level, BlockPos pos) {
        try {
            return (VoxelShape) SUPPORT.invokeExact(self, state, level, pos);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static VoxelShape getInteractionShape(BlockBehaviour self, BlockState state, BlockGetter level, BlockPos pos) {
        try {
            return (VoxelShape) INTERACTION.invokeExact(self, state, level, pos);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static VoxelShape getOcclusionShape(BlockBehaviour self, BlockState state, BlockGetter level, BlockPos pos) {
        try {
            return (VoxelShape) OCCLUSION.invokeExact(self, state, level, pos);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static MenuProvider getMenuProvider(BlockBehaviour self, BlockState state, Level level, BlockPos pos) {
        try {
            return (MenuProvider) MENU.invokeExact(self, state, level, pos);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static RenderShape getRenderShape(BlockBehaviour self, BlockState state) {
        try {
            return (RenderShape) RENDER.invokeExact(self, state);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }
}
