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
package org.onesec.raven.ivr.conference.impl;

import org.onesec.raven.ivr.conference.ConferenceInitiator;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ConferenceInitiatorNode extends BaseNode implements ConferenceInitiator {
    public final static String NAME = "Initiator";
    
    @Parameter @NotNull
    private String initiatorId;
    @Parameter 
    private String initiatorName;
    @Parameter 
    private String initiatorPhone;
    @Parameter 
    private String initiatorEmail;
    @Parameter(defaultValue = "node.initiatorId+(node.initiatorName? ', '+node.initiatorName:'')",
            valueHandlerType = ExpressionAttributeValueHandlerFactory.TYPE)
    private String nodeTitle;

    public ConferenceInitiatorNode() {
        super(NAME);
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public String getInitiatorPhone() {
        return initiatorPhone;
    }

    public void setInitiatorPhone(String initiatorPhone) {
        this.initiatorPhone = initiatorPhone;
    }

    public String getInitiatorEmail() {
        return initiatorEmail;
    }

    public void setInitiatorEmail(String initiatorEmail) {
        this.initiatorEmail = initiatorEmail;
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }
}
