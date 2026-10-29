package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.network.chat.Component;
import dev.animal.afmr.core.Library.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

public final class OverridesScreen extends MusicScreen {
    private final int slot;
    private String selected,query="",feedback="";
    private DenseList<Playlist> list;
    private FlatButton one,held;
    public OverridesScreen(Screen parent,ClientRuntime runtime,int slot,String selected){
        super(slot<0?"Playlist overrides":"Assign override slot "+(slot+1),parent,runtime);this.slot=slot;this.selected=selected;
    }
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();int third=(contentWidth-8)/3;
        list=addRenderableWidget(new DenseList<>(left,73,contentWidth,Math.max(28,bodyBottom()-153),
            (g,p,x,y,w,hover)->{if(p.id.equals(selected)||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                menuFont.drawShadow(g,fit(p.name+(p.manualOnly?" [manual]":"")+(p.enabled?"":" [auto off]"),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));},
            (p,clickX,clickY,button,twice)->{selected=p.id;updateButtons();},p->p.name));
        edit(query,"Find a playlist",left,47,contentWidth-124,value->{query=value;filter();});
        button(slot<0?"Keybind slots...":"Back to slots",left+contentWidth-118,47,118,()->{if(slot<0)minecraft.setScreen(new OverrideBindingsScreen(this,runtime));else onClose();});
        one=button(slot<0?"Play one song":"Bind: one song",left,bodyBottom()-55,third,()->choose(OverrideMode.ONE_SONG));
        held=button(slot<0?"Until released":"Bind: until released",left+third+4,bodyBottom()-55,third,()->choose(OverrideMode.UNTIL_RELEASED));
        var playOneSongHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.overrides.play_one_song");
        if(!playOneSongHelpText.getString().isBlank())one.description(playOneSongHelpText);
        var untilReleasedHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.overrides.until_released");
        if(!untilReleasedHelpText.getString().isBlank())held.description(untilReleasedHelpText);
        button(slot<0?"Automatic":"Clear binding",left+2*(third+4),bodyBottom()-55,third,()->{
            if(slot<0){runtime.returnAutomatic();feedback="Automatic selection restored.";}
            else{runtime.library().settings.overrideSlots[slot].playlistId="";runtime.save();onClose();}
        });
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);filter();updateButtons();list.reveal(runtime.library().playlist(selected));
    }
    private void filter(){String q=query.strip().toLowerCase(Locale.ROOT);list.items(runtime.library().playlists.stream().filter(p->p.name.toLowerCase(Locale.ROOT).contains(q)).toList());list.top();}
    private void updateButtons(){var p=runtime.library().playlist(selected);one.active=p!=null&&(slot>=0||runtime.inWorld());held.active=one.active;}
    private void choose(OverrideMode mode){
        if(slot>=0){var b=runtime.library().settings.overrideSlots[slot];b.playlistId=selected;b.mode=mode;runtime.save();onClose();}
        else if(runtime.overridePlaylist(selected,mode))onClose();else feedback=runtime.status();
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        if(!feedback.isBlank())menuFont.drawShadow(g,fit(feedback,contentWidth),left,bodyBottom()-74,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFF919FAF));
        if(slot<0){var p=runtime.library().playlist(runtime.engine().overrideId());menuFont.drawShadow(g,fit(p==null?"Mode: automatic":"Override: "+p.name+" / "+runtime.engine().overrideMode(),contentWidth-112),left,bodyBottom()-23,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));}
    }
}
