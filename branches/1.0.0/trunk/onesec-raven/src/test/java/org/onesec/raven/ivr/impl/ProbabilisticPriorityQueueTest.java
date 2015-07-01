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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class ProbabilisticPriorityQueueTest extends Assert {
    
    @Test
    public void addTest() throws InterruptedException {
        ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        for (int i=1; i<100; ++i) {
            Ent e1 = new Ent(1);
            queue.add(e1);
            assertEquals(1, queue.size());
            assertSame(e1, queue.take());
            assertEquals(0, queue.size());
        }
    }
    
    @Test(expected = IllegalStateException.class)
    public void addTest2() throws Exception {
        ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        queue.add(new Ent(1));
        queue.add(new Ent(1));
    }
    
    @Test
    public void putTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        for (int i=0; i<100; ++i) {
            queue.add(new Ent(1));
            long ts = System.currentTimeMillis();
            executeTake(queue);
            Ent ent = new Ent(1);
            queue.put(ent);
            assertTrue(ts+10<=System.currentTimeMillis());
            assertSame(ent, queue.take());
        }
    }
    
    @Test
    public void offerTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        Ent ent = new Ent(1);
        assertTrue(queue.offer(ent));
        assertFalse(queue.offer(new Ent(2)));
        assertEquals(1, queue.size());
        assertSame(ent, queue.take());
    }
    
    @Test
    public void offerWithTimeoutTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        Ent ent = new Ent(1);
        long ts = System.currentTimeMillis();
        assertTrue(queue.offer(ent, 100, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis()-ts<=2);
        
        ent = new Ent(2);
        ts = System.currentTimeMillis();
        executeTake(queue);
        assertTrue(queue.offer(ent, 12, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis()-ts>=10);
        assertSame(ent, queue.take());
        
        queue.offer(ent);
        ts = System.currentTimeMillis();
        assertFalse(queue.offer(ent, 5, TimeUnit.MILLISECONDS));
        long curTime = System.currentTimeMillis();
        assertTrue(curTime-ts>=5);
        assertTrue(curTime-ts<=6);
        
        assertFalse(queue.offer(new Ent(2)));
        assertEquals(1, queue.size());
        assertSame(ent, queue.take());
    }
    
    @Test
    public void pollTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        Ent ent = new Ent(1);
        
        assertTrue(queue.offer(ent));
        long ts = System.currentTimeMillis();
        assertSame(ent, queue.poll(10, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis()-ts<=2);
        
        executeOffer(queue, ent);
        ts = System.currentTimeMillis();
        assertSame(ent, queue.poll(12, TimeUnit.MILLISECONDS));
        assertTrue(System.currentTimeMillis()-ts>=10);
        
        assertNull(queue.poll(10, TimeUnit.MILLISECONDS));
    }
    
    @Test
    public void remainingCapacityTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        assertEquals(1, queue.remainingCapacity());
        assertTrue(queue.offer(new Ent(1)));
        assertEquals(0, queue.remainingCapacity());
    }
    
    @Test
    public void removeEntityTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        Ent ent = new Ent(1);
        assertFalse(queue.remove(ent));
        assertTrue(queue.offer(ent));
        assertTrue(queue.remove(ent));
        assertEquals(0, queue.size());
        assertNull(queue.poll());
    }
    
    @Test(expected = NoSuchElementException.class)
    public void removeHeadTest1() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        queue.remove();
    }

    @Test
    public void removeHeadTest2() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(1);
        Ent ent = new Ent(1);
        assertTrue(queue.offer(ent));
        assertSame(ent, queue.remove());
    }

    @Test
    public void containsTest() throws Exception {
        final ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(2);
        Ent ent1 = new Ent(1);
        Ent ent2 = new Ent(2);
        assertFalse(queue.contains(ent2));
        assertFalse(queue.contains(ent1));
        
        assertTrue(queue.offer(ent1));
        assertTrue(queue.contains(ent1));
        assertFalse(queue.contains(ent2));
        
        assertTrue(queue.offer(ent2));
        assertTrue(queue.contains(ent1));
        assertTrue(queue.contains(ent2));
    }
    
    //20 80 -> 
    @Test
    public void orderTest() {
        int c20=0; int c80=0; 
        final int testsCount=100;
        int counter = testsCount;
        while (counter-- > 0) {
            ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(200);
            for (int i=0; i<100; ++i) {
                queue.add(new Ent(20));
                queue.add(new Ent(80));
            }        
            assertEquals(200, queue.size());
            int i = 0;
            while (i++ < 100) {
                Ent e = queue.poll();
                if (e.getPriority()==20) ++c20;
                else ++c80;
            }
        }
        c20 /= testsCount;
        c80 /= testsCount;
        System.out.println("c20: "+c20);
        System.out.println("c80: "+c80);
        assertTrue(c20>=75 && c20<=85);
        assertTrue(c80>=15 && c80<=25);
    }

    //20 80 -> 
    @Test
    public void orderTest2() {
        int c20=0; int c80=0; 
        final int testsCount=100;
        int counter = testsCount;
        while (counter-- > 0) {
            ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(300);
            for (int i=0; i<200; ++i) {
                queue.add(new Ent(20));
                if (i < 20)
                    queue.add(new Ent(80));
            }        
//            assertEquals(250, queue.size());
            int i = 0;
            while (i++ < 120) {
                Ent e = queue.poll();
                if (e.getPriority()==20) ++c20;
                else ++c80;
            }
        }
        c20 /= testsCount;
        c80 /= testsCount;
        System.out.println("c20: "+c20);
        System.out.println("c80: "+c80);
        assertTrue(c20>=98 && c20<=102);
        assertTrue(c80>=18 && c80<=22);
    }

    @Test
    public void iteratorGetAllValuesTest() {
        ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(200);
        for (int i=0; i<100; ++i) {
            queue.add(new Ent(20));
            queue.add(new Ent(80));
        }        
        int count = 0;
        for (Iterator it=queue.iterator(); it.hasNext();) {
            assertNotNull(it.next());
            count++;
        }
        assertEquals(200, count);
        assertEquals(200, queue.size());
    }
    
    @Test
    public void iteratorValuesOrderTest() {
        int c20=0; int c80=0; 
        final int testsCount=100;
        int counter = testsCount;
        while (counter-- > 0) {
            ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(200);
            for (int i=0; i<100; ++i) {
                queue.add(new Ent(20));
                queue.add(new Ent(80));
            }        
            int count = 0;
            for (Iterator<Ent> it=queue.iterator(); it.hasNext() && count<100; ++count) {
                if (it.next().getPriority()==20) ++c20;
                else ++c80;
            }
        }
        c20 /= testsCount;
        c80 /= testsCount;
        System.out.println("c20: "+c20);
        System.out.println("c80: "+c80);
        assertTrue(c20>=75 && c20<=85);
        assertTrue(c80>=15 && c80<=25);
    }
    
    @Test
    public void iteratorRemoveTest() {
        ProbabilisticPriorityQueue<Ent> queue = new ProbabilisticPriorityQueue<Ent>(2);
        Ent e1 = new Ent(1);
        Ent e2 = new Ent(1);
        queue.add(e1);
        queue.add(e2);
        Iterator<Ent> it = queue.iterator();
        Ent re1 = it.next();
        assertNotNull(re1);
        it.remove();
        assertEquals(1, queue.size());
        Ent re2 = it.next();
        assertSame(re2, queue.poll());
        assertNotSame(re1, re2);
        assertFalse(queue.contains(re1));
    }
    
    private void executeTake(final ProbabilisticPriorityQueue<Ent> queue) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10);
                    assertNotNull(queue.take());
                } catch (InterruptedException ex) {
                    fail();
                }
            }
        }).start();
    }
    
    private void executeOffer(final ProbabilisticPriorityQueue<Ent> queue, final Ent ent) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10);
                    assertTrue(queue.offer(ent));
                } catch (InterruptedException ex) {
                    fail();
                }
            }
        }).start();
    }
    
    private class Ent implements ProbabilisticPriorityQueue.Entity {
        private final int priority;

        public Ent(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }
}