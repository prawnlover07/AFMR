package dev.animal.afmr;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.animal.afmr.audio.MusicSound;
import dev.animal.afmr.ui.MusicMenu;
import dev.animal.afmr.ui.PlaybackInspectorScreen;
import dev.animal.afmr.ui.OverridesScreen;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.library.ConfigBackup;
import java.util.*;
import net.minecraft.client.*;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.*;
import net.minecraftforge.client.ConfigGuiHandler;

import org.slf4j.Logger;

@EventBusSubscriber(modid = "afmr", value = Dist.CLIENT)
public final class Afmr {
    public static final Logger LOG = LogUtils.getLogger();
    private static final java.util.logging.Logger METADATA_LOG=java.util.logging.Logger.getLogger("org.jaudiotagger");
    private static final String CATEGORY="key.categories.afmr";
    private static final KeyMapping OPEN = new KeyMapping("key.afmr.open", InputConstants.KEY_M, CATEGORY);
    private static boolean menuKeyClosing;
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
        for(var key:keys)result.put(key.getName(),new ConfigBackup.Key(key.saveString(),key.getKeyModifier().name()));
        return Map.copyOf(result);
    }
    public static int unavailableBackupKeys(Map<String,ConfigBackup.Key> keys){
        int count=0;
        for(var key:keys.values())try{InputConstants.getKey(key.code());KeyModifier.valueOf(key.modifier());}
        catch(RuntimeException e){count++;}
        return count;
    }
    public static void restoreKeys(Map<String,ConfigBackup.Key> saved){
        var keys=new ArrayList<KeyMapping>(List.of(OPEN,SKIP,NEXT,PAUSE,SHOW_NOW_PLAYING,INSPECT,OVERRIDES,AUTOMATIC));
        Collections.addAll(keys,OVERRIDE_KEYS);
        for(var key:keys){
            var value=saved.get(key.getName());if(value==null)continue;
            try{key.setKeyModifierAndCode(KeyModifier.valueOf(value.modifier()),InputConstants.getKey(value.code()));}catch(RuntimeException e){key.setKeyModifierAndCode(KeyModifier.NONE,InputConstants.UNKNOWN);}
        }
        KeyMapping.resetMapping();Minecraft.getInstance().options.save();
    }

    public static void initialize() {
        // The WAV reader logs every RIFF chunk at INFO, including playback tag reads.
        // Configure it at startup so imports, cover reads and decoders all stay quiet.
        METADATA_LOG.setLevel(java.util.logging.Level.WARNING);
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,()->new ConfigGuiHandler.ConfigGuiFactory((ignored,parent)->MusicMenu.reopen(parent,ClientRuntime.get())));
    }
    @Mod.EventBusSubscriber(modid="afmr",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
            initialize();
            for(var key:List.of(OPEN,SKIP,NEXT,PAUSE,SHOW_NOW_PLAYING,INSPECT,OVERRIDES,AUTOMATIC))net.minecraftforge.client.ClientRegistry.registerKeyBinding(key);
            for(var key:OVERRIDE_KEYS)net.minecraftforge.client.ClientRegistry.registerKeyBinding(key);
        });}
        @SubscribeEvent public static void reload(SoundLoadEvent e) { ClientRuntime.requestReload(); }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var mc = Minecraft.getInstance(); var runtime = ClientRuntime.get();
        runtime.tick();
        while (OPEN.consumeClick()) if(!menuKeyClosing&&mc.screen==null)mc.setScreen(MusicMenu.reopen(null,runtime));
        while (SKIP.consumeClick()) if(mc.screen==null)runtime.skip();
        while(NEXT.consumeClick())if(mc.screen==null)runtime.nextSong();
        while(PAUSE.consumeClick())if(mc.screen==null)runtime.togglePause();
        while(SHOW_NOW_PLAYING.consumeClick())if(mc.screen==null)runtime.overlay().showCurrent();
        while(INSPECT.consumeClick())if(mc.screen==null)mc.setScreen(new PlaybackInspectorScreen(null,runtime));
        while(OVERRIDES.consumeClick())if(mc.screen==null)mc.setScreen(new OverridesScreen(null,runtime,-1,"combat"));
        while(AUTOMATIC.consumeClick())if(mc.screen==null)runtime.returnAutomatic();
        for(int i=0;i<OVERRIDE_KEYS.length;i++)while(OVERRIDE_KEYS[i].consumeClick())if(mc.screen==null)runtime.overrideSlot(i);
    }
    @SubscribeEvent public static void menuKey(ScreenEvent.KeyboardKeyPressedEvent.Pre e) {
        if (OPEN.matches(e.getKeyCode(),e.getScanCode()) && OPEN.getKeyModifier().isActive(null) && !(e.getScreen().getFocused() instanceof net.minecraft.client.gui.components.EditBox)
            && MusicMenu.hide(e.getScreen())) {
            menuKeyClosing = true;
            e.setCanceled(true);
        }
    }
    @SubscribeEvent public static void menuKeyInput(InputEvent.KeyInputEvent e) {
        if (e.getAction() == InputConstants.PRESS && OPEN.matches(e.getKey(),e.getScanCode())) menuKeyClosing = false;
        if (e.getAction() == InputConstants.RELEASE && OPEN.matches(e.getKey(),e.getScanCode()) && menuKeyClosing) {
            while (OPEN.consumeClick()) {}
            menuKeyClosing = false;
        }
    }
    @SubscribeEvent public static void sound(PlaySoundEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && e.getOriginalSound().getSource() == SoundSource.MUSIC
            && !(e.getOriginalSound() instanceof MusicSound) && ClientRuntime.get().enabled()) e.setSound(null);
    }
    @SubscribeEvent public static void attack(InputEvent.ClickInputEvent e) {
        if (!e.isCanceled() && e.isAttack() && Minecraft.getInstance().hitResult instanceof EntityHitResult hit)
            ClientRuntime.get().attack(hit.getEntity());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent e) { MusicMenu.clear();menuKeyClosing=false;ClientRuntime.disconnect(); }
    @SubscribeEvent public static void hud(RenderGameOverlayEvent.Post e) { if(e.getType()==RenderGameOverlayEvent.ElementType.ALL)ClientRuntime.get().overlay().draw(e.getMatrixStack()); }
}
