/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.ivr.vmail.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.onesec.raven.ivr.vmail.NewVMailMessage;
import org.onesec.raven.ivr.vmail.StoredVMailMessage;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailBoxDir;
import org.onesec.raven.ivr.vmail.VMailManager;
import static org.raven.RavenUtils.*;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.GroupNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=GroupNode.class)
public class VMailManagerNode extends BaseNode implements VMailManager, DataPipe, Schedulable {
    public static final String NEW_MESSAGES_DIR = "new";
    public static final String SAVED_MESSAGES_DIR = "saved";
    public static final String TEMP_MESSAGES_DIR = "temp";
    public static final String VMAIL_BOX_TAG = "vmailBox";
    public static final String VMAIL_MESSAGE_TAG = "vmailMessage";
    
    @NotNull @Parameter
    private String basePath;
    
    @NotNull @Parameter
    private DataSource dataSource;
    
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler cleanupScheduler;
    
    private Map<String, VMailBoxNode> vmailBoxes;
    private File basePathFile;
    private VMailBoxStatusChannel statusChannel;

    @Override
    protected void initFields() {
        super.initFields();
        vmailBoxes = null;
        statusChannel = null;
    }

    @Override
    public boolean isStartAfterChildrens() {
        return true;
    }

    @Override
    public boolean isSubtreeListener() {
        return true;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        initNodes(false);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        basePathFile = getOrCreatePath(basePath);
        initNodes(true);
        initVMailBoxes();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        vmailBoxes = null;
    }
    
    private void initNodes(boolean start) {
        statusChannel = (VMailBoxStatusChannel) getNode(VMailBoxStatusChannel.NAME);
        if (statusChannel==null) {
            statusChannel = new VMailBoxStatusChannel();
            addAndSaveChildren(statusChannel);
            if (start)
                statusChannel.start();
        }
    }
    
    public VMailBoxStatusChannel getVBoxStatusChannel() {
        return statusChannel;
    }
    
