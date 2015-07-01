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
package org.onesec.raven.ivr.vmail.actions;

import java.util.Date;
import javax.activation.DataSource;
import javax.script.Bindings;
import org.onesec.raven.Constants;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.StartRecordingAction;
import org.onesec.raven.ivr.actions.StartRecordingActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.vmail.NewVMailMessage;
import org.onesec.raven.ivr.vmail.impl.NewVMailMessageImpl;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataSourceHelper;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class StartVMailRecordingActionNode extends StartRecordingActionNode implements Constants {

    @Override
    protected boolean saveOnCancel() {
        return true;
    }

    @Override
    protected void sendDataToConsumers(DataSource data, DataContext context) {
        Bindings convBindings = (Bindings) context.getAt(StartRecordingAction.CONVERSATION_BINDING);
        String vboxNumber = (String) convBindings.get(VMAIL_BOX_NUMBER);
        String callingNumber = (String) convBindings.get(IvrEndpointConversation.NUMBER_BINDING);
        NewVMailMessage message = new NewVMailMessageImpl(vboxNumber, callingNumber, new Date(), data);
        DataSourceHelper.sendDataToConsumers(this, message, context);
    }
}
