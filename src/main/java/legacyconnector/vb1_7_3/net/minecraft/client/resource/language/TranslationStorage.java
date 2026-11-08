package legacyconnector.vb1_7_3.net.minecraft.client.resource.language;

import java.util.Properties;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/** Beta StringTranslate singleton, retaining mod overrides and using native language values for vanilla keys. */
public final class TranslationStorage {
    public static final TranslationStorage INSTANCE=new TranslationStorage();
    private final Properties translations=new Properties();
    private TranslationStorage(){}
    public static TranslationStorage getInstance(){return INSTANCE;}
    public String get(String key){
        String own=translations.getProperty(key);if(own!=null)return own;
        String value=ContentB173.translate(key);return value.equals(key)?net.minecraft.network.chat.Component.translatable(key).getString():value;
    }
    public String get(String key,Object... arguments){return String.format(get(key),arguments);}
    public String getClientTranslation(String key){String full=key+".name",value=get(full);return full.equals(value)?"":value;}
}
