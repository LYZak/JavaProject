// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.simulation.engine;

import java.util.concurrent.atomic.LongAdder;

public class SimulationMetrics {
    private final LongAdder submitted = new LongAdder();
    private final LongAdder granted = new LongAdder();
    private final LongAdder denied = new LongAdder();

    public void recordSubmitted() {
        submitted.increment();
    }

    public void recordGranted() {
        granted.increment();
    }

    public void recordDenied() {
        denied.increment();
    }

    public long getSubmitted() {
        return submitted.sum();
    }

    public long getGranted() {
        return granted.sum();
    }

    public long getDenied() {
        return denied.sum();
    }
}

