package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthlibProperty1201Test {
    @Test
    void mapsLegacyPropertyStringGettersToRecordAccessors() {
        assertEquals("name", AuthlibProperty1201.mapMethodName(
                AuthlibProperty1201.OWNER, "getName", "()Ljava/lang/String;"));
        assertEquals("value", AuthlibProperty1201.mapMethodName(
                AuthlibProperty1201.OWNER, "getValue", "()Ljava/lang/String;"));
        assertEquals("signature", AuthlibProperty1201.mapMethodName(
                AuthlibProperty1201.OWNER, "getSignature", "()Ljava/lang/String;"));
    }

    @Test
    void leavesUnrelatedOwnersMethodsAndDescriptorsUntouched() {
        assertEquals("getValue", AuthlibProperty1201.mapMethodName(
                "example/Property", "getValue", "()Ljava/lang/String;"));
        assertEquals("getOther", AuthlibProperty1201.mapMethodName(
                AuthlibProperty1201.OWNER, "getOther", "()Ljava/lang/String;"));
        assertEquals("getValue", AuthlibProperty1201.mapMethodName(
                AuthlibProperty1201.OWNER, "getValue", "()Ljava/lang/Object;"));
        assertEquals("value", AuthlibProperty1201.mapMethodName(
                AuthlibProperty1201.OWNER, "value", "()Ljava/lang/String;"));
    }
}
