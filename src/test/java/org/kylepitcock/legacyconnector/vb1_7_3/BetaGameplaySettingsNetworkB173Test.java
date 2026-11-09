package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BetaGameplaySettingsNetworkB173Test {
    @Test void requestContainsOnlySessionAndModeContainsOneServerBoolean(){
        var hello=BetaGameplaySettingsNetworkB173.encode(0,726,true);
        assertEquals(4,hello.length);
        assertEquals(new BetaGameplaySettingsNetworkB173.Message(726,false),BetaGameplaySettingsNetworkB173.decode(0,hello));
        for(boolean mode:new boolean[]{false,true}){
            var bytes=BetaGameplaySettingsNetworkB173.encode(1,726,mode);assertEquals(5,bytes.length);
            assertEquals(new BetaGameplaySettingsNetworkB173.Message(726,mode),BetaGameplaySettingsNetworkB173.decode(1,bytes));
        }
    }
    @Test void malformedAndOversizedPayloadsAreRejected(){
        for(int kind:new int[]{-1,0,1,2})for(int size:new int[]{0,1,3,6,1024})assertNull(BetaGameplaySettingsNetworkB173.decode(kind,new byte[size]));
        assertNull(BetaGameplaySettingsNetworkB173.decode(0,new byte[5]));
        assertNull(BetaGameplaySettingsNetworkB173.decode(1,new byte[4]));
        var bytes=BetaGameplaySettingsNetworkB173.encode(1,1,true);bytes[4]=2;
        assertNull(BetaGameplaySettingsNetworkB173.decode(1,bytes));
        assertNull(BetaGameplaySettingsNetworkB173.decode(0,null));
    }
    @Test void retriesAreBoundedAndRepliesBelongToExactConnectionAndWorld(){
        var state=new BetaGameplaySettingsNetworkB173.ClientSession();var connection=new Object();var world=new Object();
        assertFalse(state.shouldRequest(0));assertTrue(state.bind(connection,world));int id=state.id();
        assertTrue(state.shouldRequest(0));assertFalse(state.shouldRequest(999_999_999L));assertTrue(state.shouldRequest(1_000_000_000L));
        assertFalse(state.bind(connection,world));assertFalse(state.accept(id,new Object(),world));
        assertFalse(state.accept(id,connection,new Object()));assertFalse(state.accept(id-1,connection,world));
        assertTrue(state.accept(id,connection,world));assertFalse(state.shouldRequest(2_000_000_000L));
        var nextWorld=new Object();assertTrue(state.bind(connection,nextWorld));assertNotEquals(id,state.id());
        assertFalse(state.accept(id,connection,nextWorld));assertTrue(state.shouldRequest(2_000_000_001L));
        state.clear();assertFalse(state.accept(state.id(),connection,nextWorld));assertFalse(state.shouldRequest(4_000_000_000L));
        assertTrue(state.bind(new Object(),nextWorld));assertNotEquals(id,state.id());assertTrue(state.shouldRequest(4_000_000_001L));
    }
}
