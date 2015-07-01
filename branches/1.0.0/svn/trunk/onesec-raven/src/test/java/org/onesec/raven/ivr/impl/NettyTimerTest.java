/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.impl;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class NettyTimerTest extends Assert {
    private final static long EXECUTIONS_COUNT = 1000;
    private final static long TASKS_COUNT = 2000;
    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    private final Runtime runtime = Runtime.getRuntime();
    
    private final AtomicInteger finishedTasksCount = new AtomicInteger();
    private final AtomicLong averageDelta = new AtomicLong();
    private long threadsCount = 0;
    
    @Test
    public void timerTest() throws Exception {
        threadsCount = threads.getThreadCount();
        long heapUsage = getUsedMemory();
        Timer timer = createTimer();
        for (int i=0; i<TASKS_COUNT; i++)
            new Task(timer).start();
        long cpuUsage = getCpuUsage();
        do {
            Thread.sleep(100);
        } while (finishedTasksCount.get()<TASKS_COUNT);
        final long avgDelta = averageDelta.get()/TASKS_COUNT;
        System.out.println("\nAverage DELTA (ns): "+avgDelta);
        System.out.println("Average DELTA (ms): "+ TimeUnit.NANOSECONDS.toMillis(avgDelta));
        System.out.println("\nThreads used: "+(threads.getThreadCount()-threadsCount));
        System.out.println("Memory used: "+(getUsedMemory()-heapUsage));
    }
    
    @Test
    public void threadsTest() {
        
    }
    
    private Timer createTimer() {
        return new HashedWheelTimer(1, TimeUnit.MILLISECONDS, 20);
    }
    
    private long getCpuUsage() {
        long cpuTime = 0;
        for (long id: threads.getAllThreadIds())
            cpuTime += threads.getThreadCpuTime(id);
        return cpuTime;
    }
    
    private long getUsedMemory() {
        return runtime.totalMemory()-runtime.freeMemory();
    }
    
    private class Task implements TimerTask {
        private final Timer timer;
        private long delta = 0;
        private long nextExecutionTime = 0;
        private long executionsCount = 0;

        public Task(Timer timer) {
            this.timer = timer;
        }

        public void run(Timeout timeout) throws Exception {
            if (nextExecutionTime!=0)
                delta += Math.abs(System.nanoTime()-nextExecutionTime);
            if (++executionsCount < EXECUTIONS_COUNT) start();
            else {
                finishedTasksCount.incrementAndGet();
                averageDelta.addAndGet(delta / EXECUTIONS_COUNT);
            }
        }
        
        public void start() {
            nextExecutionTime = System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(20);
            timer.newTimeout(this, 20, TimeUnit.MILLISECONDS);
        }
    }
    
    private class ThreadTask implements Runnable {
        public void run() {
            throw new UnsupportedOperationException("Not supported yet."); 
        }
    }
}
