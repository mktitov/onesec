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
import javax.media.Codec;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.AudioStream;
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
    private final static int INITIAL_BUFFER_SIZE = 2;

    private final Queue<InputStreamSource> sources;
    private final String contentType;
    private final ExecutorService executorService;
    private final ConcatDataStream[] streams;
    private final Queue<Buffer> buffers;
    private final Node owner;
    private final AtomicBoolean dataConcated;
    private final AtomicBoolean stoped;
    private final Format format = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
    private final int rtpPacketSize;
    private boolean started = false;
    private boolean silenceSource = true;

    public ConcatDataSource(String contentType
            , ExecutorService executorService
            , int rtpPacketSize
            , Node owner)
    {
        this.contentType = contentType;
        this.executorService = executorService;
        this.owner = owner;
        this.rtpPacketSize = rtpPacketSize;

        sources = new ConcurrentLinkedQueue<InputStreamSource>();
        dataConcated = new AtomicBoolean(false);
        stoped = new AtomicBoolean(false);
        buffers = new ConcurrentLinkedQueue<Buffer>();
        streams = new ConcatDataStream[]{new ConcatDataStream(buffers, this, owner)};
        ResourceInputStreamSource silenceSource =
                new ResourceInputStreamSource(SILENCE_RESOURCE_NAME);
        addSource(silenceSource);
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
        stoped.set(true);
    }

    public void reset()
    {
        sources.clear();
        buffers.clear();
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
        return String.format(
                "Generating continuous audio stream from (%s) audio sources", sources.size());
    }

    public void run()
    {
        try
        {
//            int i=0;
            int bufferCount = 0;
//            for (InputStreamSource source: sources)
//            {
            List<Buffer> initialBuffer = new ArrayList<Buffer>(INITIAL_BUFFER_SIZE);
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
                        owner.getLogger().debug("AudioStream. Found new source. Processing...");
                    long ts = System.currentTimeMillis();
                    IssDataSource ids = new IssDataSource(source, contentType);
                    Processor p = Manager.createProcessor(ids);
                    p.addControllerListener(this);
                    p.configure();
                    waitForState(p, Processor.Configured);
                    TrackControl[] tracks = p.getTrackControls();
                    tracks[0].setFormat(format);
                    Codec codec[] = new Codec[3];
                    codec[0] = new com.ibm.media.codec.audio.rc.RCModule();
                    codec[1] = new com.ibm.media.codec.audio.ulaw.JavaEncoder();
                    codec[2] = new com.sun.media.codec.audio.ulaw.Packetizer();
                    ((com.sun.media.codec.audio.ulaw.Packetizer)codec[2]).setPacketSize(rtpPacketSize);
                    tracks[0].setCodecChain(codec);
                    p.realize();
                    waitForState(p, Processor.Realized);
                    p.start();
    //                waitForState(p, Processor.Started);

                    PushBufferDataSource ds = (PushBufferDataSource) p.getDataOutput();
                    ds.start();
                    PushBufferStream s = ds.getStreams()[0];
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(
                                "AudioStream. Source initialization time (ms) - "
                                +(System.currentTimeMillis()-ts));
                    try
                    {
                        boolean eom = false;
                        initialBuffer.clear();
                        boolean initialBufferInitialized = false;
                        while (!eom)
                        {
                            Buffer buffer = new Buffer();
                            s.read(buffer);
                            if (silenceSource)
                            {
                                buffers.add(buffer);
                                silenceSource = false;
                                eom = true;
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
                                    if (initialBuffer.size()==INITIAL_BUFFER_SIZE)
                                    {
                                        initialBufferInitialized = true;
                                        buffers.addAll(initialBuffer);
                                        initialBuffer.clear();
                                    }
                                }
                                else
                                    buffers.add(buffer);
                                ++bufferCount;
        //                        streams[0].transferData(null);
                                Thread.sleep(5);
                            }
                        }
                        if (!initialBufferInitialized)
                        {
                            initialBufferInitialized = true;
                            buffers.addAll(initialBuffer);
                            initialBuffer.clear();
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
            dataConcated.set(true);
            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                owner.getLogger().debug(String.format("Gathered (%s) buffers", bufferCount));
        } catch(Throwable e)
        {
            e.printStackTrace();
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error("AudioStream. Error creating continuous audio stream.", e);
        }
    }

    private static void waitForState(Processor p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(2);
            if (System.currentTimeMillis()-startTime>2000)
                throw new Exception("Processor state wait timeout");
        }
    }

    public void controllerUpdate(ControllerEvent event)
    {
    }
}
