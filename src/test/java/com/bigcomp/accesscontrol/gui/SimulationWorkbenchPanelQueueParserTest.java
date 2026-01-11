package com.bigcomp.accesscontrol.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationWorkbenchPanelQueueParserTest {

    @Test
    void parsesTokensInOrder() {
        SimulationWorkbenchPanel.ParsedQueueLine p = SimulationWorkbenchPanel.parseQueueLine("Swipe user=Alice Zhang userId=U1 resource=LobbyGate x10");
        assertNull(p.error);
        assertEquals("Swipe", p.rawAction);
        assertEquals("Alice Zhang", p.userValue);
        assertEquals("U1", p.userId);
        assertEquals("LobbyGate", p.resourceName);
        assertEquals(10, p.count);
    }

    @Test
    void parsesTokensOutOfOrder() {
        SimulationWorkbenchPanel.ParsedQueueLine p = SimulationWorkbenchPanel.parseQueueLine("UpdateBadge x3 resource=OfficeDoor user=Bob Li userId=U2");
        assertNull(p.error);
        assertEquals("UpdateBadge", p.rawAction);
        assertEquals("Bob Li", p.userValue);
        assertEquals("U2", p.userId);
        assertEquals("OfficeDoor", p.resourceName);
        assertEquals(3, p.count);
    }

    @Test
    void parsesResourceNameWithSpaces() {
        SimulationWorkbenchPanel.ParsedQueueLine p = SimulationWorkbenchPanel.parseQueueLine("Swipe user=Alice userId=U1 resource=Printer 23 x1");
        assertNull(p.error);
        assertEquals("Printer 23", p.resourceName);
        assertEquals(1, p.count);
    }

    @Test
    void defaultsCountToOne() {
        SimulationWorkbenchPanel.ParsedQueueLine p = SimulationWorkbenchPanel.parseQueueLine("StepTime(+1h)");
        assertNull(p.error);
        assertEquals(1, p.count);
    }

    @Test
    void reportsBlankLine() {
        SimulationWorkbenchPanel.ParsedQueueLine p = SimulationWorkbenchPanel.parseQueueLine("   ");
        assertNotNull(p.error);
    }
}
