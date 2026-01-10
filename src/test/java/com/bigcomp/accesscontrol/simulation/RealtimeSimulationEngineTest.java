package com.bigcomp.accesscontrol.simulation;

import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.simulation.engine.RealtimeSimulationEngine;
import com.bigcomp.accesscontrol.simulation.engine.SimulationMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RealtimeSimulationEngineTest {

    @Test
    void recordsSubmittedWhenTicking() throws Exception {
        BadgeReader reader = spy(new BadgeReader("BR1", "R1"));
        doReturn(null).when(reader).swipeBadge(any());

        Resource res = new Resource("R1", "Door", Resource.ResourceType.DOOR, "Site", "B", "1F");
        SimulationMetrics metrics = new SimulationMetrics();
        RealtimeSimulationEngine engine = new RealtimeSimulationEngine(List.of(reader), Map.of("R1", res), 1L, metrics);
        engine.setIntervalSeconds(1);
        engine.setConsistentBehavior(false);
        engine.addSimulatedUser(new User("U1", User.Gender.MALE, "A", "B", User.UserType.EMPLOYEE), new Badge("CODE1"));

        engine.start();
        Thread.sleep(1200);
        engine.stop();

        assertTrue(metrics.getSubmitted() >= 1);
    }
}

