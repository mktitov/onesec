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
package org.onesec.raven.ivr.conference.actions;

import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.actions.AbstractActionNode;
import org.onesec.raven.ivr.conference.impl.ConferenceManagerNode;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class JoinToConferenceActionNode extends AbstractActionNode {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private ConferenceManagerNode conferenceManager;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean autoConnect;

    @Override
    protected IvrAction doCreateAction() {
        return new JoinToConferenceAction(conferenceManager, autoConnect);
    }

    public ConferenceManagerNode getConferenceManager() {
        return conferenceManager;
    }

    public void setConferenceManager(ConferenceManagerNode conferenceManager) {
        this.conferenceManager = conferenceManager;
    }

    public Boolean getAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(Boolean autoConnect) {
        this.autoConnect = autoConnect;
    }
}
