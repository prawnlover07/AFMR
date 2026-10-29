package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistProfiles;
import dev.animal.afmr.core.PlaylistProfiles.Info;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ProfilesScreen extends MusicScreen {
    private String selected,error="";
    private MenuField name;
    private DenseList<Info> profiles;
    private int half,right;
    private String activeLabel,joinLabel;
    ProfilesScreen(Screen parent,ClientRuntime runtime){
        super("World / server profiles",parent,runtime);selected=runtime.library().profiles.activeId;
    }
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();var model=runtime.library().profiles;
        activeLabel="Active: "+model.activeName;
        String linked=runtime.inWorld()?model.forWorld(runtime.worldKey()):"";
        joinLabel=runtime.inWorld()?"On next join: "+model.entries().stream().filter(p->p.id().equals(linked)).map(Info::name).findFirst().orElse("Default")
            :new net.minecraft.network.chat.TranslatableComponent("afmr.copy.profiles.join_a_world_or_server_to_link_it").getString();
        half=(contentWidth-12)/2;right=left+half+12;
        profiles=addRenderableWidget(new DenseList<>(left,70,half,Math.max(28,bodyBottom()-142),this::drawProfile,
            (p,clickX,clickY,button,twice)->{selected=p.id();init(minecraft,width,height);},Info::name));profiles.items(model.entries());
        String label=model.entries().stream().filter(p->p.id().equals(selected)).findFirst().orElseThrow().name();
        name=edit(label,"Profile name",right,70,half,null);name.setMaxLength(80);
        int w=(half-4)/2;
        var newProfileHelpControl=button("New",right,94,w,()->create(false));
        var newProfileHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.profiles.new_profile");
        if(!newProfileHelpText.getString().isBlank())newProfileHelpControl.description(newProfileHelpText);
        var copySelectedProfileHelpControl=button("Copy selected",right+w+4,94,w,()->create(true));
        var copySelectedProfileHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.profiles.copy_selected_profile");
        if(!copySelectedProfileHelpText.getString().isBlank())copySelectedProfileHelpControl.description(copySelectedProfileHelpText);
        button("Rename",right,118,w,()->change(()->model.rename(selected,name.getValue())));
        var use=button("Use now",right+w+4,118,w,()->{
            if(runtime.activateProfile(selected)){
                Screen root=parent instanceof LibraryScreen editor?editor.parent:null;
                minecraft.setScreen(new LibraryScreen(root,runtime,"day_outside"));
            }
        });
        use.active=!selected.equals(model.activeId);
        var useNowHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.profiles.use_now");
        if(!useNowHelpText.getString().isBlank())use.description(useNowHelpText);
        var link=button("Link current",right,142,w,()->change(()->model.bind(runtime.worldKey(),selected)));
        link.active=runtime.inWorld();var linkCurrentWorldOrServerHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.profiles.link_current_world_or_server");
        if(!linkCurrentWorldOrServerHelpText.getString().isBlank())link.description(linkCurrentWorldOrServerHelpText);
        var unlink=button("Unlink current",right+w+4,142,w,()->change(()->model.bindings.remove(runtime.worldKey())));
        unlink.active=runtime.inWorld()&&model.bindings.containsKey(runtime.worldKey());
        var unlinkCurrentWorldOrServerHelpText=new net.minecraft.network.chat.TranslatableComponent("afmr.copy.profiles.unlink_current_world_or_server");
        if(!unlinkCurrentWorldOrServerHelpText.getString().isBlank())unlink.description(unlinkCurrentWorldOrServerHelpText);
        button("Linked worlds",right,166,w,()->minecraft.setScreen(new ProfileLinksScreen(this,runtime,selected)));
        button("Delete selected",right+w+4,166,w,()->minecraft.setScreen(new ConfirmScreen(yes->{
            if(yes){model.delete(selected);selected=model.activeId;runtime.save();}
            minecraft.setScreen(this);init(minecraft,width,height);
        },new net.minecraft.network.chat.TextComponent("Delete profile?"),new net.minecraft.network.chat.TextComponent("Delete this setup and its links? Shared music and other profiles are kept."))))
            .active=!selected.equals(PlaylistProfiles.DEFAULT)&&!selected.equals(model.activeId);
        button("Back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    private void create(boolean copy){
        change(()->selected=runtime.library().profiles.create(runtime.library(),name.getValue(),copy?selected:null));
    }
    private void change(Runnable action){
        try{action.run();runtime.save();error="";init(minecraft,width,height);}
        catch(IllegalArgumentException e){error=e.getMessage();}
    }
    private void drawProfile(PoseStack g,Info p,int x,int y,int width,boolean hover){
        if(p.id().equals(selected))net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+width,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
        String text=(p.id().equals(runtime.library().profiles.activeId)?"> ":"")+p.name();
        menuFont.draw(g,fit(text,width-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6));
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        menuFont.drawShadow(g,fit(activeLabel,contentWidth),left,51,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        menuFont.drawShadow(g,fit(error.isBlank()?joinLabel:error,contentWidth),left,bodyBottom()-48,error.isBlank()?MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2):0xFFFFA0A0);
    }
}
