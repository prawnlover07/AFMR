package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.core.TransitionRule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/** Draft edits stay owned by this screen until Save; empty numeric fields inherit. */
final class TransitionRuleScreen extends MusicScreen {
    private final Playlist source,destination;
    private TransitionRule draft;
    private int page;
    private String error="";
    private final Map<String,EditBox> fields=new LinkedHashMap<>();
    TransitionRuleScreen(Screen parent,ClientRuntime runtime,Playlist source,Playlist destination){
        super("Transition rule",parent,runtime);this.source=source;this.destination=destination;runtime.playlistEdits().watch(runtime.library(),source);
        draft=source.transitions.containsKey(destination.id)?source.transitions.get(destination.id).copy():new TransitionRule();
    }
    @Override protected void init(){
        super.init();fields.clear();
        button("Current song: "+draft.finish.name().toLowerCase(java.util.Locale.ROOT),left,60,contentWidth,()->{
            if(readDraft()){draft.finish=TransitionRule.Finish.values()[(draft.finish.ordinal()+1)%3];rebuildWidgets();}
        });
        if(page==0){
            field("Context buffer (s)",draft.customBuffer,draft.bufferSeconds,88);
            field("Fade out (s)",draft.customFadeOut,draft.fadeOutSeconds,116);
            field("Fade in (s)",draft.customFadeIn,draft.fadeInSeconds,144);
        }else{
            field("Minimum silence (s)",draft.customSilence,draft.silenceMinSeconds,88);
            field("Maximum silence (s)",draft.customSilence,draft.silenceMaxSeconds,116);
        }
        button("Use defaults",left,bodyBottom()-59,contentWidth,()->{draft=new TransitionRule();error="";rebuildWidgets();});
        pages(page,1,2,bodyBottom()-29,next->{if(readDraft()){page=next;rebuildWidgets();}});
        button("Save & back",left+contentWidth-116,bodyBottom()-29,116,this::onClose);
    }
    private void field(String label,boolean custom,double value,int y){
        var field=edit(custom?Double.toString(value):"","Inherit",left+contentWidth-108,y,108,null);
        var text=Component.translatable("afmr.copy.transitions.numeric");if(!text.getString().isBlank())field.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text));fields.put(label,field);
    }
    private boolean readDraft(){
        var next=draft.copy();
        try{
            if(page==0){
                next.customBuffer=!blank("Context buffer (s)");next.bufferSeconds=value("Context buffer (s)",60);
                next.customFadeOut=!blank("Fade out (s)");next.fadeOutSeconds=value("Fade out (s)",60);
                next.customFadeIn=!blank("Fade in (s)");next.fadeInSeconds=value("Fade in (s)",60);
            }else{
                boolean min=blank("Minimum silence (s)"),max=blank("Maximum silence (s)");if(min!=max)throw new IllegalArgumentException("Fill both silence fields, or leave both blank.");
                next.customSilence=!min;next.silenceMinSeconds=value("Minimum silence (s)",86400);next.silenceMaxSeconds=value("Maximum silence (s)",86400);
            }
            if(!next.valid())throw new IllegalArgumentException("Maximum silence must be at least minimum silence.");
            draft=next;error="";return true;
        }catch(IllegalArgumentException e){error=e.getMessage();return false;}
    }
    private boolean blank(String name){return fields.get(name).getValue().isBlank();}
    private double value(String name,double maximum){
        if(blank(name))return 0;
        try{double v=Double.parseDouble(fields.get(name).getValue());if(!Double.isFinite(v)||v<0||v>maximum)throw new NumberFormatException();return v;}
        catch(NumberFormatException e){throw new IllegalArgumentException(name+": 0-"+(int)maximum+", or blank to inherit.");}
    }
    @Override protected boolean saveChanges(){
        if(!readDraft())return false;
        if(runtime.library().playlist(source.id)!=source||runtime.library().playlist(destination.id)!=destination){error="Playlist no longer available.";return false;}
        if(draft.inheritsAll())source.transitions.remove(destination.id);
        else if(source.transitions.containsKey(destination.id)||source.transitions.size()<128)source.transitions.put(destination.id,draft.copy());
        else{error="Maximum 128 destination rules per playlist.";return false;}
        runtime.save();return true;
    }
    @Override public void onClose(){if(saveChanges())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float dt){
        g.text(menuFont,fit(source.name+" → "+destination.name,contentWidth),left,47,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
        int row=0;for(String label:fields.keySet())g.text(menuFont,fit(label,contentWidth-116),left,94+row++*28,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
        if(!error.isEmpty())g.text(menuFont,fit(error,contentWidth),left,bodyBottom()-43,0xFFFFA0A0);
    }
}
