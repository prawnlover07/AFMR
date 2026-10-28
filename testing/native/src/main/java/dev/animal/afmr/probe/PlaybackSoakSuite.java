package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.ui.LibraryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.management.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Sustained native lifecycle exercise. Never reads or writes personal game/music state. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class PlaybackSoakSuite {
    private static final Path results=Path.of(Boolean.getBoolean("afmr.probe.soakRestart")?"../../results/playback-soak-restart-results.txt":"../../results/playback-soak-results.txt");
    private record Audio(SoundInstance sound,int state,float gain,int error){}
    private static volatile Audio audio=new Audio(null,0,0,0);
    private static Minecraft mc;private static ClientRuntime rt;private static Library lib;
    private static boolean started,finished,paused;private static int startup,ticks,passed,failed;
    private static int samples,starts,musicMutedStarts,masterMutedStarts,gaps,covers,maxChannels,maxTextures,maxQueue;
    private static long began,sequence=-1;private static int lastSecond=-1;
    private static String alternate;private static CompletableFuture<Void> reload;
    private static final List<Long> heaps=new ArrayList<>();
    private static Object field(Object value,String name){for(Class<?> t=value.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(NoSuchFieldException ignored){}catch(Exception e){throw new RuntimeException(e);}throw new IllegalArgumentException(name);}
    private static void log(String text){try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}System.out.println("AFMR SOAK "+text);}
    private static void check(String text,boolean okay){if(okay)passed++;else failed++;log((okay?"PASS ":"FAIL ")+text);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.playbackSoak")||finished)return;mc=Minecraft.getInstance();
        try{
            if(!started){if(mc.level==null||mc.player==null||++startup<100)return;Files.createDirectories(results.toAbsolutePath().getParent());Files.writeString(results,"");prepare();started=true;began=System.nanoTime();return;}
            sampleAudio();if(Boolean.getBoolean("afmr.probe.soakRestart")){if(++ticks>80&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING&&(boolean)field(rt.overlay(),"hasCover")&&(boolean)field(rt.widgetBackground(),"uploaded")){check("Saved music decodes after restart",true);check("Saved artwork and GIF upload after restart",true);check("Restart has no OpenAL error",audio.error==0);finished=true;log("COMPLETE: "+passed+" passed / "+failed+" failed");mc.stop();}else if(ticks>1200)throw new IllegalStateException("Restart audio/artwork timed out");return;}observe();if(++ticks%10!=0)return;
            int second=(int)((System.nanoTime()-began)/1_000_000_000L);if(second==lastSecond)return;lastSecond=second;
            if(second>=600){finish();return;}
            if(second%60==0){System.gc();}
            if(second%60==2)measure(second);
            // Each minute contains ordinary, Music-muted and Master-muted natural starts/gaps.
            int mode=(second/60)%3;mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(mode==1?0d:1d);mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(mode==2?0d:1d);
            if(second%60==35&&!paused){rt.togglePause();paused=true;}
            if(second%60==40&&paused){rt.togglePause();paused=false;}
            if(second>0&&second%30==0&&!paused){var p=lib.playlists.get((second/30)%lib.playlists.size());rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);}
            if(second>0&&second%20==0){var p=lib.playlists.get((second/20)%lib.playlists.size());rt.queueTrack(p.tracks.keySet().iterator().next(),p.id);}
            if(second>0&&second%120==10){String next=lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT)?alternate:PlaylistProfiles.DEFAULT;rt.activateProfile(next);rt.overridePlaylist(lib.playlists.getFirst().id,OverrideMode.UNTIL_RELEASED);paused=false;}
            if(second>0&&second%90==15){mc.gui.setScreen(new LibraryScreen(null,rt,lib.playlists.getFirst().id));}
            if(second%90==20)mc.gui.setScreen(null);
            if(second==180||second==420){reload=mc.reloadResourcePacks();log("RESOURCE_RELOAD requested second="+second);}
            if(second%60==45){rt.widgetBackground().invalidate();rt.menuBackground().invalidate();rt.save();}
            if(!rt.engine().error().isBlank())throw new IllegalStateException(rt.engine().error());
        }catch(Throwable e){failed++;log("FAIL "+e);e.printStackTrace();finish();}
    }
    private static void prepare()throws Exception{
        rt=ClientRuntime.get();lib=rt.library();
        if(Boolean.getBoolean("afmr.probe.soakRestart")){
            check("History loads latest 100 after actual restart",rt.engine().history().size()==100);
            check("All restored history entries are valid",rt.engine().history().snapshot().stream().allMatch(ListeningHistory::valid));
            check("Shared generated catalog restores once",lib.tracks.stream().filter(t->t.id.startsWith("file:soak_")).count()==64);
            check("Default profile and alternate restore",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT)&&lib.profiles.inactive.size()==1);
            check("Playlist assignments restore",lib.playlists.size()==4&&lib.playlists.stream().allMatch(p->p.tracks.size()==16));
            check("Alternate profile remains independent",lib.profiles.inactive.get(0).playlists.size()==4&&lib.profiles.inactive.get(0).playlists.get(0)!=lib.playlists.get(0));
            check("Group timing settings restore",lib.settings.songsBeforeSilence==2&&lib.settings.silenceMinSeconds==1&&lib.settings.silenceMaxSeconds==1);
            check("Widget image settings restore",lib.settings.widgetImageEnabled&&!lib.settings.widgetImageName.isBlank());
            check("Session queue is empty on restart",rt.engine().queue().empty());
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1d);mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1d);
            rt.overridePlaylist(lib.playlists.get(0).id,OverrideMode.UNTIL_RELEASED);return;
        }
        rt.engine().reset();rt.clearHistory();lib.profiles=new PlaylistProfiles();lib.playlists=new ArrayList<>();lib.folders.clear();lib.tracks.removeIf(t->!t.vanilla);
        lib.settings.rescanOnLaunch=false;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=.1;lib.settings.bufferSeconds=0;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=1;lib.settings.songsBeforeSilence=2;lib.settings.jukeboxMode=JukeboxMode.OFF;
        lib.settings.nowPlaying=true;lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;lib.settings.widgetArtwork=true;lib.settings.popupDelaySeconds=0;lib.settings.popupSeconds=3;
        lib.settings.menuFontId=lib.settings.widgetFontId="minecraft:default";mc.options.pauseOnLostFocus=false;mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1d);mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1d);mc.gui.setScreen(null);
        Path root=mc.gameDirectory.toPath().toAbsolutePath().normalize().resolve("soak-fixtures/"+System.currentTimeMillis());Files.createDirectories(root);Files.copy(Path.of("../../fixtures/with-cover/cover.png"),root.resolve("cover.png"));
        for(int i=0;i<4;i++){var p=new Playlist("soak-"+i,"Soak "+i);p.enabled=false;p.includeVanilla=false;lib.playlists.add(p);for(int j=0;j<16;j++){Path file=root.resolve("tone-"+i+"-"+j+".wav");Files.copy(Path.of("../../fixtures/short.wav"),file);var track=new Track("file:soak_"+i+"_"+j,file.toString(),"Generated "+i+" / "+j,false);track.durationSeconds=1.5;lib.tracks.add(track);p.tracks.put(track.id,true);}}
        lib.rebuildTrackIndex();alternate=lib.profiles.create(lib,"Alternate soak",PlaylistProfiles.DEFAULT);rt.menuBackground().enabled(false);rt.widgetBackground().choose(Path.of("../../../../src/test/resources/backgrounds/disposal.gif"));rt.overridePlaylist(lib.playlists.getFirst().id,OverrideMode.UNTIL_RELEASED);log("START duration=600s; tracks=64; profiles=2; playlist=4; generated audio/artwork; music/master mute, queues, pauses, profile changes, reloads");
    }
    @SuppressWarnings("unchecked") private static void sampleAudio(){
        Object engine=field(mc.getSoundManager(),"soundEngine");var channels=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(engine,"instanceToChannel");
        int count=0;for(var sound:channels.keySet())if(sound.getClass().getName().equals("dev.animal.afmr.audio.MusicSound"))count++;maxChannels=Math.max(maxChannels,count);
        var sound=(SoundInstance)field(field(rt.engine(),"audio"),"sound");var handle=sound==null?null:channels.get(sound);if(handle==null)return;
        handle.execute(channel->{int source=(int)field(channel,"source");audio=new Audio(sound,org.lwjgl.openal.AL10.alGetSourcei(source,org.lwjgl.openal.AL10.AL_SOURCE_STATE),org.lwjgl.openal.AL10.alGetSourcef(source,org.lwjgl.openal.AL10.AL_GAIN),org.lwjgl.openal.AL10.alGetError());});
    }
    private static void observe(){
        samples++;if(rt.engine().phase()==PlaybackEngine.Phase.SILENCE)gaps++;if((boolean)field(rt.overlay(),"hasCover"))covers++;
        maxQueue=Math.max(maxQueue,rt.engine().queue().size());
        var map=(Map<?,?>)field(mc.getTextureManager(),"byPath");int textures=0;for(var key:map.keySet())if(key.toString().startsWith("afmr:"))textures++;maxTextures=Math.max(maxTextures,textures);
        var current=rt.engine().current();if(current==null||rt.engine().playbackSequence()==sequence)return;sequence=rt.engine().playbackSequence();starts++;
        // Queues explicitly select a song and may repeat it; shuffle is checked in its dedicated suite.
        if(mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).get()==0)musicMutedStarts++;
        if(mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).get()==0)masterMutedStarts++;
    }
    private static void measure(int second){
        long heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();heaps.add(heap);long direct=0,buffers=0;for(var pool:ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)){direct+=pool.getMemoryUsed();buffers+=pool.getCount();}
        log("MEASURE second="+second+" heapMiB="+heap/1048576.0+" bufferMiB="+direct/1048576.0+" buffers="+buffers+" starts="+starts+" history="+rt.engine().history().size()+" channelsMax="+maxChannels+" texturesMax="+maxTextures+" audio="+audio+" fps="+mc.getFps());
    }
    private static void finish(){
        if(finished)return;finished=true;if(!started){mc.stop();return;}
        check("At least 180 real playback starts",starts>=180);check("Natural starts continue with Music muted",musicMutedStarts>=20);check("Natural starts continue with Master muted",masterMutedStarts>=20);check("Normal silence occurs during sustained playback",gaps>100);check("Artwork loads repeatedly",covers>100);check("AFMR native channels remain bounded",maxChannels<=4);check("AFMR textures remain bounded",maxTextures<=16);check("Listening history remains at latest 100",rt.engine().history().size()==100);check("Queue remains bounded",maxQueue<=3);check("Resource reload completes",reload!=null&&reload.isDone()&&!reload.isCompletedExceptionally());check("Sound executor reports no OpenAL error",audio.error==0);check("Frame cap retained",mc.options.framerateLimit().get()<=60);System.gc();measure(600);
        if(heaps.size()>3)log("RETAINED_HEAP firstMiB="+heaps.get(1)/1048576.0+" lastMiB="+heaps.get(heaps.size()-1)/1048576.0+" (world/renderer noise included; not a proof against every leak)");
        log("TOTAL starts="+starts+" musicMutedStarts="+musicMutedStarts+" masterMutedStarts="+masterMutedStarts+" silenceSamples="+gaps+" coverSamples="+covers+" tickSamples="+samples);log("COMPLETE: "+passed+" passed / "+failed+" failed");mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1d);mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1d);mc.stop();
    }
}
