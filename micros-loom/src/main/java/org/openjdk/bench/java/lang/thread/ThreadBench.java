/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.lang.thread;

import jdk.internal.access.SharedSecrets;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ThreadBench {
    @Param({"virtual", "platform", "platformOptimized"})
    private String threadMode;

    private ThreadFactory threadFactory;

    private static Runnable empty = new Empty();

    private MethodHandle threadInitHandle;


    @Setup(Level.Trial)
    public void beforeRun() throws Exception {
        if ("virtual".equals(threadMode)) {
            threadFactory = Thread.ofVirtual().factory();
        } else if ("platform".equals(threadMode)) {
            threadFactory = Thread.ofPlatform().factory();
        } else {
            AccessControlContext accessControlContext = new AccessControlContext(new ProtectionDomain[0]);
            PrivilegedExceptionAction<MethodHandles.Lookup> pa = () ->
                    MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup());
            @SuppressWarnings("removal")
            MethodHandles.Lookup l = AccessController.doPrivileged(pa);
            MethodType methodType = MethodType.methodType(void.class, ThreadGroup.class, String.class, int.class, Runnable.class, long.class, AccessControlContext.class);
            threadInitHandle = l.findConstructor(Thread.class, methodType);

            threadFactory = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    try {
                        return (Thread) threadInitHandle.invoke(null, "", 0, r, 0, accessControlContext);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw new RuntimeException("!");
                    }
                }
            };
        }
    }

    @Benchmark
    public void threadCreate() {
        threadFactory.newThread(empty);
    }

    @Benchmark
    public void threadStart() throws Exception {
        Thread t = threadFactory.newThread(empty);
        t.start();
        t.join();
    }

    static final class Empty implements Runnable {

        @Override
        public void run() {
            // do nothing
        }

    }
}