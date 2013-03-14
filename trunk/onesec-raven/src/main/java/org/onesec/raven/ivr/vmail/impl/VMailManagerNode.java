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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailManager;
import org.raven.annotations.NodeClass;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.GroupNode;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=GroupNode.class)
public class VMailManagerNode extends BaseNode implements VMailManager {
    
    private Map<String, VMailBoxNode> vmailBoxes;

    @Override
    protected void initFields() {
        super.initFields();
        vmailBoxes = null;
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
    protected void doStart() throws Exception {
        super.doStart();
        initVMailBoxes();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        vmailBoxes = null;
    }

    public VMailBox getVMailBox(String phoneNumber) {
        return !isStarted()? null : vmailBoxes.get(phoneNumber);
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
        if (isStarted() && (node instanceof VMailBoxNode || node instanceof VMailBoxNumber)) {
            List<Node> numbers = new LinkedList<Node>();
            if (node instanceof VMailBoxNode)
                numbers.addAll(NodeUtils.getChildsOfType(node, VMailBoxNumber.class));
            else
                numbers.add(node);
            for (Node number: numbers)
                if (newStatus==Node.Status.STARTED) {
                    vmailBoxes.put(number.getName(), (VMailBoxNode)number.getParent());
                } else vmailBoxes.remove(number.getName());
        }
    }
}
