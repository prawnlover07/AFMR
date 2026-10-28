package dev.animal.afmr.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerConditionSessionTest {
    private final ServerConditionSession session=new ServerConditionSession();
    private final StructureContext village=new StructureContext(true,List.of(new StructureContext.Bounds("minecraft:village_plains",0,60,0,16,80,16)));
    @Test void ordinaryIdlePlayDoesNotSendRequests(){assertFalse(session.request(1,-1,false));assertFalse(session.request(10,-1,false));}
    @Test void slowResponsesAreNotStarvedByRepeatedRequests(){
        assertTrue(session.request(1,32,false));long token=session.token();
        assertFalse(session.request(1.6,32,false));assertEquals(token,session.token());
        assertTrue(session.receive(token,1.8,false,village,Set.of("minecraft:village_plains")));
        assertEquals(village,session.structures(1.8));assertTrue(session.request(1.9,32,false));
    }
    @Test void missingResponsesExpireAndRetryWithoutTrustingOldPackets(){
        assertTrue(session.request(1,0,true));long first=session.token();assertFalse(session.request(2.99,0,true));
        assertTrue(session.request(3,0,true));assertFalse(session.receive(first,3.1,true,village,Set.of()));
        assertFalse(session.targeted(3.1));assertFalse(session.structures(3.1).available());
    }
    @Test void staleTargetingAndStructuresExpireTogether(){
        session.request(1,0,true);session.receive(session.token(),1.1,true,village,Set.of());
        assertTrue(session.targeted(3.1));assertTrue(session.structures(3.1).available());
        assertFalse(session.targeted(3.1001));assertFalse(session.structures(3.1001).available());
    }
    @Test void stoppingCombatDropsTargetingImmediatelyAndSendsOneStop(){
        session.request(1,-1,true);session.receive(session.token(),1.1,true,StructureContext.UNAVAILABLE,Set.of());
        assertTrue(session.targeted(1.2));assertTrue(session.request(1.2,-1,false));assertFalse(session.targeted(1.2));
        assertFalse(session.request(2,-1,false));
    }
    @Test void radiusChangesInvalidateOldSnapshotsAndResponses(){
        session.request(1,0,false);long old=session.token();session.receive(old,1.1,false,village,Set.of());
        assertTrue(session.request(1.2,128,false));assertFalse(session.structures(1.2).available());
        assertFalse(session.receive(old,1.3,false,village,Set.of()));
    }
    @Test void worldDimensionRespawnAndProfileResetDiscardSnapshotsCatalogAndPendingReplies(){
        session.request(1,0,true);long old=session.token();session.receive(old,1.1,true,village,Set.of("minecraft:village_plains"));
        session.clear();assertTrue(session.catalog().isEmpty());assertFalse(session.targeted(1.2));assertFalse(session.structures(1.2).available());
        session.request(1.2,0,true);assertFalse(session.receive(old,1.3,true,village,Set.of("minecraft:village_plains")));
    }
    @Test void absentCatalogInLaterRepliesRetainsOwnedRegistryIds(){
        var ids=new HashSet<>(Set.of("minecraft:village_plains"));session.request(1,0,false);session.receive(session.token(),1.1,false,village,ids);ids.clear();
        session.request(1.5,0,false);session.receive(session.token(),1.6,false,village,Set.of());
        assertEquals(Set.of("minecraft:village_plains"),session.catalog());
    }
    @Test void duplicateResponseCannotRefreshAnExpiredSnapshot(){
        session.request(1,0,true);long token=session.token();session.receive(token,1.1,true,village,Set.of());
        assertFalse(session.receive(token,4,true,village,Set.of()));assertFalse(session.targeted(4));
    }
    @Test void combatChangesKeepFreshStructureBoundsWhileRequestingNewTargetState(){
        session.request(1,0,false);session.receive(session.token(),1.1,false,village,Set.of());
        assertTrue(session.request(1.2,0,true));assertEquals(village,session.structures(1.2));
        assertFalse(session.targeted(1.2));
    }

}