    public void executeScheduledJob(Scheduler scheduler) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            logger.debug("Cleaning up voice mail messages");
        long curTime = System.currentTimeMillis();
        for (VMailBoxNode vbox: NodeUtils.getEffectiveChildsOfType(this, VMailBoxNode.class)) {
            try {
                deleteMessageIfNeed(curTime, TimeUnit.DAYS.toMillis(vbox.getNewMessagesLifeTime()), 
                        vbox.getNewMessages());
                deleteMessageIfNeed(curTime, TimeUnit.DAYS.toMillis(vbox.getSavedMessagesLifeTime()), 
                        vbox.getSavedMessages());
            } catch (Exception e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(String.format("Error while deleting old messages from vbox (%s)", vbox), e);
            }
        }
            
    }
    
    private void deleteMessageIfNeed(long curTime, long addTime, List<? extends StoredVMailMessage> messages) {
        for (StoredVMailMessage mess: messages)
            try {
                if (curTime > mess.getMessageDate().getTime()+addTime)
                    mess.delete();
            } catch (Exception e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(String.format("Can't delete vmail message (%s)", mess), e);
            }
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (isStarted() && data instanceof NewVMailMessage) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Received new voice mail message. "+data);
            NewVMailMessage message = (NewVMailMessage) data;
            VMailBoxNode vbox = vmailBoxes.get(message.getVMailBoxNumber());
            if (vbox==null) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Voice mail box with number ({}) not found", message.getVMailBoxNumber());
                return;
            }
            try {
                vbox.addMessage(message);
                createAndSendCdr(message, vbox, context);
            } catch (Exception ex) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(String.format("Error adding new message (%s) to voice mail box (%s)", 
                            message, vbox.getPath()), ex);
            }
        }
    }
    
    private void createAndSendCdr(NewVMailMessage message, VMailBoxNode vbox, DataContext context) throws Exception {
        RecordSchema schema = recordSchema;
        if (schema!=null) {
            Record rec = schema.createRecord();
            rec.setTag(VMAIL_BOX_TAG, vbox);
            rec.setTag(VMAIL_MESSAGE_TAG, message.getAudioSource());
            rec.setValues(asMap(
                pair(VMailCdrRecordSchema.VMAIL_BOX_ID, (Object)vbox.getId()),
                pair(VMailCdrRecordSchema.VMAIL_BOX_NUMBER, (Object)message.getVMailBoxNumber()),
                pair(VMailCdrRecordSchema.SENDER_PHONE_NUMBER, (Object)message.getSenderPhoneNumber()),
                pair(VMailCdrRecordSchema.MESSAGE_DATE, (Object)message.getMessageDate())
            ));
            DataSourceHelper.sendDataToConsumers(this, rec, context);
        }
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Pull operation not supported by this data pipe");
    }

    public VMailBox getVMailBox(String phoneNumber) {
        return !isStarted()? null : vmailBoxes.get(phoneNumber);
    }
    
    public VMailBoxDir getVMailBoxDir(VMailBoxNode vbox) throws Exception {
        return new VMailBoxDirImpl(
                getOrCreatePath(mkPath(vbox.getId(), NEW_MESSAGES_DIR)),
                getOrCreatePath(mkPath(vbox.getId(), SAVED_MESSAGES_DIR)),
                getOrCreatePath(mkPath(vbox.getId(), TEMP_MESSAGES_DIR)));
    }
    
    private String mkPath(Object... elems) {
        StringBuilder path = new StringBuilder(basePathFile.getAbsolutePath());
        for (Object elem: elems)
            path.append(File.separator).append(elem);
        return path.toString();
    }
    
    private File getOrCreatePath(String path) throws Exception {
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs())
                throw new IOException("Can't create directory: "+dir.getAbsolutePath());
        } else if (!dir.isDirectory())
            throw new IOException(String.format("File (%s) exists but it is not a directory", 
                    dir.getAbsolutePath()));
        return dir;
    }
    
    private void initVMailBoxes() {
        vmailBoxes = new ConcurrentHashMap<String, VMailBoxNode>();
        for (VMailBoxNode vbox: NodeUtils.getEffectiveChildsOfType(this, VMailBoxNode.class))
            for (VMailBoxNumber vboxnumber: NodeUtils.getChildsOfType(vbox, VMailBoxNumber.class))
                vmailBoxes.put(vboxnumber.getName(), vbox);
    }

    @Override
    public void nodeNameChanged(Node node, String oldName, String newName) {
        super.nodeNameChanged(node, oldName, newName);
        if (node instanceof VMailBoxNumber) {
            VMailBoxNode vbox = vmailBoxes.remove(oldName);
            if (vbox!=null)
                vmailBoxes.put(newName, vbox);
        }
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus) {
        super.nodeStatusChanged(node, oldStatus, newStatus);
        if (isStarted() && (node instanceof VMailBoxNode || node instanceof VMailBoxNumber)) 
            for (Node number: collectVBoxNumbers(node))
                if (newStatus==Node.Status.STARTED) {
                    vmailBoxes.put(number.getName(), (VMailBoxNode)number.getParent());
                } else vmailBoxes.remove(number.getName());
    }

    @Override
    public void nodeMoved(Node node) {
        if (!isStarted())
            return;
        if (node instanceof VMailBoxNode) {
            if (!node.getEffectiveParent().equals(this))
                for (VMailBoxNumber number: NodeUtils.getChildsOfType(node, VMailBoxNumber.class))
                    vmailBoxes.remove(number.getName());            
        } else if (node instanceof VMailBoxNumber) {
            vmailBoxes.remove(node.getName());
            if (node.getParent().getEffectiveParent().equals(this) && node.isStarted())
                vmailBoxes.put(node.getName(), (VMailBoxNode)node.getParent());
        }
    }

    @Override
    public void nodeRemoved(Node removedNode) {
        super.nodeRemoved(removedNode);
        if (!isStarted())
            return;
        if (removedNode instanceof VMailBoxNode || removedNode instanceof VMailBoxNumber) 
            for (Node number: collectVBoxNumbers(removedNode))
                vmailBoxes.remove(number.getName());
    }
    
    private List<Node> collectVBoxNumbers(Node node) {
        List<Node> numbers = new LinkedList<Node>();
        if (node instanceof VMailBoxNode)
            numbers.addAll(NodeUtils.getChildsOfType(node, VMailBoxNumber.class));
        else
            numbers.add(node);
        return numbers;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    public Scheduler getCleanupScheduler() {
        return cleanupScheduler;
    }

    public void setCleanupScheduler(Scheduler cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getStopProcessingOnError() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }    

}
