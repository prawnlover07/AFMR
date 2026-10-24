package dev.animal.afmr.probe;

import com.google.gson.Gson;
import dev.animal.afmr.Afmr;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistTemplates;
import dev.animal.afmr.ui.LibraryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

/** Relative catalog-scale measurements; no personal configuration or source file is written. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class PerformanceSuite {
    private static final Path results=Path.of("../../results/performance-results.txt").toAbsolutePath().normalize();
    private static final long[] postTimes=new long[4096],tickTimes=new long[4096];
    private static final int[] fps=new int[4096];
    private static long postStart,tickStart,phaseStart,observed,maxPost,maxTick;
    private static int startup,phase=-1,phaseTicks,count;
    private static boolean finished;
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib,metadata;
    private static Playlist small,large;
    private static String label="";
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforeTick(ClientTickEvent.Pre e){tickStart=System.nanoTime();}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforePost(ClientTickEvent.Post e){postStart=System.nanoTime();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void after(ClientTickEvent.Post e){
        if(!Boolean.getBoolean("afmr.probe.performance")||finished)return;
        long end=System.nanoTime();mc=Minecraft.getInstance();
        try{
            if(mc.level==null||mc.player==null)return;
            if(phase<0){if(++startup<160)return;setup();next();return;}
            if(phaseTicks++>=60){
                int slot=(int)(observed++%postTimes.length);count=Math.min(count+1,postTimes.length);
                postTimes[slot]=end-postStart;tickTimes[slot]=end-tickStart;fps[slot]=mc.getFps();
                maxPost=Math.max(maxPost,postTimes[slot]);maxTick=Math.max(maxTick,tickTimes[slot]);
            }
            if(phase==4){if(rt.scanning()&&phaseTicks<12000)return;report();next();}
            else if(phaseTicks>=300){report();next();}
        }catch(Throwable ex){finished=true;log("FAIL "+ex);Afmr.LOG.error("Performance probe",ex);mc.stop();}
    }
    private static void log(String text){try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}Afmr.LOG.info("AFMR PERFORMANCE: {}",text);}
    private static void setup()throws Exception{
        Files.createDirectories(results.getParent());Files.writeString(results,"");
        rt=ClientRuntime.get();lib=rt.library();mc.options.pauseOnLostFocus=false;
        mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
        try(var reader=Files.newBufferedReader(Path.of("performance-library.json"))){metadata=new Gson().fromJson(reader,Library.class);}
        log("Real catalog metadata: "+metadata.tracks.size()+" local tracks / "+metadata.folders.size()+" linked folders. Source music remains read-only.");
        lib.tracks.removeIf(t->!t.vanilla);lib.playlists=new ArrayList<>(PlaylistTemplates.create());lib.folders.clear();
        for(var p:lib.playlists){p.enabled=false;p.includeVanilla=false;}
        var tone=new Track("file:performance_fixture",Path.of("../../fixtures/long.wav").toAbsolutePath().normalize().toString(),"Generated performance fixture",false);
        lib.tracks.add(tone);lib.rebuildTrackIndex();small=lib.createPlaylist("Small active playlist");small.includeVanilla=false;small.tracks.put(tone.id,true);
        large=lib.createPlaylist("Large catalog playlist");large.enabled=false;large.includeVanilla=false;
        lib.settings.bufferSeconds=lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=1;lib.settings.rescanOnLaunch=false;lib.settings.jukeboxMode=JukeboxMode.OFF;
        rt.nextSong();mc.gui.setScreen(null);
    }
    private static void next()throws Exception{
        phase++;phaseTicks=count=0;observed=maxPost=maxTick=0;phaseStart=System.nanoTime();
        switch(phase){
            case 0->label="Small library / gameplay";
            case 1->{lib.tracks.addAll(metadata.tracks);lib.rebuildTrackIndex();label="Full metadata / gameplay";}
            case 2->{for(var t:metadata.tracks)large.tracks.put(t.id,true);label="Full assignments / gameplay";}
            case 3->{long start=System.nanoTime();mc.gui.setScreen(new LibraryScreen(null,rt,large.id));log("Large workspace open ms: "+(System.nanoTime()-start)/1e6);label="Full assignments / workspace";}
            case 4->{
                if(Boolean.getBoolean("afmr.probe.skipScan")){complete();return;}
                mc.gui.setScreen(null);lib.folders.addAll(metadata.folders);long start=System.nanoTime();rt.rescan();
                log("Rescan request/snapshot ms: "+(System.nanoTime()-start)/1e6);label="Read-only real-folder rescan / gameplay";
            }
            case 5->{label="Post-rescan / gameplay";}
            default->{complete();return;}
        }
        log("PHASE "+phase+" "+label);
    }
    private static void complete()throws Exception{finished=true;log("COMPLETE");Files.writeString(results.resolveSibling("performance-complete.txt"),"complete\n");mc.stop();}
    private static void report(){
        long[] post=Arrays.copyOf(postTimes,count),tick=Arrays.copyOf(tickTimes,count);int[] frames=Arrays.copyOf(fps,count);
        Arrays.sort(post);Arrays.sort(tick);Arrays.sort(frames);
        if(count==0){log("FAIL no samples for "+label);return;}
        log(String.format(Locale.ROOT,"%s | retained=%d observed=%d wall=%.2fs | post median=%.4f p95=%.4f whole-max=%.3f ms | tick median=%.4f p95=%.4f whole-max=%.3f ms | FPS median=%d p05=%d | usedHeap=%.1f MiB",
            label,count,observed,(System.nanoTime()-phaseStart)/1e9,post[count/2]/1e6,post[(int)(count*.95)]/1e6,maxPost/1e6,
            tick[count/2]/1e6,tick[(int)(count*.95)]/1e6,maxTick/1e6,frames[count/2],frames[(int)(count*.05)],
            (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576.0));
        if(phase==3)Screenshot.grab(mc.gameDirectory,"performance-full-library.png",mc.gameRenderer.mainRenderTarget(),1,m->{});
        if(phase==4)log("Rescan completed="+!rt.scanning()+"; issues="+(rt.sourceHealth()==null?-1:rt.sourceHealth().issues().size())+"; progress="+rt.scanProgress().stage()+" "+rt.scanProgress().processed()+" / "+rt.scanProgress().discovered());
    }
}
