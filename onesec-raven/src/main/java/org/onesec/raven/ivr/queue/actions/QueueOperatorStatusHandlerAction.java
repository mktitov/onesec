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
package org.onesec.raven.ivr.queue.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.script.Bindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AbstractSayWordsAction;
import org.onesec.raven.ivr.queue.impl.CallsQueueOperatorNode;
import org.raven.conv.BindingScope;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
public class QueueOperatorStatusHandlerAction extends AbstractSayWordsAction {
    
    public final static String HELLO_PLAYED_BINDING = "helloPlayedFlag";
    
    private final static String ACTION_NAME = "Queue operator status";
    private final QueueOperatorStatusHandlerActionNode actionNode;
    

    public QueueOperatorStatusHandlerAction(QueueOperatorStatusHandlerActionNode actionNode) {
        super(ACTION_NAME, null, 0, 0, null);
        this.actionNode = actionNode;
    }

    @Override
    protected List<Object> formWords(IvrEndpointConversation conv) throws Exception {
        BindingSupport bindings = actionNode.getBindingSupport();
        try {
            Bindings convBindings = conv.getConversationScenarioState().getBindings();
            bindings.putAll(convBindings);
            String operNumber = (String) convBindings.get(IvrEndpointConversation.NUMBER_BINDING);
            CallsQueueOperatorNode oper = actionNode.getCallsQueues().getOperatorByPhoneNumber(operNumber);
            if (oper==null)
                return null;
            boolean active = oper.getActive()!=null && oper.getActive();
//            getLogger().debug("DTMF: "+convBindings.get(IvrEndpointConversation.DTMF_BINDING));
//            getLogger().debug("active: "+active);
            if ("1".equals(convBindings.get(IvrEndpointConversation.DTMF_BINDING))) 
                oper.setActive(!active);
            //сформируем последовательность аудио файлов
            List audioFiles = new ArrayList<>(3);
            if (!convBindings.containsKey(HELLO_PLAYED_BINDING)) {
                audioFiles.add(actionNode.getHelloAudio());
                conv.getConversationScenarioState().setBinding(HELLO_PLAYED_BINDING, true, BindingScope.POINT);
            }
            audioFiles.add(actionNode.getCurrentStatusAudio());
            if (oper.getActive()==null || !oper.getActive())
                audioFiles.add(actionNode.getUnavailableStatusAudio());
            else
                audioFiles.add(actionNode.getAvailableStatusAudio());
            audioFiles.add(actionNode.getPressOneToChangeStatusAudio());
            
            return Arrays.asList((Object)audioFiles);
        } finally {
            bindings.reset();
        }
    }
}
