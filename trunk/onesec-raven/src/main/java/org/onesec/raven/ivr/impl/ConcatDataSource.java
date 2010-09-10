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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.Buffer;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.PacketSizeControl;
import javax.media.control.TrackControl;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataSource
        extends PushBufferDataSource
        implements AudioStream, Task, ControllerListener
{
    public final static String SILENCE_RESOURCE_NAME = "/org/onesec/raven/ivr/silence.wav";

    private final Queue<InputStreamSource> sources;
    private final String contentType;
    private final ExecutorService executorService;
    private final ConcatDataStream[] streams;
    private final Queue<Buffer> buffers;
    private final Node owner;
    private final AtomicBoolean dataConcated;
    private final AtomicBoolean stoped;
    private final AtomicBoolean sourceThreadRunning;
    private final AtomicBoolean streamThreadRunning;
//    private final Format FORMAT = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
    public final Format format;
    private final int rtpPacketSize;
    private final int rtpInitialBufferSize;
    private final int rtpMaxSendAheadPacketsCount;
    private boolean started = false;
    private AtomicBoolean silenceSource;
    private Thread thread;
    private String logPrefix;

    public ConcatDataSource(String contentType
            , ExecutorService executorService
            , Codec codec
            , int rtpPacketSize
            , int rtpInitialBufferSize
            , int rtpMaxSendAheadPacketsCount
            , Node owner)
    {
        this.contentType = contentType;
        this.executorService = executorService;
        this.owner = owner;
        this.rtpPacketSize = rtpPacketSize;
        this.rtpInitialBufferSize = rtpInitialBufferSize;
        this.rtpMaxSendAheadPacketsCount = rtpMaxSendAheadPacketsCount;

        format = codec.getAudioFormat();

        sources = new ConcurrentLinkedQueue<InputStreamSource>();
        dataConcated = new AtomicBoolean(false);
        stoped = new AtomicBoolean(false);
        sourceThreadRunning = new AtomicBoolean(false);
        streamThreadRunning = new AtomicBoolean(false);
        silenceSource = new AtomicBoolean(true);
        buffers = new ConcurrentLinkedQueue<Buffer>();
        streams = new ConcatDataStream[]{
            new ConcatDataStream(buffers, this, owner, rtpPacketSize, rtpMaxSendAheadPacketsCount)};
        streams[0].setLogPrefix(logPrefix);
        ResourceInputStreamSource silenceSource =
                new ResourceInputStreamSource(SILENCE_RESOURCE_NAME);
        addSource(silenceSource);
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public DataSource getDataSource()
    {
        return this;
    }

    public void addSource(InputStreamSource source)
    {
        if (source!=null)
        {
            sources.add(source);
        }
    }

    public Format getFormat()
    {
        return format;
    }

    public boolean isPlaying()
    {
        return !isDataConcated() && (!sources.isEmpty() || !buffers.isEmpty());
    }

    public boolean isDataConcated()
    {
        return dataConcated.get();
    }

    @Override
    public PushBufferStream[] getStreams()
    {
        return streams;
    }

    @Override
    public String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    @Override
    public void connect() throws IOException
    {
    }

    @Override
    public void disconnect()
    {
    }

    @Override
    public void start() throws IOException
    {
        if (!started)
        {
            started = true;
            try
            {
                executorService.execute(this);
                executorService.execute(streams[0]);
            }
            catch (ExecutorServiceException ex)
            {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public void stop() throws IOException
    {
    }

    public void close() 
    {
        if (stoped.get())
            return;
        stoped.set(true);
        sources.clear();
        buffers.clear();
        try
        {
            long stopStart = System.currentTimeMillis();
            while (sourceThreadRunning.get() && System.currentTimeMillis()-stopStart<=5000)
                Thread.sleep(100);
            if (System.currentTimeMillis()-stopStart>5000 && sourceThreadRunning.get())
                thread.interrupt();
            while (streamThreadRunning.get())
                Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("ConcatDataSource close operation was interrupted"), e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isClosed()
    {
        return stoped.get();
    }

    public void reset()
    {
        try {
            while (silenceSource.get())
                TimeUnit.MILLISECONDS.sleep(10);
        }
        catch (InterruptedException e)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("Error reseting audio stream. Error waiting for audio source initialization"), e);
            Thread.currentThread().interrupt();
        }
        sources.clear();
        buffers.clear();
    }

    void setStreamThreadRunning(boolean streamThreadRunning)
    {
        this.streamThreadRunning.set(streamThreadRunning);
    }

    @Override
    public Object getControl(String arg0)
    {
        return null;
    }

    @Override
    public Object[] getControls()
    {
        return new Object[0];
    }

    @Override
    public Time getDuration()
    {
        return DURATION_UNKNOWN;
    }

    public Node getTaskNode()
    {
        return owner;
    }

    public String getStatusMessage()
    {
        return logMess("Generating continuous audio stream from (%s) audio sources", sources.size());
    }

    public void run()
    {
        sourceThreadRunning.set(true);
        try
        {
            thread = Thread.currentThread();
            try
            {
                int bufferCount = 0;
                List<Buffer> initialBuffer = new ArrayList<Buffer>(rtpInitialBufferSize);
                while (!stoped.get())
                {
                    InputStreamSource source = sources.peek();
                    if (source==null)
                    {
                        Thread.sleep(5);
                        continue;
                    }
                    try
                    {
                        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                            owner.getLogger().debug(logMess("Found new source. Processing..."));
                        long ts = System.currentTimeMillis();
                        IssDataSource ids = new IssDataSource(source, contentType);
                        Processor p = Manager.createProcessor(ids);
                        p.addControllerListener(this);
                        p.configure();
                        waitForState(p, Processor.Configured);
                        TrackControl[] tracks = p.getTrackControls();
                        tracks[0].setFormat(format);
                        p.realize();
                        waitForState(p, Processor.Realized);

                        PacketSizeControl packetSizeControl =
                                (PacketSizeControl) p.getControl(PacketSizeControl.class.getName());
                        packetSizeControl.setPacketSize(rtpPacketSize);

                        PushBufferDataSource ds = (PushBufferDataSource) p.getDataOutput();
                        p.start();
                        waitForState(p, Processor.Started);
                        PushBufferStream s = ds.getStreams()[0];
                        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                            owner.getLogger().debug(logMess(
                                    "Source initialization time (ms) - "+(System.currentTimeMillis()-ts)));
                        try
                        {
                            boolean eom = false;
                            initialBuffer.clear();
                            boolean initialBufferInitialized = false;
                            while (!eom)
                            {
                                Buffer buffer = new Buffer();
                                s.read(buffer);
                                if (buffer.isDiscard())
                                {
                                    Thread.sleep(2);
                                    continue;
                                }
                                if (silenceSource.get())
                                {
                                    buffers.add(buffer);
                                    silenceSource.set(false);
                                    eom = true;
                                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                                        owner.getLogger().debug(logMess("Silence buffer initialized"));
                                }
                                else
                                {
                                    if (buffer.isEOM())
                                    {
                                        eom = true;
                                        buffer.setEOM(false);
                                    }
                                    if (!initialBufferInitialized)
                                    {
                                        initialBuffer.add(buffer);
                                        if (initialBuffer.size()==rtpInitialBufferSize)
                                        {
                                            initialBufferInitialized = true;
//                                            System.out.println("!!!>>>Flushing initial buffer: "+initialBuffer.size());
                                            buffers.addAll(initialBuffer);
    //                                        initialBuffer.clear();
                                        }
                                    }
                                    else
                                        buffers.add(buffer);
                                    ++bufferCount;
            //                        streams[0].transferData(null);
                                    Thread.sleep(2);
                                }
                            }
                            if (!initialBufferInitialized && !initialBuffer.isEmpty())
                            {
//                                System.out.println("!!!>>>Flushing initial buffer: "+initialBuffer.size());
                                initialBufferInitialized = true;
                                buffers.addAll(initialBuffer);
    //                            initialBuffer.clear();
                            }
                        }
                        finally
                        {
                            ids.stop();
                            p.stop();
                            ds.stop();
                            p.close();
                        }
                    }
                    finally
                    {
                        sources.poll();
                    }
                }
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(logMess("Gathered (%s) buffers", bufferCount));
            } catch(Throwable e)
            {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("Error creating continuous audio stream"), e);
            }
        }
        finally
        {
            silenceSource.set(false);
            dataConcated.set(true);
            sourceThreadRunning.set(false);
        }
    }

    private static void waitForState(Processor p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(5);
            if (System.currentTimeMillis()-startTime>2000)
                throw new Exception("Processor state wait timeout");
        }
    }

    public void controllerUpdate(ControllerEvent event)
    {
    }

    String logMess(String mess, Object... args)
    {
        return (logPrefix==null? "" : logPrefix)+"AudioStream. "+String.format(mess, args);
    }
}
