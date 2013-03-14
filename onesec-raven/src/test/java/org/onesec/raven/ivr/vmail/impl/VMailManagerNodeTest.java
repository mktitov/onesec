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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.GroupNode;

/**
 *
 * @author Mikhail Titov
 */
public class VMailManagerNodeTest extends OnesecRavenTestCase {
    
    private VMailManagerNode manager;
    
    @Before
    public void prepare() {
        manager = new VMailManagerNode();
        manager.setName("vmail manager");
        testsNode.addAndSaveChildren(manager);
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
    
}
