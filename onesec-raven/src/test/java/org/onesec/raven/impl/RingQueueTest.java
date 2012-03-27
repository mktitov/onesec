/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class RingQueueTest extends Assert {
    
    private final AtomicInteger counter = new AtomicInteger();
    private final Lock lock = new ReentrantLock();
    private  int counter3;
    
    @Test
    public void test() {
        RingQueue queue = new RingQueue(4);
        assertFalse(queue.hasElement());
        assertNull(queue.peek());
        assertNull(queue.pop());
        
        queue.push("1");
        assertTrue(queue.hasElement());
        assertEquals("1", queue.peek());
        assertTrue(queue.hasElement());
        assertEquals("1", queue.pop());
        assertFalse(queue.hasElement());
    }
    
    @Test
    public void overflowTest() {
        RingQueue queue = new RingQueue(1);
        assertTrue(queue.push("test"));
        assertFalse(queue.push("test2"));
        assertEquals("test", queue.pop());
        assertTrue(queue.push("test2"));
    }
    
//    @Test
    public void syncTest() {
        long cycles = 1000000000; 
        counter3 = 100;
        long startTs = System.currentTimeMillis();
        counter.set(100);
        int counter2 = 0;
        long p = 0;
        for (int i=0; i<cycles; ++i) {
            p = i+1;
//            synchronized(this) {
                counter2+=p;
//                counter2 += counter.get();
//                counter2 += counter3;
//            }
        }
        long diff = System.currentTimeMillis()-startTs;
        System.out.println("!! time: "+diff);
        System.out.println("counter2: "+counter2);
        startTs = System.currentTimeMillis();
        counter.set(100);
        counter2 = 0;
        for (int i=0; i<cycles; ++i) {
            long p1 = i+1;
//            lock.lock();
//            try {
                counter2+=p1;
//            } finally {
//                lock.unlock();
//            }
//            counter.incrementAndGet();
//            counter2 += counter.get();
//            counter2 += counter3;
//            counter+=i%8;
        }
        diff = System.currentTimeMillis()-startTs;
        System.out.println("!! time: "+diff);
        System.out.println("counter2: "+counter2);
    }
    
}
