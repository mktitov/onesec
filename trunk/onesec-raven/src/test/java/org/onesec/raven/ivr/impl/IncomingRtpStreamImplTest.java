/*
 *  Copyright 2010 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import org.onesec.raven.ivr.BufferCache;
import java.io.IOException;
import org.onesec.raven.JMFHelper;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.RtpManagerTestCase;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class IncomingRtpStreamImplTest extends RtpManagerTestCase 
{
    @Before
    public void prepare()
    {
    }

    @After
    public void closeTest() throws InterruptedException
    {
        Thread.sleep(500);
    }

    public void sendOverRtpTest() throws Exception
    {
        OperationState state = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, "10.50.1.85", 1234);
        state.join();
    }

//    @Test
    public void dataSourceListenerEventsTest() throws Exception
    {
        IncomingRtpStream irtp = manager.getIncomingRtpStream(manager);

        IncomingRtpStreamDataSourceListener listener = createMock(
                IncomingRtpStreamDataSourceListener.class);
        listener.dataSourceCreated(same(irtp), isA(DataSource.class));
        listener.streamClosing(irtp);

        replay(listener);

        String address = getInterfaceAddress().getHostName();
        irtp.open(address);
        irtp.addDataSourceListener(listener, null);
        OperationState sendControl = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, address, irtp.getPort());
        sendControl.join();
        irtp.release();
        
        verify(listener);
    }

    @Test
    public void zenitTest() throws Exception
    {
        IncomingRtpStream irtp = manager.getIncomingRtpStream(manager);
        String address = getInterfaceAddress().getHostName();
        DataSourceListener fileWriter = new DataSourceListener("target/recorded.wav");
        irtp.addDataSourceListener(fileWriter, null);
        irtp.open(address);
//        OperationState sendControl = sendOverRtp(
//                "src/test/wav/test.wav", Codec.G711_MU_LAW, address, irtp.getPort());
//        sendControl.join();
        Thread.sleep(20000);
        irtp.release();
    }

//    @Test
    public void oneListenerTest() throws Exception
    {
        IncomingRtpStream irtp = manager.getIncomingRtpStream(manager);
        String address = getInterfaceAddress().getHostName();
        irtp.open(address);
        DataSourceListener fileWriter = new DataSourceListener("target/recorded.wav");
        irtp.addDataSourceListener(fileWriter, null);
        OperationState sendControl = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, address, irtp.getPort());
        sendControl.join();
        irtp.release();
    }

//    @Test
    public void tenListenerTest() throws Exception
    {
        IncomingRtpStream irtp = manager.getIncomingRtpStream(manager);
        String address = getInterfaceAddress().getHostName();
        irtp.open(address);
        //creating 5 listeners before rtp starts
        int i=0;
        for (;i<5;++i){
            DataSourceListener fileWriter = new DataSourceListener("target/recorded_"+i+".wav");
            irtp.addDataSourceListener(fileWriter, null);
        }
        OperationState sendControl = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, address, irtp.getPort());
        //creating 5 listeners after rtp starts
        for (;i<10;++i){
//            Thread.sleep(100);
            DataSourceListener fileWriter = new DataSourceListener("target/recorded_"+i+".wav");
            irtp.addDataSourceListener(fileWriter, null);
        }
        sendControl.join();
        irtp.release();
    }

//    @Test
    public void copyToConcatDataSource() throws Exception
    {
        IncomingRtpStream irtp = manager.getIncomingRtpStream(manager);
        String address = getInterfaceAddress().getHostName();
        Codec codec = Codec.G711_MU_LAW;
        CopyDsConcatDataSource fileWriter = new CopyDsConcatDataSource(
                "target/copy_to_concatDs.wav", codec);
        Thread.sleep(1000);
        irtp.addDataSourceListener(fileWriter, null);
        OperationState sendControl = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, address, irtp.getPort());
        irtp.open(address);
        sendControl.join();
        irtp.release();
        fileWriter.ds.addSource(new TestInputStreamSource("src/test/wav/test2.wav"));
        Thread.sleep(6000);
        fileWriter.close();
        Thread.sleep(1000);
    }

    private class DataSourceListener implements IncomingRtpStreamDataSourceListener
    {
        private final String filename;
        private JMFHelper.OperationController writeControl;

        public DataSourceListener(String filename) {
            this.filename = filename;
            this.writeControl = null;
        }

        public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource) {
            logger.debug("Received dataSourceCreated event");
            if (dataSource!=null)
                try {
                    logger.debug("Creating new file writer ({})", filename);
                    writeControl = JMFHelper.writeToFile(dataSource, filename);
                } catch (Exception ex) {
                    logger.error("Error writing to file ({})", filename, ex);
                }

        }

        public void streamClosing(IncomingRtpStream stream) {
            if (writeControl!=null)
                writeControl.stop();
        }
    }

    private class CopyDsConcatDataSource implements IncomingRtpStreamDataSourceListener
    {
        private final String filename;
        private final Codec codec;

        private ConcatDataSource ds;
        private JMFHelper.OperationController writeControl;
        private DataSource dataSource;

        public CopyDsConcatDataSource(String filename, Codec codec) throws  Exception
        {
            this.filename = filename;
            this.codec = codec;
            ds = new ConcatDataSource(FileTypeDescriptor.WAVE, executor, codec, 240, 0, 0, manager,
                    registry.getService(BufferCache.class));
            ds.start();
            writeControl = JMFHelper.writeToFile(ds, filename);
        }

        public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource)
        {
            if (dataSource==null) 
                logger.error("CopyDsConcatDataSource. Received null dataSource");
            else {
                try {
                    this.dataSource = dataSource;
                    ds.addSource(dataSource);
                } catch(Exception e){
                    logger.error("CopyDsConcatDataSource. Error creating ConcatDataSource", e);
                }
            }
        }

        public void streamClosing(IncomingRtpStream stream)
        {
            try {
//                dataSource.stop();
//                InputStreamSource iss = new TestInputStreamSource("src/test/wav/test2.wav");
//                ds.addSource(iss);
//                while (ds.isPlaying())
//                    Thread.sleep(20);
            } catch (Exception ex) {
                logger.error("CopyDsConcatDataSource. Error stream closing", ex);
            }
        }

        public void close() throws IOException
        {
            ds.stop();
            writeControl.stop();
        }
    }
}