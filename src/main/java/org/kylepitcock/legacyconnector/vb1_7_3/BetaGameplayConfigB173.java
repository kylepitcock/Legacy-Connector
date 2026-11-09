package org.kylepitcock.legacyconnector.vb1_7_3;

import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;
import org.kylepitcock.legacyconnector.core.LegacyConfig;

/** Beta gameplay choices share the connector's properties file; remote clients follow their server. */
public final class BetaGameplayConfigB173 {
    public static final String FOOD_KEY="b173.foodUsesHunger";
    private static volatile Boolean serverFoodUsesHunger;
    private BetaGameplayConfigB173(){}
    public static boolean localFoodUsesHunger(){return LegacyConfig.b173FoodUsesHunger();}
    public static boolean foodUsesHunger(){
        Boolean value=serverFoodUsesHunger;
        return value!=null&&EffectiveSide.get()==LogicalSide.CLIENT?value:localFoodUsesHunger();
    }
    public static boolean hasServerOverride(){return serverFoodUsesHunger!=null;}
    public static void applyServerOverride(boolean value){serverFoodUsesHunger=value;}
    public static void clearServerOverride(){serverFoodUsesHunger=null;}
}
