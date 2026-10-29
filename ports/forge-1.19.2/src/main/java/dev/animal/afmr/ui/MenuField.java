package dev.animal.afmr.ui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
/** Text entry with the workspace's placeholder and authored description. */
final class MenuField extends EditBox implements Described {
 private final Font menuFont;
 private Component placeholder=Component.empty(),description;
 MenuField(Font font,int x,int y,int width,int height,Component label){super(font,x,y,width,height,label);menuFont=font;}
 void placeholder(Component value){placeholder=value;}
 public Component description(){return description;}
 public void description(Component value){description=value;}
 @Override public void renderButton(PoseStack pose,int mx,int my,float delta){
  super.renderButton(pose,mx,my,delta);
  if(getValue().isEmpty()&&!isFocused())menuFont.draw(pose,menuFont.plainSubstrByWidth(placeholder.getString(),Math.max(0,getWidth()-8)),x+4,y+(height-8)/2,0xFF777A85);
 }
}
