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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.vmail.NewVMailMessage;
import org.onesec.raven.ivr.vmail.SavableStoredVMailMessage;
import org.onesec.raven.ivr.vmail.StoredVMailMessage;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailBoxDir;
import org.onesec.raven.ivr.vmail.VMailMessage;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSourceViewableObject;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=VMailManagerNode.class)
public class VMailBoxNode extends BaseNode implements VMailBox, Viewable {
    
    public final static String DATE_PATTERN = "yyyyMMdd_HHmmss_SSS";
    
    @NotNull @Parameter(defaultValue="20")
    private Integer maxMessageDuration;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean ignoreNewMessages;
    
    @NotNull @Parameter(defaultValue="7")
    private Integer newMessagesLifeTime;
    
    @NotNull @Parameter(defaultValue="30")
    private Integer savedMessagesLifeTime;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFile greeting;
    
    @Message private static String newMessagesTitle;
    @Message private static String savedMessagesTitle;
    @Message private static String senderPhoneColumnName;
    @Message private static String messageDateColumnName;
    @Message private static String messageFileColumnName;
    @Message private static String messageDatePattern;

    public Integer getMaxMessageDuration() {
        return maxMessageDuration;
    }

    public void setMaxMessageDuration(Integer maxMessageDuration) {
        this.maxMessageDuration = maxMessageDuration;
    }

    public AudioFile getGreeting() {
        return greeting;
    }

    public void setGreeting(AudioFile greeting) {
        this.greeting = greeting;
    }

    public List<String> getOwners() {
        List<VMailBoxNumber> numbers = NodeUtils.getChildsOfType(this, VMailBoxNumber.class);
        if (numbers.isEmpty())
            return Collections.EMPTY_LIST;
        List<String> owners = new ArrayList<String>(numbers.size());
        for (VMailBoxNumber number: numbers)
            owners.add(number.getName());
        return owners;
    }
    
    private VMailManagerNode getManager() {
        return (VMailManagerNode) getEffectiveParent();
    }
    
    private VMailBoxDir getDir() throws Exception {
        return getManager().getVMailBoxDir(this);
    }
    
    private int getMessagesCount(File dir) {
        return !isStarted()? 0 : dir.list().length;
    }

    public int getNewMessagesCount() throws Exception {
        return getMessagesCount(getDir().getNewMessagesDir());
    }

    public List<SavableStoredVMailMessage> getNewMessages() throws Exception {
        List<SavableStoredVMailMessage> messages = new LinkedList<SavableStoredVMailMessage>();
        getMessages(messages, true);
        return messages;
    }

    public int getSavedMessagesCount() throws Exception {
        return getMessagesCount(getDir().getSavedMessagesDir());
    }

    public List<StoredVMailMessage> getSavedMessages() throws Exception {
        List<StoredVMailMessage> messages = new LinkedList<StoredVMailMessage>();
        getMessages(messages, false);
        return messages;
    }

    public void addMessage(NewVMailMessage message) throws Exception {
        if (ignoreNewMessages) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Ignoring new incoming message because of ignoreNewMessage==true");
        } else {
            String filename = createMessageFileName(message);
            File messFile = new File(getDir().getTempDir(), filename);
            boolean boxWasEmpty = getNewMessagesCount()==0;
            InputStream in = message.getAudioSource().getInputStream();
            try {
                FileOutputStream out = new FileOutputStream(messFile);
                try {
                    IOUtils.copy(in, out);
                    messFile.renameTo(new File(getDir().getNewMessagesDir(), filename));
                    if (boxWasEmpty)
                        getManager().getVBoxStatusChannel().pushEvent(
                                this, VMailBoxStatusChannel.EventType.NBOX_BECAME_NON_EMPTY);
                } finally {
                    IOUtils.closeQuietly(out);
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }
    
    public void refreshStatus() throws Exception {
        fireStatusEvent(getNewMessagesCount()==0? VMailBoxStatusChannel.EventType.NBOX_BECAME_EMPTY 
                : VMailBoxStatusChannel.EventType.NBOX_BECAME_NON_EMPTY);
        fireStatusEvent(getSavedMessagesCount()==0? VMailBoxStatusChannel.EventType.SBOX_BECAME_EMPTY
                : VMailBoxStatusChannel.EventType.SBOX_BECAME_NON_EMPTY);
    }
    
    void fireStatusEvent(VMailBoxStatusChannel.EventType eventType) {
        getManager().getVBoxStatusChannel().pushEvent(this, eventType);
    }
    
    private String createMessageFileName(VMailMessage message) {
        return new SimpleDateFormat(DATE_PATTERN).format(message.getMessageDate())+"-"
                              + message.getSenderPhoneNumber()+".wav";
    }
    
    private void getMessages(List messages, boolean isNew) throws Exception {
        if (!isStarted())
            return;
        File[] files = isNew? getDir().getNewMessagesDir().listFiles() : getDir().getSavedMessagesDir().listFiles();
        Arrays.sort(files);
        for (File file: files) {
            String[] elems = FilenameUtils.getBaseName(file.getName()).split("-");
            Date date = new SimpleDateFormat(DATE_PATTERN).parse(elems[0]);
            if (isNew)
                messages.add(new SavableStoredVMailMessageImpl(this, getDir().getSavedMessagesDir(), file, elems[1], date));
            else
                messages.add(new StoredVMailMessageImpl(this, file, elems[1], date));
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(6);
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<br>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+newMessagesTitle+"</b>"));
        vos.add(createTableForMessages(getNewMessages()));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<br>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>"+savedMessagesTitle+"</b>"));
        vos.add(createTableForMessages(getSavedMessages()));
        return vos;
    }
    
    private ViewableObject createTableForMessages(List<? extends StoredVMailMessage> messages) {
        TableImpl table = new TableImpl(new String[]{
            senderPhoneColumnName, messageDateColumnName, messageFileColumnName});
        SimpleDateFormat fmt = new SimpleDateFormat(messageDatePattern);
        for (StoredVMailMessage mess: messages)
            table.addRow(new Object[]{
                mess.getSenderPhoneNumber(), 
                fmt.format(mess.getMessageDate()),
                new DataSourceViewableObject(mess.getAudioSource(), "audio/wav", this, createMessageFileName(mess))
            });
        return new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public Boolean getIgnoreNewMessages() {
        return ignoreNewMessages;
    }

    public void setIgnoreNewMessages(Boolean ignoreNewMessages) {
        this.ignoreNewMessages = ignoreNewMessages;
    }

    public Integer getNewMessagesLifeTime() {
        return newMessagesLifeTime;
    }

    public void setNewMessagesLifeTime(Integer newMessagesLifeTime) {
        this.newMessagesLifeTime = newMessagesLifeTime;
    }

    public Integer getSavedMessagesLifeTime() {
        return savedMessagesLifeTime;
    }

    public void setSavedMessagesLifeTime(Integer savedMessagesLifeTime) {
        this.savedMessagesLifeTime = savedMessagesLifeTime;
    }
}
