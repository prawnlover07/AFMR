package dev.animal.afmr.probe;

import dev.animal.afmr.*;
import dev.animal.afmr.core.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Zombie;

/** Actual socket connection, packaged client and companion; disposable capped profile only. */
public final class CompanionSuite {
    private static final List<String> results=new ArrayList<>();
    private static int ticks,stage,wait;
    private static boolean finished;
    private static ClientRuntime runtime;
    private static void check(boolean ok,String name){results.add((ok?"PASS ":"FAIL ")+name);if(!ok)throw new AssertionError(name);}
    static void run(Minecraft mc){
        if(!Boolean.getBoolean("afmr.smoke.serverCompanion")||finished)return;
        try{
            if(stage==4){if(wait-->0)return;finish(mc,null);return;}
            if(++ticks>1800)throw new AssertionError("Client connection timed out");
            if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen){
                for(var field:mc.screen.getClass().getDeclaredFields())if(net.minecraft.network.chat.Component.class.isAssignableFrom(field.getType())){field.setAccessible(true);System.out.println("DISCONNECT "+field.getName()+" "+field.get(mc.screen));}
                throw new AssertionError("Disconnected before completion");
            }
            if(mc.level==null||mc.player==null)return;
            if(wait-->0)return;
            if(stage==0){
                if(ticks<120)return;
                mc.options.pauseOnLostFocus=false;mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0d);
                runtime=ClientRuntime.get();var library=runtime.library();
                library.settings.combatQuietSeconds=1;library.settings.combatOnAttack=true;library.settings.combatOnDamage=false;
                var p=library.createPlaylist("Server condition probe");p.conditions.structureEnabled=true;p.conditions.structures.add("minecraft:mineshaft");
                check(mc.getSingleplayerServer()==null,"uses a separate dedicated server");
                stage=1;wait=60;return;
            }
            if(stage==1&&Boolean.getBoolean("afmr.smoke.noCompanion")){
                check(!runtime.serverConditionsAvailable(),"vanilla server connects without observation channels");
                check(!runtime.context().structures().available(),"vanilla server reports unavailable structure data");
                check(!runtime.context().combat(),"vanilla server starts peaceful");
                Zombie target=null;for(var entity:mc.level.entitiesForRendering())if(entity instanceof Zombie z){target=z;break;}
                check(target!=null,"vanilla fixture mob reaches client");runtime.attack(target);stage=5;wait=80;return;
            }
            if(stage==5){
                check(!runtime.context().combat(),"vanilla server retains event timeout without target retention");
                check(!runtime.context().playerCombat(),"vanilla mob event does not start PvP");
                check(mc.options.framerateLimit().get()==60&&!mc.options.enableVsync().get(),"fallback client remains capped at 60 FPS");
                mc.player.connection.sendCommand("stop");stage=4;wait=10;return;
            }
            if(stage==1){
                check(runtime.serverConditionsAvailable(),"optional observation channels negotiated");
                check(runtime.context().structures().available(),"actual network structure observation received");
                check(runtime.context().structures().matches(Set.of("minecraft:mineshaft"),0,mc.player.getX(),mc.player.getY(),mc.player.getZ()),"remote loaded bounds match the actual client position");
                check(runtime.structureIds().contains("minecraft:mineshaft"),"remote registry catalog reaches client");
                check(!runtime.context().combat(),"targeting alone cannot start client combat");
                Zombie target=null;for(var entity:mc.level.entitiesForRendering())if(entity instanceof Zombie z){target=z;break;}
                check(target!=null,"server mob is tracked by client");runtime.attack(target);
                stage=2;wait=80;return;
            }
            if(stage==2){
                check(runtime.context().combat(),"target observation retains event-started combat beyond the one-second quiet period");
                check(!runtime.context().playerCombat(),"mob target does not extend PvP timer");
                mc.player.connection.sendCommand("kill @e[type=minecraft:zombie]");stage=3;wait=80;return;
            }
            if(stage==3){
                check(!runtime.context().combat(),"target loss and quiet countdown end client combat");
                check(runtime.context().structures().available(),"structure data survives combat start and end");
                check(mc.options.framerateLimit().get()==60&&!mc.options.enableVsync().get(),"client remains capped at 60 FPS with VSync off");
                mc.player.connection.sendCommand("stop");stage=4;wait=10;return;
            }
            finish(mc,null);
        }catch(Throwable failure){finish(mc,failure);}
    }
    private static void finish(Minecraft mc,Throwable failure){
        finished=true;if(failure!=null){results.add("FAIL "+failure);failure.printStackTrace();if(mc.player!=null)mc.player.connection.sendCommand("stop");}
        try{Files.write(Path.of(Boolean.getBoolean("afmr.smoke.noCompanion")?"server-without-companion-network-results.txt":"server-companion-network-results.txt"),results);}catch(Exception e){e.printStackTrace();}
        mc.stop();
    }
}
