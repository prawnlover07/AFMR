package dev.animal.afmr.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriorityDragTest {
    @Test void clickAndSmallJitterDoNotMove(){var drag=new PriorityDrag();drag.begin("a",20,30);drag.update(22,31,20,0,10);assertTrue(drag.armed());assertFalse(drag.dragging());drag.cancel();assertFalse(drag.armed());}
    @Test void downwardInsertionAccountsForTheRemovedSource(){var drag=new PriorityDrag();drag.begin("a",20,27);drag.update(20,20+3*14+10,20,0,10);assertTrue(drag.dragging());assertEquals(4,drag.slot());assertEquals(3,drag.target(0));}
    @Test void upwardInsertionAndScrolledEndsAreClamped(){var drag=new PriorityDrag();drag.begin("a",20,100);drag.update(20,0,20,5,10);assertEquals(4,drag.slot());assertEquals(4,drag.target(8));drag.update(20,1000,20,5,10);assertEquals(10,drag.slot());assertEquals(9,drag.target(8));drag.update(20,-1000,20,0,10);assertEquals(0,drag.slot());}
}
