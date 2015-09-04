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
package org.onesec.raven.ivr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.RecordSchema;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNodeWithBehavior;
import org.raven.tree.impl.LoggerHelper;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CdrGeneratorNode extends BaseNodeWithBehavior implements DataSource {
    @NotNull @Parameter(valueHandlerType = RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchema cdrSchema;
    
    @NotNull @Parameter(defaultValue="CONVERSATION_FINISHED")
    private String cdrSendEvents;
    
    @NotNull @Parameter(defaultValue = "60")
    private Long cdrCompletetionTimeout;
    
    @NotNull @Parameter(defaultValue = "MINUTES")
    private TimeUnit cdrCompletionTimeoutTimeUnit;

    @Override
    protected DataProcessorFacade createBehaviour() {
        String[] strEventTypes = RavenUtils.split(cdrSendEvents); 
        List<CdrGeneratorDP.CallEventType> eventTypes = new ArrayList(strEventTypes.length);
        for (String strEventType: strEventTypes)
            eventTypes.add(CdrGeneratorDP.CallEventType.valueOf(strEventType));
        CdrGeneratorDP dp = new CdrGeneratorDP(this, cdrSchema, EnumSet.copyOf(eventTypes), 
                cdrCompletionTimeoutTimeUnit.toMillis(cdrCompletetionTimeout));        
        return new DataProcessorFacadeConfig(
                    "CDR transmitter", this, dp, getExecutor(), new LoggerHelper(this, "")
                ).build();
        
    }

    public void registerCallEvent(CdrGeneratorDP.CallEvent callEvent) {
        sendMessageToBehavior(callEvent);
    }
    
    public RecordSchema getCdrSchema() {
        return cdrSchema;
    }

    public void setCdrSchema(RecordSchema cdrSchema) {
        this.cdrSchema = cdrSchema;
    }

    public String getCdrSendEvents() {
        return cdrSendEvents;
    }

    public void setCdrSendEvents(String cdrSendEvents) {
        this.cdrSendEvents = cdrSendEvents;
    }

    public Long getCdrCompletetionTimeout() {
        return cdrCompletetionTimeout;
    }

    public void setCdrCompletetionTimeout(Long cdrCompletetionTimeout) {
        this.cdrCompletetionTimeout = cdrCompletetionTimeout;
    }

    public TimeUnit getCdrCompletionTimeoutTimeUnit() {
        return cdrCompletionTimeoutTimeUnit;
    }

    public void setCdrCompletionTimeoutTimeUnit(TimeUnit cdrCompletionTimeoutTimeUnit) {
        this.cdrCompletionTimeoutTimeUnit = cdrCompletionTimeoutTimeUnit;
    }
    
    
    @Parameter(readOnly = true)
    public Integer getActiveCdrRecords() {
        return askBehaviour(CdrGeneratorDP.GET_ACTIVE_CDR_COUNT, null);
    }

    @Parameter(readOnly = true)
    public Long getCompletedCdrRecords() {
        return askBehaviour(CdrGeneratorDP.GET_COMPLETED_CDR_COUNT, null);
    }    

    @Override
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pool operations not supported by this data source");
    }

    @Override
    public Boolean getStopProcessingOnError() {
        return true;
    }

    @Override
    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
}
