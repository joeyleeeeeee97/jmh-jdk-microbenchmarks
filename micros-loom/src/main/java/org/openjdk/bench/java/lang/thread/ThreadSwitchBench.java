package org.openjdk.bench.java.lang.thread;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ThreadSwitchBench {
    @Param({"virtual", "platform"})
    private String threadMode;

    @Param({"0", "1", "64", "1024", "2048"})
    private int stackDepth;

    // An approximate level of context switches
    @Param({"0", "1", "64", "1048", "10000", "80000"})
    private int yieldN;

    private Thread.Builder builder;

    @Setup(Level.Trial)
    public void beforeRun() throws Exception {
        if ("virtual".equals(threadMode)) {
            builder = Thread.ofVirtual();
        } else if ("platform".equals(threadMode)) {
            builder = Thread.ofPlatform();
        }
    }

    @Benchmark
    public void switchBench() throws InterruptedException {
        AtomicBoolean yieldFinished = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(2);

        builder.start(new Runnable() {
            @Override
            public void run() {
                while (yieldN > 0) {
                    yieldN--;
                    Thread.yield();
                }
                yieldFinished.set(true);
                done.countDown();
            }
        });

        builder.start(new Runnable() {
            @Override
            public void run() {
                while (!yieldFinished.get()) {
                    stackDepthHelper(stackDepth);
                }
                done.countDown();
            }
        });
        done.await();
    }

    @Benchmark
    public int switchBenchWithReturnVal() throws InterruptedException {
        AtomicBoolean yieldFinished = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(2);

        builder.start(new Runnable() {
            @Override
            public void run() {
                while (yieldN > 0) {
                    yieldN--;
                    Thread.yield();
                }
                yieldFinished.set(true);
                done.countDown();
            }
        });
        int[] cnt = new int[1];

        builder.start(new Runnable() {
            @Override
            public void run() {
                while (!yieldFinished.get()) {
                    cnt[0] += stackDepthHelperWithReturnValue(stackDepth);
                }
                done.countDown();
            }
        });
        done.await();
        return cnt[0];
    }


    private void stackDepthHelper(int depth) {
        if (depth <= 0) {
            Thread.yield(); // switch
        } else {
            stackDepthHelper(depth - 1);
        }
    }

    private int stackDepthHelperWithReturnValue(int depth) {
        if (depth <= 0) {
            Thread.yield(); // switch
            return 0;
        } else {
            return stackDepthHelperWithReturnValue(depth - 1) + 1;
        }
    }
}
