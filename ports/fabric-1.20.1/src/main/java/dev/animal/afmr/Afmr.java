package dev.animal.afmr;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.library.ConfigBackup;
import java.util.*;
import dev.animal.afmr.ui.MusicMenu;
import dev.animal.afmr.ui.OverridesScreen;
import dev.animal.afmr.ui.PlaybackInspectorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;

public final class Afmr implements ClientModInitializer {
    public static final Logger LOG=LogUtils.getLogger();
    // JUL retains loggers weakly. Keep this parent alive so WAV chunk logging stays quiet.
    private static final java.util.logging.Logger METADATA_LOG=java.util.logging.Logger.getLogger("org.jaudiotagger");
    private static final String CATEGORY="key.categories.afmr";
    private static final KeyMapping OPEN=key("open",InputConstants.KEY_M);
    private static final KeyMapping SKIP=key("skip",-1),NEXT=key("next",-1),PAUSE=key("pause",-1);
    private static final KeyMapping SHOW_NOW_PLAYING=key("show_now_playing",-1);
    private static final KeyMapping INSPECT=key("inspect",-1),OVERRIDES=key("overrides",-1),AUTOMATIC=key("automatic",-1);
    private static final KeyMapping[] OVERRIDE_KEYS=new KeyMapping[Library.OVERRIDE_SLOTS];
    private static boolean suppressOpen;
    private static KeyMapping key(String name,int code){return new KeyMapping("key.afmr."+name,code,CATEGORY);}
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
        for(var binding:new KeyMapping[]{OPEN,SKIP,NEXT,PAUSE,SHOW_NOW_PLAYING,INSPECT,OVERRIDES,AUTOMATIC})KeyBindingHelper.registerKeyBinding(binding);
        for(int i=0;i<OVERRIDE_KEYS.length;i++)OVERRIDE_KEYS[i]=KeyBindingHelper.registerKeyBinding(key("override_"+(i+1),-1));
        ClientTickEvents.END_CLIENT_TICK.register(Afmr::tick);
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->ScreenKeyboardEvents.allowKeyPress(screen).register((current,key,scan,modifiers)->{
            if(OPEN.matches(key,scan)&&!(current.getFocused() instanceof EditBox)&&MusicMenu.hide(current)){suppressOpen=true;return false;}
            return true;
        }));
        AttackEntityCallback.EVENT.register((player,world,hand,target,hit)->{
            if(world.isClientSide&&!player.isSpectator())ClientRuntime.get().attack(target);
            return InteractionResult.PASS;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{MusicMenu.clear();suppressOpen=false;ClientRuntime.disconnect();});
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->ClientRuntime.shutdown());
        HudRenderCallback.EVENT.register((graphics,delta)->ClientRuntime.get().overlay().draw(graphics));
        ClientConditions.register();
    }
    private static void tick(Minecraft mc){
        var runtime=ClientRuntime.get();runtime.tick();
        while(OPEN.consumeClick())if(!suppressOpen&&mc.screen==null)mc.setScreen(MusicMenu.reopen(null,runtime));
        suppressOpen=false;
        while(SKIP.consumeClick())if(mc.screen==null)runtime.skip();
        while(NEXT.consumeClick())if(mc.screen==null)runtime.nextSong();
        while(PAUSE.consumeClick())if(mc.screen==null)runtime.togglePause();
        while(SHOW_NOW_PLAYING.consumeClick())if(mc.screen==null)runtime.overlay().showCurrent();
        while(INSPECT.consumeClick())if(mc.screen==null)mc.setScreen(new PlaybackInspectorScreen(null,runtime));
        while(OVERRIDES.consumeClick())if(mc.screen==null)mc.setScreen(new OverridesScreen(null,runtime,-1,"combat"));
        while(AUTOMATIC.consumeClick())if(mc.screen==null)runtime.returnAutomatic();
        for(int i=0;i<OVERRIDE_KEYS.length;i++)while(OVERRIDE_KEYS[i].consumeClick())if(mc.screen==null)runtime.overrideSlot(i);
    }
}
