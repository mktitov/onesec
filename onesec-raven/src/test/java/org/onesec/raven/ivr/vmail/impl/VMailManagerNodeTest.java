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
import java.util.Calendar;
import java.util.Date;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.TestSchedulerNode;
import org.onesec.raven.ivr.vmail.VMailBoxDir;
import org.raven.ds.Record;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import org.raven.tree.impl.GroupNode;

/**
 *
 * @author Mikhail Titov
 */
public class VMailManagerNodeTest extends OnesecRavenTestCase {  
    private VMailManagerNode manager;
    private File base = new File("target/vmailboxes");
    private PushDataSource ds;
    
    @Before
    public void prepare() throws Exception {
        if (base.exists())
            FileUtils.deleteDirectory(base);
        
        ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        TestSchedulerNode scheduler = new TestSchedulerNode();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        manager = new VMailManagerNode();
        manager.setName("vmail manager");
        testsNode.addAndSaveChildren(manager);
        manager.setBasePath(base.getPath());
        manager.setDataSource(ds);
        manager.setCleanupScheduler(scheduler);
    }
    
    @Test
    public void initBaseDirTest() {
        assertFalse(base.exists());
        assertTrue(manager.start());
        assertTrue(base.exists());
    }
    
    @Test
    public void initVMailBoxesMapTest() {
        GroupNode group = createGroup(manager, "group");
        VMailBoxNode vbox = createVBox(group, "vbox1", false);
        createVBoxNumber(vbox, "111", true);
        createVBox(group, "vbox2", true);
        VMailBoxNode vbox3 = createVBox(group, "vbox3", false);
        createVBoxNumber(vbox3, "000", false);
        VMailBoxNode vbox4 = createVBox(group, "vbox4", true);
        createVBoxNumber(vbox4, "222", true);
        createVBoxNumber(vbox4, "333", true);
        createVBoxNumber(vbox4, "444", false);
        VMailBoxNode vbox5 = createVBox(manager, "vbox5", true);
        createVBoxNumber(vbox5, "555", true);
        
        assertTrue(manager.start());
        assertNull(manager.getVMailBox("111"));
        assertNull(manager.getVMailBox("000"));
        assertSame(vbox4, manager.getVMailBox("222"));
        assertSame(vbox4, manager.getVMailBox("333"));
        assertNull(manager.getVMailBox("444"));
        assertSame(vbox5, manager.getVMailBox("555"));
    }
    
    @Test
    public void vboxNumberRenameTest() {
        GroupNode group = createGroup(manager, "group");
        VMailBoxNode vbox = createVBox(group, "vbox", true);
        VMailBoxNumber number = createVBoxNumber(vbox, "111", true);
        
        assertTrue(manager.start());
        assertSame(vbox, manager.getVMailBox("111"));
        assertNull(manager.getVMailBox("222"));
        
        number.setName("222");
        assertSame(vbox, manager.getVMailBox("222"));
        assertNull(manager.getVMailBox("111"));
    }
    
    @Test
    public void vmailBoxNodeStatusChangeTest() {
        GroupNode group = createGroup(manager, "group");
        VMailBoxNode vbox = createVBox(group, "vbox", true);
        VMailBoxNumber number = createVBoxNumber(vbox, "111", true);
        assertTrue(manager.start());
        assertSame(vbox, manager.getVMailBox("111"));
        
        vbox.stop();
        assertNull(manager.getVMailBox("111"));
        
        vbox.start();
        assertSame(vbox, manager.getVMailBox("111"));
    }
    
    @Test
    public void vmailBoxRemoveTest() {
        GroupNode group = createGroup(manager, "group");
        VMailBoxNode vbox = createVBox(group, "vbox", true);
        VMailBoxNumber number = createVBoxNumber(vbox, "111", true);
        assertTrue(manager.start());
        assertSame(vbox, manager.getVMailBox("111"));
        
        group.removeChildren(vbox);
        assertNull(manager.getVMailBox("111"));
    }
    
    @Test
    public void vmailBoxNumberRemoveTest() {
        GroupNode group = createGroup(manager, "group");
        VMailBoxNode vbox = createVBox(group, "vbox", true);
        VMailBoxNumber number = createVBoxNumber(vbox, "111", true);
        assertTrue(manager.start());
        assertSame(vbox, manager.getVMailBox("111"));
        
        vbox.removeChildren(number);
        assertNull(manager.getVMailBox("111"));
    }
    
    @Test
    public void vmailBoxNumberMoveTest() throws Exception {
        GroupNode group = createGroup(manager, "group");
        VMailBoxNode vbox = createVBox(group, "vbox", true);
        VMailBoxNode vbox2 = createVBox(group, "vbox2", true);
        VMailBoxNumber number = createVBoxNumber(vbox, "111", true);        
        assertTrue(manager.start());
        assertSame(vbox, manager.getVMailBox("111"));
        
        tree.move(number, vbox2, null);
        assertSame(vbox2, manager.getVMailBox("111"));
    }
    
