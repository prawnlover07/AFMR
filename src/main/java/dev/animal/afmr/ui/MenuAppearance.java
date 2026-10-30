package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;

/** Menu palette resolution; surface opacity never changes text/indicator alpha. */
final class MenuAppearance {
    static final net.minecraft.network.chat.Component CONTRAST_WARNING=net.minecraft.network.chat.Component.translatable("afmr.copy.themes.low_contrast");
    static net.minecraft.client.gui.Font font(){return ClientRuntime.get().fonts().font(CustomFonts.Target.MENU);}
    static int background(MenuColor role,int shade) {
        var settings=ClientRuntime.get().library().settings;
        var preview=ClientRuntime.get().themes().preview();
        return role.argb(preview==null?settings.menuColors:preview.menuColors,shade,preview==null?settings.menuBackgroundOpacity:preview.menuOpacity);
    }
    static int color(MenuColor role,int shade){
        var runtime=ClientRuntime.get();var preview=runtime.themes().preview();
        return role.argb(preview==null?runtime.library().settings.menuColors:preview.menuColors,shade,1);
    }
}
