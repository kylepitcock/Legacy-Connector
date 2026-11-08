package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.widget.TextFieldWidget;
import legacyconnector.vb1_7_3.net.minecraft.client.resource.language.TranslationStorage;
import org.junit.jupiter.api.Test;

class TextFieldWidgetB173Test {
    private static final class Font extends TextRenderer {
        String rendered;int color;
        @Override public void drawWithShadow(String text,int x,int y,int color){rendered=text;this.color=color;}
    }
    private static final class Parent extends Screen {int tabs;@Override public void handleTab(){tabs++;}}
    @Test void focusEnabledStateCharacterRepertoireAndOriginalLengthRulesControlEditing(){
        var field=new TextFieldWidget(null,new Font(),10,20,100,20,"Pet");
        field.keyPressed('x',45);assertEquals("Pet",field.getText());
        field.mouseClicked(10,20,0);assertTrue(field.focused);field.setMaxLength(4);field.keyPressed('x',45);field.keyPressed('y',21);assertEquals("Petx",field.getText());
        field.keyPressed('\b',14);field.keyPressed('\u00a7',0);field.keyPressed('\u6f22',0);assertEquals("Pet",field.getText());
        field.keyPressed('\u00e9',0);assertEquals("Pet\u00e9",field.getText());
        field.enabled=false;field.keyPressed('\b',14);assertEquals("Pet\u00e9",field.getText());field.mouseClicked(20,25,0);assertFalse(field.focused);
        field.enabled=true;field.mouseClicked(110,25,0);assertFalse(field.focused);field.mouseClicked(20,40,0);assertFalse(field.focused);
        field.setText("longer than the cap");assertEquals("longer than the cap",field.getText());
        field.setMaxLength(0);field.setFocused(true);field.keyPressed('!',0);assertTrue(field.getText().endsWith("!"));
    }
    @Test void tabDelegatesToOriginalScreenAndCaretResetsOnlyOnFocusTransition(){
        var parent=new Parent();var font=new Font();var field=new TextFieldWidget(parent,font,0,0,80,20,"Name");field.setFocused(true);
        field.keyPressed('\t',15);assertEquals(1,parent.tabs);field.render();assertEquals("Name_",font.rendered);
        for(int i=0;i<6;i++)field.tick();field.setFocused(true);field.render();assertEquals("Name",font.rendered);
        field.setFocused(false);field.setFocused(true);field.render();assertEquals("Name_",font.rendered);
        field.enabled=false;field.render();assertEquals("Name",font.rendered);assertEquals(0x707070,font.color);
    }
    @Test void translationSingletonResolvesVanillaLabelsAndModOverrides(){
        var language=TranslationStorage.getInstance();assertSame(TranslationStorage.INSTANCE,language);
        assertEquals("Cancel",language.get("gui.cancel"));assertEquals("missing.beta.key",language.get("missing.beta.key"));
        ContentB173.addLocalization("test.beta.pet.format","Pet %s");ContentB173.addLocalization("test.beta.pet.name","Original pet");
        assertEquals("Pet Ada",language.get("test.beta.pet.format","Ada"));assertEquals("Original pet",language.getClientTranslation("test.beta.pet"));assertEquals("",language.getClientTranslation("test.beta.unknown"));
    }
    @Test void clipboardIsScopedAndKeepsBetasSeparateThirtyTwoCharacterPasteLimit(){
        var field=new TextFieldWidget(null,new Font(),0,0,80,20,"Pet");field.setFocused(true);field.setMaxLength(4);
        String before=Screen.getClipboard();
        Screen.mcc$withClipboard("x".repeat(40),()->{
            field.keyPressed((char)22,47);assertEquals("Pet"+"x".repeat(29),field.getText());
            Screen.mcc$withClipboard("nested",()->assertEquals("nested",Screen.getClipboard()));assertEquals("x".repeat(40),Screen.getClipboard());
        });
        assertEquals(before,Screen.getClipboard());field.keyPressed('!',0);assertEquals(32,field.getText().length());
    }
}
