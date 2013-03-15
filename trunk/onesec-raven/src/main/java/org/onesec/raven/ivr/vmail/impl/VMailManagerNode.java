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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailBoxDir;
import org.onesec.raven.ivr.vmail.VMailManager;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.GroupNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=GroupNode.class)
public class VMailManagerNode extends BaseNode implements VMailManager {
    public static final String NEW_MESSAGES_DIR = "new";
    public static final String SAVED_MESSAGED_DIR = "saved";
    
    @NotNull @Parameter
    private String basePath;
    
    private Map<String, VMailBoxNode> vmailBoxes;
    private File basePathFile;

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
        basePathFile = getOrCreatePath(basePath);
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
    
    public VMailBoxDir getVMailBoxDir(VMailBoxNode vbox) throws Exception {
        return new VMailBoxDirImpl(
                getOrCreatePath(mkPath(vbox.getId(), NEW_MESSAGES_DIR)),
                getOrCreatePath(mkPath(vbox.getId(), SAVED_MESSAGED_DIR)));
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

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
