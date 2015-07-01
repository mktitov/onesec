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
import org.onesec.raven.ivr.*;
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
    private final AtomicLong activatingTimestamp;
    private final AtomicLong activatedTimestamp;
    private final LinkedList<IvrConversationsBridgeListener> listeners;
    private BridgeConnection conn1;
    private BridgeConnection conn2;
    private boolean activated = false;
    private final AtomicReference<IvrConversationsBridgeStatus> status;
    private final boolean passDtmf;
//    private final ReentrantLock lock = new ReentrantLock();

    public IvrConversationsBridgeImpl(
            IvrEndpointConversation conv1, IvrEndpointConversation conv2, Node owner, String logPrefix, boolean passDtmf)
    {
        this.conv1 = conv1;
        this.conv2 = conv2;
        this.owner = owner;
        this.logPrefix = logPrefix;
        this.createdTimestamp = System.currentTimeMillis();
        this.passDtmf = passDtmf;
        activatingTimestamp = new AtomicLong();
        activatedTimestamp = new AtomicLong();
        listeners = new LinkedList<IvrConversationsBridgeListener>();
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

    public void activateBridge()
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("Activating bridge"));
        synchronized(this) {
            activatingTimestamp.set(System.currentTimeMillis());
            status.set(IvrConversationsBridgeStatus.ACTIVATING);
            conn1 = new BridgeConnection(conv1, conv2);
            conn2 = new BridgeConnection(conv2, conv1);
        }
        conn1.init();
        conn2.init();
    }

    public synchronized void reactivateBridge() {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess("ReActivating bridge"));
        if (status.get()==IvrConversationsBridgeStatus.DEACTIVATED) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(logMess("Can't reactivate DEACTIVATED bridge"));
        } else {
            fireBridgeDeactivatedEvent();
            if (status.get()==IvrConversationsBridgeStatus.ACTIVATED) {
                activated = false;
                fireBridgeActivatedEvent();
                activated = true;
            }
        }
    }

    public void deactivateBridge() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized void addBridgeListener(IvrConversationsBridgeListener listener) {
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
                +"("+conv1.getCallingNumber()+"->"+conv1.getCalledNumber()+") >-< "
                +"("+conv2.getCallingNumber()+"->"+conv2.getCalledNumber()+") : "
                +String.format(message, args);
    }

    @Override
    public String toString() {
        return logMess("");
    }
    
    private class BridgeConnection
            implements IvrEndpointConversationListener, IncomingRtpStreamDataSourceListener
    {
        private final IvrEndpointConversation conv1;
        private final IvrEndpointConversation conv2;
        private final AtomicReference<AudioStream> audioStream = new AtomicReference<AudioStream>();
        private final AtomicReference<IncomingRtpStream> inRtp = new AtomicReference<IncomingRtpStream>();
        private volatile ConnectionState state = ConnectionState.INITIALIZING;

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
                inRtp.set(conv1.getIncomingRtpStream());
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
            if (rtp!=inRtp.get()) {
                inRtp.set(rtp);
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
            if (stream==inRtp.get() && state!=ConnectionState.INVALID) {
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

        public void dtmfReceived(IvrDtmfReceivedConversationEvent ev) {
            if (ev.getConversation()!=conv1 || !passDtmf)
                return;
            conv2.sendDTMF(""+ev.getDtmf());
        }

        public void streamClosing(IncomingRtpStream stream) {
            if (state != ConnectionState.INVALID) {
                state = ConnectionState.INITIALIZING;
                inRtp.set(null);
                checkBridgeState();
            }
        }

        private void addListenerToRtpStream() {
            try {
                IncomingRtpStream _rtp = inRtp.get();
                if (_rtp!=null)
                    _rtp.addDataSourceListener(this, null);
            } catch (RtpStreamException ex) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(logMess("Error adding rtp data source listener"), ex);
                state = ConnectionState.INVALID;
                checkBridgeState();
            }
        }
    }
}
