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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.impl.ProbabilisticPriorityQueue.Entity;

/**
 *
 * @author Mikhail Titov
 */
public class ProbabilisticPriorityQueue<T extends Entity> implements BlockingQueue<T> {
    private final ConcurrentMap<Integer, Queue<T>> queues = new ConcurrentHashMap<Integer, Queue<T>>();
    private final int maxSize;
    private final AtomicInteger size = new AtomicInteger();
    private final ReentrantLock entityFreeLock = new ReentrantLock();
    private final ReentrantLock entityAddLock = new ReentrantLock();
    private final Condition entityFreedCond = entityFreeLock.newCondition();
    private final Condition entityAddedCond = entityAddLock.newCondition();
    private final Random random = new Random();

    public ProbabilisticPriorityQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean add(T e) {
        if (!tryIncSize()) throw new IllegalStateException("Exceeded the maximum queue capacity");
        else {
            addEntity(e);
            return true;
        }
    }
    
    private boolean tryIncSize() {
        if (size.incrementAndGet() <= maxSize) return true;
        else {
            size.decrementAndGet();
            return false;
        }
    }
    
    private void addEntity(T elem) {
        Queue<T> queue = queues.get(elem.getPriority());
        if (queue==null) {
            Queue<T> newQueue = new ConcurrentLinkedQueue<T>();
            queue = queues.putIfAbsent(elem.getPriority(), newQueue);
            if (queue==null)
                queue = newQueue;
        }
        queue.offer(elem);
        if (entityAddLock.hasWaiters(entityAddedCond))
            if (entityAddLock.tryLock()) {
                try {
                    entityAddedCond.signal();
                } finally {
                    entityFreeLock.unlock();
                }
            }
    }
    
    private T getEnity() {
        ArrayList<Integer> priorites = new ArrayList(queues.keySet());
        if (priorites.isEmpty()) 
            return null;
        ArrayList<Double> koefs = new ArrayList<Double>(priorites.size());
        double sum = 0;
        for (Integer pr: priorites) 
            sum += pr;
        double sum2 = 0.;
        for (Integer pr: priorites) {
            double v = sum-pr;
            sum2 += v;
            koefs.add(v);
        }
        for (int i=0; i<koefs.size(); ++i) 
            koefs.set(i, koefs.get(i)/sum2 + (i==0? 0.0 : koefs.get(i-1)));
        double koef = random.nextDouble();
        
    }

    public boolean offer(T e) {
        if (!tryIncSize()) return false;
        else {
            addEntity(e);
            return true;
        }
    }

    public void put(T e) throws InterruptedException {
        if (!offer(e)) {
            entityFreeLock.lock();
            try {
                while (!offer(e)) 
                    entityFreedCond.await();
            } finally {
                entityFreeLock.unlock();
            }
        }
    }

    public boolean offer(T e, long l, TimeUnit tu) throws InterruptedException {
        if (offer(e)) return true;
        else {
            long deadline = System.nanoTime() + tu.toNanos(l);
            if (!entityFreeLock.tryLock(l, tu)) return false;
            else {
                try {
                    long i;
                    boolean o = false;
                    while (!(o = offer(e)) && (i = deadline - System.nanoTime())>0)
                        entityFreedCond.await(i, TimeUnit.NANOSECONDS);
                    return o;
                } finally {
                    entityFreeLock.unlock();
                }
            }
        }
    }

    public T take() throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public T poll(long l, TimeUnit tu) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int remainingCapacity() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int drainTo(Collection<? super T> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int drainTo(Collection<? super T> clctn, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public T remove() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public T poll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public T element() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public T peek() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int size() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public <T> T[] toArray(T[] ts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean containsAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean addAll(Collection<? extends T> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean removeAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean retainAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    public static interface Entity {
        public int getPriority();
    }
}
