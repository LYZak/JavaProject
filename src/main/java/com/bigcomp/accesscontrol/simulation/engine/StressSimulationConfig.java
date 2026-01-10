package com.bigcomp.accesscontrol.simulation.engine;

public class StressSimulationConfig {
    public enum RateModel {
        FIXED,
        POISSON,
        STEP
    }

    private int threads = 4;
    private int requestsPerSecond = 200;
    private int durationSeconds = 10;
    private long seed = 1L;
    private RateModel rateModel = RateModel.FIXED;
    private int stepStartRps = 100;
    private int stepEndRps = 500;
    private int stepSeconds = 5;

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = Math.max(1, Math.min(128, threads));
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(int requestsPerSecond) {
        this.requestsPerSecond = Math.max(1, requestsPerSecond);
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = Math.max(1, durationSeconds);
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public RateModel getRateModel() {
        return rateModel;
    }

    public void setRateModel(RateModel rateModel) {
        this.rateModel = rateModel != null ? rateModel : RateModel.FIXED;
    }

    public int getStepStartRps() {
        return stepStartRps;
    }

    public void setStepStartRps(int stepStartRps) {
        this.stepStartRps = Math.max(1, stepStartRps);
    }

    public int getStepEndRps() {
        return stepEndRps;
    }

    public void setStepEndRps(int stepEndRps) {
        this.stepEndRps = Math.max(1, stepEndRps);
    }

    public int getStepSeconds() {
        return stepSeconds;
    }

    public void setStepSeconds(int stepSeconds) {
        this.stepSeconds = Math.max(1, stepSeconds);
    }
}
