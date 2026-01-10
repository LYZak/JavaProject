package com.bigcomp.accesscontrol.simulation;

import com.bigcomp.accesscontrol.core.Router;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.simulation.engine.SimulationMetrics;
import com.bigcomp.accesscontrol.simulation.engine.StressSimulationConfig;
import com.bigcomp.accesscontrol.simulation.engine.StressSimulationEngine;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StressSimulationEngineTest {

    @Test
    void submitsRequestsThroughRouter() throws Exception {
        Router router = mock(Router.class);
        doNothing().when(router).addAccessEventListener(any());

        AtomicInteger submitted = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            submitted.incrementAndGet();
            if (submitted.get() >= 5) {
                latch.countDown();
            }
            return null;
        }).when(router).submitAccessRequest(any(AccessRequest.class));

        SimulationMetrics metrics = new SimulationMetrics();
        StressSimulationEngine engine = new StressSimulationEngine(router, metrics);
        StressSimulationConfig cfg = new StressSimulationConfig();
        cfg.setThreads(1);
        cfg.setRequestsPerSecond(20);
        cfg.setDurationSeconds(1);
        engine.setConfig(cfg);

        engine.addUser(new com.bigcomp.accesscontrol.model.User("U1", com.bigcomp.accesscontrol.model.User.Gender.MALE, "A", "B", com.bigcomp.accesscontrol.model.User.UserType.EMPLOYEE),
            new com.bigcomp.accesscontrol.model.Badge("CODE1"));
        engine.addResource(new com.bigcomp.accesscontrol.model.Resource("R1", "Door", com.bigcomp.accesscontrol.model.Resource.ResourceType.DOOR, "L", "B", "1F"),
            null);

        engine.start();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(submitted.get() >= 5);
    }
}

