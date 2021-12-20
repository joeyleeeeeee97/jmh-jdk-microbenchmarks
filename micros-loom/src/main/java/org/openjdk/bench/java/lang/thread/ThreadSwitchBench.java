package org.openjdk.bench.java.lang.thread;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ThreadSwitchBench {

    @Param({"0", "100", "400", "800"})
    private int stackDepth;

    @Param({"1", "50", "200"})
    private int yieldN;

    @Param({"virtual", "platform"})
    private String threadMode;

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
        int depth = stackDepth;
        builder.start(new Runnable() {
            @Override
            public void run() {
                int n = yieldN;
                while (n-- > 0)
                    stackDepthHelper(depth);
            }
        }).join();
    }

    @Benchmark
    public void tailSwitchBench() throws InterruptedException {
        int depth = stackDepth;
        builder.start(new Runnable() {
            @Override
            public void run() {
                tailStackDepthHelper(depth);
            }
        }).join();
    }

    private void stackDepthHelper(int depth) {
        if (depth == 0) {
            Thread.yield(); // switch
        } else {
            stackDepthHelper(depth - 1);
        }
    }
    private void tailStackDepthHelper(int depth) {
        if (depth == 0) {
            int x = yieldN;
            while (x-- > 0)
                Thread.yield(); // switch
        } else {
            stackDepthHelper(depth - 1);
        }
    }

}
