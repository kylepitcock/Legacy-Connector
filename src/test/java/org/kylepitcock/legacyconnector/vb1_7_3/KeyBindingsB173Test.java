package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding;
import org.junit.jupiter.api.Test;

class KeyBindingsB173Test {
    @Test void nonRepeatingBindingRequiresReleaseBeforeAnotherCallback(){
        var state=new KeyBindingsB173.Edge();
        assertFalse(state.sample(false,false));assertTrue(state.sample(true,false));
        assertFalse(state.sample(true,false));assertFalse(state.sample(true,false));
        assertFalse(state.sample(false,false));assertTrue(state.sample(true,false));
    }
    @Test void repeatingBindingFiresEveryHeldWorldTickAndReregistrationResetsTheEdge(){
        var state=new KeyBindingsB173.Edge();
        assertTrue(state.sample(true,true));assertTrue(state.sample(true,true));
        assertFalse(state.sample(false,true));assertTrue(state.sample(true,true));
        assertFalse(state.sample(true,false));state.reset();assertTrue(state.sample(true,false));
    }
    @Test void nativeRebindingAndPressedQueriesRemainLive(){
        var key=new KeyBinding("Fixture",30);int[] code={44};boolean[] down={false};
        key.mcc$bind(()->code[0],()->down[0],value->code[0]=value);
        assertEquals(44,KeyBinding.mcc$get$code(key));assertFalse(key.isPressed());
        code[0]=17;down[0]=true;
        assertEquals(17,KeyBinding.mcc$get$code(key));assertTrue(key.isPressed());
        KeyBinding.mcc$set$code(key,16);assertEquals(16,code[0]);assertEquals(16,KeyBinding.mcc$get$code(key));
    }
    @Test void originalGuiQueryUsesInstanceSemanticsAndNullMeansNoScreen(){
        class Parent {} class Child extends Parent {}
        assertTrue(GuiQueriesB173.matches(null,null));assertFalse(GuiQueriesB173.matches(null,new Child()));
        assertTrue(GuiQueriesB173.matches(Parent.class,new Child()));assertTrue(GuiQueriesB173.matches(Child.class,new Child()));
        assertFalse(GuiQueriesB173.matches(Child.class,new Parent()));assertFalse(GuiQueriesB173.matches(Parent.class,null));
    }
}
