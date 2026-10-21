package dev.animal.afmr.probe;

import dev.animal.afmr.*;
import dev.animal.afmr.audio.*;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.openal.AL10;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Test-only fixture. Not part of the AFMR release. All mutations stay in the copied game profile. */
@Mod(value="afmrprobe",dist=Dist.CLIENT)
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class NativeSuite {
    private record Step(int waitTicks,Runnable action) {}
    private record NativeState(int source,int state,float offset,float gain,int error,String track,long advances) {}
    private static Minecraft mc;
    private static final List<Step> steps=new ArrayList<>();
    private static ClientRuntime rt;
    private static Library lib;
    private static PlaybackEngine engine;
    private static int startup,step=-1,wait,passed,failed,codecWaited;
    private static volatile NativeState nativeState=new NativeState(0,0,0,0,0,"",0);
    private static NativeState frozen;
    private static double gap;
    private static String oldTrack;
    private static final Set<String> cycle=new HashSet<>();
    private static boolean started,finished;
    private static Path result;
    private static float frozenGain;
    private static SoundInstance otherSound;
    private static volatile int otherState;

    private static Object field(Object instance,String name)throws Exception{
        Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(instance);
    }
    private static void nativeSample()throws Exception{
        Object se=field(mc.getSoundManager(),"soundEngine");
        @SuppressWarnings("unchecked") var map=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(se,"instanceToChannel");
        Object audio=field(engine,"audio");
        SoundInstance sound=(SoundInstance)field(audio,"sound");
        var handle=sound==null?null:map.get(sound);
        if(handle!=null){
            String id=engine.current()==null?"":engine.current().id;
            handle.execute(channel->{
                try{
                    int src=(int)field(channel,"source");
                    int state=AL10.alGetSourcei(src,AL10.AL_SOURCE_STATE);
                    float offset=AL10.alGetSourcef(src,org.lwjgl.openal.AL11.AL_SEC_OFFSET),gain=AL10.alGetSourcef(src,AL10.AL_GAIN);
                    long advances=nativeState.advances+(Math.abs(nativeState.offset-offset)>0.002?1:0);
                    nativeState=new NativeState(src,state,offset,gain,AL10.alGetError(),id,advances);
                }catch(Exception ex){Afmr.LOG.error("Native probe failed",ex);}
            });return;
        }
        nativeState=new NativeState(0,0,0,0,0,"",0);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(finished)return;
        mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return;
        try{
            if(!started){if(++startup<160)return;start();started=true;if(finished)return;}
            nativeSample();
            if(wait-->0)return;
            if(++step>=steps.size()){
                finished=true;rt.save();
                log("COMPLETE: "+passed+" passed / "+failed+" failed. Native UI checks remain separate.");
                Files.writeString(result.resolveSibling("complete.txt"),passed+" passed / "+failed+" failed\n");return;
            }
            Step s=steps.get(step);wait=s.waitTicks;
            try{s.action.run();}catch(Throwable ex){failed++;log("FAIL step "+step+": "+ex);Afmr.LOG.error("Probe step failure",ex);}
        }catch(Exception ex){finished=true;Afmr.LOG.error("Native test fixture failed",ex);}
    }
    private static void log(String message){
        Afmr.LOG.info("AFMR NATIVE TEST: {}",message);
        try{Files.writeString(result,message+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void check(String name,boolean okay){
        if(okay)passed++;else failed++;
        log((okay?"PASS ":"FAIL ")+name+" | source="+engine.sourcePlaylistId()+" phase="+engine.phase()+" paused="+engine.paused()+" native="+nativeState+" context="+rt.context());
    }
    private static void add(int ticks,Runnable action){steps.add(new Step(ticks,action));}
    private static String current(){return engine.current()==null?"":engine.current().id;}
    private static Track track(String id,String name,String file){
        Track t=new Track(id,file,name,false);lib.tracks.add(t);return t;
    }
    private static void command(String text){
        var server=mc.getSingleplayerServer();if(server==null)throw new IllegalStateException("Only isolated singleplayer supported");
        server.execute(()->{
            var player=server.getPlayerList().getPlayers().getFirst();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(),text);
        });
    }
    private static void start()throws Exception{
        rt=ClientRuntime.get();lib=rt.library();engine=rt.engine();
        result=Path.of(Boolean.getBoolean("afmr.probe.keysOnly")?"../results/persistence-results.txt":"../results/native-results.txt").toAbsolutePath();Files.createDirectories(result.getParent());Files.writeString(result,"");
        log("AFMR 0.5.2 packaged JAR native integration. Generated fixtures / copied world only.");
        var ui=lib.playlists.stream().filter(p->p.name.equals("UI Test Playlist")).findFirst().orElse(null);
        if(ui!=null){
            check("UI priority and captured biome survive restart",lib.playlists.getFirst()==ui&&ui.conditions.biomeEnabled&&ui.conditions.biomes.contains("minecraft:dappled_forest"));
            check("independent entry/exit switches survive restart",ui.finishCurrentSong&&ui.finishCurrentSongOnExit);
            check("restored Minecraft timing persists",lib.settings.silenceMinSeconds==600&&lib.settings.silenceMaxSeconds==1200&&lib.settings.fadeSeconds==0&&lib.settings.bufferSeconds==0);
            check("5000-file library scan persists",lib.tracks.stream().filter(t->t.path.replace('\\','/').contains("/scan-fixtures/")).count()==5000);
            check("explicit pause is cleared by restart",!engine.paused());
            var keys=Arrays.asList(mc.options.keyMappings);
            check("physical Next and Pause bindings persist",keys.stream().anyMatch(k->k.getName().equals("key.afmr.next")&&k.getKey().getName().equals("key.keyboard.n"))&&keys.stream().anyMatch(k->k.getName().equals("key.afmr.pause")&&k.getKey().getName().equals("key.keyboard.p")));
        }
        if(Boolean.getBoolean("afmr.probe.keysOnly")){
            check("persistence fixture present",ui!=null);finished=true;log("PERSISTENCE COMPLETE: "+passed+" passed / "+failed+" failed.");return;
        }
        lib.tracks.removeIf(t->!t.vanilla);lib.playlists.removeIf(Playlist::custom);
        for(var p:lib.playlists){p.includeVanilla=false;p.enabled=true;p.manualOnly=false;p.tracks.clear();p.finishCurrentSong=false;p.finishCurrentSongOnExit=false;}
        String longFile=Path.of("../fixtures/long.wav").toAbsolutePath().toString();
        String shortFile=Path.of("../fixtures/short.wav").toAbsolutePath().toString();
        for(int i=0;i<4;i++)track("file:probe"+i,"Generated tone "+i,longFile);
        track("file:short","Generated short tone",shortFile);
        for(var p:lib.playlists)p.tracks.put("file:probe0",true);
        lib.playlist("day_outside").tracks.clear();for(int i=0;i<4;i++)lib.playlist("day_outside").tracks.put("file:probe"+i,true);
        var manual=lib.createPlaylist("Native tests - automatic off");manual.id="custom:manual";manual.enabled=false;
        for(int i=0;i<4;i++)manual.tracks.put("file:probe"+i,true);
        var shortList=lib.createPlaylist("Short one-shot");shortList.id="custom:short";shortList.manualOnly=true;shortList.tracks.put("file:short",true);
        lib.rebuildTrackIndex();
        lib.settings.silenceMinSeconds=1;lib.settings.silenceMaxSeconds=1;lib.settings.fadeSeconds=0.3;lib.settings.bufferSeconds=0.3;
        command("execute in minecraft:overworld run tp @s 2.5 90 2.5");command("gamemode creative @s");command("time set 6000");command("time pause");command("weather clear");
        engine.reset();rt.save();

        add(60,()->rt.nextSong());
        add(0,()->check("packaged mixins + local audio start",engine.current()!=null&&nativeState.state==AL10.AL_PLAYING&&nativeState.error==0));
        add(0,()->check("day outside without biome constraint",rt.context().day()&&!rt.context().underground()&&"day_outside".equals(engine.sourcePlaylistId())));
        add(20,()->{oldTrack=current();rt.togglePause();});
        add(60,()->{frozen=nativeState;check("native channel paused",engine.paused()&&frozen.state==AL10.AL_PAUSED);});
        add(0,()->check("native playback position stays fixed",nativeState.source==frozen.source&&Math.abs(nativeState.offset-frozen.offset)<0.002&&oldTrack.equals(current())));
        add(20,()->{mc.getSoundManager().pauseAllExcept(net.minecraft.sounds.SoundSource.MASTER);mc.getSoundManager().resume();});
        add(0,()->check("Minecraft global resume preserves AFMR pause",engine.paused()&&nativeState.state==AL10.AL_PAUSED&&Math.abs(nativeState.offset-frozen.offset)<0.002));
        add(20,()->rt.togglePause());
        add(0,()->check("native resume advances same source",nativeState.source==frozen.source&&nativeState.state==AL10.AL_PLAYING&&nativeState.advances>frozen.advances+3));
        add(10,()->{rt.preview(lib.track("file:probe0"));rt.togglePause();});
        add(40,()->{frozen=nativeState;check("pause before delayed stream startup",engine.paused()&&nativeState.state==AL10.AL_PAUSED);});
        add(0,()->check("delayed start never advances while paused",nativeState.source==frozen.source&&Math.abs(nativeState.offset-frozen.offset)<0.002));
        add(20,()->{rt.nextSong();check("Next queues without starting during pause",engine.paused()&&engine.current()==null);});
        add(20,()->rt.togglePause());
        add(0,()->check("queued Next resumes automatic choice",engine.current()!=null&&"day_outside".equals(engine.sourcePlaylistId())&&nativeState.state==AL10.AL_PLAYING));
        add(20,()->{lib.settings.silenceMinSeconds=8;lib.settings.silenceMaxSeconds=8;rt.skip();rt.togglePause();});
        add(60,()->gap=engine.remainingSilence(rt.now()));
        add(0,()->check("silent countdown frozen",Math.abs(engine.remainingSilence(rt.now())-gap)<0.002&&gap>7.9));
        add(20,()->{rt.togglePause();rt.nextSong();});
        add(0,()->check("Next bypasses long gap",engine.current()!=null&&nativeState.state==AL10.AL_PLAYING));
        add(20,()->rt.overridePlaylist("custom:manual",OverrideMode.UNTIL_RELEASED));
        add(0,()->check("held override plays Automatic-off playlist",engine.current()!=null&&"custom:manual".equals(engine.overrideId())&&"custom:manual".equals(engine.sourcePlaylistId())));
        add(20,()->{oldTrack=current();rt.nextSong();});
        add(0,()->check("Next retains held override and avoids repeat",!oldTrack.equals(current())&&"custom:manual".equals(engine.overrideId())));
        add(20,()->{lib.settings.silenceMinSeconds=1;lib.settings.silenceMaxSeconds=1;rt.overridePlaylist("custom:manual",OverrideMode.ONE_SONG);});
        add(20,()->{oldTrack=current();rt.nextSong();});
        add(0,()->check("Next retains one-song override",!oldTrack.equals(current())&&engine.overrideMode()==OverrideMode.ONE_SONG));
        add(80,()->rt.overridePlaylist("custom:short",OverrideMode.ONE_SONG));
        add(0,()->check("one-song natural completion returns to automatic",engine.overrideId()==null&&"day_outside".equals(engine.selected().id)));
        add(20,()->{lib.settings.fadeSeconds=0;lib.settings.bufferSeconds=0;rt.returnAutomatic();engine.reset();cycle.clear();rt.nextSong();});
        for(int i=0;i<8;i++){
            final int index=i;
            add(0,()->{if(index==4)cycle.clear();check("shuffle unique entry "+(index+1),cycle.add(current()));});
            add(12,()->{oldTrack=current();rt.nextSong();});
            add(0,()->check("shuffle boundary repeat guard "+(index+1),!oldTrack.equals(current())));
        }
        add(30,()->{command("time set 18000");});
        add(0,()->check("outdoor night selects Night outside",!rt.context().day()&&!rt.context().covered()&&"night_outside".equals(engine.sourcePlaylistId())));
        add(60,()->{
            command("fill 0 40 0 6 50 6 minecraft:stone hollow");command("execute in minecraft:overworld run tp @s 3.5 41 3.5");
        });
        add(0,()->check("enclosed dark area selects Dark exploration",rt.context().covered()&&rt.context().lightLevel()<=4&&"dark_exploration".equals(engine.sourcePlaylistId())));
        add(60,()->{command("setblock 3 42 3 minecraft:glowstone");});
        add(0,()->check("lit enclosed area below Y cutoff selects Underground",rt.context().covered()&&rt.context().underground()&&rt.context().lightLevel()>4&&"underground".equals(engine.sourcePlaylistId())));
        add(30,()->lib.settings.undergroundMaxY=40);
        add(0,()->check("Y cutoff requires BOTH cover and elevation",rt.context().covered()&&!rt.context().underground()&&"night_outside".equals(engine.sourcePlaylistId())));
        add(30,()->{lib.settings.undergroundMaxY=64;rt.captureHome();});
        add(0,()->check("captured Home takes priority",rt.context().atHome()&&"home".equals(engine.sourcePlaylistId())));
        add(30,()->{lib.home.configured=false;lib.settings.combatQuietSeconds=3;rt.attack();});
        add(0,()->check("attack trigger enters Combat",rt.context().combat()&&"combat".equals(engine.sourcePlaylistId())));
        add(100,()->{lib.settings.combatEnabled=false;rt.overridePlaylist("combat",OverrideMode.UNTIL_RELEASED);});
        add(0,()->check("forced Combat works with global trigger disabled",!rt.context().combat()&&"combat".equals(engine.sourcePlaylistId())&&"combat".equals(engine.overrideId())));
        add(80,()->{rt.returnAutomatic();lib.settings.darkInNether=true;command("execute in minecraft:the_nether run tp @s 2.5 200 2.5");});
        add(0,()->check("Nether toggle applies throughout dimension","minecraft:the_nether".equals(rt.context().dimension())&&"dark_exploration".equals(engine.sourcePlaylistId())));
        add(40,()->lib.settings.darkInNether=false);
        add(0,()->check("Nether toggle off excludes Dark exploration",!"dark_exploration".equals(engine.sourcePlaylistId())));
        add(100,()->{lib.settings.darkInEnd=true;command("execute in minecraft:the_end run tp @s 0.5 100 0.5");});
        add(0,()->check("End toggle applies throughout dimension","minecraft:the_end".equals(rt.context().dimension())&&"dark_exploration".equals(engine.sourcePlaylistId())));
        add(40,()->lib.settings.darkInEnd=false);
        add(0,()->check("End toggle off excludes Dark exploration",!"dark_exploration".equals(engine.sourcePlaylistId())));
        add(80,()->{command("execute in minecraft:overworld run tp @s 2.5 90 2.5");command("time set 6000");});
        add(20,()->{
            var p=lib.playlist("custom:manual");p.enabled=true;p.manualOnly=false;
            var c=p.conditions;c.dimensionEnabled=true;c.dimension="minecraft:overworld";c.biomeEnabled=true;c.biomes.add(rt.context().biome());
            c.timeEnabled=true;c.startTime=5000;c.endTime=7000;c.elevationEnabled=true;c.minY=(int)Math.floor(rt.context().y());c.maxY=c.minY;
            c.temperatureEnabled=true;c.minTemperature=rt.context().biomeTemperature();c.maxTemperature=c.minTemperature;
            c.cover=PlaylistConditions.State.NO;
        });
        add(0,()->check("combined custom biome/time/dimension/Y/temperature/cover matches","custom:manual".equals(engine.sourcePlaylistId())));
        add(20,()->lib.playlist("custom:manual").conditions.minY=lib.playlist("custom:manual").conditions.maxY+1);
        add(0,()->check("single failed custom condition falls through",!"custom:manual".equals(engine.sourcePlaylistId())));
        add(40,()->{lib.playlist("custom:manual").enabled=false;lib.settings.fadeSeconds=1;lib.settings.combatDeathMusic=DeathMusic.CUT;rt.overridePlaylist("combat",OverrideMode.UNTIL_RELEASED);});
        add(40,()->{rt.togglePause();command("kill @s");});
        add(0,()->check("combat death CUT while paused stops native channel",rt.context().dead()&&engine.paused()&&engine.current()==null&&nativeState.source==0));
        add(20,()->{rt.togglePause();rt.returnAutomatic();mc.player.respawn();});
        add(20,()->{
            var vanilla=lib.tracks.stream().filter(t->t.vanilla&&t.path.contains("a_familiar_room")).findFirst().orElseThrow();
            rt.preview(vanilla);
        });
        add(20,()->{rt.togglePause();});
        add(40,()->{frozen=nativeState;check("vanilla streamed music pauses",engine.paused()&&engine.current()!=null&&engine.current().vanilla&&nativeState.state==AL10.AL_PAUSED);});
        add(0,()->check("vanilla position retained",nativeState.source==frozen.source&&Math.abs(nativeState.offset-frozen.offset)<0.002));
        add(30,()->{rt.togglePause();});
        add(0,()->check("vanilla resumes on same source",nativeState.state==AL10.AL_PLAYING&&nativeState.source==frozen.source));
        add(50,()->{rt.togglePause();mc.getSoundManager().reload();});
        add(0,()->check("sound reload clears explicit pause",!engine.paused()));
        add(20,()->{lib.settings.fadeSeconds=0.5;lib.settings.bufferSeconds=0.5;lib.settings.combatEnabled=true;rt.nextSong();rt.save();});
        moreCases();
    }

    private static void moreCases(){
        add(20,()->{command("execute in minecraft:overworld run tp @s 2.5 90 2.5");command("time set 6000");lib.home.configured=false;lib.settings.combatEnabled=false;});
        for(int combination=0;combination<4;combination++){
            final int flags=combination;
            add(20,()->{
                lib.settings.fadeSeconds=0.2;lib.settings.bufferSeconds=0;command("time set 6000");
                lib.playlist("day_outside").finishCurrentSongOnExit=(flags&1)!=0;
                lib.playlist("night_outside").finishCurrentSong=(flags&2)!=0;
                engine.reset();
            });
            add(20,()->rt.nextSong());
            add(40,()->{oldTrack=current();command("time set 18000");});
            add(0,()->check("entry/exit protection combination "+flags,flags==0?!"day_outside".equals(engine.sourcePlaylistId()):oldTrack.equals(current())&&"day_outside".equals(engine.sourcePlaylistId())));
        }
        add(20,()->{lib.playlist("day_outside").finishCurrentSongOnExit=true;lib.playlist("night_outside").finishCurrentSong=false;command("time set 6000");rt.nextSong();});
        add(20,()->{oldTrack=current();command("time set 18000");});
        add(20,()->{var p=lib.playlist("custom:manual");p.enabled=true;p.conditions=new PlaylistConditions();});
        add(0,()->check("A retained through B to C keeps original source",oldTrack.equals(current())&&"day_outside".equals(engine.sourcePlaylistId())&&"custom:manual".equals(engine.selected().id)));
        add(20,()->{lib.playlist("custom:manual").enabled=false;command("time set 6000");});
        add(0,()->check("returning to retained source avoids restart",oldTrack.equals(current())&&"day_outside".equals(engine.sourcePlaylistId())&&engine.phase()==PlaybackEngine.Phase.PLAYING));
        add(20,()->{
            for(var p:lib.playlists){p.finishCurrentSong=false;p.finishCurrentSongOnExit=false;}
            lib.settings.fadeSeconds=4;rt.preview(lib.track("file:probe0"));
        });
        add(20,()->{rt.togglePause();});
        add(40,()->{frozenGain=nativeState.gain;frozen=nativeState;});
        add(0,()->check("native fade-in gain and position frozen",nativeState.state==AL10.AL_PAUSED&&Math.abs(nativeState.gain-frozenGain)<0.002&&Math.abs(nativeState.offset-frozen.offset)<0.002));
        add(10,()->rt.togglePause());
        add(0,()->check("native fade-in continues without jumping to full gain",nativeState.gain>frozenGain&&nativeState.gain<0.9));
        add(20,()->{lib.settings.fadeSeconds=0;engine.reset();rt.nextSong();});
        add(20,()->{lib.settings.fadeSeconds=4;command("time set 18000");});
        add(20,()->rt.togglePause());
        add(40,()->{frozenGain=nativeState.gain;frozen=nativeState;});
        add(0,()->check("native fade-out gain and position frozen",engine.phase()==PlaybackEngine.Phase.FADING&&nativeState.state==AL10.AL_PAUSED&&Math.abs(nativeState.gain-frozenGain)<0.002&&Math.abs(nativeState.offset-frozen.offset)<0.002));
        add(10,()->rt.togglePause());
        add(0,()->check("native fade-out continues at held gain",nativeState.gain<frozenGain&&nativeState.gain>0));
        add(40,()->{lib.settings.fadeSeconds=0;engine.reset();rt.nextSong();});
        add(20,()->{
            rt.togglePause();
            otherSound=new net.minecraft.client.resources.sounds.SimpleSoundInstance(net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value().location(),net.minecraft.sounds.SoundSource.AMBIENT,0.01f,1,SoundInstance.createUnseededRandom(),true,0,SoundInstance.Attenuation.NONE,0,0,0,true);
            mc.getSoundManager().play(otherSound);
        });
        add(10,()->{
            try{
                var se=field(mc.getSoundManager(),"soundEngine");
                @SuppressWarnings("unchecked") var map=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(se,"instanceToChannel");
                var handle=map.get(otherSound);if(handle!=null)handle.execute(c->{try{otherState=AL10.alGetSourcei((int)field(c,"source"),AL10.AL_SOURCE_STATE);}catch(Exception ex){throw new RuntimeException(ex);}});
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
        add(0,()->check("non-music audio continues during AFMR pause",nativeState.state==AL10.AL_PAUSED&&otherState==AL10.AL_PLAYING));
        add(20,()->{mc.getSoundManager().stop(otherSound);rt.togglePause();lib.settings.combatDeathMusic=DeathMusic.FADE;lib.settings.combatDeathFadeSeconds=1;rt.overridePlaylist("combat",OverrideMode.UNTIL_RELEASED);});
        add(10,()->{command("kill @s");});
        add(0,()->check("combat death begins configured native fade immediately",rt.context().dead()&&engine.phase()==PlaybackEngine.Phase.FADING&&nativeState.gain<1&&nativeState.gain>0));
        add(30,()->{});
        add(0,()->check("combat death fade stops and blocks new dead-player music",rt.context().dead()&&engine.current()==null&&nativeState.source==0));
        add(30,()->{rt.returnAutomatic();mc.player.respawn();});
        add(30,()->{rt.overridePlaylist("combat",OverrideMode.UNTIL_RELEASED);});
        add(30,()->{rt.togglePause();command("kill @s");});
        add(0,()->check("death fade requested while paused retains native paused source",rt.context().dead()&&engine.phase()==PlaybackEngine.Phase.FADING&&nativeState.state==AL10.AL_PAUSED));
        add(30,()->rt.togglePause());
        add(0,()->check("death fade completes after explicit resume",engine.current()==null&&nativeState.source==0));
        add(30,()->{rt.returnAutomatic();mc.player.respawn();});
        add(30,()->{lib.settings.combatDeathMusic=DeathMusic.KEEP_PLAYING;rt.overridePlaylist("combat",OverrideMode.UNTIL_RELEASED);});
        add(30,()->{oldTrack=current();command("kill @s");});
        add(0,()->check("combat death Finish retains the playing song",rt.context().dead()&&oldTrack.equals(current())&&engine.finishingAfterDeath()&&nativeState.state==AL10.AL_PLAYING));
        add(30,()->{rt.returnAutomatic();mc.player.respawn();});
        add(20,()->{lib.settings.fadeSeconds=0;lib.settings.combatDeathMusic=DeathMusic.FADE;command("time set 6000");rt.nextSong();});
        add(20,()->{rt.togglePause();rt.overridePlaylist("custom:manual",OverrideMode.ONE_SONG);});
        add(0,()->check("override request while paused does not start it",engine.paused()&&nativeState.state==AL10.AL_PAUSED));
        add(20,()->rt.togglePause());
        add(0,()->check("queued override starts after resume","custom:manual".equals(engine.sourcePlaylistId())&&nativeState.state==AL10.AL_PLAYING));
        add(20,()->{
            rt.returnAutomatic();var p=lib.playlist("custom:manual");p.tracks.replaceAll((id,on)->false);rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);
        });
        add(0,()->check("all-disabled override target refused",engine.overrideId()==null));
        add(20,()->{
            var p=lib.playlist("custom:manual");p.tracks.replaceAll((id,on)->true);rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);
        });
        add(20,()->lib.playlist("custom:manual").tracks.clear());
        add(0,()->check("emptied held override releases",engine.overrideId()==null));
        add(20,()->{
            var p=lib.playlist("custom:manual");p.tracks.put("file:probe0",true);rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);
        });
        add(20,()->lib.removePlaylist("custom:manual"));
        add(0,()->check("deleted override target releases",engine.overrideId()==null));
        for(String name:List.of("test.mp3","test.flac","test.ogg","test.wav","test24.flac","test24.wav","testfloat.wav")){
            add(3,()->{
                codecWaited=0;Track t=track("file:format"+name.replace('.','_'),"Format "+name,Path.of("../../../src/test/resources/audio/"+name).toAbsolutePath().normalize().toString());lib.rebuildTrackIndex();rt.preview(t);
            });
            add(0,()->{
                if(nativeState.state==AL10.AL_INITIAL&&codecWaited++<40){step--;wait=2;return;}
                check("packaged native decoder "+name,engine.current()!=null&&engine.current().title.equals("Format "+name)&&nativeState.state==AL10.AL_PLAYING&&nativeState.error==0);
            });
        }
        add(8,()->{
            try{
                Path invalid=Path.of("../fixtures/invalid.mp3").toAbsolutePath();Files.writeString(invalid,"invalid audio fixture");
                var t=track("file:invalid","Intentional decoder failure",invalid.toString());lib.rebuildTrackIndex();rt.preview(t);
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
        add(0,()->check("async decoder error quarantines and recovers",engine.failedTracks().contains("file:invalid")&&!engine.error().isBlank()));
        add(20,()->{engine.retryFailed();rt.nextSong();});
        add(0,()->check("Retry failed clears decoder quarantine",engine.failedTracks().isEmpty()&&engine.error().isBlank()&&nativeState.state==AL10.AL_PLAYING));
        add(20,()->{
            for(int i=0;i<70000;i++)lib.tracks.add(new Track("file:unused"+i,"C:/AFMR-test-fixtures/unused-"+i+".mp3","Unused "+i,false));lib.rebuildTrackIndex();
            long[] times=new long[2000];for(int i=0;i<2000;i++){long start=System.nanoTime();engine.tick(rt.now(),lib,rt.context(),rt.applicableVanilla());times[i]=System.nanoTime()-start;}
            Arrays.sort(times);log("PERFORMANCE 70,000 unused tracks: median "+times[1000]+" ns; p95 "+times[1900]+" ns per real engine tick (not full rendering/client tick)");
            check("70k-library native selection remains indexed",times[1900]<5_000_000&&engine.current()!=null);
            lib.tracks.removeIf(t->t.id.startsWith("file:unused"));lib.rebuildTrackIndex();
        });
        add(20,()->{lib.settings.combatEnabled=true;lib.settings.silenceMinSeconds=1;lib.settings.silenceMaxSeconds=1;lib.settings.fadeSeconds=0.5;lib.settings.bufferSeconds=0.5;rt.returnAutomatic();rt.nextSong();rt.save();});
    }
}
