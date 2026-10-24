package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;

/** Menu palette resolution; surface opacity never changes text/indicator alpha. */
final class MenuAppearance {
    static int background(MenuColor role,int shade) {
        var settings=ClientRuntime.get().library().settings;
        return role.argb(settings.menuColors,shade,settings.menuBackgroundOpacity);
    }
    static int color(MenuColor role,int shade){
        return role.argb(ClientRuntime.get().library().settings.menuColors,shade,1);
    }
}
