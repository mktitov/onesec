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
package org.onesec.raven.ivr.queue.actions;

import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class SayTimeLeftForAnswerActionNode extends BaseNode {
    
    public final static String NODE1_NAME = "say operator will answer";
    public final static String NODE2_NAME = "say number";
    public final static String NODE3_NAME = "say minutes";
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean recreateChildNodes;
    
    @NotNull @Parameter(defaultValue="60")
    private Integer minRepeatInterval;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (recreateChildNodes)
            createChildNodes();
    }
    
    private void createChildNodes() {
        recreateChildNodes = false;
    }
    
    private void createAudioNode(String name, String resourcePath) throws Exception {
        if (getChildren(name)==null) {
            PlayAudioActionNode node = new PlayAudioActionNode();
            node.setName(name);
            addAndSaveChildren(node);
            NodeAttribute attr = node.getNodeAttribute(PlayAudioActionNode.AUDIO_FILE_ATTR);
            attr.setValueHandlerType(ResourceReferenceValueHandlerFactory.TYPE);
            attr.setValue(resourcePath);
        }
    }

    public Boolean getRecreateChildNodes() {
        return recreateChildNodes;
    }

    public void setRecreateChildNodes(Boolean recreateChildNodes) {
        this.recreateChildNodes = recreateChildNodes;
    }

    public Integer getMinRepeatInterval() {
        return minRepeatInterval;
    }

    public void setMinRepeatInterval(Integer minRepeatInterval) {
        this.minRepeatInterval = minRepeatInterval;
    }
}
