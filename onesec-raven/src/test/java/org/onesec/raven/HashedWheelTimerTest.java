/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class HashedWheelTimerTest {
    @Test
    public void test() throws InterruptedException {
        HashedWheelTimer timer = new HashedWheelTimer(1000, TimeUnit.MICROSECONDS);
        final long delay = 100l;
        final int count = 100;
        final CountDownLatch counter = new CountDownLatch(count);
        final AtomicLong totalDispertion = new AtomicLong();
        for (int i=0; i<count; ++i) {
            timer.newTimeout(new TimerTask() {
                private final long expectedExecTime = System.currentTimeMillis()+delay;
                @Override public void run(Timeout tmt) throws Exception {
                    System.out.println("DISPERTION: "+(System.currentTimeMillis()-expectedExecTime));
                    totalDispertion.addAndGet((System.currentTimeMillis()-expectedExecTime));
                    counter.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);
            Thread.sleep(delay);
        }
        counter.await();
        System.out.println("TOTAL DISPERTION: "+totalDispertion);
    }
}
