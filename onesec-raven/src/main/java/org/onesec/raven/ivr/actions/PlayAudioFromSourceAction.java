/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.actions;

import javax.script.Bindings;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.raven.BindingNames;
import org.raven.conv.ConversationScenarioState;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioFromSourceAction extends AsyncAction {
    
    private final static String NAME = "Play audio from source";
    private final PlayAudioFromSourceActionNode owner;

    public PlayAudioFromSourceAction(PlayAudioFromSourceActionNode owner) {
        super(NAME);
        this.owner = owner;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        BindingSupport bindingSupport = owner.getBindingSupport();
        try {
            ConversationScenarioState state = conversation.getConversationScenarioState();
            Bindings bindings = state.getBindings();
            bindingSupport.putAll(bindings);
            DataContext context = (DataContext) bindings.get(BindingNames.DATA_CONTEXT_BINDING);
            if (context==null)
                context = new DataContextImpl();
            Object data = owner.getFieldValue(context);
            if (!(data instanceof InputStreamSource)) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error("Received null or data of invalid type: "+data);
                return;
            }
            IvrUtils.playAudioInAction(this, conversation, (InputStreamSource)data);
        } finally {
            bindingSupport.reset();
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }
    
}
