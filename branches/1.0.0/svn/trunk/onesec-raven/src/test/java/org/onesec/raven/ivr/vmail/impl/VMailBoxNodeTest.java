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
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.activation.FileDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.vmail.SavableStoredVMailMessage;
import org.onesec.raven.ivr.vmail.StoredVMailMessage;

/**
 *
 * @author Mikhail Titov
 */
public class VMailBoxNodeTest extends OnesecRavenTestCase {
    
    private File base = new File("target/vmailboxes");
    private File testFile = new File("target/test_file");

    private VMailManagerNode manager;
    private VMailBoxNode vbox;
    
    @Before
    public void prepare() throws IOException {
        if (base.exists())
            FileUtils.deleteDirectory(base);
        if (!testFile.exists())
            testFile.delete();
        FileUtils.writeStringToFile(testFile, "1234");
        manager = new VMailManagerNode();
        manager.setName("vmail manager");
        testsNode.addAndSaveChildren(manager);
        manager.setBasePath(base.getPath());
        assertTrue(manager.start());
        
        vbox = new VMailBoxNode();
        vbox.setName("vbox");
        manager.addAndSaveChildren(vbox);
        assertTrue(vbox.start());
    }
    
    @Test
    public void addMessageTest() throws Exception {
        Date date = new Date();
        vbox.addMessage(new NewVMailMessageImpl("123", "222", date, new FileDataSource(testFile)));
        String filename = new SimpleDateFormat(VMailBoxNode.DATE_PATTERN).format(date)+"-"+"222.wav";
        File messFile = new File(manager.getVMailBoxDir(vbox).getNewMessagesDir()+File.separator+filename);
        assertTrue(messFile.exists());
        assertTrue(FileUtils.contentEquals(testFile, messFile));
    }
    
    @Test
    public void getNewMessagesCount() throws Exception {
        assertEquals(0, vbox.getNewMessagesCount());
        vbox.addMessage(new NewVMailMessageImpl("123", "222", new Date(), new FileDataSource(testFile)));
        assertEquals(1, vbox.getNewMessagesCount());
        vbox.addMessage(new NewVMailMessageImpl("123", "333", new Date(), new FileDataSource(testFile)));
        assertEquals(2, vbox.getNewMessagesCount());
    }
    
    @Test
    public void getNewMessages() throws Exception {
        assertTrue(vbox.getNewMessages().isEmpty());
        Date messDate = new Date();
        vbox.addMessage(new NewVMailMessageImpl("123", "333", messDate, new FileDataSource(testFile)));
        assertEquals(1, vbox.getNewMessages().size());
        List<SavableStoredVMailMessage> messages = vbox.getNewMessages();
        SavableStoredVMailMessage mess = messages.get(0);
        assertEquals(messDate, mess.getMessageDate());
        assertEquals("333", mess.getSenderPhoneNumber());
        assertTrue(IOUtils.contentEquals(new FileInputStream(testFile), mess.getAudioSource().getInputStream()));
        
        Thread.sleep(10);
        vbox.addMessage(new NewVMailMessageImpl("123", "222", new Date(), new FileDataSource(testFile)));
        Thread.sleep(10);
        vbox.addMessage(new NewVMailMessageImpl("123", "111", new Date(), new FileDataSource(testFile)));
        
        messages = vbox.getNewMessages();
        assertEquals(3, messages.size());
        assertEquals("333", messages.get(0).getSenderPhoneNumber());
        assertEquals("222", messages.get(1).getSenderPhoneNumber());
        assertEquals("111", messages.get(2).getSenderPhoneNumber());
    }
    
    @Test
    public void saveNewMessageTest() throws Exception {
        Date date = new Date();
        vbox.addMessage(new NewVMailMessageImpl("123", "222", date, new FileDataSource(testFile)));
        
        vbox.getNewMessages().get(0).save();
        String filename = new SimpleDateFormat(VMailBoxNode.DATE_PATTERN).format(date)+"-"+"222.wav";
        File savedMessFile = new File(manager.getVMailBoxDir(vbox).getSavedMessagesDir()+File.separator+filename);
        assertTrue(savedMessFile.exists());
        assertTrue(FileUtils.contentEquals(testFile, savedMessFile));
        assertEquals(0, vbox.getNewMessagesCount());
        assertEquals(1, vbox.getSavedMessagesCount());
    }
    
    @Test
    public void deleteNewMessageTest() throws Exception {
        vbox.addMessage(new NewVMailMessageImpl("123", "222", new Date(), new FileDataSource(testFile)));
        assertEquals(1, vbox.getNewMessagesCount());
        vbox.getNewMessages().get(0).delete();
        assertEquals(0, vbox.getNewMessagesCount());
    }

    @Test
    public void getSavedMessagesTest() throws Exception {
        Date messDate = new Date();
        vbox.addMessage(new NewVMailMessageImpl("123", "333", messDate, new FileDataSource(testFile)));
        Thread.sleep(10);
        vbox.addMessage(new NewVMailMessageImpl("123", "222", new Date(), new FileDataSource(testFile)));
        Thread.sleep(10);
        vbox.addMessage(new NewVMailMessageImpl("123", "111", new Date(), new FileDataSource(testFile)));
        assertEquals(0, vbox.getSavedMessagesCount());
        for (SavableStoredVMailMessage mess: vbox.getNewMessages())
            mess.save();
        assertEquals(3, vbox.getSavedMessagesCount());
        List<StoredVMailMessage> messages = vbox.getSavedMessages();
        StoredVMailMessage mess = messages.get(0);
        assertEquals(messDate, mess.getMessageDate());
        assertEquals("333", mess.getSenderPhoneNumber());
        assertTrue(IOUtils.contentEquals(new FileInputStream(testFile), mess.getAudioSource().getInputStream()));
        assertEquals("222", messages.get(1).getSenderPhoneNumber());
        assertEquals("111", messages.get(2).getSenderPhoneNumber());
    }
}
