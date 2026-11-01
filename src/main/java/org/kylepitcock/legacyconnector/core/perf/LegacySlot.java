package org.kylepitcock.legacyconnector.core.perf;

/**
 * One field mixed into a modern game object (Block, Level, Entity: {@code mixin.perf.*SlotMixin}) that holds the 1.7.10 object
 * the legacy layer made for it (the legacy block of a modern block, the legacy world view of a level, the legacy view of an
 * entity). Reading the field replaces a lookup in a synchronized or weak-keyed map on paths that run for every block and entity
 * every tick. The maps stay the source of truth: the field is only ever set to the value the map holds for that object.
 */
public interface LegacySlot {
    Object legacyconnector$slot1710();

    void legacyconnector$setSlot1710(Object value);
}
