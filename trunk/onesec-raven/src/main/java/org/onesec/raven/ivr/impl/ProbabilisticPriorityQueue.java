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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    private final AtomicInteger queuesCount = new AtomicInteger();
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
                        entityFreedCond.awaitNanos(i);
                    return o;
                } finally {
                    entityFreeLock.unlock();
                }
            }
        }
    }

    public T take() throws InterruptedException {
        T value = getEnity();
        if (value != null) return value;
        else {
            entityAddLock.lock();
            try {
                while ( (value=getEnity())==null )
                    entityAddedCond.await();
                return value;
            } finally {
                entityAddLock.unlock();
            }
        }
    }

    public T poll(long l, TimeUnit tu) throws InterruptedException {
        T value = getEnity();
        if (value != null) return value;
        else {
            long deadline = System.nanoTime() + tu.toNanos(l);
            if (!entityAddLock.tryLock(l, tu)) return null;
            else {
                try {
                    long i;
                    while ( (value=getEnity())==null && (i = deadline-System.nanoTime())>0)
                        entityAddedCond.awaitNanos(i);
                    return value;
                } finally {
                    entityAddLock.unlock();
                }
            }
        }
    }

    public int remainingCapacity() {
        final int res = maxSize - size.get();
        return res >= 0? res : 0;
    }

    public boolean remove(Object o) {
        T entity = (T) o;
        Queue<T> queue = queues.get(entity.getPriority());
        if (queue==null || !queue.remove(o)) return false;
        else {
            size.decrementAndGet();
            informWaiters(entityFreeLock, entityFreedCond);
            return true;
        }
    }

    public boolean contains(Object o) {
        T entity = (T) o;
        Queue<T> queue = queues.get(entity.getPriority());
        return queue==null? false : queue.contains(o);
    }

    public int drainTo(Collection<? super T> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public int drainTo(Collection<? super T> clctn, int i) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public T remove() {
        T entity = getEnity();
        if (entity == null) throw new NoSuchElementException();
        else return entity;
    }

    public T poll() {
        return getEnity();
    }

    public T element() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public T peek() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public int size() {
        return size.get();
    }

    public boolean isEmpty() {
        return size.get()==0;
    }

    public Iterator<T> iterator() {
        return new QueuesIterator();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public <T> T[] toArray(T[] ts) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public boolean containsAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public boolean addAll(Collection<? extends T> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public boolean removeAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public boolean retainAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); 
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
            if (queue==null) {
                queuesCount.incrementAndGet();
                queue = newQueue;
            }
        }
        queue.offer(elem);
        informWaiters(entityAddLock, entityAddedCond);
    }
    
    /**
     * Returns the list of the priorities rates in percent.
     */
    public List<PriorityRate> getPriorityRatesInPercent() {
        List<Integer> priorities = getPriorities();
        if (priorities.isEmpty()) return Collections.EMPTY_LIST;
        else {
            Collections.sort(priorities);
            List<PriorityRate> res = new ArrayList<PriorityRate>(priorities.size());
            double[] rates = getPrioritiesRatios(priorities);
            for (int i=0; i<rates.length; i++)
                res.add(new PriorityRate(priorities.get(i), (int) (rates[i]*100)));
            return res;
        }
    }
    
    private T getEnity() {
        T entity = null;
        List<Integer> priorities = getPriorities();
        while (!priorities.isEmpty() && entity==null) {
            double[] rates = getPrioritiesRatios(priorities);
            int ind = getNextPriorityInd(rates, random);
            entity = getEntityFromQueue(priorities.get(ind));
            if (entity==null) 
                priorities.remove(ind);
        } 
        return entity;
    }
    
    private List<Integer> getPriorities() {
        final int _queuesCount = queuesCount.get();
        if (_queuesCount <= 0)
            return Collections.EMPTY_LIST;
        List<Integer> priorities = new ArrayList<Integer>(_queuesCount);
        for (Map.Entry<Integer, Queue<T>> entry: queues.entrySet())
            if (!entry.getValue().isEmpty())
                priorities.add(entry.getKey());
        return priorities.isEmpty()? Collections.EMPTY_LIST : priorities;
    }
    
    private T getEntityFromQueue(int priority) {
        Queue<T> queue = queues.get(priority);
        if (queue==null) return null;
        else {
            T entity = queue.poll();
            if (entity==null) return null;
            else {
                size.decrementAndGet();
                informWaiters(entityFreeLock, entityFreedCond);
                return entity;
            }
        }
    }
    
    private void informWaiters(ReentrantLock lock, Condition cond) {
        try {
            if (lock.tryLock(1, TimeUnit.MILLISECONDS))
                try {
                    cond.signal();
                } finally {
                    lock.unlock();
                }
        } catch (InterruptedException ex) {
        }
    }

    private int getNextPriorityInd(double[] rates, Random random) {
        if (rates.length==1) return 0;
        else {
            double k = random.nextDouble();
            for (int i=0; i<rates.length; i++)
                if (k <= rates[i]) 
                    return i;
            throw new IllegalStateException();
        }
    }

    private double[] getPrioritiesRatios(List<Integer> priorities) {
        double[] rates = new double[priorities.size()];
        if (priorities.size()==1) rates = new double[]{1.0};
        else {
            double sum = 0;
            //calculating coefficient
            for (int i=0; i<rates.length; i++) {
                double v = 1/(double)priorities.get(i);
                sum += v;
                rates[i] = v;
            }
            double k = 1/sum;
            //transforming weights to range from 0 to 1
            for (int i=0; i<rates.length; i++) 
                rates[i] = rates[i] * k + (i==0? 0.0 : rates[i-1]);
            
//            double sum = 0;
//            //calculating total wieght
//            for (Integer pr: priorities) 
//                sum += pr;
//            double sum2 = 0.;
//            //inverting weights
//            for (int i=0; i<priorities.size(); ++i) {
//                double v = sum-priorities.get(i);
//                sum2 += v;
//                rates[i] = v;
//            }
//            //transforming weights to range from 0 to 1
//            for (int i=0; i<rates.length; ++i) 
//                rates[i] = rates[i]/sum2 + (i==0? 0.0 : rates[i-1]);
        }
        return rates;
    }
    
    
    public static interface Entity {
        public int getPriority();
    }
    
    private class QueuesIterator implements Iterator<T> {
        private final Random random = new Random();
        private final List<Iterator<T>> iterators;
        private final List<Integer> priorities;
        private double[] rates;
        private Iterator<T> lastIterator;

        public QueuesIterator() {
            priorities = getPriorities();
            if (priorities.isEmpty()) iterators = Collections.EMPTY_LIST;
            else {
                iterators = new ArrayList<Iterator<T>>(priorities.size());
                for (Integer priority: priorities) {
                    Queue<T> queue = queues.get(priority);
                    iterators.add(queue==null? null : queue.iterator());
                }
                rates = getPrioritiesRatios(priorities);
            }
        }

        public boolean hasNext() {
            for (Iterator<T> it: iterators)
                if (it.hasNext()) return true;
            return false;
        }

        public T next() {
            T entity = null;
            do {
                if (priorities.isEmpty())
                    throw new NoSuchElementException();
                int ind = getNextPriorityInd(rates, random);
                Iterator<T> it = iterators.get(ind);
                if (it.hasNext()) {
                    entity = it.next();
                    lastIterator = it;
                } else {
                    priorities.remove(ind);
                    iterators.remove(ind);
                    rates = getPrioritiesRatios(priorities);
                }
            } while (entity==null);
            return entity;
        }

        public void remove() {
            if (lastIterator==null)
                throw new IllegalStateException();
            lastIterator.remove();
            size.decrementAndGet();
            informWaiters(entityFreeLock, entityFreedCond);
            lastIterator = null;
        }
    }
    
    public class PriorityRate {
        private final int priority;
        private final int rateInPercent;

        public PriorityRate(int priority, int rateInPercent) {
            this.priority = priority;
            this.rateInPercent = rateInPercent;
        }

        public int getPriority() {
            return priority;
        }

        public int getRateInPercent() {
            return rateInPercent;
        }
    }
}
