package dev.animal.afmr.mixin;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.NowPlayingStyle;
import dev.animal.afmr.core.Library.WidgetCorner;
import dev.animal.afmr.core.NowPlayingLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feed AFMR metadata to the real toast, including its shared pause-menu renderer. */
@Mixin(NowPlayingToast.class)
public abstract class NowPlayingToastMixin {
    @Inject(method="xPos",at=@At("HEAD"),cancellable=true)
    private void afmr$cornerX(int screenWidth,float visiblePortion,CallbackInfoReturnable<Float> cir){
        if(Minecraft.getInstance().level==null)return;
        var runtime=ClientRuntime.get();var s=runtime.library().settings;
        if(Minecraft.getInstance().level==null||!runtime.enabled()||s.nowPlayingStyle!=NowPlayingStyle.MINECRAFT||s.widgetCorner==WidgetCorner.DEFAULT)return;
        int width=((NowPlayingToast)(Object)this).width();
        float anchor=NowPlayingLayout.anchorX(s.nowPlayingStyle,s.widgetCorner,width,screenWidth);
        cir.setReturnValue(anchor+width*(1-visiblePortion)*(s.widgetCorner.right()?1:-1));
    }
    @Inject(method="yPos",at=@At("HEAD"),cancellable=true)
    private void afmr$cornerY(int firstSlotIndex,CallbackInfoReturnable<Float> cir){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var runtime=ClientRuntime.get();var s=runtime.library().settings;
        if(mc.level==null||!runtime.enabled()||s.nowPlayingStyle!=NowPlayingStyle.MINECRAFT||s.widgetCorner==WidgetCorner.DEFAULT)return;
        cir.setReturnValue(NowPlayingLayout.anchorY(s.nowPlayingStyle,s.widgetCorner,((NowPlayingToast)(Object)this).width(),30,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight()));
    }
    @Inject(method="getCurrentSongName",at=@At("HEAD"),cancellable=true)
    private static void afmr$songName(CallbackInfoReturnable<String> cir){
        if(Minecraft.getInstance().level!=null&&ClientRuntime.get().enabled())
            cir.setReturnValue(ClientRuntime.get().overlay().nativeSongName());
    }

    @Inject(method="getNowPlayingString",at=@At("HEAD"),cancellable=true)
    private static void afmr$songText(String song,CallbackInfoReturnable<Component> cir){
        if(Minecraft.getInstance().level!=null&&ClientRuntime.get().enabled())
            cir.setReturnValue(ClientRuntime.get().overlay().nativeSongText());
    }
}
