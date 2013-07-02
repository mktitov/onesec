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

package org.onesec.raven;

import groovy.lang.Closure;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeAttributeListener;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.NodeShutdownError;
import org.raven.tree.NodeTuner;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAdapter implements Node {

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LogLevel getLogLevel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Logger getLogger() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setId(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Status getStatus() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isStarted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isInitialized() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setStatus(Status status) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte getLevel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getIndex() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setIndex(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getParent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setParent(Node parent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setName(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isDynamic() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isChildrensDynamic() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addChildren(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addAndSaveChildren(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeChildren(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void detachChildren(Node node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Node> getChildrens() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Node> getNodes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasNode(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node find(Closure<Boolean> filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Node> findAll(Closure<Boolean> filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getChildrenCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getNodesCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Node> getChildrenList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Node> getSortedChildrens() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isConditionalNode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getEffectiveParent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Node> getEffectiveChildrens() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Node> getEffectiveNodes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getChildren(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getNode(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getNodeById(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getChildrenByPath(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getNodeByPath(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addListener(NodeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeListener(NodeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeListener> getListeners() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Class> getChildNodeTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isContainer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addNodeAttribute(NodeAttribute attr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addAttr(NodeAttribute attr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeAttribute addUniqAttr(String protoAttrName, Object value) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeAttribute addUniqAttr(String protoAttrName, Object value, boolean reuseAttrWithNullValue) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addNodeAttributeDependency(String attributeName, NodeAttributeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeNodeAttributeDependency(String attributeName, NodeAttributeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeNodeAttribute(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeAttr(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> getNodeAttributes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> getAttrs() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasAttr(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeAttribute getNodeAttribute(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeAttribute getAttr(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isInitializeAfterChildrens() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isStartAfterChildrens() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isAutoStart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void init() throws NodeError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void shutdown() throws NodeShutdownError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean start() throws NodeError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stop() throws NodeError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean addDependentNode(Node dependentNode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean removeDependentNode(Node dependentNode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Set<Node> getDependentNodes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeAttribute getParentAttribute(String attributeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NodeAttribute getParentAttr(String attributeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getParentAttributeValue(String attributeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getParentAttrValue(String attributeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T getParentAttributeRealValue(String attributeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T getParentAttrRealValue(String attributeName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isLogLevelEnabled(LogLevel level) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isTemplate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getTemplate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node cloneTo(Node dest, String newNodeName, NodeTuner nodeTuner, boolean useEffectiveChildrens) throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void formExpressionBindings(Bindings bindings) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, Object> getVariables() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int compareTo(Node o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getPath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
