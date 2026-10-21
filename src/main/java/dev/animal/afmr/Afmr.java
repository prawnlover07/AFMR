package dev.animal.afmr;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.animal.afmr.audio.MusicSound;
import dev.animal.afmr.ui.LibraryScreen;
import dev.animal.afmr.ui.PlaybackInspectorScreen;
import dev.animal.afmr.ui.OverridesScreen;
import dev.animal.afmr.core.Library;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
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
    private static final KeyMapping OPEN = new KeyMapping("key.afmr.open", InputConstants.KEY_M, KeyMapping.Category.MISC);
    private static final KeyMapping SKIP = new KeyMapping("key.afmr.skip", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);
    private static final KeyMapping NEXT=new KeyMapping("key.afmr.next",InputConstants.UNKNOWN.getValue(),KeyMapping.Category.MISC);
    private static final KeyMapping PAUSE=new KeyMapping("key.afmr.pause",InputConstants.UNKNOWN.getValue(),KeyMapping.Category.MISC);
    private static final KeyMapping INSPECT=new KeyMapping("key.afmr.inspect",InputConstants.UNKNOWN.getValue(),KeyMapping.Category.MISC);
    private static final KeyMapping OVERRIDES=new KeyMapping("key.afmr.overrides",InputConstants.UNKNOWN.getValue(),KeyMapping.Category.MISC);
    private static final KeyMapping AUTOMATIC=new KeyMapping("key.afmr.automatic",InputConstants.UNKNOWN.getValue(),KeyMapping.Category.MISC);
    private static final KeyMapping[] OVERRIDE_KEYS=new KeyMapping[Library.OVERRIDE_SLOTS];
    static{for(int i=0;i<OVERRIDE_KEYS.length;i++)OVERRIDE_KEYS[i]=new KeyMapping("key.afmr.override_"+(i+1),InputConstants.UNKNOWN.getValue(),KeyMapping.Category.MISC);}
    public static String overrideKeyName(int slot){return OVERRIDE_KEYS[slot].getTranslatedKeyMessage().getString();}

    public Afmr(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (ignored, parent) -> new LibraryScreen(parent, ClientRuntime.get(),"day_outside"));
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) {
        e.register(OPEN);e.register(SKIP);e.register(NEXT);e.register(PAUSE);e.register(INSPECT);e.register(OVERRIDES);e.register(AUTOMATIC);
        for(var key:OVERRIDE_KEYS)e.register(key);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance(); var runtime = ClientRuntime.get();
        runtime.tick();
        while (OPEN.consumeClick()) if(mc.gui.screen()==null)mc.gui.setScreen(new LibraryScreen(null, runtime,"day_outside"));
        while (SKIP.consumeClick()) if(mc.gui.screen()==null)runtime.skip();
        while(NEXT.consumeClick())if(mc.gui.screen()==null)runtime.nextSong();
        while(PAUSE.consumeClick())if(mc.gui.screen()==null)runtime.togglePause();
        while(INSPECT.consumeClick())if(mc.gui.screen()==null)mc.gui.setScreen(new PlaybackInspectorScreen(null,runtime));
        while(OVERRIDES.consumeClick())if(mc.gui.screen()==null)mc.gui.setScreen(new OverridesScreen(null,runtime,-1,"combat"));
        while(AUTOMATIC.consumeClick())if(mc.gui.screen()==null)runtime.returnAutomatic();
        for(int i=0;i<OVERRIDE_KEYS.length;i++)while(OVERRIDE_KEYS[i].consumeClick())if(mc.gui.screen()==null)runtime.overrideSlot(i);
    }
    @SubscribeEvent public static void sound(PlaySoundEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && e.getOriginalSound().getSource() == SoundSource.MUSIC
            && !(e.getOriginalSound() instanceof MusicSound) && ClientRuntime.get().enabled()) e.setSound(null);
    }
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isCanceled() && e.isAttack() && Minecraft.getInstance().hitResult instanceof EntityHitResult)
            ClientRuntime.get().attack();
    }
    @SubscribeEvent public static void reload(SoundEngineLoadEvent e) { ClientRuntime.requestReload(); }
    @SubscribeEvent public static void streaming(PlayStreamingSourceEvent e){
        // This event runs on the sound executor, including when decoding finished after pause was requested.
        if(e.getSound() instanceof MusicSound music&&music.paused())e.getChannel().pause();
    }
    @SubscribeEvent public static void shutdown(GameShuttingDownEvent e) { ClientRuntime.shutdown(); }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) { ClientRuntime.get().overlay().draw(e.getGuiGraphics()); }
}
