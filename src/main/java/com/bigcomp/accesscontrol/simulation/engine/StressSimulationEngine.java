// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.simulation.engine;

import com.bigcomp.accesscontrol.core.Router;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.util.SystemClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StressSimulationEngine implements SimulationEngine {
    private final Router router;
    private final List<UserBadge> userBadges = new ArrayList<>();
    private final List<ResourceReader> resources = new ArrayList<>();
    private final SimulationMetrics metrics;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private StressSimulationConfig config = new StressSimulationConfig();
    private ExecutorService executor;

    public StressSimulationEngine(Router router, SimulationMetrics metrics) {
        this.router = router;
        this.metrics = metrics;
        this.router.addAccessEventListener((request, response) -> {
            if (response != null && response.isGranted()) {
                metrics.recordGranted();
            } else {
                metrics.recordDenied();
            }
        });
    }

    public void setConfig(StressSimulationConfig config) {
        if (config != null) {
            this.config = config;
        }
    }

    public void clearTargets() {
        userBadges.clear();
        resources.clear();
    }

    public void addUser(User user, Badge badge) {
        if (user == null || badge == null) {
            return;
        }
        userBadges.add(new UserBadge(user, badge));
    }

    public void addResource(Resource resource, BadgeReader reader) {
        if (resource == null) {
            return;
        }
        String badgeReaderId = reader != null ? reader.getId() : "STRESS";
        resources.add(new ResourceReader(resource.getId(), badgeReaderId));
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newFixedThreadPool(config.getThreads());
        CountDownLatch done = new CountDownLatch(config.getThreads());
        long seedBase = config.getSeed();

        int threads = config.getThreads();
        long endAtMillis = System.currentTimeMillis() + config.getDurationSeconds() * 1000L;
        StressSimulationConfig.RateModel model = config.getRateModel();
        double lambdaPerThread = Math.max(0.0001, config.getRequestsPerSecond() / (double) threads);

        for (int t = 0; t < threads; t++) {
            long seed = seedBase + t * 31L;
            executor.submit(() -> {
                try {
                    Random random = new Random(seed);
                    if (model == StressSimulationConfig.RateModel.POISSON) {
                        runPoisson(random, lambdaPerThread, endAtMillis);
                    } else if (model == StressSimulationConfig.RateModel.STEP) {
                        runStep(random, threads, endAtMillis);
                    } else {
                        runFixed(random, threads, endAtMillis);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                done.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                stop();
            }
        });
    }

    private void runFixed(Random random, int threads, long endAtMillis) {
        int totalRps = config.getRequestsPerSecond();
        int rpsPerThread = Math.max(1, totalRps / threads);
        while (running.get() && System.currentTimeMillis() < endAtMillis) {
            long tickStart = System.nanoTime();
            for (int i = 0; i < rpsPerThread; i++) {
                submitOne(random);
            }
            long elapsedNanos = System.nanoTime() - tickStart;
            long sleepNanos = TimeUnit.SECONDS.toNanos(1) - elapsedNanos;
            if (sleepNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void runPoisson(Random random, double lambdaPerThread, long endAtMillis) {
        while (running.get() && System.currentTimeMillis() < endAtMillis) {
            submitOne(random);
            double u = 1.0 - random.nextDouble();
            double delaySeconds = -Math.log(u) / lambdaPerThread;
            long sleepNanos = (long) (delaySeconds * 1_000_000_000L);
            if (sleepNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void runStep(Random random, int threads, long endAtMillis) {
        int start = config.getStepStartRps();
        int end = config.getStepEndRps();
        int stepSeconds = config.getStepSeconds();
        int duration = config.getDurationSeconds();
        int steps = Math.max(1, duration / stepSeconds);
        int inc = (end - start) / steps;
        if (inc == 0) {
            inc = end > start ? 1 : -1;
        }
        long startMillis = System.currentTimeMillis();

        while (running.get() && System.currentTimeMillis() < endAtMillis) {
            long nowMillis = System.currentTimeMillis();
            int elapsedSeconds = (int) ((nowMillis - startMillis) / 1000L);
            int stepIndex = elapsedSeconds / stepSeconds;
            int current = start + stepIndex * inc;
            int min = Math.min(start, end);
            int max = Math.max(start, end);
            if (current < min) current = min;
            if (current > max) current = max;

            int rpsPerThread = Math.max(1, current / threads);
            long tickStart = System.nanoTime();
            for (int i = 0; i < rpsPerThread; i++) {
                submitOne(random);
            }
            long elapsedNanos = System.nanoTime() - tickStart;
            long sleepNanos = TimeUnit.SECONDS.toNanos(1) - elapsedNanos;
            if (sleepNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void submitOne(Random random) {
        if (userBadges.isEmpty() || resources.isEmpty()) {
            return;
        }
        UserBadge ub = userBadges.get(random.nextInt(userBadges.size()));
        ResourceReader rr = resources.get(random.nextInt(resources.size()));

        LocalDateTime now = SystemClock.now();
        AccessRequest request = new AccessRequest(ub.badge.getCode(), rr.badgeReaderId, rr.resourceId, now);
        metrics.recordSubmitted();
        router.submitAccessRequest(request);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private static final class UserBadge {
        private final Badge badge;

        private UserBadge(User user, Badge badge) {
            this.badge = badge;
        }
    }

    private static final class ResourceReader {
        private final String resourceId;
        private final String badgeReaderId;

        private ResourceReader(String resourceId, String badgeReaderId) {
            this.resourceId = resourceId;
            this.badgeReaderId = badgeReaderId;
        }
    }
}
