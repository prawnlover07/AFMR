package dev.animal.afmr.ui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
/** Screen-owned hover timing; no global references retain closed menus. */
final class MenuDescriptions {
 private AbstractWidget previous;
 private long since;
 void draw(Screen owner,PoseStack pose,int mx,int my){
  AbstractWidget found=null;
  for(var child:owner.children())if(child instanceof AbstractWidget widget&&widget.visible&&widget instanceof Described text&&text.description()!=null&&widget.isHoveredOrFocused())found=widget;
  if(found!=previous){previous=found;since=System.nanoTime();}
  if(found!=null&&System.nanoTime()-since>=400_000_000L)owner.renderTooltip(pose,((Described)found).description(),mx,my);
 }
}
