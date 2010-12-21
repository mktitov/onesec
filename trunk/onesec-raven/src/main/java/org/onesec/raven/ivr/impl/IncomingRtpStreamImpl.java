/*
 *  Copyright 2009 Mikhail Titov.
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

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.media.Controller;
import javax.media.DataSink;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.SourceCloneable;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.RTPManagerService;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class IncomingRtpStreamImpl extends AbstractRtpStream 
        implements IncomingRtpStream, ReceiveStreamListener
{
    @Service
    private static RTPManagerService rtpManagerService;

    private RTPManager rtpManager;
    private SessionAddress destAddress;
    private ReceiveStream stream;
    private DataSource source;
    private List<Consumer> consumers;
    private Lock lock;

    public IncomingRtpStreamImpl(InetAddress address, int port)
    {
        super(address, port, "Incoming RTP");
        consumers = new LinkedList<Consumer>();
        lock = new ReentrantLock();
    }

    public long getHandledBytes()
    {
        return 0;
    }

    public long getHandledPackets()
    {
        return 0;
    }

    @Override
    public void doRelease() throws Exception
    {
//        stream.getDataSource().stop();
        try{
            rtpManager.removeTargets("Disconnected");
        }finally{
            rtpManager.dispose();
        }
    }

    public void open(String remoteHost, int remotePort) throws RtpStreamException
    {
        try
        {
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess(
                        "Trying to open incoming RTP stream to the remote host (%s) using port (%s)"
                        , remoteHost, remotePort));

            rtpManager = rtpManagerService.createRtpManager();
            rtpManager.addReceiveStreamListener(this);
            rtpManager.initialize(new SessionAddress(address, port));
            InetAddress dest = InetAddress.getByName(remoteHost);
            destAddress = new SessionAddress(dest, SessionAddress.ANY_PORT);
            rtpManager.addTarget(destAddress);

        }
        catch(Exception e)
        {
            throw new RtpStreamException(
                    String.format(
                        "Outgoing RTP. Error opening RTP stream to remote host (%s) using port (%s)"
                        , remoteHost, remotePort)
                    , e);
        }
    }

    public void addDataSourceListener(IncomingRtpStreamDataSourceListener listener)
    {
        try{
            if (lock.tryLock(100, TimeUnit.MICROSECONDS)){
                try{

                }finally{
                    lock.unlock();
                }
            }
            consumers.add(new Consumer(listener));
        }catch(InterruptedException e){
            owner.getLogger().error("Error adding listener", e);
        }
    }

    public void update(final ReceiveStreamEvent event)
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Received stream event (%s)", event.getClass().getName()));

        if (event instanceof NewReceiveStreamEvent)
        {
            stream = event.getReceiveStream();
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(logMess("Received new stream"));

            new Thread(){
                @Override
                public void run(){
                    try{
                        Thread.sleep(4000);
                        DataSource ds = stream.getDataSource();

                        RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
                        if (ctl!=null)
                            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                                owner.getLogger().debug(logMess("The format of the stream: %s", ctl.getFormat()));

                        // create a player by passing datasource to the Media Manager

                        ds = javax.media.Manager.createCloneableDataSource(ds);
                        SourceCloneable cloneable = (SourceCloneable) ds;
                        saveToFile(ds,"test.wav", 10000);
                        Thread.sleep(5000);
                        saveToFile(cloneable.createClone(),"test2.wav", 5000);
                    }
                    catch(Exception e){
                            owner.getLogger().error(logMess("Error."), e);
                    }
                }
            }.start();
        }
    }

    private void saveToFile(DataSource ds, String filename, final long closeAfter) throws Exception
    {
        Processor p = javax.media.Manager.createProcessor(ds);
        p.configure();
        waitForState(p, Processor.Configured);
        p.getTrackControls()[0].setFormat(new AudioFormat(
                AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED));
        p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
        p.realize();
        waitForState(p, Processor.Realized);
        final DataSink fileWriter = javax.media.Manager.createDataSink(
                p.getDataOutput(), new MediaLocator("file:///home/tim/tmp/"+filename));
        fileWriter.open();
        p.start();
        fileWriter.start();
        new Thread(){
            @Override
            public void run() {
                try {
                    Thread.sleep(closeAfter);
                    fileWriter.stop();
                    fileWriter.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void waitForState(Controller p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(5);
            if (System.currentTimeMillis()-startTime>2000)
                throw new Exception("Processor state wait timeout");
        }
    }

    private class Consumer
    {
        private IncomingRtpStreamDataSourceListener listener;
        private boolean createEventFired = false;

        public Consumer(IncomingRtpStreamDataSourceListener listener) {
            this.listener = listener;
        }
    }

}
