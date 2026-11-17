/*
 * Copyright (c) 2026 kentvillamor08
 * Licensed under the MIT License.
 * See LICENSE in the project root.
 */
package org.kylepitcock.legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.SpawnContext;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.SpawnContext.Scope;

class SpawnContext1201Test {
    @Test
    void pendingTagFlowsThroughMatchingCreateAndActiveScopes() {
        Object entityType = new Object();
        Object mob = new Object();
        CompoundTag tag = tag("source", "explicit");

        try (Scope pending = SpawnContext.pushPending(entityType, tag)) {
            assertNull(SpawnContext.currentTagFor(mob));

            try (Scope create = SpawnContext.enterCreate(entityType)) {
                assertNull(SpawnContext.currentTagFor(mob));

                try (Scope active = SpawnContext.enterActiveFromCreate(mob)) {
                    assertSame(tag, SpawnContext.currentTagFor(mob));
                }

                assertNull(SpawnContext.currentTagFor(mob));
            }

            assertNull(SpawnContext.currentTagFor(mob));
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void pendingTagIsNotConsumedByDifferentEntityType() {
        Object expectedType = new Object();
        Object otherType = new Object();
        Object mob = new Object();
        CompoundTag tag = tag("source", "expected");

        try (Scope pending = SpawnContext.pushPending(expectedType, tag)) {
            try (Scope unrelatedCreate = SpawnContext.enterCreate(otherType);
                 Scope unrelatedActive = SpawnContext.enterActiveFromCreate(mob)) {
                assertNull(SpawnContext.currentTagFor(mob));
            }

            try (Scope matchingCreate = SpawnContext.enterCreate(expectedType);
                 Scope matchingActive = SpawnContext.enterActiveFromCreate(mob)) {
                assertSame(tag, SpawnContext.currentTagFor(mob));
            }
        }
    }

    @Test
    void matchingPendingTagIsClaimedByOnlyFirstCreate() {
        Object entityType = new Object();
        Object mob = new Object();
        CompoundTag tag = tag("source", "one-shot");

        try (Scope pending = SpawnContext.pushPending(entityType, tag)) {
            try (Scope firstCreate = SpawnContext.enterCreate(entityType);
                 Scope firstActive = SpawnContext.enterActiveFromCreate(mob)) {
                assertSame(tag, SpawnContext.currentTagFor(mob));
            }

            // The pending scope remains open until the outer spawn returns, but
            // the first matching create has claimed it. Later reentrant creates
            // must not receive the same legacy tag.
            try (Scope laterCreate = SpawnContext.enterCreate(entityType);
                 Scope laterActive = SpawnContext.enterActiveFromCreate(mob)) {
                assertNull(SpawnContext.currentTagFor(mob));
            }
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void nestedNullTagShadowsAndThenRestoresOuterActiveTagForSameMob() {
        Object outerType = new Object();
        Object innerType = new Object();
        Object mob = new Object();
        CompoundTag outerTag = tag("source", "outer");

        try (Scope outerPending = SpawnContext.pushPending(outerType, outerTag);
             Scope outerCreate = SpawnContext.enterCreate(outerType);
             Scope outerActive = SpawnContext.enterActiveFromCreate(mob)) {
            assertSame(outerTag, SpawnContext.currentTagFor(mob));

            try (Scope innerPending = SpawnContext.pushPending(innerType, null);
                 Scope innerCreate = SpawnContext.enterCreate(innerType);
                 Scope innerActive = SpawnContext.enterActiveFromCreate(mob)) {
                // A present-but-null inner tag must not fall through to the outer Active frame.
                assertNull(SpawnContext.currentTagFor(mob));
            }

            assertSame(outerTag, SpawnContext.currentTagFor(mob));
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void nestedCreateWithoutPendingTagDoesNotInheritParentTag() {
        Object entityType = new Object();
        Object mob = new Object();
        CompoundTag outerTag = tag("source", "outer");

        try (Scope pending = SpawnContext.pushPending(entityType, outerTag);
             Scope create = SpawnContext.enterCreate(entityType);
             Scope active = SpawnContext.enterActiveFromCreate(mob)) {
            assertSame(outerTag, SpawnContext.currentTagFor(mob));

            try (Scope nestedCreate = SpawnContext.enterCreate(entityType);
                 Scope nestedActive = SpawnContext.enterActiveFromCreate(mob)) {
                assertNull(SpawnContext.currentTagFor(mob));
            }

            assertSame(outerTag, SpawnContext.currentTagFor(mob));
        }
    }

    @Test
    void explicitActiveScopeUsesItsSuppliedTag() {
        Object mob = new Object();
        CompoundTag tag = tag("source", "direct-call");

        try (Scope active = SpawnContext.pushActive(mob, tag)) {
            assertSame(tag, SpawnContext.currentTagFor(mob));
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void scopesAreCleanedWhenNestedWorkThrows() {
        Object entityType = new Object();
        Object mob = new Object();
        CompoundTag tag = tag("source", "exception");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            try (Scope pending = SpawnContext.pushPending(entityType, tag);
                 Scope create = SpawnContext.enterCreate(entityType);
                 Scope active = SpawnContext.enterActiveFromCreate(mob)) {
                assertSame(tag, SpawnContext.currentTagFor(mob));
                throw new IllegalStateException("test failure");
            }
        });

        assertEquals("test failure", thrown.getMessage());
        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void scopesRequireLifoCloseAndCanBeClosedIdempotently() {
        Object entityType = new Object();
        Scope pending = SpawnContext.pushPending(entityType, null);
        Scope create = SpawnContext.enterCreate(entityType);

        assertThrows(IllegalStateException.class, pending::close);

        create.close();
        pending.close();
        pending.close();

        assertNull(SpawnContext.currentTagFor(new Object()));
    }

    private static CompoundTag tag(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }
}
