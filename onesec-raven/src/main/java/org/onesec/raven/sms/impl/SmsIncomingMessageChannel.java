/*
 * Copyright 2015 Mikhail Titov.
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
package org.onesec.raven.sms.impl;

import java.util.Collection;
import org.onesec.raven.sms.queue.InQueue;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;
import static org.onesec.raven.sms.impl.IncomingSmsRecordSchemaNode.*;
/**
 *
 * @author Mikhail Titov
 */
public class SmsIncomingMessageChannel extends AbstractDataSource {
    public final static String NAME = "Incoming messages channel";
    
    @NotNull @Parameter(valueHandlerType = RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode incomingShortMessageSchema;

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context) throws Exception {
        throw new UnsupportedOperationException("Pull operation not supported by this data source");
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
    }

    public RecordSchemaNode getIncomingShortMessageSchema() {
        return incomingShortMessageSchema;
    }

    public void setIncomingShortMessageSchema(RecordSchemaNode incomingShortMessageSchema) {
        this.incomingShortMessageSchema = incomingShortMessageSchema;
    }
    
    public void sendMessage(InQueue.MessagePart message) {
        if (isStarted()) {
            try {
                Record rec = incomingShortMessageSchema.createRecord();
                rec.setValue(MESSAGE_ID, message.getMessageId());
                rec.setValue(MESSAGE_SEG_COUNT, message.getMessageSegCount());
//                rec.setValue(SEQUENCE_NUMBER, message.get)
                rec.setValue(SRC_ADDRESS, message.getSrcAddress());
                rec.setValue(SRC_NPI, message.getSrcNpi());
                rec.setValue(SRC_TON, message.getSrcTon());
                rec.setValue(DST_ADDRESS, message.getDstAddress());
                rec.setValue(DST_NPI, message.getDstNpi());
                rec.setValue(DST_TON, message.getDstTon());
                rec.setValue(MESSAGE, message.getMessage());
                rec.setValue(RECEIVE_TS, message.getReceiveTs());
                DataSourceHelper.sendDataToConsumers(this, rec, new DataContextImpl(), (DataConsumer)getParent());
            } catch (RecordException e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error sending incoming SMS record", e);
            }
        }
    }
}