    @Test
    public void getVMailBoxDirTest() throws Exception {
        VMailBoxNode vbox = createVBox(manager, "vbox", true);
        assertTrue(manager.start());
        VMailBoxDir vboxDir = manager.getVMailBoxDir(vbox);
        assertNotNull(vboxDir);
        assertNotNull(vboxDir.getNewMessagesDir());
        assertNotNull(vboxDir.getSavedMessagesDir());
        File newDir = new File(base.getAbsolutePath()+"/"+vbox.getId()+"/new");
        assertTrue(newDir.exists());
        assertEquals(newDir, vboxDir.getNewMessagesDir());
        File savedDir = new File(base.getAbsolutePath()+"/"+vbox.getId()+"/saved");
        assertTrue(savedDir.exists());
        assertEquals(savedDir, vboxDir.getSavedMessagesDir());
    }
    
    @Test
    public void pushNewMessageTest() throws Exception {
        File testFile = createTestFile();
        
        VMailBoxNode vbox = createVBox(manager, "vbox1", true);
        createVBoxNumber(vbox, "111", true);
        assertTrue(manager.start());
        
        assertEquals(0, vbox.getNewMessagesCount());
        ds.pushData(new NewVMailMessageImpl("111", "222", new Date(), new FileDataSource(testFile)));
        assertEquals(1, vbox.getNewMessagesCount());
    }
    
    @Test
    public void sendCdrTest() throws Exception {
        DataCollector collector = createDataCollector();
        VMailCdrRecordSchema schema = createRecordSchema();
        manager.setRecordSchema(schema);
        
        VMailBoxNode vbox = createVBox(manager, "vbox1", true);
        createVBoxNumber(vbox, "111", true);
        assertTrue(manager.start());
        
        File testFile = createTestFile();
        Date messageDate = new Date();
        ds.pushData(new NewVMailMessageImpl("111", "222", messageDate, new FileDataSource(testFile)));
        
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof Record);
        Record rec = (Record) collector.getDataList().get(0);
        assertSame(vbox, rec.getTag(VMailManagerNode.VMAIL_BOX_TAG));
        assertTrue(rec.getTag(VMailManagerNode.VMAIL_MESSAGE_TAG) instanceof DataSource);
        assertEquals(new Long(vbox.getId()), rec.getValue(VMailCdrRecordSchema.VMAIL_BOX_ID));
        assertEquals("111", rec.getValue(VMailCdrRecordSchema.VMAIL_BOX_NUMBER));
        assertEquals("222", rec.getValue(VMailCdrRecordSchema.SENDER_PHONE_NUMBER));
        assertEquals(messageDate, rec.getValue(VMailCdrRecordSchema.MESSAGE_DATE));
    }
    
    @Test
    public void cleanupOldMessagesTest() throws Exception {
        VMailBoxNode vbox = createVBox(manager, "vbox1", true);
        createVBoxNumber(vbox, "111", true);
        vbox.setNewMessagesLifeTime(1);
        vbox.setSavedMessagesLifeTime(2);
        assertTrue(manager.start());
        
        File testFile = createTestFile();
        ds.pushData(new NewVMailMessageImpl("111", "222", new Date(), new FileDataSource(testFile)));
        vbox.getNewMessages().get(0).save();
        ds.pushData(new NewVMailMessageImpl("111", "333", minusDays(new Date(), 3), new FileDataSource(testFile)));
        vbox.getNewMessages().get(0).save();
        ds.pushData(new NewVMailMessageImpl("111", "333", new Date(), new FileDataSource(testFile)));
        ds.pushData(new NewVMailMessageImpl("111", "444", minusDays(new Date(), 2), new FileDataSource(testFile)));
        
        assertEquals(2, vbox.getNewMessagesCount());
        assertEquals(2, vbox.getSavedMessagesCount());
        manager.executeScheduledJob(null);
        assertEquals(1, vbox.getNewMessagesCount());
        assertEquals(1, vbox.getSavedMessagesCount());
        assertEquals("222", vbox.getSavedMessages().get(0).getSenderPhoneNumber());
        assertEquals("333", vbox.getNewMessages().get(0).getSenderPhoneNumber());
    }
    
    private Date minusDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, -days);
        cal.add(Calendar.MINUTE, -1);
        return cal.getTime();
    }
    
    private GroupNode createGroup(Node owner, String name) {
        GroupNode group = new GroupNode();
        group.setName(name);
        owner.addAndSaveChildren(group);
        assertTrue(group.start());
        return group;
    }
    
    private VMailBoxNode createVBox(Node owner, String name, boolean start) {
        VMailBoxNode vbox = new VMailBoxNode();
        vbox.setName(name);
        owner.addAndSaveChildren(vbox);
        if (start)
            assertTrue(vbox.start());
        return vbox;
    }
    
    private VMailBoxNumber createVBoxNumber(Node owner, String name, boolean start) {
        VMailBoxNumber number = new VMailBoxNumber();
        number.setName(name);
        owner.addAndSaveChildren(number);
        if (start)
            assertTrue(number.start());
        return number;
    }

    private File createTestFile() throws IOException {
        File testFile = new File("target/test_file");
        if (!testFile.exists())
            testFile.delete();
        FileUtils.writeStringToFile(testFile, "1234");
        return testFile;
    }

    private DataCollector createDataCollector() throws NodeError {
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(manager);
        assertTrue(collector.start());
        return collector;
    }

    private VMailCdrRecordSchema createRecordSchema() throws NodeError {
        VMailCdrRecordSchema schema = new VMailCdrRecordSchema();
        schema.setName("schema");
        testsNode.addAndSaveChildren(schema);
        assertTrue(schema.start());
        return schema;
    }
    
}
