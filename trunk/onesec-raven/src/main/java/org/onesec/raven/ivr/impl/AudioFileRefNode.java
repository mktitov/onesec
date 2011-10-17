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

import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.onesec.raven.ivr.AudioFile;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.DataFile;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class AudioFileRefNode extends BaseNode implements AudioFile, Viewable
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFile audioFileNode;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public BindingSupport getBindingSupport() {
        return bindingSupport;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
        throws Exception
    {
        return Status.STARTED.equals(getStatus()) && audioFileNode instanceof Viewable?
            ((Viewable)audioFileNode).getViewableObjects(refreshAttributes) : null;
    }

    public long getCacheChecksum() {
        return audioFileNode.getCacheChecksum();
    }

    public String getCacheKey() {
        return audioFileNode.getCacheKey();
    }

    public boolean isCacheable() {
        return audioFileNode.isCacheable();
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public AudioFile getAudioFileNode() {
        return audioFileNode;
    }

    public void setAudioFileNode(AudioFile audioFileNode) {
        this.audioFileNode = audioFileNode;
    }

    public DataFile getAudioFile() {
        return audioFileNode.getAudioFile();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
}
