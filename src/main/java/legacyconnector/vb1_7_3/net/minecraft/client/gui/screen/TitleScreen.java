package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen;

/** Native title-screen identity and read-only activity view for legacy main-menu base-class patches. */
public class TitleScreen extends Screen {
    public static boolean mmactive;
    private static volatile java.util.function.BooleanSupplier ACTIVITY;
    public TitleScreen(){}
    /** Installed only by the native client; common/dedicated code never loads client classes. */
    public static void mcc$nativeActivity(java.util.function.BooleanSupplier activity){ACTIVITY=activity;}
    public static boolean mcc$get$mmactive(){var activity=ACTIVITY;return activity!=null&&activity.getAsBoolean();}
    /** Menus before a world remain active; ordinary in-world screens do not become title menus. */
    public static boolean mcc$menuActive(boolean hasLevel,boolean hasScreen,boolean titleScreen){return titleScreen||!hasLevel&&hasScreen;}
    /**
     * The title screen is always a menu, even over a running world (a main menu that plays a world behind its buttons). While a world is
     * starting or joined, the loading screens before the client has a level are not menus: original code that freezes its player in menu
     * mode would otherwise pin a real player to an unset position during the first server ticks.
     */
    public static boolean mcc$menuActive(boolean hasLevel,boolean hasScreen,boolean titleScreen,boolean connectedOrHosting){
        return titleScreen||!connectedOrHosting&&mcc$menuActive(hasLevel,hasScreen,false);
    }
}
