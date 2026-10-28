package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuImageLayout.Fit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class CustomBackgroundScreen extends MusicScreen {
    private final CustomBackground background;
    private final boolean widget;
    private boolean wasBusy,wasEnabled;
    private String wasName;
    CustomBackgroundScreen(Screen parent,ClientRuntime runtime,CustomBackground.Target target){
        super(target==CustomBackground.Target.MENU?"Custom menu background":"Custom widget background",parent,runtime);
        widget=target==CustomBackground.Target.WIDGET;background=widget?runtime.widgetBackground():runtime.menuBackground();
    }
    @Override protected void init(){
        super.init();wasBusy=background.busy();wasEnabled=background.enabled();wasName=background.name();
        var chooseImageHelpControl=button("Choose image…",left,47,contentWidth,()->NativePicker.open(runtime,widget?NativePicker.Kind.WIDGET_BACKGROUND:NativePicker.Kind.BACKGROUND,""));
        var chooseImageHelpText=Component.translatable("afmr.copy.menubackground.choose_image");
        if(!chooseImageHelpText.getString().isBlank())chooseImageHelpControl.setTooltip(Tooltip.create(chooseImageHelpText));
        int half=(contentWidth-6)/2;
        toggle("Image",left,75,half,background::enabled,()->{background.enabled(!background.enabled());rebuildWidgets();}).active=!background.name().isBlank();
        var imageSizingHelpControl=button("Sizing: "+switch(background.fit()){case FILL->"Fill";case FIT->"Fit";case STRETCH->"Stretch";},left+half+6,75,half,()->{
            background.fit(Fit.values()[(background.fit().ordinal()+1)%Fit.values().length]);runtime.save();rebuildWidgets();
        });
        var imageSizingHelpText=Component.translatable("afmr.copy.menubackground.image_sizing");
        if(!imageSizingHelpText.getString().isBlank())imageSizingHelpControl.setTooltip(Tooltip.create(imageSizingHelpText));
        var opacity=addRenderableWidget(new OpacitySlider("Image opacity",left,103,contentWidth,background.opacity(),background::opacity));
        var imageOpacityHelpText=Component.translatable("afmr.copy.menubackground.image_opacity");
        if(!imageOpacityHelpText.getString().isBlank())opacity.setTooltip(Tooltip.create(imageOpacityHelpText));
        button("Remove image",left,131,half,()->{background.remove();rebuildWidgets();}).active=!background.name().isBlank();
        var resetBackgroundHelpControl=button("Reset image settings",left+half+6,131,half,()->{
            background.fit(Fit.FILL);background.opacity(1);background.enabled(false);rebuildWidgets();
        });
        var resetBackgroundHelpText=Component.translatable("afmr.copy.menubackground.reset_background");
        if(!resetBackgroundHelpText.getString().isBlank())resetBackgroundHelpControl.setTooltip(Tooltip.create(resetBackgroundHelpText));
        if(wasBusy)for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        if(widget)button("Preview",left,bodyBottom()-53,contentWidth,()->minecraft.setScreen(new NowPlayingPreviewScreen(this,runtime)));
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    @Override public void tick(){
        if(wasBusy!=background.busy()||wasEnabled!=background.enabled()||!wasName.equals(background.name()))rebuildWidgets();
    }
    @Override public void removed(){runtime.save();super.removed();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        String name=background.name();
        g.drawString(menuFont,fit(name.isBlank()?"No image selected":name,contentWidth),left,160,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        String status=background.status();
        if(!status.isBlank())g.drawString(menuFont,fit(status,contentWidth),left,174,0xFFFFC48E);
    }
}
