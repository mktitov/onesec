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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.RavenRuntimeException;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.sched.ExecutorServiceException;
import static org.onesec.raven.ivr.impl.CallCdrRecordSchemaNode.*;
/**
 *
 * @author Mikhail Titov
 */
public class SendCallCdrDP extends AbstractDataProcessorLogic {

    public enum CallEventType{CONVERSATION_INITIALIZED, CONVERSATION_STARTED, CONVERSATION_FINISHED};
    
    public final static String GET_CDRS = "GET_FORMING_CDRS";
    public final static String CHECK_CDR_COMPLETE_TIMEOUT = "CHECK_CDR_COMPLETE_TIMEOUT";
    public final static long CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL = 60_0000;
    
    private final DataSource sender;
    private final RecordSchema cdrSchema;
    private final EnumSet<CallEventType> enabledEvent;
    private final long cdrCompleteTimeout;

    private final Map<IvrEndpointConversation, Record> cdrs = new HashMap<>();

    public SendCallCdrDP(DataSource sender, RecordSchema cdrSchema, EnumSet<CallEventType> enabledEvent, 
            long cdrCompleteTimeout) 
    {
        this.sender = sender;
        this.cdrSchema = cdrSchema;
        this.enabledEvent = enabledEvent;
        this.cdrCompleteTimeout = cdrCompleteTimeout;
    }

    @Override
    public void postInit() {
        super.postInit();
        try {
            getFacade().sendRepeatedly(CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL, CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL, 
                    0, CHECK_CDR_COMPLETE_TIMEOUT);
        } catch (ExecutorServiceException ex) {
            throw new RavenRuntimeException("Initialization error", ex);
        }
    }


    @Override
    public Object processData(Object data) throws Exception {
        if      (data == GET_CDRS) return getCdrsList();
        else if (data == CHECK_CDR_COMPLETE_TIMEOUT) return checkCdrCompleteTimeout();
        else if (data instanceof CallEvent) return processCallEvent((CallEvent)data);
        else return UNHANDLED;            
    }

    private List<Record> getCdrsList() {
        return cdrs.isEmpty()? Collections.EMPTY_LIST : new ArrayList<>(cdrs.values());
    }

    private Object checkCdrCompleteTimeout() throws Exception {
        if (!cdrs.isEmpty()) {
            Iterator<Map.Entry<IvrEndpointConversation, Record>> it = cdrs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<IvrEndpointConversation, Record> entry = it.next();
                if (System.currentTimeMillis() - entry.getKey().getConversationStartTime() > cdrCompleteTimeout) {
                    if (getLogger().isWarnEnabled())
                        getLogger().warn("Found CDR that forms more than {} minutes. CDR: {}", cdrCompleteTimeout/1000/60, entry.getValue());
                    it.remove();
                    completeCdr(entry.getKey(), entry.getValue());
                    trySendCdrToConcumers(entry.getValue(), CallEventType.CONVERSATION_STARTED);
                }
            }
        }
        return VOID;
    }
        
    private void trySendCdrToConcumers(final Record cdr, final CallEventType eventType) throws Exception {
        if (enabledEvent.contains(eventType)) {
            cdr.setTag("eventType", eventType.name());
            DataSourceHelper.sendDataToConsumers(sender, cdr, new DataContextImpl());                
        }
    }

    private void completeCdr(IvrEndpointConversation key, Record value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private Object processCallEvent(CallEvent callEvent) throws Exception {
        Record cdr;
        switch (callEvent.eventType) {
            case CONVERSATION_INITIALIZED:
                cdr = createCdr(callEvent);                
                cdrs.put(callEvent.conversation, cdr);
                break;
            case CONVERSATION_STARTED:
            case CONVERSATION_FINISHED:
                cdr = cdrs.get(callEvent.conversation);
                if (cdr==null) {
                    if (getLogger().isErrorEnabled())
                        getLogger().error("Can't process {} call event because CDR not initialized.", callEvent);
                    return VOID;
                }
                if (callEvent.eventType==CallEventType.CONVERSATION_STARTED)
                    processConversationStartedEvent(callEvent, cdr);
                else
                    processConversationFinishedEvent(callEvent, cdr);
                break;
            default:                
                return UNHANDLED;
        }
        trySendCdrToConcumers(cdr, callEvent.eventType);
        return VOID;
    }            
    
    private Record createCdr(final CallEvent callEvent) throws Exception {
        final Record cdr = cdrSchema.createRecord();
        final IvrEndpointConversation conv = callEvent.conversation;
        cdr.setValue(ID, conv.getConversationId());
        cdr.setValue(ENDPOINT_ADDRESS, callEvent.endpoint.getAddress());
        cdr.setValue(CALLING_NUMBER, conv.getCallingNumber());
        cdr.setValue(CALLED_NUMBER, conv.getCalledNumber());
        cdr.setValue(LAST_REDIRECTED_NUMBER, conv.getLastRedirectedNumber());
        cdr.setValue(CALL_START_TIME, conv.getCallStartTime());
        cdr.setValue(GET_CDRS, VOID);
        return cdr;        
    }
    
    private void processConversationStartedEvent(CallEvent callEvent, Record cdr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void processConversationFinishedEvent(CallEvent callEvent, Record cdr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
   
    public static class CallEvent {        
        private final CallEventType eventType;
        private final IvrEndpointConversation conversation;        
        private final AbstractEndpointNode endpoint;

        public CallEvent(CallEventType eventType, IvrEndpointConversation conversation) {
            this.eventType = eventType;
            this.conversation = conversation;
        }        

        @Override
        public String toString() {
            return "CALL_EVENT: "+eventType+", "+conversation;            
        }        
    }
}
