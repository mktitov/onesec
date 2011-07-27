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
import javax.media.protocol.DataSource;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationListener;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.RtpStreamException;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class IvrConversationsBridgeImpl implements IvrConversationsBridge
{
    private final IvrEndpointConversation conv1;
    private final IvrEndpointConversation conv2;
    private final Node owner;
    private LinkedList<IvrConversationsBridgeListener> listeners;
    private Listener listener1;
    private Listener listener2;
    private int dataSourceCounter;

    public IvrConversationsBridgeImpl(
            IvrEndpointConversation conv1, IvrEndpointConversation conv2, Node owner)
    {
        this.conv1 = conv1;
        this.conv2 = conv2;
        this.owner = owner;
        listeners = new LinkedList<IvrConversationsBridgeListener>();
        dataSourceCounter = 0;
    }

    public synchronized void activateBridge()
    {
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug("Activating bridge");
        listener1 = new Listener(conv1);
        listener2 = new Listener(conv2);
    }

    public void deactivateBridge() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized void addBridgeListener(IvrConversationsBridgeListener listener)
    {
        listeners.add(listener);
    }

    private void fireBridgeDeactivatedEvent()
    {
        for (IvrConversationsBridgeListener listener: listeners)
            listener.bridgeDeactivated(this);
    }
    
    private void conversationStopped(IvrEndpointConversation conv)
    {
        fireBridgeDeactivatedEvent();
    }

    private synchronized void dsCreated(IvrEndpointConversation conv, DataSource dataSource)
    {
        IvrEndpointConversation targetConv = conv==conv1? conv2 : conv1;
        targetConv.getAudioStream().addSource(dataSource);
        dataSourceCounter++;
        if (owner.isLogLevelEnabled(LogLevel.DEBUG))
            owner.getLogger().debug(logMess(
                    "Incoming RTP stream from %s routed to the outgoing RTP of the %s"
                    , conv.getCallingNumber(), targetConv.getCallingNumber()));
        if (dataSourceCounter==2 && owner.isLogLevelEnabled(LogLevel.INFO))
            owner.getLogger().info(logMess("Bridge successfully activated"));
    }

    private String logMess(String message, Object... args)
    {
        return "Bridge. "+conv1.getCallingNumber()+">-<"+conv2.getCallingNumber()+": "
                +String.format(message, args);
    }

    private class Listener implements IncomingRtpStreamDataSourceListener, IvrEndpointConversationListener
    {
        private final IvrEndpointConversation conv;

        public Listener(IvrEndpointConversation conv)
        {
            this.conv = conv;
            conv.addConversationListener(this);
            try {
                conv.getIncomingRtpStream().addDataSourceListener(this, null, null);
            } catch (RtpStreamException ex) {
                conversationStopped(conv);
            }
        }

        public void dataSourceCreated(DataSource dataSource) {
            dsCreated(conv, dataSource);
        }

        public void streamClosing() {
            conversationStopped(conv);
        }

        public void listenerAdded() {
            if (conv.getState().getId()!=IvrEndpointConversationState.TALKING)
                conversationStopped(conv);
        }

        public void conversationStarted() { }

        public void conversationStoped(CompletionCode completionCode) {
            conversationStopped(conv);
        }

        public void conversationTransfered(String address) { }
    }
}
