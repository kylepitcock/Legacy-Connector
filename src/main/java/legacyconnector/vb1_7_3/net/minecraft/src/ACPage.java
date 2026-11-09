package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/** ShockAhPI page membership, exposed through native advancement tabs. */
public class ACPage {
    private static int nextID=1;
    private static final List<ACPage> PAGES=new ArrayList<>();
    public final int id;
    public final String title;
    public final String mcc$namespace;
    public final int mcc$titleOrdinal;
    final ArrayList<Integer> list=new ArrayList<>();
    private final Set<Achievement> members=Collections.newSetFromMap(new IdentityHashMap<>());
    public ACPage(){this(0,"Minecraft");}
    public ACPage(String title){this(nextID++,title);}
    private ACPage(int id,String title){this.id=id;this.title=title;var owner=ContentB173.current();mcc$namespace=owner==null?"legacyconnector":owner.modernId;mcc$titleOrdinal=(int)PAGES.stream().filter(page->page.mcc$namespace.equals(mcc$namespace)&&Objects.equals(page.title,title)).count();PAGES.add(this);}
    public void addAchievements(Achievement... achievements){for(var achievement:achievements){list.add(achievement.id);members.add(achievement);}}
    public static ACPage mcc$pageOf(Achievement achievement){for(var page:PAGES)if(page.members.contains(achievement))return page;return null;}
    public static List<ACPage> mcc$pages(){return List.copyOf(PAGES);}
}
