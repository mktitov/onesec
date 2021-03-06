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
package org.onesec.raven.ivr.impl;

import org.onesec.raven.ivr.IvrDtmfReceivedConversationEvent;
import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
public class IvrDtmfReceivedConversationEventImpl extends IvrEndpointConversationEventImpl 
    implements IvrDtmfReceivedConversationEvent
{
    private final char dtmf;

    public IvrDtmfReceivedConversationEventImpl(IvrEndpointConversation conversation, char dtmf) {
        super(conversation);
        this.dtmf = dtmf;
    }

    public char getDtmf() {
        return dtmf;
    }
}
