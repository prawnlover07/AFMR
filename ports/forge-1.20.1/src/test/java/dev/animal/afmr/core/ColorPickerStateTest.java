package dev.animal.afmr.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ColorPickerStateTest {
    @Test void primaryAndSecondaryHuesMatchRgbEndpoints(){
        int[] expected={0xFF0000,0xFFFF00,0x00FF00,0x00FFFF,0x0000FF,0xFF00FF,0xFF0000};
        for(int i=0;i<expected.length;i++)assertEquals(expected[i],ColorPickerState.rgb(i/6d,1,1));
    }
    @Test void allGrayLevelsAndRandomRgbValuesRoundTripExactly(){
        for(int i=0;i<256;i++)assertEquals(i*0x010101,new ColorPickerState(i*0x010101).rgb());
        var random=new Random(3);for(int i=0;i<10000;i++){int rgb=random.nextInt(0x1000000);assertEquals(rgb,new ColorPickerState(rgb).rgb());}
    }
    @Test void hsbConversionAgreesWithIndependentJdkReference(){
        for(int h=0;h<24;h++)for(int s=0;s<=10;s++)for(int v=0;v<=10;v++){
            int actual=ColorPickerState.rgb(h/24d,s/10d,v/10d),reference=java.awt.Color.HSBtoRGB(h/24f,s/10f,v/10f)&0xFFFFFF;
            for(int shift:new int[]{0,8,16})assertEquals((reference>>shift)&255,(actual>>shift)&255,1);
        }
    }
    @Test void hueSurvivesGrayAndBlackHexEdits(){
        var color=new ColorPickerState(0x0000FF);color.rgb(0x808080);assertEquals(2/3d,color.hue(),1e-10);color.rgb(0);
        assertEquals(2/3d,color.hue(),1e-10);color.saturationValue(1,1);assertEquals(0x0000FF,color.rgb());
    }
    @Test void saturationAndValueCornersMatchVisualSquare(){
        var color=new ColorPickerState(0x00FF00);color.saturationValue(0,1);assertEquals(0xFFFFFF,color.rgb());
        color.saturationValue(1,1);assertEquals(0x00FF00,color.rgb());color.saturationValue(1,0);assertEquals(0,color.rgb());
        color.saturationValue(0.5,0.5);assertEquals(0x408040,color.rgb());
    }
    @Test void pointerAndNonFiniteInputsClampWithoutInvalidRgb(){
        var color=new ColorPickerState(0);color.hue(2);color.saturationValue(2,2);assertEquals(0xFF0000,color.rgb());
        color.hue(-1);color.saturationValue(-1,1);assertEquals(0xFFFFFF,color.rgb());
        color.hue(Double.NaN);color.saturationValue(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY);assertEquals(0,color.rgb());
    }
    @Test void alphaBitsDoNotEnterTheRgbPickerState(){
        assertEquals(0x123456,new ColorPickerState(0x80123456).rgb());
    }
}
