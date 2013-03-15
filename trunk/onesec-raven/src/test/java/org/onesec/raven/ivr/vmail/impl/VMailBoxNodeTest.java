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
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.activation.FileDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;

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
}
