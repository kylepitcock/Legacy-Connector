package legacyconnector.vb1_7_3.net.minecraft.client.sound;

import java.io.File;
import java.util.*;
import org.kylepitcock.legacyconnector.vb1_7_3.AudioB173;

/** Beta SoundPool: numbered effect variants share a name; streaming names keep digits. */
public class SoundEntry {
    public Random random = new Random();
    public Map<String,List<Sound>> weightedSoundSet = new LinkedHashMap<>();
    public List<Sound> loadedSounds = new ArrayList<>();
    public int loadedSoundCount;
    public boolean isRandom = true;
    private final Map<String,Sound> explicit = new LinkedHashMap<>();
    private List<Sound> external = List.of();
    private final AudioB173.Kind kind;

    public SoundEntry() { this(null); }
    public SoundEntry(AudioB173.Kind kind) { this.kind=kind; isRandom=kind!=AudioB173.Kind.STREAMING; }
    public static String key(String name, boolean random) {
        String key=name.replace('\\','/');
        int dot=key.indexOf('.'); if(dot>=0)key=key.substring(0,dot);
        if(random)while(!key.isEmpty() && Character.isDigit(key.charAt(key.length()-1)))key=key.substring(0,key.length()-1);
        return key.replace('/','.');
    }
    public Sound loadStatic(String name, File file) {
        try {
            Sound sound=new Sound(name,file.toURI().toURL());
            synchronized(this) { explicit.put(name,sound); rebuild(); }
            if(kind!=null)AudioB173.register(kind,name,file.toPath(),isRandom);
            return sound;
        } catch(java.net.MalformedURLException e) { throw new IllegalArgumentException(e); }
    }
    public synchronized void mcc$external(List<AudioB173.Source> sources) {
        external=sources.stream().map(s->{try{return new Sound(s.name(),s.file().toUri().toURL());}catch(java.net.MalformedURLException e){throw new IllegalArgumentException(e);}}).toList();
        rebuild();
    }
    private void rebuild() {
        weightedSoundSet.clear();loadedSounds.clear();
        Map<String,Sound> all=new LinkedHashMap<>();for(Sound s:external)all.put(s.id,s);all.putAll(explicit);
        for(Sound s:all.values()){loadedSounds.add(s);weightedSoundSet.computeIfAbsent(key(s.id,isRandom),k->new ArrayList<>()).add(s);}
        loadedSoundCount=loadedSounds.size();
    }
    public synchronized Sound get(String name) { var list=weightedSoundSet.get(name);return list==null||list.isEmpty()?null:list.get(random.nextInt(list.size())); }
    public synchronized Sound getSounds() { return loadedSounds.isEmpty()?null:loadedSounds.get(random.nextInt(loadedSounds.size())); }
}
