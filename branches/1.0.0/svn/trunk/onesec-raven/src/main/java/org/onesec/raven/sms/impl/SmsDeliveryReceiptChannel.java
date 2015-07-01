/*
 * Copyright 2014 Mikhail Titov.
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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.InvisibleNode;
import org.weda.annotations.constraints.NotNull;
import static org.onesec.raven.sms.impl.SmsDeliveryReceiptRecordSchemaNode.*;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.log.LogLevel;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class)
public class SmsDeliveryReceiptChannel extends AbstractDataSource {
    public final static String NAME = "Delivery receipt channel";
    
    @NotNull @Parameter(valueHandlerType = RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode deliveryReceiptSchema;
    
    @NotNull @Parameter(defaultValue = "id:(?<messageId>(?:\\d|\\p{Alpha})+).*submit date:(?<submitDate>\\d+) done date:(?<doneDate>\\d+) stat:(?<status>\\p{Alpha}+) err:(?<errorCode>(?:\\d|\\p{Alpha})+).*")
    private String pattern;
    
    private Pattern regexpPattern;

    public SmsDeliveryReceiptChannel() {
        super(NAME);
    }

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context) throws Exception {
        throw new UnsupportedOperationException("Pull operation not supported by this data source");
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        regexpPattern = Pattern.compile(pattern);
        
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
    }
    
//    public void sendReport(String messageId, Date submitDate, Date doneDate, String status, String errCode) 
    public void sendReport(String report) 
    {
        if (!isStarted()) 
            return;
        try {
            Matcher matcher = regexpPattern.matcher(report);
            if (matcher.matches()) {
                Record rec = deliveryReceiptSchema.createRecord();
                SimpleDateFormat fmt = new SimpleDateFormat("yyMMddHHmm");
                String messageId = matcher.group(MESSAGE_ID);
                if (StringUtils.isNumeric(messageId))
                    rec.setValue(MESSAGE_ID, Long.toHexString(new Long(messageId)));
                else
                    rec.setValue(MESSAGE_ID, messageId);
                rec.setValue(SUBMIT_DATE, fmt.parse(matcher.group(SUBMIT_DATE)));
                rec.setValue(DONE_DATE, fmt.parse(matcher.group(DONE_DATE)));
                rec.setValue(STATUS, matcher.group(STATUS));
                rec.setValue(ERROR_CODE, matcher.group(ERROR_CODE));
                if (isLogLevelEnabled(LogLevel.TRACE))
                    getLogger().trace("Created delivery report record: "+rec);
                DataSourceHelper.sendDataToConsumers(this, rec, new DataContextImpl(), (DataConsumer)getParent());
            } else if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("Can't parse delivery receipt report: {}", report);
        } catch (Exception e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error creating DELIVERY RECEIPT report", e);
        }
    }    
    
//    public Record createRecord() {
//    }

    public RecordSchemaNode getDeliveryReceiptSchema() {
        return deliveryReceiptSchema;
    }

    public void setDeliveryReceiptSchema(RecordSchemaNode deliveryReceiptSchema) {
        this.deliveryReceiptSchema = deliveryReceiptSchema;
    }    

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }    
}
