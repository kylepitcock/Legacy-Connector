package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.junit.jupiter.api.Test;

final class Forge1201EventBridgeTest {
    @Test
    void plainEventRetainsFalseCancelabilityAndRejectsMutation() {
        PlainEvent event = new PlainEvent();

        assertFalse(Forge1201EventBridge.isCancelable(event));
        assertFalse(Forge1201EventBridge.isCanceled(event));
        assertThrows(UnsupportedOperationException.class,
                () -> Forge1201EventBridge.setCanceled(event, true));
    }

    @Test
    void directlyCancelableEventReadsAndWritesTargetEventState() {
        CancelableEvent event = new CancelableEvent();

        assertTrue(Forge1201EventBridge.isCancelable(event));
        assertFalse(Forge1201EventBridge.isCanceled(event));

        Forge1201EventBridge.setCanceled(event, true);
        assertTrue(Forge1201EventBridge.isCanceled(event));

        Forge1201EventBridge.setCanceled(event, false);
        assertFalse(Forge1201EventBridge.isCanceled(event));
    }

    @Test
    void inheritedCancelableChildAndCustomOverrideKeepVirtualSetCanceledSemantics() {
        OverrideCancelableEvent event = new OverrideCancelableEvent();

        assertTrue(Forge1201EventBridge.isCancelable(event));
        Forge1201EventBridge.setCanceled(event, true);
        assertTrue(Forge1201EventBridge.isCanceled(event));
        assertTrue(event.overrideObserved);
    }


    @Test
    void customIsCancelableOverrideDoesNotChangeBaseSuperAnnotationLookup() {
        CustomOverrideOnly event = new CustomOverrideOnly();

        assertTrue(Forge1201EventBridge.isCancelable(event));
        assertFalse(Forge1201EventBridge.baseIsCancelable(event),
                "The source Forge Event superclass checks @Cancelable, not the synthetic target interface.");
        Forge1201EventBridge.setCanceled(event, true);
        assertTrue(event.superValue);
        assertTrue(Forge1201EventBridge.isCanceled(event));
    }

    private static final class CustomOverrideOnly extends PlainEvent
            implements ICancellableEvent, Forge1201CustomCancelableOverrideMarker {
        private boolean superValue;

        @Override
        public boolean isCancelable() {
            superValue = super.isCancelable();
            return true;
        }
    }

    private static class PlainEvent extends Event implements Forge1201EventCompat {
        @Override
        public boolean isCancelable() {
            return Forge1201EventBridge.baseIsCancelable(this);
        }

        @Override
        public boolean isCanceled() {
            return Forge1201EventBridge.baseIsCanceled(this);
        }

        @Override
        public void setCanceled(boolean canceled) {
            Forge1201EventBridge.baseSetCanceled(this, canceled);
        }
    }

    private static class CancelableEvent extends PlainEvent
            implements Forge1201CancelableEventMarker, ICancellableEvent {
    }

    private static final class OverrideCancelableEvent extends CancelableEvent {
        private boolean overrideObserved;

        @Override
        public void setCanceled(boolean canceled) {
            overrideObserved = true;
            super.setCanceled(canceled);
        }
    }
}
