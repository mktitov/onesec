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
package org.onesec.raven.ivr.actions;

import org.onesec.raven.ivr.IvrEndpointConversation;

/**
 *
 * @author Mikhail Titov
 */
public class SendDTMFAction extends AsyncAction {
    public final static String NAME = "Send DTMF";
    
    private final String dtmfs;

    public SendDTMFAction(String dtmfs) {
        super(NAME);
        this.dtmfs = dtmfs;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("Sending DTMFs: "+dtmfs);
        conversation.sendDTMF(dtmfs);
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
