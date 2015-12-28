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

import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.BindingNames;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.BindingSupport;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioFromSourceAction extends AbstractPlayAudioAction
{
    
    private final PlayAudioFromSourceActionNode owner;

    public PlayAudioFromSourceAction(PlayAudioFromSourceActionNode owner, TypeConverter converter) {
        super("Play from source: "+owner.getDataSource().getName(), converter);
        this.owner = owner;
    }

////    public PlayAudioFromSourceAction(PlayAudioFromSourceActionNode owner, TypeConverter converter) {
////        super(NAME);
////        this.owner = owner;
////        this.converter = converter;
////    }
////
////    @Override?
//    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
//        BindingSupport bindingSupport = owner.getBindingSupport();
//        try {
//            ConversationScenarioState state = conversation.getConversationScenarioState();
//            Bindings bindings = state.getBindings();
//            bindingSupport.putAll(bindings);
//            DataContext context = (DataContext) bindings.get(BindingNames.DATA_CONTEXT_BINDING);
//            if (context==null)
//                context = new DataContextImpl();
//            InputStreamSource data = converter.convert(InputStreamSource.class, owner.getFieldValue(context), null);
//            if (data==null) {
//                if (owner.isLogLevelEnabled(LogLevel.ERROR))
//                    owner.getLogger().error("Received null data");
//            } else IvrUtils.playAudioInAction(this, conversation, data);
//        } finally {
//            bindingSupport.reset();
//        }
//    }

    @Override
    protected Object getAudio(IvrEndpointConversation conversation) throws Exception {
        DataContext context = (DataContext) conversation.getConversationScenarioState().getBindings().get(
                BindingNames.DATA_CONTEXT_BINDING);
        if (context==null)
            context = new DataContextImpl();
        return owner.getFieldValue(context);
    }

    @Override
    protected BindingSupport getBindingSupport() {
        return owner.getBindingSupport();
    }
}
