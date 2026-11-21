package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MiddleClientCompatEntityReplacementTest {

    @Test
    void removesExistingSyntheticIdOccupantBeforeAddingTheNextEffect() {
        Object previous = new Object();
        Object next = new Object();
        Object[] current = {previous};
        List<String> calls = new ArrayList<>();

        MiddleClientCompat.replaceNonPlayerEntity(0, next,
                id -> {
                    assertEquals(0, id);
                    calls.add("lookup");
                    return current[0];
                },
                id -> {
                    assertEquals(0, id);
                    calls.add("remove");
                    current[0] = null;
                },
                entity -> {
                    calls.add("add");
                    current[0] = entity;
                });

        assertEquals(List.of("lookup", "remove", "add"), calls);
        assertSame(next, current[0]);
    }

    @Test
    void addsWithoutRemovalWhenSyntheticIdIsUnused() {
        Object next = new Object();
        Object[] current = {null};
        List<String> calls = new ArrayList<>();

        MiddleClientCompat.replaceNonPlayerEntity(0, next,
                id -> current[0],
                id -> calls.add("remove"),
                entity -> {
                    calls.add("add");
                    current[0] = entity;
                });

        assertEquals(List.of("add"), calls);
        assertSame(next, current[0]);
    }

    @Test
    void doesNotRemoveWhenTheSameEntityInstanceIsAlreadyRegistered() {
        Object currentEntity = new Object();
        List<String> calls = new ArrayList<>();

        MiddleClientCompat.replaceNonPlayerEntity(0, currentEntity,
                id -> currentEntity,
                id -> calls.add("remove"),
                entity -> calls.add("add"));

        assertEquals(List.of("add"), calls);
    }
}
