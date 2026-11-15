package legacyconnector.vb1_7_3.net.minecraft.client.util;

/** The Beta login session a mod reads for the player's name ({@code Minecraft.session.username}). */
public class Session {
    public String username;
    public String sessionId;
    public String mpPass;
    public Session(String username,String sessionId){this.username=username;this.sessionId=sessionId;}
    /** The name of the logged-in native user; a dedicated server has none. */
    public static Session mcc$current(){
        String name="Player";
        // a dedicated server must not touch the client class at all: each attempt is refused and logged (a player's per-tick view made one)
        if(!net.neoforged.fml.loading.FMLEnvironment.dist.isClient())return new Session(name,"-");
        try{var user=net.minecraft.client.Minecraft.getInstance();if(user!=null&&user.getUser()!=null)name=user.getUser().getName();}
        catch(Throwable unavailable){/* no client */}
        return new Session(name,"-");
    }
}
