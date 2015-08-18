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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.impl.ConcatDataSource.LastBuffer;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataStream implements PushBufferStream, Task
{
    public static final int MAX_QUEUE_SIZE = 7;
    public static int MAX_SILENCE_BUFFER_COUNT = 1500;
    public static final int MAX_TIME_SKEW = 300;

    private final LoggerHelper logger;
    private final Buffer silentBuffer;
    public final Queue<Buffer> bufferQueue;
    private final ConcatDataSource dataSource;
    private final ContentDescriptor contentDescriptor;
    private final Node owner;
    private final int packetLength; //ms?
    
    private BufferTransferHandler transferHandler;
    private Buffer bufferToSend;
    private String action;
    private long packetNumber;
    private long sleepTime;
    private final AtomicInteger sentPackets = new AtomicInteger(0);
    private final AtomicInteger silencePacketCount = new AtomicInteger(0);
//    private String logPrefix;
    private final AtomicReference<ConcatDataSource.SourceProcessor> sourceInfo = new AtomicReference<>();
    private final AtomicInteger emptyQueueEvents = new AtomicInteger(0);
    private volatile Map<String, String> stat;

    public ConcatDataStream(
            Queue<Buffer> bufferQueue, ConcatDataSource dataSource, Node owner
            , int packetSize, Codec codec, int maxSendAheadPacketsCount, Buffer silentBuffer, 
            LoggerHelper logger)
    {
        this.logger = logger;
        this.bufferQueue = bufferQueue;
        this.dataSource = dataSource;
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
        this.owner = owner;
        this.packetLength = (int) codec.getMillisecondsForPacketSize(packetSize);
//        this.packetLength = (int) dataSource.getPacketSizeInMillis();
        this.silentBuffer = silentBuffer;
    }
    
    void sourceInitialized(ConcatDataSource.SourceProcessor source) {
        sourceInfo.set(source);
        emptyQueueEvents.set(0);
    }
    
    void sourceClosed(ConcatDataSource.SourceProcessor source) {
        sourceInfo.compareAndSet(source, null);
    }

    @Override
    public Format getFormat() {
        return dataSource.getFormat();
    }

    @Override
    public void read(Buffer buffer) throws IOException
    {
        action = "reading buffer";
        if (bufferToSend==null) {
            silencePacketCount.incrementAndGet();
            buffer.copy(silentBuffer);
        } else {
            silencePacketCount.set(0);
            buffer.copy(bufferToSend);            
        }
        sentPackets.incrementAndGet();
    }

    @Override
    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    @Override
    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }

    @Override
    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    @Override
    public boolean endOfStream() {
        return bufferQueue.size()==0 && dataSource.isClosed();
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Object getControl(String controlType) {
        return null;
    }

    private void sendBuffer() {
        if (transferHandler!=null)
            transferHandler.transferData(this);
    }

    @Override
    public Node getTaskNode() {
        return owner;
    }

    @Override
    public String getStatusMessage() {
        return String.format(logger.logMess(
                "Transfering buffers to rtp session. Action: %s. packetCount: %s; sleepTime: %s"
                , action, packetNumber, sleepTime));
    }

    @Override
    public void run() {
        dataSource.setStreamThreadRunning(true);
        try
        {
            long startTime = System.currentTimeMillis();
            packetNumber = 0;
            ConcatDataSource.SourceProcessor si = null;
            if (logger.isDebugEnabled())
                logger.debug("Concat stream started with time quant {} ms", packetLength);
            boolean debugEnabled = logger.isDebugEnabled();
            long prevTime = System.currentTimeMillis();
            long maxTransferTime = 0;
            long droppedPacketCount = 0;
            long transferTimeSum = 0;
            long emptyBufferEventCount = 0;
            long skew = 0l;
            long missedPackets = 0l;
            long timeDiff = 0l;
            long expectedPacketNumber = 0l;
            long prevSkew = 0l;
            long maxSkew = 0l;
            long maxSkewTime = 0l;
            while ((!dataSource.isClosed() || !bufferQueue.isEmpty()))
//                    && (silencePacketCount.get()<MAX_SILENCE_BUFFER_COUNT || (si!=null && si.isRealTime())))
            {
//                try {
                    long cycleStartTs = System.currentTimeMillis();
                    action = "getting new buffer from queue";
                    si = sourceInfo.get();
                    bufferToSend = bufferQueue.poll();
                    if (bufferToSend instanceof LastBuffer) {
                        ((LastBuffer)bufferToSend).getSourceListener().sourceProcessed();
                        continue;
                    }
                    if (bufferToSend!=null && si!=null && si.isRealTime()) {
                        if (   bufferToSend.getTimeStamp()+MAX_TIME_SKEW<cycleStartTs
                            || bufferQueue.size()>MAX_QUEUE_SIZE) 
                        {
                            droppedPacketCount++;
                            continue;
                        }
                    }
                    action = "sending transfer event";
//                    if (bufferToSend!=null || si==null || !si.isRealTime()) {
                        sendBuffer();
                        long tt = System.currentTimeMillis()-cycleStartTs;
                        transferTimeSum+=tt;
                        if (tt>maxTransferTime)
                            maxTransferTime=tt;
//                    }
                    ++packetNumber;
                    action = "sleeping";
                    final long curTime = System.currentTimeMillis();
                    timeDiff = curTime - startTime;
                    expectedPacketNumber = timeDiff/packetLength;
//                    final long correction = timeDiff % packetLength;
                    emptyBufferEventCount = expectedPacketNumber - packetNumber;
                    skew = timeDiff - sentPackets.get()*packetLength;
                    if (packetNumber>1 && maxSkew<skew-prevSkew) {                        
                        maxSkew = skew-prevSkew;
                        maxSkewTime = curTime;
                    }
                    
                    missedPackets = expectedPacketNumber - sentPackets.get();
                    if (si!=null && debugEnabled && curTime-prevTime>30000) {
                        prevTime = curTime;
                        String mess = getStatString(emptyBufferEventCount, transferTimeSum, maxTransferTime, 
                                droppedPacketCount, skew, maxSkew, maxSkewTime, expectedPacketNumber, missedPackets, 
                                startTime);
                        logger.debug(mess);
                    }
                    prevSkew = skew;
//                    sleepTime = (packetNumber-expectedPacketNumber)*packetLength - correction - 5;                    
//                    if (sleepTime>0)
//                        TimeUnit.MILLISECONDS.sleep(sleepTime);
//                } catch (InterruptedException ex) {
//                    if (logger.isErrorEnabled())
//                        logger.error("Transfer buffers to rtp session task was interrupted", ex);
//                    Thread.currentThread().interrupt();
//                    break;
//                }
            }
            if (debugEnabled) {
                logger.debug("Transfer buffers to rtp session task was finished");
            }
            skew = timeDiff - sentPackets.get()*packetLength;
            missedPackets = expectedPacketNumber - sentPackets.get();
            String mess = getStatString(emptyBufferEventCount, transferTimeSum, maxTransferTime, 
                    droppedPacketCount, skew, maxSkew, maxSkewTime, expectedPacketNumber, missedPackets, 
                    startTime);
            if (missedPackets*packetLength>500 && logger.isWarnEnabled())
                logger.warn("Missed packets more than 500ms. {}", mess);
            else if (logger.isDebugEnabled())
                logger.debug("Transfer of RTP packets finished. {}", mess);
        } finally {
            dataSource.setStreamThreadRunning(false);
        }
    }
    
    public Map<String, String> getStat() {
        return stat;
    } 

    private String getStatString(final long emptyBufferEventCount, final long transferTimeSum, 
            final long maxTransferTime, 
            final long droppedPacketCount, final long skew, final long maxSkew, final long maxSkewTime, 
            final long expectedPacketNumber, final long missedPackets, final long startTime) 
    {
        final long currTime = System.currentTimeMillis();
        final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        final long maxSkewDur = maxSkewTime-startTime;
        final String avgSkew = String.format("%.2f", expectedPacketNumber==0? 0 : skew/(double)expectedPacketNumber);
        final String avgTransferTime = String.format("%.2f", packetNumber==0? 0 : transferTimeSum/(double)packetNumber);
        final String _startTime = fmt.format(new Date(startTime));
        final long _dur = (currTime-startTime)/1000;
        final String _maxSkewTime = fmt.format(new Date(maxSkewTime))+"/"+maxSkewDur/1000+"."+maxSkewDur%1000;
        final Map<String, String> _stat = new LinkedHashMap<>();        
        _stat.put("format", getFormat().getEncoding());
        _stat.put("startTime", _startTime);
        _stat.put("duration", ""+_dur);
        _stat.put("skew", ""+skew);
        _stat.put("maxSkew", ""+maxSkew);
        _stat.put("maxSkewTime", _maxSkewTime);
        _stat.put("avgSkew", avgSkew);
        _stat.put("expectedPackets", ""+expectedPacketNumber);
        _stat.put("sentPackets", ""+sentPackets.get());
        _stat.put("missedPackets", ""+missedPackets);
        _stat.put("droppedPackets", ""+droppedPacketCount);
        _stat.put("siliencePackets", ""+silencePacketCount.get());
        _stat.put("avgTransferTime", ""+avgTransferTime);
        _stat.put("maxTransferTime", ""+maxTransferTime);
        _stat.put("emptyBufferEventCount", ""+emptyBufferEventCount);
        this.stat = _stat;
        return String.format(
                "\n\tformat: %s; "+
                "\n\tstartTime: %s; dur: %s sec; skew: %s; maxSkew: %s; maxSkewTime: %s; avgSkew: %s; "+
                "\n\texpectedPackets: %s; sentPackets: %s; missedPackets: %s; droppedPackets: %s; "+
                "siliencePackets: %s; "+
                "\n\tavgTransferTime: %s; maxTransferTime: %s; "+
                "emptyBufferEvents: %s; buffersSize: %s"
                , getFormat().getEncoding()
                , _startTime
                , (currTime-startTime)/1000, skew, maxSkew
                , _maxSkewTime
                , avgSkew
                , expectedPacketNumber, sentPackets.get(), missedPackets, droppedPacketCount
                , silencePacketCount.get()
                , avgTransferTime, maxTransferTime, emptyBufferEventCount, bufferQueue.size());
    }

}
