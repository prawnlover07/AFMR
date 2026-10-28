package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.HealthComparison;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

final class HealthConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private boolean enabled;
    private HealthComparison comparison;
    private EditBox threshold;
    private FlatButton toggle,mode;
    private String error="";

    HealthConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){
        super("Health condition",parent,runtime);this.conditions=conditions;
        enabled=conditions.healthEnabled;comparison=conditions.healthComparison;
    }
    @Override protected void init(){
        super.init();
        toggle=toggle("Filter",left,47,contentWidth,()->enabled,()->{enabled=!enabled;updateLabels();});
        mode=button("",left,75,contentWidth,()->{comparison=comparison==HealthComparison.BELOW?HealthComparison.ABOVE:HealthComparison.BELOW;updateLabels();});
        var aboveOrBelowHelpText=Component.translatable("afmr.copy.healthcondition.above_or_below");
        if(!aboveOrBelowHelpText.getString().isBlank())mode.setTooltip(Tooltip.create(aboveOrBelowHelpText));
        threshold=edit(Double.toString(conditions.healthThreshold),"Red hearts",left+contentWidth-100,119,100,null);
        var heartThresholdHelpText=Component.translatable("afmr.copy.healthcondition.heart_threshold");
        if(!heartThresholdHelpText.getString().isBlank())threshold.setTooltip(Tooltip.create(heartThresholdHelpText));
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
        updateLabels();
    }
    private void updateLabels(){
        mode.setMessage(Component.literal("Health: "+(comparison==HealthComparison.BELOW?"below":"above")));
    }
    @Override protected boolean saveChanges(){
        double value=conditions.healthThreshold;
        if(enabled){
            try{value=Double.parseDouble(threshold.getValue().strip());if(!Double.isFinite(value)||value<0)throw new NumberFormatException();}
            catch(NumberFormatException e){error="Enter a finite, non-negative number of hearts.";return false;}
        }
        conditions.healthEnabled=enabled;conditions.healthComparison=comparison;conditions.healthThreshold=value;
        runtime.save();error="";return true;
    }
    @Override public void onClose(){if(saveChanges())super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(menuFont,fit("Threshold (red hearts)",contentWidth-108),left,125,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        if(runtime.inWorld())g.text(menuFont,fit(String.format(Locale.ROOT,"Current health: %.2f hearts",runtime.context().healthHearts()),contentWidth),left,159,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
        g.text(menuFont,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}
