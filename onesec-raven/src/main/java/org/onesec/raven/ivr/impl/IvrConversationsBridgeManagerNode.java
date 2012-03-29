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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.onesec.raven.ivr.IvrConversationBridgeExeption;
import org.onesec.raven.ivr.IvrConversationsBridge;
import org.onesec.raven.ivr.IvrConversationsBridgeListener;
import org.onesec.raven.ivr.IvrConversationsBridgeManager;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.annotations.NodeClass;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class IvrConversationsBridgeManagerNode extends BaseNode 
        implements IvrConversationsBridgeManager, IvrConversationsBridgeListener, Viewable
{
    private Set<IvrConversationsBridge> bridges;
    private ReadWriteLock lock;

    @Message
    private static String phoneNumberMessage;
    @Message
    private static String bridgeStatusMessage;
    @Message
    private static String conversationStatusMessage;
    @Message
    private static String createdTimestampMessage;
    @Message
    private static String activatingTimestampMessage;
    @Message
    private static String activatedTimestampMessage;
    
    @Override
    protected void initFields()
    {
        super.initFields();
        bridges = new HashSet<IvrConversationsBridge>();
        lock = new ReentrantReadWriteLock();
    }

    public IvrConversationsBridge createBridge(IvrEndpointConversation conv1, IvrEndpointConversation conv2,
            String logPrefix)
        throws IvrConversationBridgeExeption
    {
        lock.writeLock().lock();
        try {
            IvrConversationsBridge bridge = new IvrConversationsBridgeImpl(conv1, conv2, this, logPrefix);
            bridges.add(bridge);
            bridge.addBridgeListener(this);
            return bridge;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void bridgeActivated(IvrConversationsBridge bridge) { 
        for (IvrConversationsBridgeListener listener: getBridgeListeners())
            listener.bridgeActivated(bridge);
    }

    public void bridgeReactivated(IvrConversationsBridge bridge) { 
        for (IvrConversationsBridgeListener listener: getBridgeListeners())
            listener.bridgeReactivated(bridge);
    }

    public void bridgeDeactivated(IvrConversationsBridge bridge) {
        lock.writeLock().lock();
        try {
            bridges.remove(bridge);
        } finally {
            lock.writeLock().unlock();
        }
        for (IvrConversationsBridgeListener listener: getBridgeListeners())
            listener.bridgeDeactivated(bridge);
    }

    /**
     * For test purposes
     */
    Set<IvrConversationsBridge> getBridges() {
        return bridges;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        List orderedBridges = null;
        lock.readLock().lock();
        try {
            orderedBridges = new ArrayList(bridges);
        } finally {
            lock.readLock().unlock();
        }
        Collections.sort(orderedBridges);
        TableImpl table = new TableImpl(new String[]{
            bridgeStatusMessage, phoneNumberMessage+" 1", phoneNumberMessage+" 2",
            conversationStatusMessage+"1", conversationStatusMessage+"2", createdTimestampMessage,
            activatingTimestampMessage, activatedTimestampMessage});
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (IvrConversationsBridge b: bridges)
            table.addRow(new Object[]{
                b.getStatus(),
                b.getConversation1().getCallingNumber(), b.getConversation2().getCallingNumber(),
                b.getConversation1().getState().getIdName(), b.getConversation2().getState().getIdName(),
                formatTs(b.getCreatedTimestamp(), fmt), formatTs(b.getActivatingTimestamp(), fmt),
                formatTs(b.getActivatedTimestamp(), fmt)
            });
        ViewableObject obj = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
        return Arrays.asList(obj);
    }

    private String formatTs(long ts, SimpleDateFormat fmt) {
        return ts==0? "" : fmt.format(new Date(ts));
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }
    
    private List<IvrConversationsBridgeListener> getBridgeListeners() {
        return NodeUtils.extractNodesOfType(getDependentNodes(), IvrConversationsBridgeListener.class);
    }
}
