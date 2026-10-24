package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;

import dev.animal.afmr.core.Library.WidgetCorner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractSliderButton;
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

    @Override protected void init(){
        super.init();var s=runtime.library().settings;
        delay=duration=scale=widgetWidth=backgroundColor=opacity=null;controls.clear();labels.clear();
        int half=(contentWidth-6)/2;
        button("Widget",left,47,half,()->{if(apply()){page=0;rebuildWidgets();}}).active=page!=0;
        button("Menus",left+half+6,47,half,()->{if(apply()){page=1;rebuildWidgets();}}).active=page!=1;
        if(page==0){
            int first=children().size();buildWidget();
            for(int i=first;i<children().size();i++)if(children().get(i) instanceof AbstractWidget control)
                controls.add(new Positioned(control,(control.getY()-TOP)/ROW));
            rows=Math.max(1,(height-53-TOP+4)/ROW);scroll=Math.max(0,Math.min(scroll,Math.max(0,totalRows-rows)));
            positionControls();
            addRenderableWidget(new SettingsScrollbar(left+contentWidth-8,TOP,rows*ROW-4,Math.max(0,totalRows-rows),rows,()->scroll,this::scrollTo));
        }else{
            var menuOpacity=addRenderableWidget(new AbstractSliderButton(left,79,contentWidth,20,
                Component.literal("Background opacity: "+Math.round(s.menuBackgroundOpacity*100)+"%"),s.menuBackgroundOpacity){
                @Override protected void updateMessage(){setMessage(Component.literal("Background opacity: "+Math.round(value*100)+"%"));}
                @Override protected void applyValue(){s.menuBackgroundOpacity=value;}
            });
            var menuOpacityHelpText=Component.translatable("afmr.copy.nowplayingsettings.menu_opacity");
            if(!menuOpacityHelpText.getString().isBlank())menuOpacity.setTooltip(Tooltip.create(menuOpacityHelpText));
            button("Custom background…",left,107,half,()->{if(apply())minecraft.setScreen(new MenuBackgroundScreen(this,runtime));});
            button("Menu colors…",left+half+6,107,half,()->{if(apply())minecraft.setScreen(new MenuColorsScreen(this,runtime));});
            button("Reset menu appearance",left,135,contentWidth,()->change(()->{s.menuColors=MenuColor.defaults();s.menuBackgroundOpacity=0.80;s.menuImageOpacity=1;s.menuImageFit=dev.animal.afmr.core.MenuImageLayout.Fit.FILL;runtime.menuBackground().enabled(false);}));

        }
        if(page==0){
            var preview=button("Preview widget",left,height-29,half,()->{
                if(apply()){runtime.overlay().refresh();minecraft.setScreen(new NowPlayingPreviewScreen(this,runtime));}
            });
            var previewWidgetHelpText=Component.translatable("afmr.copy.nowplayingsettings.preview_widget.fabric");
            if(!previewWidgetHelpText.getString().isBlank())preview.setTooltip(Tooltip.create(previewWidgetHelpText));
        }
        button("Save & back",page==1?left:left+half+6,height-29,page==1?contentWidth:half,this::onClose);
    }

    private void buildWidget(){
        var s=runtime.library().settings;
        int body=contentWidth-12,half=(body-6)/2,right=left+half+6,row=0;
        button("Now playing: "+(s.nowPlaying?"on":"off"),left,TOP,half,()->change(()->s.nowPlaying=!s.nowPlaying));
        var widgetCornerHelpControl=button("Corner: "+s.widgetCorner.label(),right,TOP,half,()->change(()->s.widgetCorner=WidgetCorner.values()[(s.widgetCorner.ordinal()+1)%WidgetCorner.values().length]));
        var widgetCornerHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_corner.fabric");
        if(!widgetCornerHelpText.getString().isBlank())widgetCornerHelpControl.setTooltip(Tooltip.create(widgetCornerHelpText));
        row++;
        delay=number("Delay (s)",Double.toString(s.popupDelaySeconds),"Delay seconds",left,TOP+row++*ROW,body);
        var delayHelp=Component.translatable("afmr.copy.nowplayingsettings.delay");
        if(!delayHelp.getString().isBlank())delay.setTooltip(Tooltip.create(delayHelp));
        duration=number("Duration (s)",Double.toString(s.popupSeconds),"Popup seconds",left,TOP+row*ROW,half);
        var widgetDurationHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_duration.fabric");
        if(!widgetDurationHelpText.getString().isBlank())duration.setTooltip(Tooltip.create(widgetDurationHelpText));
        scale=number("Scale (%)",Integer.toString((int)Math.round(s.widgetScale*100)),"Scale percent",right,TOP+row++*ROW,half);
        var widgetScaleHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_scale.fabric");
        if(!widgetScaleHelpText.getString().isBlank())scale.setTooltip(Tooltip.create(widgetScaleHelpText));
        widgetWidth=number("Width",Integer.toString(s.widgetWidth),"Width",left,TOP+row*ROW,half);
        var widgetWidthHelpText=Component.translatable("afmr.copy.nowplayingsettings.widget_width.fabric");
        if(!widgetWidthHelpText.getString().isBlank())widgetWidth.setTooltip(Tooltip.create(widgetWidthHelpText));
        var textLayout=button("Text: "+(s.widgetWrap?"word wrap":"single line"),right,TOP+row++*ROW,half,()->change(()->s.widgetWrap=!s.widgetWrap));
        var textLayoutHelpText=Component.translatable("afmr.copy.nowplayingsettings.text_layout");
        if(!textLayoutHelpText.getString().isBlank())textLayout.setTooltip(Tooltip.create(textLayoutHelpText));
        toggle("Artwork / note",s.widgetArtwork,left,TOP+row*ROW,half,()->s.widgetArtwork=!s.widgetArtwork);
        toggle("Song title",s.widgetTitle,right,TOP+row++*ROW,half,()->s.widgetTitle=!s.widgetTitle);
        toggle("Artist",s.widgetArtist,left,TOP+row*ROW,half,()->s.widgetArtist=!s.widgetArtist);
        toggle("Album",s.widgetAlbum,right,TOP+row++*ROW,half,()->s.widgetAlbum=!s.widgetAlbum);
        toggle("Song length",s.widgetDuration,left,TOP+row*ROW,half,()->s.widgetDuration=!s.widgetDuration);
        toggle("Source playlist",s.widgetPlaylist,right,TOP+row++*ROW,half,()->s.widgetPlaylist=!s.widgetPlaylist);
        toggle("Scroll long text",s.widgetScroll,left,TOP+row++*ROW,body,()->s.widgetScroll=!s.widgetScroll);
        int y=TOP+row*ROW;
        backgroundColor=edit(String.format(java.util.Locale.ROOT,"#%06X",s.widgetBackgroundColor),"Background color (#RRGGBB)",left+half-76,y,76,null);
        backgroundColor.setMaxLength(7);labels.add(new Label(backgroundColor,fit("Background",half-106),left));
        var cardBackgroundColorHelpText=Component.translatable("afmr.copy.nowplayingsettings.card_background_color.fabric");
        if(!cardBackgroundColorHelpText.getString().isBlank())backgroundColor.setTooltip(Tooltip.create(cardBackgroundColorHelpText));
        var picker=addRenderableWidget(new ColorSwatchButton("Pick Background color",left+half-102,y,20,20,()->s.widgetBackgroundColor,this::pickBackgroundColor));
        opacity=number("Opacity (%)",Double.toString(s.widgetBackgroundOpacity*100),"Background opacity percent",right,TOP+row++*ROW,half);
        var cardOpacityHelpText=Component.translatable("afmr.copy.nowplayingsettings.card_opacity");
        if(!cardOpacityHelpText.getString().isBlank())opacity.setTooltip(Tooltip.create(cardOpacityHelpText));
        String[] names={"Black","Gray","Blue","White"},colors={"#000000","#303030","#102030","#FFFFFF"};int presetWidth=(body-12)/4;
        for(int i=0;i<names.length;i++){
            String color=colors[i];button(names[i],left+i*(presetWidth+4),TOP+row*ROW,presetWidth,()->{backgroundColor.setValue(color);if(apply())rebuildWidgets();});
        }
        row++;
        button("Reset appearance",left,TOP+row++*ROW,body,()->{backgroundColor.setValue("#000000");opacity.setValue("92");if(apply())rebuildWidgets();});
        totalRows=row;
    }
    private EditBox number(String label,String value,String hint,int x,int y,int width){
        var field=edit(value,hint,x+width-60,y,60,null);
        labels.add(new Label(field,fit(label,width-66),x));return field;
    }
    private void positionControls(){
        for(var positioned:controls){
            var control=positioned.control();control.setY(TOP+(positioned.row()-scroll)*ROW);
            control.visible=positioned.row()>=scroll&&positioned.row()<scroll+rows;
            if(!control.visible&&control.isFocused()){control.setFocused(false);if(getFocused()==control)setFocused(null);}
        }
    }
    private void scrollTo(int target){scroll=Math.max(0,Math.min(target,Math.max(0,totalRows-rows)));positionControls();}
    @Override public boolean mouseScrolled(double x,double y,double dy){
        if(page==0&&dy!=0&&x>=left&&x<left+contentWidth&&y>=TOP&&y<TOP+rows*ROW){
            scrollTo(scroll-(int)Math.copySign(Math.max(1,Math.ceil(Math.abs(dy)*3)),dy));return true;
        }
        return super.mouseScrolled(x,y,dy);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(page==0){
            if(key==InputConstants.KEY_PAGEUP){scrollTo(scroll-rows);return true;}
            if(key==InputConstants.KEY_PAGEDOWN){scrollTo(scroll+rows);return true;}
            if(!(getFocused() instanceof EditBox)){
                if(key==InputConstants.KEY_HOME){scrollTo(0);return true;}
                if(key==InputConstants.KEY_END){scrollTo(totalRows);return true;}
            }
        }
        return super.keyPressed(key,scan,modifiers);
    }

    private void toggle(String label,boolean value,int x,int y,int width,Runnable action){
        var control=button(label+": "+(value?"on":"off"),x,y,width,()->change(action));
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
        minecraft.setScreen(new ColorPickerScreen(this,runtime,"Widget background color",initial,rgb->{
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
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        if(page==0)for(var label:labels)if(label.field().visible)
            g.drawString(font,label.text(),label.x(),label.field().getY()+6,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        if(!error.isBlank())g.drawString(font,fit(error,contentWidth),left,height-44,0xFFFFA0A0);
    }
}
