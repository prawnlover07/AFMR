package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.NowPlayingStyle;
import dev.animal.afmr.core.Library.WidgetCorner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class NowPlayingSettingsScreen extends MusicScreen {
    private EditBox delay,duration,scale,widgetWidth,backgroundColor,opacity;
    private String error="";
    private int page,scroll,rows,totalRows;
    private static final int TOP=79,ROW=24;
    private record Positioned(AbstractWidget control,int row){}
    private record Label(EditBox field,String text,int x){}
    private final List<Positioned> controls=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();

    NowPlayingSettingsScreen(Screen parent,ClientRuntime runtime){super("Display settings",parent,runtime);}
    NowPlayingSettingsScreen(Screen parent,ClientRuntime runtime,boolean menus){this(parent,runtime);page=menus?1:0;}

    @Override protected void init(){
        super.init();var s=runtime.library().settings;
        delay=duration=scale=widgetWidth=backgroundColor=opacity=null;controls.clear();labels.clear();
        int half=(contentWidth-6)/2;
        button("Widget",left,47,half,()->{if(apply()){page=0;scroll=0;rebuildWidgets();}}).active=page!=0;
        button("Menus",left+half+6,47,half,()->{if(apply()){page=1;scroll=0;rebuildWidgets();}}).active=page!=1;
        int first=children().size();
        if(page==0){
            buildWidget();
        }else{
            var menuOpacity=addRenderableWidget(new OpacitySlider("Background opacity",left,79,contentWidth,s.menuBackgroundOpacity,value->s.menuBackgroundOpacity=value));
            var menuOpacityHelpText=Component.translatable("afmr.copy.nowplayingsettings.menu_opacity");
            if(!menuOpacityHelpText.getString().isBlank())menuOpacity.setTooltip(Tooltip.create(menuOpacityHelpText));
            var menuBlurHelpControl=toggle("Blur background",left,103,contentWidth,()->s.menuBlur,()->change(()->s.menuBlur=!s.menuBlur));
            var menuBlurHelpText=Component.translatable("afmr.copy.nowplayingsettings.menu_blur");
            if(!menuBlurHelpText.getString().isBlank())menuBlurHelpControl.setTooltip(Tooltip.create(menuBlurHelpText));
            button("Custom background >",left,127,half,()->{if(apply())minecraft.gui.setScreen(new CustomBackgroundScreen(this,runtime,CustomBackground.Target.MENU));});
            button("Themes >",left+half+6,127,half,()->{if(apply())minecraft.gui.setScreen(new ThemesScreen(this,runtime));});
            button("Reset menu appearance",left,151,contentWidth,()->change(()->{s.menuColors=MenuColor.defaults();s.menuBackgroundOpacity=0.80;s.menuBlur=true;s.menuImageOpacity=1;s.menuImageFit=dev.animal.afmr.core.MenuImageLayout.Fit.FILL;runtime.menuBackground().enabled(false);}));
            button("Menu font: "+runtime.fonts().name(CustomFonts.Target.MENU)+" >",left,175,contentWidth,()->{if(apply())minecraft.gui.setScreen(new FontSettingsScreen(this,runtime,CustomFonts.Target.MENU));});

        }
        if(page==1)totalRows=5;
        for(int i=first;i<children().size();i++)if(children().get(i) instanceof AbstractWidget control)controls.add(new Positioned(control,(control.getY()-TOP)/ROW));
        rows=Math.max(1,(bodyBottom()-53-TOP+4)/ROW);scroll=Math.max(0,Math.min(scroll,Math.max(0,totalRows-rows)));positionControls();
        addRenderableWidget(new SettingsScrollbar(left+contentWidth-8,TOP,rows*ROW-4,Math.max(0,totalRows-rows),rows,()->scroll,this::scrollTo));
        if(page==0){
            var preview=button("Preview widget",left,bodyBottom()-29,half,()->{
                if(apply()){runtime.overlay().refresh();minecraft.gui.setScreen(new NowPlayingPreviewScreen(this,runtime));}
            });
            preview.active=s.nowPlayingStyle!=NowPlayingStyle.MINECRAFT;
            var previewWidgetHelpText=Component.translatable("afmr.copy.nowplayingsettings.preview_widget");
            if(!previewWidgetHelpText.getString().isBlank())preview.setTooltip(Tooltip.create(previewWidgetHelpText));
        }
        button("Save & back",page==1?left:left+half+6,bodyBottom()-29,page==1?contentWidth:half,this::onClose);
    }

    private void buildWidget(){
        var s=runtime.library().settings;boolean custom=s.nowPlayingStyle!=NowPlayingStyle.MINECRAFT,afmr=s.nowPlayingStyle==NowPlayingStyle.AFMR;
        int body=contentWidth-12,half=(body-6)/2,right=left+half+6,row=0;
        toggle("Now playing",left,TOP,half,()->s.nowPlaying,()->change(()->s.nowPlaying=!s.nowPlaying));
        var widgetCornerHelpControl=button("Corner: "+s.widgetCorner.label(),right,TOP,half,()->change(()->s.widgetCorner=WidgetCorner.values()[(s.widgetCorner.ordinal()+1)%WidgetCorner.values().length]));
        var widgetCornerHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_corner");
        if(!widgetCornerHelpText.getString().isBlank())widgetCornerHelpControl.setTooltip(Tooltip.create(widgetCornerHelpText));
        row++;
        String name=switch(s.nowPlayingStyle){case MINECRAFT->"Minecraft toast";case HYBRID->"Hybrid toast";case AFMR->"AFMR album-art card";};
        var widgetStyleHelpControl=button("Style: "+name,left,TOP+row++*ROW,body,()->change(()->s.nowPlayingStyle=switch(s.nowPlayingStyle){
            case MINECRAFT->NowPlayingStyle.HYBRID;case HYBRID->NowPlayingStyle.AFMR;case AFMR->NowPlayingStyle.MINECRAFT;
        }));
        var widgetStyleHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_style");
        if(!widgetStyleHelpText.getString().isBlank())widgetStyleHelpControl.setTooltip(Tooltip.create(widgetStyleHelpText));
        var nativeOption=addRenderableWidget(minecraft.options.musicToast().createButton(minecraft.options,left,TOP+row++*ROW,body));
        nativeOption.active=s.nowPlaying&&!custom;
        delay=number("Delay (s)",Double.toString(s.popupDelaySeconds),"Delay seconds",left,TOP+row++*ROW,body,s.nowPlaying);
        var delayHelp=Component.translatable("afmr.copy.nowplayingsettings.delay");
        if(!delayHelp.getString().isBlank())delay.setTooltip(Tooltip.create(delayHelp));
        duration=number("Duration (s)",Double.toString(s.popupSeconds),"Popup seconds",left,TOP+row*ROW,half,custom);
        var widgetDurationHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_duration");
        if(!widgetDurationHelpText.getString().isBlank())duration.setTooltip(Tooltip.create(widgetDurationHelpText));
        scale=number("Scale (%)",Integer.toString((int)Math.round(s.widgetScale*100)),"Scale percent",right,TOP+row++*ROW,half,custom);
        var widgetScaleHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_scale");
        if(!widgetScaleHelpText.getString().isBlank())scale.setTooltip(Tooltip.create(widgetScaleHelpText));
        widgetWidth=number("Width",Integer.toString(s.widgetWidth),"Width",left,TOP+row*ROW,half,custom);
        var widgetWidthHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_width");
        if(!widgetWidthHelpText.getString().isBlank())widgetWidth.setTooltip(Tooltip.create(widgetWidthHelpText));
        var textLayout=toggle("Wrap text",right,TOP+row++*ROW,half,()->s.widgetWrap,()->change(()->s.widgetWrap=!s.widgetWrap));
        textLayout.active=custom;var textLayoutHelpText=Component.translatable("afmr.copy.nowplayingsettings.text_layout");
        if(!textLayoutHelpText.getString().isBlank())textLayout.setTooltip(Tooltip.create(textLayoutHelpText));
        toggle("Artwork / note",s.widgetArtwork,left,TOP+row*ROW,half,custom,()->s.widgetArtwork=!s.widgetArtwork);
        toggle("Song title",s.widgetTitle,right,TOP+row++*ROW,half,custom,()->s.widgetTitle=!s.widgetTitle);
        toggle("Artist",s.widgetArtist,left,TOP+row*ROW,half,custom,()->s.widgetArtist=!s.widgetArtist);
        toggle("Album",s.widgetAlbum,right,TOP+row++*ROW,half,custom,()->s.widgetAlbum=!s.widgetAlbum);
        toggle("Song length",s.widgetDuration,left,TOP+row*ROW,half,custom,()->s.widgetDuration=!s.widgetDuration);
        toggle("Source playlist",s.widgetPlaylist,right,TOP+row++*ROW,half,custom,()->s.widgetPlaylist=!s.widgetPlaylist);
        toggle("Scroll long text",s.widgetScroll,left,TOP+row++*ROW,body,custom,()->s.widgetScroll=!s.widgetScroll);
        int y=TOP+row*ROW;
        backgroundColor=edit(String.format(java.util.Locale.ROOT,"#%06X",s.widgetBackgroundColor),"Background color (#RRGGBB)",left+half-76,y,76,null);
        backgroundColor.setMaxLength(7);backgroundColor.active=afmr;labels.add(new Label(backgroundColor,fit("Background",half-106),left));
        var cardBackgroundColorHelpText=Component.translatable("afmr.copy.nowplayingsettings.card_background_color");
        if(!cardBackgroundColorHelpText.getString().isBlank())backgroundColor.setTooltip(Tooltip.create(cardBackgroundColorHelpText));
        var picker=addRenderableWidget(new ColorSwatchButton("Pick Background color",left+half-102,y,20,20,()->s.widgetBackgroundColor,this::pickBackgroundColor));picker.active=afmr;
        opacity=number("Opacity (%)",Double.toString(s.widgetBackgroundOpacity*100),"Background opacity percent",right,TOP+row++*ROW,half,afmr);
        var cardOpacityHelpText=Component.translatable("afmr.copy.nowplayingsettings.card_opacity");
        if(!cardOpacityHelpText.getString().isBlank())opacity.setTooltip(Tooltip.create(cardOpacityHelpText));
        button("Widget font: "+runtime.fonts().name(CustomFonts.Target.WIDGET)+" >",left,TOP+row++*ROW,body,()->{if(apply())minecraft.gui.setScreen(new FontSettingsScreen(this,runtime,CustomFonts.Target.WIDGET));}).active=custom;
        button("Custom background >",left,TOP+row++*ROW,body,()->{if(apply())minecraft.gui.setScreen(new CustomBackgroundScreen(this,runtime,CustomBackground.Target.WIDGET));}).active=afmr;
        button("Reset appearance",left,TOP+row++*ROW,body,()->{backgroundColor.setValue("#000000");opacity.setValue("92");if(apply()){runtime.widgetBackground().fit(dev.animal.afmr.core.MenuImageLayout.Fit.FILL);runtime.widgetBackground().opacity(1);runtime.widgetBackground().enabled(false);rebuildWidgets();}}).active=afmr;
        totalRows=row;
    }
    private EditBox number(String label,String value,String hint,int x,int y,int width,boolean active){
        var field=edit(value,hint,x+width-60,y,60,null);field.active=active;
        labels.add(new Label(field,fit(label,width-66),x));return field;
    }
    private void positionControls(){
        for(var positioned:controls){
            var control=positioned.control();control.setY(TOP+(positioned.row()-scroll)*ROW);
            control.visible=positioned.row()>=scroll&&positioned.row()<scroll+rows;
            if(!control.visible&&control.isFocused()){control.setFocused(false);if(getFocused()==control)setFocused(null);}
        }
    }
    @Override protected void revealControl(AbstractWidget control){for(var position:controls)if(position.control()==control){scrollTo(position.row());break;}}
    private void scrollTo(int target){scroll=Math.max(0,Math.min(target,Math.max(0,totalRows-rows)));positionControls();}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        if(dy!=0&&x>=left&&x<left+contentWidth&&y>=TOP&&y<TOP+rows*ROW){
            scrollTo(scroll-(int)Math.copySign(Math.max(1,Math.ceil(Math.abs(dy)*3)),dy));return true;
        }
        return super.mouseScrolled(x,y,dx,dy);
    }
    @Override public boolean keyPressed(KeyEvent event){
        {
            if(event.key()==InputConstants.KEY_PAGEUP){scrollTo(scroll-rows);return true;}
            if(event.key()==InputConstants.KEY_PAGEDOWN){scrollTo(scroll+rows);return true;}
            if(!(getFocused() instanceof EditBox)&&!(getFocused() instanceof SeekBar)){
                if(event.key()==InputConstants.KEY_HOME){scrollTo(0);return true;}
                if(event.key()==InputConstants.KEY_END){scrollTo(totalRows);return true;}
            }
        }
        return super.keyPressed(event);
    }

    private void toggle(String label,boolean value,int x,int y,int width,boolean active,Runnable action){
        var control=toggle(label,x,y,width,()->value,()->change(action));
        control.active=active;
        var toggleHelpText=label.equals("Scroll long text")?Component.translatable("afmr.copy.nowplayingsettings.scroll_long_text"):Component.translatable("afmr.copy.nowplayingsettings.information_field",label,value?"on":"off");
        if(!toggleHelpText.getString().isBlank())control.setTooltip(Tooltip.create(toggleHelpText));
    }

    private void change(Runnable action){
        if(!apply())return;
        action.run();runtime.save();runtime.overlay().refresh();rebuildWidgets();
    }
    private void pickBackgroundColor(){
        var s=runtime.library().settings;String draft=backgroundColor.getValue();int initial;
        try{initial=MenuColor.parse(draft);}catch(IllegalArgumentException e){initial=s.widgetBackgroundColor;}
        backgroundColor.setValue(String.format(java.util.Locale.ROOT,"#%06X",initial));
        if(!apply()){backgroundColor.setValue(draft);return;}
        minecraft.gui.setScreen(new ColorPickerScreen(this,runtime,"Widget background color",initial,rgb->{
            s.widgetBackgroundColor=rgb;backgroundColor.setValue(String.format(java.util.Locale.ROOT,"#%06X",rgb));runtime.overlay().refresh();
        }));
    }

    private boolean apply(){
        var s=runtime.library().settings;
        try{
            double delaySeconds=delay==null?s.popupDelaySeconds:Double.parseDouble(delay.getValue());
            if(!Double.isFinite(delaySeconds)||delaySeconds<0||delaySeconds>60){error="Notification delay: 0-60 seconds.";return false;}
            double seconds=duration==null?s.popupSeconds:Double.parseDouble(duration.getValue());
            double percent=scale==null?s.widgetScale*100:Double.parseDouble(scale.getValue());
            int width=widgetWidth==null?s.widgetWidth:Integer.parseInt(widgetWidth.getValue());
            double backgroundOpacity=opacity==null?s.widgetBackgroundOpacity*100:Double.parseDouble(opacity.getValue());
            if(!Double.isFinite(seconds)||seconds<1||seconds>60){error="Popup duration: 1-60 seconds.";return false;}
            if(!Double.isFinite(percent)||percent<50||percent>200){error="Widget scale: 50-200%.";return false;}
            if(width<140||width>600){error="Widget width: 140-600 GUI pixels.";return false;}
            if(!Double.isFinite(backgroundOpacity)||backgroundOpacity<0||backgroundOpacity>100){error="Background opacity: 0-100%.";return false;}
            int rgb=s.widgetBackgroundColor;
            if(backgroundColor!=null){
                String hex=backgroundColor.getValue().strip();if(hex.startsWith("#"))hex=hex.substring(1);
                if(!hex.matches("[0-9a-fA-F]{6}")){error="Background color: six hex digits, e.g. #000000.";return false;}
                rgb=Integer.parseInt(hex,16);
            }
            boolean changed=s.popupDelaySeconds!=delaySeconds||s.popupSeconds!=seconds||s.widgetScale!=percent/100||s.widgetWidth!=width
                ||s.widgetBackgroundColor!=rgb||s.widgetBackgroundOpacity!=backgroundOpacity/100;
            s.popupDelaySeconds=delaySeconds;s.popupSeconds=seconds;s.widgetScale=percent/100;s.widgetWidth=width;
            s.widgetBackgroundColor=rgb;s.widgetBackgroundOpacity=backgroundOpacity/100;
            runtime.save();if(changed)runtime.overlay().refresh();error="";return true;
        }catch(NumberFormatException e){error="Duration, scale, width and opacity must be numbers.";return false;}
    }

    @Override public void removed(){runtime.save();minecraft.options.save();super.removed();}
    @Override protected boolean saveChanges(){return apply();}
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        if(page==0)for(var label:labels)if(label.field().visible)
            g.text(menuFont,label.text(),label.x(),label.field().getY()+6,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        if(!error.isBlank())g.text(menuFont,fit(error,contentWidth),left,bodyBottom()-44,0xFFFFA0A0);
    }
}
