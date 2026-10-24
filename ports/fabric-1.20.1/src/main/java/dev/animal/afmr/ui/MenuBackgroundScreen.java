package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuImageLayout.Fit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MenuBackgroundScreen extends MusicScreen {
    private boolean wasBusy,wasEnabled;
    private String wasName;
    MenuBackgroundScreen(Screen parent,ClientRuntime runtime){super("Custom menu background",parent,runtime);}
    @Override protected void init(){
        super.init();var s=runtime.library().settings;var background=runtime.menuBackground();wasBusy=background.busy();wasEnabled=s.menuImageEnabled;wasName=s.menuImageName;
        var chooseImageHelpControl=button("Choose image…",left,47,contentWidth,()->NativePicker.open(runtime,NativePicker.Kind.BACKGROUND,""));
        var chooseImageHelpText=Component.translatable("afmr.copy.menubackground.choose_image");
        if(!chooseImageHelpText.getString().isBlank())chooseImageHelpControl.setTooltip(Tooltip.create(chooseImageHelpText));
        int half=(contentWidth-6)/2;
        button("Image: "+(s.menuImageEnabled?"on":"off"),left,75,half,()->{background.enabled(!s.menuImageEnabled);rebuildWidgets();}).active=!s.menuImageName.isBlank();
        var imageSizingHelpControl=button("Sizing: "+switch(s.menuImageFit){case FILL->"Fill";case FIT->"Fit";case STRETCH->"Stretch";},left+half+6,75,half,()->{
            s.menuImageFit=Fit.values()[(s.menuImageFit.ordinal()+1)%Fit.values().length];runtime.save();rebuildWidgets();
        });
        var imageSizingHelpText=Component.translatable("afmr.copy.menubackground.image_sizing");
        if(!imageSizingHelpText.getString().isBlank())imageSizingHelpControl.setTooltip(Tooltip.create(imageSizingHelpText));
        var opacity=addRenderableWidget(new AbstractSliderButton(left,103,contentWidth,20,Component.empty(),s.menuImageOpacity){
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.literal("Image opacity: "+Math.round(value*100)+"%"));}
            @Override protected void applyValue(){s.menuImageOpacity=value;}
        });
        var imageOpacityHelpText=Component.translatable("afmr.copy.menubackground.image_opacity");
        if(!imageOpacityHelpText.getString().isBlank())opacity.setTooltip(Tooltip.create(imageOpacityHelpText));
        button("Remove image",left,131,half,()->{background.remove();rebuildWidgets();}).active=!s.menuImageName.isBlank();
        var resetBackgroundHelpControl=button("Reset image settings",left+half+6,131,half,()->{
            s.menuImageFit=Fit.FILL;s.menuImageOpacity=1;background.enabled(false);rebuildWidgets();
        });
        var resetBackgroundHelpText=Component.translatable("afmr.copy.menubackground.reset_background");
        if(!resetBackgroundHelpText.getString().isBlank())resetBackgroundHelpControl.setTooltip(Tooltip.create(resetBackgroundHelpText));
        if(wasBusy)for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        button("Back",left,height-29,contentWidth,this::onClose);
    }
    @Override public void tick(){
        var s=runtime.library().settings;
        if(wasBusy!=runtime.menuBackground().busy()||wasEnabled!=s.menuImageEnabled||!wasName.equals(s.menuImageName))rebuildWidgets();
    }
    @Override public void removed(){runtime.save();super.removed();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        String name=runtime.library().settings.menuImageName;
        g.drawString(font,fit(name.isBlank()?"No image selected":name,contentWidth),left,160,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        String status=runtime.menuBackground().status();
        if(!status.isBlank())g.drawString(font,fit(status,contentWidth),left,174,0xFFFFC48E);
    }
}
