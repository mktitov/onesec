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

package org.onesec.raven.ivr.actions;

import java.nio.charset.Charset;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.SendMessageDirection;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class SendMessageActionNode extends AbstractActionNode
{
    @NotNull @Parameter(defaultValue="windows-1251")
    private Charset encoding;

    @NotNull @Parameter
    private String message;

    @NotNull @Parameter
    private SendMessageDirection sendDirection;

    @Override
    protected Action doCreateAction() {
        return new SendMessageAction(this, bindingSupport);
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SendMessageDirection getSendDirection() {
        return sendDirection;
    }

    public void setSendDirection(SendMessageDirection sendDirection) {
        this.sendDirection = sendDirection;
    }
}
