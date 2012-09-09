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
package org.onesec.raven.net.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.net.ByteBufferHolder;
import org.onesec.raven.net.ByteBufferPool;

/**
 *
 * @author Mikhail Titov
 */
public class ByteBufferPoolImpl implements ByteBufferPool {
    private final static Integer BUFFERS_LIST_INITIAL_CAPACITY = 64;
    
    private final Map<Integer, List<ByteBufferHolderImpl>> pool = new HashMap<Integer, List<ByteBufferHolderImpl>>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile int buffersCount = 0;

    public ByteBufferHolder getBuffer(int bufferSize) {
        List<ByteBufferHolderImpl> bufferList = null;
        lock.readLock().lock();
        try {
            bufferList = pool.get(bufferSize);
            if (bufferList!=null) {
                ByteBufferHolder holder = getFromList(bufferList, bufferSize);
                if (holder!=null)
                    return holder;
            }
        } finally {
            lock.readLock().unlock();
        }
        return bufferList==null? createBuffersListAndByteBuffer(bufferSize) 
                : createByteBufferAndAddToList(bufferList, bufferSize);
    }

    public int getBuffersCount() {
        return buffersCount;
    }
    
    private ByteBufferHolder getFromList(List<ByteBufferHolderImpl> bufferList, int bufferSize) {
        for (ByteBufferHolderImpl holder: bufferList)
            if (holder.use())
                return holder;
        return null;
    }
    
    private ByteBufferHolder createBuffersListAndByteBuffer(int bufferSize) {
        lock.writeLock().lock();
        try {
            List<ByteBufferHolderImpl> list = pool.get(bufferSize);
            if (list==null) {
                list = new ArrayList<ByteBufferHolderImpl>(BUFFERS_LIST_INITIAL_CAPACITY);
                pool.put(bufferSize, list);
            }
            ByteBufferHolderImpl bufferHolder = new ByteBufferHolderImpl(bufferSize);
            bufferHolder.use();
            list.add(bufferHolder);
            buffersCount++;
            return bufferHolder;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private ByteBufferHolder createByteBufferAndAddToList(List<ByteBufferHolderImpl> bufferList, int bufferSize) {
        lock.writeLock().lock();
        try {
            ByteBufferHolderImpl bufferHolder = new ByteBufferHolderImpl(bufferSize);
            bufferHolder.use();
            bufferList.add(bufferHolder);
            buffersCount++;
            return bufferHolder;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
