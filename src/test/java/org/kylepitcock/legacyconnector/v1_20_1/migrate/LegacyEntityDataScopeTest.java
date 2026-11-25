package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.Pig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LegacyEntityDataScopeTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        MigrateContextResetTest.bootstrapMinecraft();
    }

    @Test
    void readsLiveBuilderValuesAndKeepsNestedScopesSeparateOnFailure() throws Exception {
        Pig outer = uninitializedEntity();
        Pig inner = uninitializedEntity();
        ViewBuilder outerBuilder = new ViewBuilder(outer);
        ViewBuilder innerBuilder = new ViewBuilder(inner);
        var first = new EntityDataAccessor<>(0, EntityDataSerializers.INT);
        var second = new EntityDataAccessor<>(1, EntityDataSerializers.INT);

        MigrateContext.withEntityData(outer, outerBuilder, () -> {
            assertNull(MigrateContext.getEntityData(outer));
            MigrateContext.remember(outerBuilder);
            var view = MigrateContext.getEntityData(outer);
            MiscCompat.define(view, first, 13);
            MiscCompat.define(view, second, view.get(first) * 3);
            assertEquals(39, view.get(second));
            assertSame(view, MigrateContext.entityData(outer, null));
            assertNull(MigrateContext.getEntityData(inner));

            assertThrows(IllegalArgumentException.class, () -> MigrateContext.withEntityData(inner, innerBuilder, () -> {
                assertNull(MigrateContext.getEntityData(inner));
                MigrateContext.remember(innerBuilder);
                MiscCompat.define(MigrateContext.getEntityData(inner), first, 7);
                assertEquals(7, MigrateContext.getEntityData(inner).get(first));
                assertEquals(13, MigrateContext.getEntityData(outer).get(first));
                throw new IllegalArgumentException("callback failed");
            }));
            assertSame(outerBuilder, MigrateContext.builder());
            assertNull(MigrateContext.getEntityData(inner));
        });
        assertNull(MigrateContext.getEntityData(outer));
        assertThrows(IllegalStateException.class, MigrateContext::builder);
    }

    private static Pig uninitializedEntity() throws Exception {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Pig) ((sun.misc.Unsafe) field.get(null)).allocateInstance(Pig.class);
    }

    /** Unit tests do not apply mixins. Expose the same live slots that the runtime mixin exposes. */
    private static final class ViewBuilder extends SynchedEntityData.Builder implements OverridableBuilder {
        private final Pig owner;

        ViewBuilder(Pig owner) {
            super(owner);
            this.owner = owner;
        }

        @Override
        public SynchedEntityData legacyconnector$view() {
            try {
                var field = SynchedEntityData.Builder.class.getDeclaredField("itemsById");
                field.setAccessible(true);
                return new SynchedEntityData(owner, (SynchedEntityData.DataItem<?>[]) field.get(this));
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        public <T> void legacyconnector$overrideDefault(EntityDataAccessor<T> accessor, T value) {
            throw new AssertionError("unexpected default override");
        }
    }
}