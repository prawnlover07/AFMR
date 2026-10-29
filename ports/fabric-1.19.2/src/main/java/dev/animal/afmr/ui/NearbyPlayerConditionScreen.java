package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.NearbyPlayers;
import dev.animal.afmr.core.PlaylistConditions;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.animal.afmr.ui.MenuField;
import net.minecraft.client.gui.screens.Screen;
import dev.animal.afmr.core.MenuColor;

final class NearbyPlayerConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private MenuField name,radius;
    private String error="",chosenName,chosenUuid,guidance;
    NearbyPlayerConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Nearby player condition",parent,runtime);this.conditions=conditions;chosenName=conditions.nearbyPlayerName;chosenUuid=conditions.nearbyPlayerUuid;
    }
    @Override protected void init(){
        super.init();guidance=fit(net.minecraft.network.chat.Component.translatable("afmr.copy.nearbyplayer.guidance").getString(),contentWidth);
        toggle("Nearby player",left,48,(contentWidth-6)/2,()->conditions.nearbyPlayerEnabled,()->{
            if(conditions.nearbyPlayerEnabled){conditions.nearbyPlayerEnabled=false;runtime.save();rebuildWidgets();}
            else if(apply()){if(conditions.nearbyPlayerName.isEmpty()){error="Choose or enter a player.";return;}conditions.nearbyPlayerEnabled=true;runtime.save();rebuildWidgets();}
        });
        button("Proximity volume…",left+(contentWidth-6)/2+6,48,contentWidth-(contentWidth-6)/2-6,()->{if(apply())minecraft.setScreen(new PlayerProximityVolumeScreen(this,runtime,conditions));})
            .description(net.minecraft.network.chat.Component.translatable("afmr.copy.nearbyplayer.proximity_volume"));
        name=edit(conditions.nearbyPlayerName,"Player name",left,76,contentWidth-106,null);name.setMaxLength(16);
        radius=edit(Double.toString(conditions.nearbyPlayerRadius),"Radius (blocks)",left+contentWidth-100,76,100,null);
        int rows=Math.max(1,(bodyBottom()-177)/16);
        var players=minecraft.getConnection()==null?java.util.List.<net.minecraft.client.multiplayer.PlayerInfo>of():minecraft.getConnection().getOnlinePlayers();
        var list=addRenderableWidget(new DenseList<net.minecraft.client.multiplayer.PlayerInfo>(left,119,contentWidth,rows*16,
            (g,p,x,y,w,hover)->menuFont.drawShadow(g,fit(p.getProfile().getName(),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED)),
            (p,clickX,clickY,button,twice)->{name.setValue(p.getProfile().getName());chosenUuid=p.getProfile().getId().toString();chosenName=p.getProfile().getName();},p->p.getProfile().getName()));
        list.items(players.stream().filter(p->minecraft.player==null||!p.getProfile().getId().equals(minecraft.player.getUUID())).toList());
        button("Save & back",left,bodyBottom()-29,contentWidth,this::onClose);
    }
    private boolean apply(){
        String target=name.getValue().strip();double blocks;
        try{blocks=Double.parseDouble(radius.getValue());if(!Double.isFinite(blocks)||blocks<1||blocks>1024)throw new NumberFormatException();}
        catch(NumberFormatException e){error="Radius: 1-1024 blocks.";return false;}
        if(!target.isEmpty()&&!NearbyPlayers.validName(target)){error="Use a Minecraft player name (1-16 letters, digits or underscores).";return false;}
        if(conditions.nearbyPlayerEnabled&&target.isEmpty()){error="Choose or enter a player.";return false;}
        String uuid=target.equalsIgnoreCase(chosenName)?chosenUuid:"";

        if(minecraft.getConnection()!=null)for(var player:minecraft.getConnection().getOnlinePlayers())if(player.getProfile().getName().equalsIgnoreCase(target)){
            uuid=player.getProfile().getId().toString();break;
        }
        conditions.nearbyPlayerName=target;conditions.nearbyPlayerUuid=uuid;conditions.nearbyPlayerRadius=blocks;
        chosenName=target;chosenUuid=uuid;
        runtime.save();error="";return true;
    }
    @Override protected boolean saveChanges(){return apply();}
    @Override public void onClose(){if(apply())super.onClose();}
    @Override protected void drawBody(PoseStack g,int x,int y,float dt){
        menuFont.drawShadow(g,guidance,left,103,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        if(!error.isEmpty())menuFont.drawShadow(g,fit(error,contentWidth),left,bodyBottom()-44,0xFFFFA0A0);
    }
}
