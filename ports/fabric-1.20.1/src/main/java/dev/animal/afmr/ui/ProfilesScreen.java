package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistProfiles;
import dev.animal.afmr.core.PlaylistProfiles.Info;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ProfilesScreen extends MusicScreen {
    private String selected,error="";
    private EditBox name;
    private DenseList<Info> profiles;
    private int half,right;
    private String activeLabel,joinLabel;
    ProfilesScreen(Screen parent,ClientRuntime runtime){
        super("World / server profiles",parent,runtime);selected=runtime.library().profiles.activeId;
    }
    @Override protected void init(){
        super.init();var model=runtime.library().profiles;
        activeLabel="Active: "+model.activeName;
        String linked=runtime.inWorld()?model.forWorld(runtime.worldKey()):"";
        joinLabel=runtime.inWorld()?"On next join: "+model.entries().stream().filter(p->p.id().equals(linked)).map(Info::name).findFirst().orElse("Default")
            :Component.translatable("afmr.copy.profiles.join_a_world_or_server_to_link_it").getString();
        half=(contentWidth-12)/2;right=left+half+12;
        profiles=addRenderableWidget(new DenseList<>(left,70,half,Math.max(28,bodyBottom()-142),this::drawProfile,
            (p,clickX,clickY,button,twice)->{selected=p.id();rebuildWidgets();},Info::name));profiles.items(model.entries());
        String label=model.entries().stream().filter(p->p.id().equals(selected)).findFirst().orElseThrow().name();
        name=edit(label,"Profile name",right,70,half,null);name.setMaxLength(80);
        int w=(half-4)/2;
        var newProfileHelpControl=button("New",right,94,w,()->create(false));
        var newProfileHelpText=Component.translatable("afmr.copy.profiles.new_profile");
        if(!newProfileHelpText.getString().isBlank())newProfileHelpControl.setTooltip(Tooltip.create(newProfileHelpText));
        var copySelectedProfileHelpControl=button("Copy selected",right+w+4,94,w,()->create(true));
        var copySelectedProfileHelpText=Component.translatable("afmr.copy.profiles.copy_selected_profile");
        if(!copySelectedProfileHelpText.getString().isBlank())copySelectedProfileHelpControl.setTooltip(Tooltip.create(copySelectedProfileHelpText));
        button("Rename",right,118,w,()->change(()->model.rename(selected,name.getValue())));
        var use=button("Use now",right+w+4,118,w,()->{
            if(runtime.activateProfile(selected)){
                Screen root=parent instanceof LibraryScreen editor?editor.parent:null;
                minecraft.setScreen(new LibraryScreen(root,runtime,"day_outside"));
            }
        });
        use.active=!selected.equals(model.activeId);
        var useNowHelpText=Component.translatable("afmr.copy.profiles.use_now");
        if(!useNowHelpText.getString().isBlank())use.setTooltip(Tooltip.create(useNowHelpText));
        var link=button("Link current",right,142,w,()->change(()->model.bind(runtime.worldKey(),selected)));
        link.active=runtime.inWorld();var linkCurrentWorldOrServerHelpText=Component.translatable("afmr.copy.profiles.link_current_world_or_server");
        if(!linkCurrentWorldOrServerHelpText.getString().isBlank())link.setTooltip(Tooltip.create(linkCurrentWorldOrServerHelpText));
        var unlink=button("Unlink current",right+w+4,142,w,()->change(()->model.bindings.remove(runtime.worldKey())));
        unlink.active=runtime.inWorld()&&model.bindings.containsKey(runtime.worldKey());
        var unlinkCurrentWorldOrServerHelpText=Component.translatable("afmr.copy.profiles.unlink_current_world_or_server");
        if(!unlinkCurrentWorldOrServerHelpText.getString().isBlank())unlink.setTooltip(Tooltip.create(unlinkCurrentWorldOrServerHelpText));
        button("Linked worlds",right,166,w,()->minecraft.setScreen(new ProfileLinksScreen(this,runtime,selected)));
        button("Delete selected",right+w+4,166,w,()->minecraft.setScreen(new ConfirmScreen(yes->{
            if(yes){model.delete(selected);selected=model.activeId;runtime.save();}
            minecraft.setScreen(this);rebuildWidgets();
        },Component.literal("Delete profile?"),Component.literal("Delete this setup and its links? Shared music and other profiles are kept."))))
            .active=!selected.equals(PlaylistProfiles.DEFAULT)&&!selected.equals(model.activeId);
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    private void create(boolean copy){
        change(()->selected=runtime.library().profiles.create(runtime.library(),name.getValue(),copy?selected:null));
    }
    private void change(Runnable action){
        try{action.run();runtime.save();error="";rebuildWidgets();}
        catch(IllegalArgumentException e){error=e.getMessage();}
    }
    private void drawProfile(GuiGraphics g,Info p,int x,int y,int width,boolean hover){
        if(p.id().equals(selected))g.fill(x,y,x+width,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
        String text=(p.id().equals(runtime.library().profiles.activeId)?"> ":"")+p.name();
        g.drawString(menuFont,fit(text,width-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6),false);
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        g.drawString(menuFont,fit(activeLabel,contentWidth),left,51,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        g.drawString(menuFont,fit(error.isBlank()?joinLabel:error,contentWidth),left,bodyBottom()-48,error.isBlank()?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2):0xFFFFA0A0);
    }
}
