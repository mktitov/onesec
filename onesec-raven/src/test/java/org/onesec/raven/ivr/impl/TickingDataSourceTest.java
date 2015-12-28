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
package org.onesec.raven.ivr.impl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.media.Buffer;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class TickingDataSourceTest extends Assert {
    private final static Logger logger = LoggerFactory.getLogger(BufferSplitterDataSourceTest.class);
    private static final LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "[TEST] ", null, logger);
    
    @Test
    public void test(
            @Mocked final PushBufferDataSource source,
            @Mocked final PushBufferStream stream,
            @Mocked final BufferTransferHandler receiver
    ) throws Exception {
        final AtomicReference<BufferTransferHandler> transferHandler = new AtomicReference<>();
        final Buffer buf1 = new Buffer(); buf1.setData(1);
        final Buffer buf2 = new Buffer(); buf2.setData(2);
        final AtomicInteger bufferCounter = new AtomicInteger();
        new Expectations(){{
            source.getStreams(); result = new PushBufferStream[]{stream};
            stream.setTransferHandler((BufferTransferHandler) any); result = new Delegate() {
                void setTransferHandler(BufferTransferHandler handler) {
                    transferHandler.set(handler);
                }
            };
            stream.read((Buffer) any); times=2; result = new Delegate() {
                void read(Buffer buf) {
                   buf.setData(bufferCounter.incrementAndGet());
                }
            };
            source.start(); result = new Delegate(){
                void start() {
                    transferHandler.get().transferData(stream);
                    transferHandler.get().transferData(stream);
                }
            };
        }};
        
        final TickingDataSource ds = new TickingDataSource(source, 100, loggerHelper);
                
        new StrictExpectations() {{
            receiver.transferData(ds.getStreams()[0]); result = new Delegate() {
                void transferData(PushBufferStream stream)  {
                    try {
                        Buffer buffer = new Buffer();
                        stream.read(buffer);
                        assertEquals(1, buffer.getData());
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(TickingDataSourceTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            receiver.transferData(ds.getStreams()[0]); result = new Delegate() {
                void transferData(PushBufferStream stream)  {
                    try {
                        Buffer buffer = new Buffer();
                        stream.read(buffer);
                        assertEquals(2, buffer.getData());
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(TickingDataSourceTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
        }};
        ds.getStreams()[0].setTransferHandler(receiver);
        
        long startTs = System.currentTimeMillis();
        ds.start();
        long finishTs = System.currentTimeMillis();
        assertTrue(finishTs-startTs>=200);
        assertTrue(finishTs-startTs<250);
    }
}
