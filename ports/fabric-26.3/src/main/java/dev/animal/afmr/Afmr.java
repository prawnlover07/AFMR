package dev.animal.afmr;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.animal.afmr.ui.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.library.ConfigBackup;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;

public final class Afmr implements ClientModInitializer {
    public static final Logger LOG = LogUtils.getLogger();
    private static final java.util.logging.Logger METADATA_LOG=java.util.logging.Logger.getLogger("org.jaudiotagger");
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("afmr","afmr"));
    private static final KeyMapping OPEN = new KeyMapping("key.afmr.open", InputConstants.KEY_M, CATEGORY);
    private static boolean suppressOpen;
    private static final KeyMapping SKIP = new KeyMapping("key.afmr.skip", InputConstants.UNKNOWN.getValue(), CATEGORY);
    private static final KeyMapping NEXT=new KeyMapping("key.afmr.next",InputConstants.UNKNOWN.getValue(),CATEGORY);
    private static final KeyMapping PAUSE=new KeyMapping("key.afmr.pause",InputConstants.UNKNOWN.getValue(),CATEGORY);
    private static final KeyMapping SHOW_NOW_PLAYING=new KeyMapping("key.afmr.show_now_playing",InputConstants.UNKNOWN.getValue(),CATEGORY);
    private static final KeyMapping INSPECT=new KeyMapping("key.afmr.inspect",InputConstants.UNKNOWN.getValue(),CATEGORY);
    private static final KeyMapping OVERRIDES=new KeyMapping("key.afmr.overrides",InputConstants.UNKNOWN.getValue(),CATEGORY);
    private static final KeyMapping AUTOMATIC=new KeyMapping("key.afmr.automatic",InputConstants.UNKNOWN.getValue(),CATEGORY);
    private static final KeyMapping[] OVERRIDE_KEYS=new KeyMapping[Library.OVERRIDE_SLOTS];
    static{for(int i=0;i<OVERRIDE_KEYS.length;i++)OVERRIDE_KEYS[i]=new KeyMapping("key.afmr.override_"+(i+1),InputConstants.UNKNOWN.getValue(),CATEGORY);}
    public static String overrideKeyName(int slot){return OVERRIDE_KEYS[slot].getTranslatedKeyMessage().getString();}

    public static Map<String,ConfigBackup.Key> backupKeys(){
        var result=new LinkedHashMap<String,ConfigBackup.Key>();
        var keys=new ArrayList<KeyMapping>(List.of(OPEN,SKIP,NEXT,PAUSE,SHOW_NOW_PLAYING,INSPECT,OVERRIDES,AUTOMATIC));
        Collections.addAll(keys,OVERRIDE_KEYS);
        for(var key:keys)result.put(key.getName(),new ConfigBackup.Key(key.saveString(),"NONE"));
        return Map.copyOf(result);
    }
    public static int unavailableBackupKeys(Map<String,ConfigBackup.Key> keys){
        int count=0;
        for(var key:keys.values())try{InputConstants.getKey(key.code());if(!key.modifier().equals("NONE"))count++;}
        catch(RuntimeException e){count++;}
        return count;
    }
    public static void restoreKeys(Map<String,ConfigBackup.Key> saved){
        var keys=new ArrayList<KeyMapping>(List.of(OPEN,SKIP,NEXT,PAUSE,SHOW_NOW_PLAYING,INSPECT,OVERRIDES,AUTOMATIC));
        Collections.addAll(keys,OVERRIDE_KEYS);
        for(var key:keys){
            var value=saved.get(key.getName());if(value==null)continue;
            try{key.setKey(value.modifier().equals("NONE")?InputConstants.getKey(value.code()):InputConstants.UNKNOWN);}catch(RuntimeException e){key.setKey(InputConstants.UNKNOWN);}
        }
        KeyMapping.resetMapping();Minecraft.getInstance().options.save();
    }

    @Override public void onInitializeClient(){
        METADATA_LOG.setLevel(java.util.logging.Level.WARNING);
        for(var key:List.of(OPEN,SKIP,NEXT,PAUSE,SHOW_NOW_PLAYING,INSPECT,OVERRIDES,AUTOMATIC))KeyMappingHelper.registerKeyMapping(key);
        for(var key:OVERRIDE_KEYS)KeyMappingHelper.registerKeyMapping(key);
        ClientTickEvents.END_CLIENT_TICK.register(Afmr::tick);
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->ScreenKeyboardEvents.allowKeyPress(screen).register((current,event)->{
            if(OPEN.matches(event)&&!current.isInputCaptured()&&MusicMenu.hide(current)){suppressOpen=true;return false;}
            return true;
        }));
        AttackEntityCallback.EVENT.register((player,world,hand,target,hit)->{
            if(world.isClientSide()&&!player.isSpectator())ClientRuntime.get().attack(target);
            return InteractionResult.PASS;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{MusicMenu.clear();suppressOpen=false;ClientRuntime.disconnect();});
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->ClientRuntime.shutdown());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("afmr","now_playing"),(graphics,delta)->ClientRuntime.get().overlay().draw(graphics));
        ClientConditions.register();
    }
    private static void tick(Minecraft ignored){
        var mc = Minecraft.getInstance(); var runtime = ClientRuntime.get();
        runtime.tick();
        while (OPEN.consumeClick()) if(!suppressOpen&&mc.gui.screen()==null)mc.gui.setScreen(MusicMenu.reopen(null,runtime));
        suppressOpen=false;
        while (SKIP.consumeClick()) if(mc.gui.screen()==null)runtime.skip();
        while(NEXT.consumeClick())if(mc.gui.screen()==null)runtime.nextSong();
        while(PAUSE.consumeClick())if(mc.gui.screen()==null)runtime.togglePause();
        while(SHOW_NOW_PLAYING.consumeClick())if(mc.gui.screen()==null)runtime.overlay().showCurrent();
        while(INSPECT.consumeClick())if(mc.gui.screen()==null)mc.gui.setScreen(new PlaybackInspectorScreen(null,runtime));
        while(OVERRIDES.consumeClick())if(mc.gui.screen()==null)mc.gui.setScreen(new OverridesScreen(null,runtime,-1,"combat"));
        while(AUTOMATIC.consumeClick())if(mc.gui.screen()==null)runtime.returnAutomatic();
        for(int i=0;i<OVERRIDE_KEYS.length;i++)while(OVERRIDE_KEYS[i].consumeClick())if(mc.gui.screen()==null)runtime.overrideSlot(i);
    }
}
