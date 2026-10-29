package dev.animal.afmr.ui;

import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConditionOptionTest {
    @Test void everyRuleTogglesWithoutChangingOtherRulesOrParameters(){
        for(var option:ConditionOption.values()){
            var c=new PlaylistConditions();c.startTime=1234;c.endTime=4321;c.area.x=47;c.area.radius=123;c.nearbyPlayerName="Fixture";
            var remembered=new EnumMap<ConditionOption,State>(ConditionOption.class);
            assertFalse(option.enabled(c));option.toggle(c,remembered);assertTrue(option.enabled(c));
            for(var other:ConditionOption.values())if(other!=option)assertFalse(other.enabled(c),option+" changed "+other);
            option.toggle(c,remembered);assertFalse(option.enabled(c));
            assertEquals(1234,c.startTime);assertEquals(4321,c.endTime);assertEquals(47,c.area.x);assertEquals(123,c.area.radius);assertEquals("Fixture",c.nearbyPlayerName);
        }
    }
    @Test void negativeChoicesSurviveDisablingAndReenablingDuringEditing(){
        for(var option:new ConditionOption[]{ConditionOption.COVER,ConditionOption.COMBAT,ConditionOption.WATER,ConditionOption.RIDING}){
            var c=new PlaylistConditions();c.cover=c.combat=c.water=c.ridingAnimal=State.NO;
            var remembered=new EnumMap<ConditionOption,State>(ConditionOption.class);
            option.toggle(c,remembered);assertFalse(option.enabled(c));option.toggle(c,remembered);assertTrue(option.enabled(c));
            assertEquals(State.NO,c.cover);assertEquals(State.NO,c.combat);assertEquals(State.NO,c.water);assertEquals(State.NO,c.ridingAnimal);
        }
    }
}
