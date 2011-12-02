/*
 *  Copyright 2011 Mikhail Titov.
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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.media.protocol.DataSource;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.IvrEndpointConversationStoppedEvent;
import org.onesec.raven.ivr.IvrEndpointConversationTransferedEvent;
import org.onesec.raven.ivr.IvrIncomingRtpStartedEvent;
import org.onesec.raven.ivr.IvrOutgoingRtpStartedEvent;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class IvrConversationsBridgeImpl implements IvrConversationsBridge, Comparable<IvrConversationsBridgeImpl>
{
    public enum ConnectionState {INITIALIZING, ACTIVE, INVALID}

    private final IvrEndpointConversation conv1;
    private final IvrEndpointConversation conv2;
    private final Node owner;
    private final long createdTimestamp;
    private final String logPrefix;
    private AtomicLong activatingTimestamp;
    private AtomicLong activatedTimestamp;
    private LinkedList<IvrConversationsBridgeListener> listeners;
//    private Listener listener1;
//    private Listener listener2;
    private BridgeConnection conn1;
    private BridgeConnection conn2;
    private boolean activated = false;
//    private int dataSourceCounter;
    private AtomicReference<IvrConversationsBridgeStatus> status;

    public IvrConversationsBridgeImpl(
            IvrEndpointConversation conv1, IvrEndpointConversation conv2, Node owner, String logPrefix)
    {
        this.conv1 = conv1;
        this.conv2 = conv2;
        this.owner = owner;
        this.logPrefix = logPrefix;
        this.createdTimestamp = System.currentTimeMillis();
        activatingTimestamp = new AtomicLong();
        activatedTimestamp = new AtomicLong();
        listeners = new LinkedList<IvrConversationsBridgeListener>();
//        dataSourceCounter = 0;
        status = new AtomicReference<IvrConversationsBridgeStatus>(IvrConversationsBridgeStatus.CREATED);
    }

    public IvrConversationsBridgeStatus getStatus() {
        return status.get();
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public long getActivatingTimestamp() {
        return activatingTimestamp.get();
    }

    public long getActivatedTimestamp() {
        return activatedTimestamp.get();
    }

    public IvrEndpointConversation getConversation1() {
        return conv1;
    }

    public IvrEndpointConversation getConversation2() {
        return conv2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IvrConversationsBridge))
            return false;
        if (obj==this)
            return true;
        IvrConversationsBridge br = (IvrConversationsBridge) obj;
        return br.getConversation1()==conv1 && br.getConversation2()==conv2;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.conv1 != null ? this.conv1.hashCode() : 0);
        hash = 53 * hash + (this.conv2 != null ? this.conv2.hashCode() : 0);
        hash = 53 * hash + (int) (this.createdTimestamp ^ (this.createdTimestamp >>> 32));
        return hash;
    }

    public int compareTo(IvrConversationsBridgeImpl o) {
        return createdTimestamp==o.createdTimestamp? 0 : (createdTimestamp>o.createdTimestamp? 1 : -1);
    }

    public synchronized void activateBridge()
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Activating bridge"));
        activatingTimestamp.set(System.currentTimeMillis());
        status.set(IvrConversationsBridgeStatus.ACTIVATING);
        conn1 = new BridgeConnection(conv1, conv2);
        conn2 = new BridgeConnection(conv2, conv1);
        conn1.init();
        conn2.init();
//        listener1 = new Listener(conv1);
//        listener2 = new Listener(conv2);
    }

    public void deactivateBridge() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized void addBridgeListener(IvrConversationsBridgeListener listener)
    {
        listeners.add(listener);
    }

    private void fireBridgeActivatedEvent() {
        for (IvrConversationsBridgeListener listener: listeners)
            if (!activated)
                listener.bridgeActivated(this);
            else
                listener.bridgeReactivated(this);
    }

    private void fireBridgeDeactivatedEvent() {
        for (IvrConversationsBridgeListener listener: listeners)
            listener.bridgeDeactivated(this);
    }
    
//    private synchronized void convStopped(IvrEndpointConversation conv)
//    {
//        status.set(IvrConversationsBridgeStatus.DEACTIVATED);
//        if (owner.isLogLevelEnabled(LogLevel.INFO))
//            owner.getLogger().info(logMess("Bridge successfully deactivated"));
//        fireBridgeDeactivatedEvent();
//    }

//    private synchronized void dsCreated(IvrEndpointConversation conv, DataSource dataSource)
//    {
//        IvrEndpointConversation targetConv = conv==conv1? conv2 : conv1;
//        targetConv.getAudioStream().addSource(dataSource);
//        dataSourceCounter++;
//        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//            owner.getLogger().debug(logMess(
//                    "Incoming RTP stream from %s routed to the outgoing RTP of the %s"
//                    , getNumber(conv), getNumber(targetConv)));
//        if (dataSourceCounter==2){
//            status.set(IvrConversationsBridgeStatus.ACTIVATED);
//            activatedTimestamp.set(System.currentTimeMillis());
//            fireBridgeActivatedEvent();
//            if (owner.isLogLevelEnabled(LogLevel.INFO))
//                owner.getLogger().info(logMess("Bridge successfully activated"));
//        }
//    }

    private String getNumber(IvrEndpointConversation conv) {
        return conv==conv1? conv.getCallingNumber() : conv.getCalledNumber();
    }

    private synchronized void checkBridgeState() {
        if (status.get()==IvrConversationsBridgeStatus.DEACTIVATED)
            return;
        if (conn1.state==ConnectionState.INVALID || conn2.state==ConnectionState.INVALID) {
            status.set(IvrConversationsBridgeStatus.DEACTIVATED);
            fireBridgeDeactivatedEvent();
            if (owner.isLogLevelEnabled(LogLevel.INFO))
                owner.getLogger().info(logMess("Bridge deactivated"));
        } else if (conn1.state==ConnectionState.ACTIVE && conn2.state==ConnectionState.ACTIVE) {
            status.set(IvrConversationsBridgeStatus.ACTIVATED);
            activatedTimestamp.set(System.currentTimeMillis());
            fireBridgeActivatedEvent();
            activated = true;
            if (owner.isLogLevelEnabled(LogLevel.INFO))
                owner.getLogger().info(logMess("Bridge successfully activated"));
        } else {
            status.set(IvrConversationsBridgeStatus.ACTIVATING);
        }
    }

    private String logMess(String message, Object... args)
    {
        return (logPrefix==null?"":logPrefix)+"Bridge. "
                +conv1.getCallingNumber()+" >-< "+conv2.getCalledNumber()+" : "
                +String.format(message, args);
    }
    
    private class BridgeConnection
            implements IvrEndpointConversationListener, IncomingRtpStreamDataSourceListener
    {
        private final IvrEndpointConversation conv1;
        private final IvrEndpointConversation conv2;
        private final AtomicReference<AudioStream> audioStream = new AtomicReference<AudioStream>();
        private IncomingRtpStream inRtp;
        private ConnectionState state = ConnectionState.INITIALIZING;

        public BridgeConnection(IvrEndpointConversation conv1, IvrEndpointConversation conv2) {
            this.conv1 = conv1;
            this.conv2 = conv2;
        }
        
        public void init() {
            audioStream.set(conv2.getAudioStream());
            conv1.addConversationListener(this);
            conv2.addConversationListener(this);
        }

        public void listenerAdded(IvrEndpointConversationEvent ev) {
            if (ev.getConversation()!=conv1)
                return;
            int convState = conv1.getState().getId();
            if (convState==IvrEndpointConversationState.INVALID) {
                state = ConnectionState.INVALID;
                checkBridgeState();
            } else {
                inRtp = conv1.getIncomingRtpStream();
                addListenerToRtpStream();
            }
        }

        public void conversationStarted(IvrEndpointConversationEvent event) { }

        public void conversationStopped(IvrEndpointConversationStoppedEvent ev) {
            if (ev.getConversation()!=conv1)
                return;
            state = ConnectionState.INVALID;
            checkBridgeState();
        }

        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) {
        }

        public void incomingRtpStarted(IvrIncomingRtpStartedEvent ev) {
            if (ev.getConversation()!=conv1)
                return;
            IncomingRtpStream rtp = ev.getIncomingRtpStream();
            if (rtp!=inRtp) {
                inRtp = rtp;
                addListenerToRtpStream();
            }
        }

        public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent ev) {
            if (ev.getConversation()!=conv2)
                return;
            if (audioStream.get()!=ev.getAudioStream()) {
                if (owner.isLogLevelEnabled(LogLevel.DEBUG)) 
                    owner.getLogger().debug(logMess(
                            "The outgoing rtp where changed for (%s). Rerouting incoming rtp from (%s) to (%s)"
                            , getNumber(conv2), getNumber(conv1), getNumber(conv2)));
                audioStream.set(ev.getAudioStream());
                addListenerToRtpStream();
            }
            audioStream.set(ev.getAudioStream());
        }

        public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource) {
            if (stream==inRtp && state!=ConnectionState.INVALID) {
                AudioStream audio = audioStream.get();
                if (audio!=null) {
                    audio.addSource(dataSource);
                    state = ConnectionState.ACTIVE;
                    if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                        owner.getLogger().debug(logMess(
                                "Incoming RTP stream from %s routed to the outgoing RTP of the %s"
                                , getNumber(conv1), getNumber(conv2)));
                }
                checkBridgeState();
            }
        }

        public void streamClosing(IncomingRtpStream stream) {
            if (state != ConnectionState.INVALID) {
                state = ConnectionState.INITIALIZING;
                inRtp = null;
                checkBridgeState();
            }
        }

        private void addListenerToRtpStream() {
            try {
                inRtp.addDataSourceListener(this, null);
            } catch (RtpStreamException ex) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("Error adding rtp data source listener"), ex);
                state = ConnectionState.INVALID;
                checkBridgeState();
            }
        }
    }
    
//    private class Listener implements IncomingRtpStreamDataSourceListener, IvrEndpointConversationListener
//    {
//        private final IvrEndpointConversation conv;
//
//        public Listener(IvrEndpointConversation conv)
//        {
//            this.conv = conv;
//            conv.addConversationListener(this);
//            try {
//                conv.getIncomingRtpStream().addDataSourceListener(this, null);
//            } catch (Throwable ex) {
//                convStopped(conv);
//            }
//        }
//
//        public void dataSourceCreated(DataSource dataSource) {
//            dsCreated(conv, dataSource);
//        }
//
//        public void streamClosing() {
//            convStopped(conv);
//        }
//
//        public void listenerAdded(IvrEndpointConversationEvent event) {
//            if (conv.getState().getId()!=IvrEndpointConversationState.TALKING)
//                convStopped(conv);
//        }
//
//        public void conversationStarted(IvrEndpointConversationEvent event) { }
//
//        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
//            convStopped(conv);
//        }
//
//        public void conversationTransfered(IvrEndpointConversationTransferedEvent event) { }
//    }


}
