/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.impl;

import java.util.Collection;
import java.util.Map;
import org.onesec.core.services.Operator;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CCMCallOperatorNode.class)
public class CheckOperatorNumberNode extends AbstractDataSource
{
    public static final String PHONENUMBER_ATTRIBUTE = "phoneNumber";

    @Service
    private static MessagesRegistry messagesRegistry;

    @Service
    private static Operator operator;

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        NodeAttribute attr = attributes.get(PHONENUMBER_ATTRIBUTE);
        boolean result = false;
        String phoneNumber = null;
        if (attr==null || attr.getValue()==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Missed required attribute (%s)", PHONENUMBER_ATTRIBUTE));
        }
        else
        {
            phoneNumber = attr.getValue();
            result = operator.isOperatorNumber(phoneNumber);
        }
        if (result)
            dataConsumer.setData(this, phoneNumber);
        else
            dataConsumer.setData(this, null);

        return true;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttributeImpl attr = new NodeAttributeImpl(
                PHONENUMBER_ATTRIBUTE, String.class, null, null);
        MessageComposer messageComposer = new MessageComposer(messagesRegistry);
        messageComposer.append(messagesRegistry.createMessageKeyForStringValue(
                CheckOperatorNumberNode.class.getName(), PHONENUMBER_ATTRIBUTE));
        attr.setDescriptionContainer(messageComposer);
        consumerAttributes.add(attr);
    }
}
