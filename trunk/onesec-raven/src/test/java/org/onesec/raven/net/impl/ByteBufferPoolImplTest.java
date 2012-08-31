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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.net.ByteBufferHolder;

/**
 *
 * @author Mikhail Titov
 */
public class ByteBufferPoolImplTest {
    
    @Test
    public void test() {
        ByteBufferPoolImpl pool = new ByteBufferPoolImpl();
        ByteBufferHolder holder = pool.getBuffer(128);
        assertNotNull(holder);
        ByteBuffer buffer = holder.getBuffer();
        assertNotNull(buffer);
        assertEquals(128, buffer.capacity());
        holder.release();
        
        ByteBufferHolder holder2 = pool.getBuffer(128);
        assertSame(holder, holder2);
        
        ByteBufferHolder holder3 = pool.getBuffer(128);
        assertNotNull(holder3);
        assertNotSame(holder, holder3);
    }
    
    @Test
    public void concurrencyTest() throws InterruptedException {
        final ByteBufferPoolImpl pool = new ByteBufferPoolImpl();
        List<Thread> threads = new ArrayList<Thread>(100);
        for (int i=1; i<100; ++i) {
            Thread thread = new Thread(){
                @Override public void run() {
                    for (int c=0; c<100; c++) {
                        try {
                            ByteBufferHolder holder = pool.getBuffer(128);
                            Thread.sleep(4);
                            holder.release();
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            threads.add(thread);
            thread.start();
        }
        Thread.sleep(1000);
        for (Thread thread: threads)
            assertFalse(thread.isAlive());
        assertTrue(pool.getBuffersCount()<=101);
    }
}
