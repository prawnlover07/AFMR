package dev.animal.afmr.probe;

import dev.animal.afmr.*;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Packaged AFMR against a separate, unmodified vanilla server on loopback only. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class MultiplayerSuite {
    private record Step(int waitTicks,Runnable action) {}
    private record Sample(int source,int state,float offset,int error) {}
    private static final List<Step> steps=new ArrayList<>();
    private static final Path result=Path.of("../../results/multiplayer-results.txt").toAbsolutePath();
    private static final String address="127.0.0.1:25575";
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static PlaybackEngine engine;
    private static int ticks,startup,step=-1,wait,passed,failed;
    private static boolean started,finished;
    // Only the sound executor writes native observations; the client consumes one immutable snapshot.
    private static volatile Sample sample=new Sample(0,0,0,0);
    private static Sample frozen;
    private static String homeKey,oldTrack,serverProfile;

    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.multiplayer")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(++ticks>12000)throw new IllegalStateException("Multiplayer fixture timed out");
            if(!started){
                if(mc.level==null||mc.player==null||++startup<120)return;
                if(mc.player.isDeadOrDying()){mc.player.respawn();startup=0;return;}
                Files.createDirectories(result.getParent());Files.writeString(result,"");
                rt=ClientRuntime.get();lib=rt.library();engine=rt.engine();
                start();started=true;
            }
            sampleAudio();
            if(wait-->0)return;
            if(++step==steps.size()){
                log("COMPLETE: "+passed+" passed / "+failed+" failed");
                Files.writeString(result.resolveSibling("multiplayer-complete.txt"),passed+" passed / "+failed+" failed\n");
                finished=true;mc.stop();return;
            }
            Step s=steps.get(step);wait=s.waitTicks();
            try{s.action().run();}catch(Throwable ex){failed++;log("FAIL step "+step+": "+ex);Afmr.LOG.error("Multiplayer probe step",ex);}
        }catch(Throwable ex){
            finished=true;Afmr.LOG.error("Multiplayer fixture stopped",ex);
            try{log("FATAL: "+ex);}finally{mc.stop();}
        }
    }
    private static void log(String value){
        Afmr.LOG.info("AFMR MULTIPLAYER TEST: {}",value);
        try{Files.writeString(result,value+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception ex){throw new RuntimeException(ex);}
    }
    private static void check(String name,boolean okay){
        if(okay)passed++;else failed++;
        log((okay?"PASS ":"FAIL ")+name+" | source="+engine.sourcePlaylistId()+" paused="+engine.paused()+" audio="+sample+" context="+rt.context());
    }
    private static void add(int ticks,Runnable action){steps.add(new Step(ticks,action));}
    private static void command(String text){mc.player.connection.sendCommand(text);}
    private static boolean source(String id){return id.equals(engine.sourcePlaylistId())&&engine.current()!=null;}
    private static boolean playing(){return sample.source()!=0&&sample.state()==AL10.AL_PLAYING&&sample.error()==0;}
    private static String current(){return engine.current()==null?"":engine.current().id;}
    private static Object field(Object object,String name)throws Exception{
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static void sampleAudio()throws Exception{
        Object soundEngine=field(mc.getSoundManager(),"soundEngine");
        @SuppressWarnings("unchecked") var channels=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(soundEngine,"instanceToChannel");
        var sound=(SoundInstance)field(field(engine,"audio"),"sound");
        var handle=sound==null?null:channels.get(sound);
        if(handle==null){sample=new Sample(0,0,0,0);return;}
        handle.execute(channel->{
            try{
                int id=(int)field(channel,"source");
                sample=new Sample(id,AL10.alGetSourcei(id,AL10.AL_SOURCE_STATE),AL10.alGetSourcef(id,AL11.AL_SEC_OFFSET),AL10.alGetError());
            }catch(Exception ex){Afmr.LOG.error("Multiplayer audio sample",ex);}
        });
    }
    private static void start(){
        log("Packaged AFMR; vanilla Minecraft 26.3 server; disposable profile and generated audio.");
        check("joined remote vanilla server without integrated server",mc.getSingleplayerServer()==null&&mc.getCurrentServer()!=null&&address.equals(mc.getCurrentServer().ip));
        check("unlinked vanilla server selects Default profile",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT));
        lib.tracks.removeIf(t->!t.vanilla);lib.playlists=new ArrayList<>(dev.animal.afmr.core.PlaylistTemplates.create());
        for(var p:lib.playlists){p.enabled=true;p.manualOnly=false;p.includeVanilla=false;p.tracks.clear();p.finishCurrentSong=false;p.finishCurrentSongOnExit=false;}
        String file=Path.of("../../fixtures/long.wav").toAbsolutePath().normalize().toString();
        for(int i=0;i<2;i++){
            var track=new Track("file:multiplayer"+i,file,"Multiplayer generated tone "+i,false);lib.tracks.add(track);
            for(var p:lib.playlists)p.tracks.put(track.id,true);
        }
        var manual=lib.createPlaylist("Multiplayer manual");manual.enabled=false;manual.tracks.put("file:multiplayer0",true);
        var custom=lib.createPlaylist("Multiplayer conditions");custom.enabled=false;custom.tracks.put("file:multiplayer1",true);
        lib.rebuildTrackIndex();lib.playlist("home").conditions.area.configured=false;
        lib.settings.bufferSeconds=0;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;lib.settings.silenceMinSeconds=0;lib.settings.silenceMaxSeconds=0;
        lib.settings.combatEnabled=true;lib.settings.combatQuietSeconds=2;
        command("time pause");command("time set 6000");command("weather clear");
        command("gamemode creative @s");
        command("fill 0 89 0 8 89 8 minecraft:stone");command("tp @s 3.5 90 3.5");
        engine.reset();
        add(80,()->{});
        add(0,()->check("local music and day outside on vanilla server",source("day_outside")&&playing()&&rt.context().day()));
        add(30,()->{oldTrack=current();rt.nextSong();});
        add(0,()->check("Next changes local song",source("day_outside")&&!oldTrack.equals(current())&&playing()));
        add(20,()->rt.togglePause());
        add(40,()->{frozen=sample;check("manual music pause while server continues",!mc.isPaused()&&engine.paused()&&sample.state()==AL10.AL_PAUSED);});
        add(30,()->{check("native position retained online",sample.source()==frozen.source()&&Math.abs(sample.offset()-frozen.offset())<0.002);command("time set 18000");});
        add(20,()->{check("night selection updates while music paused",!rt.context().day()&&"night_outside".equals(engine.selected().id));rt.togglePause();});
        add(0,()->check("night music resumes online",source("night_outside")&&playing()));
        add(40,()->rt.overridePlaylist(manual.id,OverrideMode.UNTIL_RELEASED));
        add(0,()->check("Automatic-off held override online",source(manual.id)&&manual.id.equals(engine.overrideId())&&playing()));
        add(20,()->rt.normalizePlaylist(manual.id));
        add(0,()->check("normalization waits without decoding during multiplayer",rt.loudnessJob()!=null&&rt.loudnessJob().report()==null&&rt.loudnessJob().completed()==0&&!rt.loudnessJob().allowed()));
        add(50,()->{rt.returnAutomatic();command("fill 0 40 0 6 50 6 minecraft:stone hollow");command("tp @s 3.5 41 3.5");});
        add(0,()->check("synchronized cover and darkness",rt.context().covered()&&rt.context().coverDepth()>=9&&source("dark_exploration")));
        add(40,()->command("setblock 3 43 3 minecraft:glowstone"));
        add(0,()->check("synchronized light and Underground",rt.context().lightLevel()>4&&lib.playlist("underground").conditions.matches(rt.context(),lib.settings)&&source("underground")));
        add(20,()->{lib.playlist("home").autoplayHere(rt.context());homeKey=rt.worldKey();});
        add(0,()->check("Home captures server address and location",("server:"+address).equals(homeKey)&&lib.playlist("home").conditions.matches(rt.context(),lib.settings)&&source("home")));
        add(40,()->{lib.playlist("home").conditions.area.configured=false;command("time set 6000");command("tp @s 3.5 90 3.5");});
        add(20,()->{
            custom.enabled=true;var c=custom.conditions;
            c.dimensionEnabled=true;c.dimension=rt.context().dimension();c.biomeEnabled=true;c.biomes.add(rt.context().biome());
            c.timeEnabled=true;c.startTime=5000;c.endTime=7000;c.elevationEnabled=true;c.minY=90;c.maxY=90;
            c.temperatureEnabled=true;c.minTemperature=rt.context().biomeTemperature();c.maxTemperature=c.minTemperature;
            c.cover=PlaylistConditions.State.NO;
        });
        add(0,()->check("synced biome temperature time dimension Y custom rule",source(custom.id)&&"minecraft:plains".equals(rt.context().biome())&&Math.abs(rt.context().biomeTemperature()-0.8)<0.001));
        add(40,()->{custom.enabled=false;command("fill 0 90 0 6 94 6 minecraft:water");command("tp @s 3.5 91 3.5");});
        add(0,()->check("underwater state synchronized",rt.context().underwater()));
        add(40,()->{command("fill 0 90 0 6 94 6 minecraft:air");command("tp @s 3.5 90 3.5");lib.playlist("dark_exploration").conditions.groups.get(1).enabled=true;command("execute in minecraft:the_nether run tp @s 3.5 200 3.5");});
        add(0,()->check("Nether dimension music online","minecraft:the_nether".equals(rt.context().dimension())&&source("dark_exploration")&&homeKey.equals(rt.worldKey())));
        add(80,()->{lib.playlist("dark_exploration").conditions.groups.get(2).enabled=true;command("execute in minecraft:the_end run tp @s 3.5 100 3.5");});
        add(0,()->check("End dimension music online","minecraft:the_end".equals(rt.context().dimension())&&source("dark_exploration")));
        add(60,()->{command("execute in minecraft:overworld run tp @s 3.5 90 3.5");command("gamemode survival @s");});
        add(20,()->command("damage @s 1 minecraft:generic"));
        add(0,()->check("environmental server damage does not trigger Combat",!rt.context().combat()));
        add(20,()->{lib.settings.combatThreatsOnly=false;command("damage @s 1 minecraft:generic");});
        add(0,()->check("broad damage option triggers Combat",rt.context().combat()&&source("combat")));
        add(0,()->lib.settings.combatThreatsOnly=true);
        add(70,()->{});
        add(0,()->check("combat quiet period expires online",!rt.context().combat()&&source("day_outside")));
        add(20,()->{rt.attack(new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE,mc.level));});
        add(0,()->check("client attack observation triggers Combat",rt.context().combat()&&source("combat")));
        add(30,()->{lib.settings.combatDeathMusic=DeathMusic.CUT;rt.overridePlaylist("combat",OverrideMode.UNTIL_RELEASED);command("kill @s");});
        add(0,()->check("server death cuts Combat and blocks new music",rt.context().dead()&&engine.current()==null&&!playing()));
        add(60,()->{rt.returnAutomatic();mc.player.respawn();});
        add(0,()->check("respawn clears combat and resumes playback",!rt.context().dead()&&!rt.context().combat()&&engine.current()!=null&&playing()));
        add(40,()->rt.preview(lib.tracks.stream().filter(t->t.vanilla&&t.path.contains("a_familiar_room")).findFirst().orElseThrow()));
        add(0,()->check("vanilla resource audio streams online",engine.current()!=null&&engine.current().vanilla&&playing()));
        add(20,()->{rt.overridePlaylist(manual.id,OverrideMode.UNTIL_RELEASED);});
        add(20,()->rt.togglePause());
        add(30,()->{
            check("paused override ready for disconnect",engine.paused()&&manual.id.equals(engine.overrideId()));
            serverProfile=lib.profiles.create(lib,"Linked vanilla server",PlaylistProfiles.DEFAULT);
            for(var p:lib.profiles.playlists(lib,serverProfile))p.volume=0.37;
            lib.profiles.bind(homeKey,serverProfile);rt.save();
            check("binding an inactive server profile leaves current setup running",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT)&&engine.paused());
            mc.level.disconnect(Component.literal("AFMR multiplayer test"));
            mc.disconnect(new TitleScreen(),false);
            check("disconnect immediately clears playback pause and override",engine.current()==null&&!engine.paused()&&engine.overrideId()==null);
            check("disconnect immediately clears world context",rt.context().world().isEmpty()&&rt.applicableVanilla().isEmpty());
        });
        add(100,()->ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("AFMR vanilla test",address,ServerData.Type.OTHER),false,null));
        add(0,()->check("reconnect restores server context and automatic playback",rt.inWorld()&&homeKey.equals(rt.worldKey())&&!engine.paused()&&engine.overrideId()==null&&engine.current()!=null&&playing()));
        add(0,()->{
            check("actual server reconnect selects linked profile",lib.profiles.activeId.equals(serverProfile));
            check("linked server setup restores independent playlist volume",lib.playlists.stream().allMatch(p->p.volume==0.37));
        });
        add(0,()->check("waiting normalization finishes after disconnect",rt.lastLoudnessReport()!=null&&rt.lastLoudnessReport().count(dev.animal.afmr.library.LoudnessScan.Source.SCANNED)==1&&lib.track("file:multiplayer0").loudness!=null));
        add(80,()->{command("gamemode creative @s");command("execute in minecraft:the_nether run tp @s 3.5 200 3.5");});
        add(0,()->check("server dimension change keeps linked profile",lib.profiles.activeId.equals(serverProfile)&&rt.context().dimension().equals("minecraft:the_nether")&&homeKey.equals(rt.worldKey())));
        add(80,()->command("execute in minecraft:overworld run tp @s 3.5 90 3.5"));
        add(0,()->check("returning server dimension keeps linked profile",lib.profiles.activeId.equals(serverProfile)&&rt.context().dimension().equals("minecraft:overworld")));
        add(0,()->{
            lib.profiles.bindings.remove(homeKey);rt.save();
            check("unlink affects next join without switching current setup",lib.profiles.activeId.equals(serverProfile));
            mc.level.disconnect(Component.literal("AFMR profile fallback test"));mc.disconnect(new TitleScreen(),false);
            check("server logout returns to Default profile",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT));
        });
        add(100,()->ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Same address, different display name",address,ServerData.Type.OTHER),false,null));
        add(0,()->{
            check("unlinked server reconnect falls back to Default",rt.inWorld()&&lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT));
            check("server address identity ignores display name",homeKey.equals(rt.worldKey()));
            check("Default volume remains independent after server reconnect",lib.playlists.stream().allMatch(p->p.volume==1)&&playing());
        });
    }
}
