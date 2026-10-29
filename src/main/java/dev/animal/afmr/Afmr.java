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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.*;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.slf4j.Logger;

@Mod(value = "afmr", dist = Dist.CLIENT)
@EventBusSubscriber(modid = "afmr", value = Dist.CLIENT)
public final class Afmr {
    public static final Logger LOG = LogUtils.getLogger();
    private static final java.util.logging.Logger METADATA_LOG=java.util.logging.Logger.getLogger("org.jaudiotagger");
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("afmr","afmr"));
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

    public Afmr(ModContainer container) {
        // The WAV reader logs every RIFF chunk at INFO, including playback tag reads.
        // Configure it at startup so imports, cover reads and decoders all stay quiet.
        METADATA_LOG.setLevel(java.util.logging.Level.WARNING);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (ignored, parent) -> MusicMenu.reopen(parent, ClientRuntime.get()));
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) {
        e.register(OPEN);e.register(SKIP);e.register(NEXT);e.register(PAUSE);e.register(SHOW_NOW_PLAYING);e.register(INSPECT);e.register(OVERRIDES);e.register(AUTOMATIC);
        for(var key:OVERRIDE_KEYS)e.register(key);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance(); var runtime = ClientRuntime.get();
        runtime.tick();
        while (OPEN.consumeClick()) if(!menuKeyClosing&&mc.gui.screen()==null)mc.gui.setScreen(MusicMenu.reopen(null,runtime));
        while (SKIP.consumeClick()) if(mc.gui.screen()==null)runtime.skip();
        while(NEXT.consumeClick())if(mc.gui.screen()==null)runtime.nextSong();
        while(PAUSE.consumeClick())if(mc.gui.screen()==null)runtime.togglePause();
        while(SHOW_NOW_PLAYING.consumeClick())if(mc.gui.screen()==null)runtime.overlay().showCurrent();
        while(INSPECT.consumeClick())if(mc.gui.screen()==null)mc.gui.setScreen(new PlaybackInspectorScreen(null,runtime));
        while(OVERRIDES.consumeClick())if(mc.gui.screen()==null)mc.gui.setScreen(new OverridesScreen(null,runtime,-1,"combat"));
        while(AUTOMATIC.consumeClick())if(mc.gui.screen()==null)runtime.returnAutomatic();
        for(int i=0;i<OVERRIDE_KEYS.length;i++)while(OVERRIDE_KEYS[i].consumeClick())if(mc.gui.screen()==null)runtime.overrideSlot(i);
    }
    @SubscribeEvent public static void menuKey(ScreenEvent.KeyPressed.Pre e) {
        if (OPEN.matches(e.getKeyEvent()) && OPEN.getKeyModifier().isActive(null) && !e.getScreen().isInputCaptured()
            && MusicMenu.hide(e.getScreen())) {
            menuKeyClosing = true;
            e.setCanceled(true);
        }
    }
    @SubscribeEvent public static void menuKeyInput(InputEvent.Key e) {
        if (e.getAction() == InputConstants.PRESS && OPEN.matches(e.getKeyEvent())) menuKeyClosing = false;
        if (e.getAction() == InputConstants.RELEASE && OPEN.matches(e.getKeyEvent()) && menuKeyClosing) {
            while (OPEN.consumeClick()) {}
            menuKeyClosing = false;
        }
    }
    @SubscribeEvent public static void sound(PlaySoundEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && e.getOriginalSound().getSource() == SoundSource.MUSIC
            && !(e.getOriginalSound() instanceof MusicSound) && ClientRuntime.get().enabled()) e.setSound(null);
    }
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isCanceled() && e.isAttack() && Minecraft.getInstance().hitResult instanceof EntityHitResult hit)
            ClientRuntime.get().attack(hit.getEntity());
    }
    @SubscribeEvent public static void reload(SoundEngineLoadEvent e) { ClientRuntime.requestReload(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { MusicMenu.clear();menuKeyClosing=false;ClientRuntime.disconnect(); }
    @SubscribeEvent public static void streaming(PlayStreamingSourceEvent e){
        // This event runs on the sound executor, including when decoding finished after pause was requested.
        if(e.getSound() instanceof MusicSound music){if(music.ended())e.getChannel().stop();else if(music.paused())e.getChannel().pause();}
    }
    @SubscribeEvent public static void shutdown(GameShuttingDownEvent e) { ClientRuntime.shutdown(); }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) { ClientRuntime.get().overlay().draw(e.getGuiGraphics()); }
}
