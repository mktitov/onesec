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
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.raven.RavenRuntimeException;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.sched.ExecutorServiceException;
import static org.onesec.raven.ivr.impl.CallCdrRecordSchemaNode.*;
import org.raven.ds.RecordException;
/**
 *
 * @author Mikhail Titov
 */
public class CdrGeneratorDP extends AbstractDataProcessorLogic {

    public enum CallEventType { 
        CONVERSATION_INITIALIZED, CONVERSATION_STARTED, CONNECTION_ESTABLISHED, CONVERSATION_FINISHED
    };
    
    public final static String GET_CDRS = "GET_FORMING_CDRS";
    public final static String GET_ACTIVE_CDR_COUNT = "GET_ACTIVE_CDR_COUNT";
    public final static String GET_COMPLETED_CDR_COUNT = "GET_COMPLETED_CDR_COUNT";
    public final static String CHECK_CDR_COMPLETE_TIMEOUT = "CHECK_CDR_COMPLETE_TIMEOUT";
    public final static long CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL = 60_0000;
    public static final String CDR_COMPLETION_TOO_LONG = "CDR_COMPLETION_TOO_LONG";
    
    private final DataSource cdrSource;
    private final RecordSchema cdrSchema;
    private final EnumSet<CallEventType> sendCdrOnEvents;
    private final long cdrCompleteTimeout;

    private final Map<IvrEndpointConversation, Record> cdrs = new HashMap<>();
    private long completedCdrCount = 0l;

    public CdrGeneratorDP(DataSource cdrSource, RecordSchema cdrSchema, EnumSet<CallEventType> sendCdrOnEvents, 
            long cdrCompleteTimeout) 
    {
        this.cdrSource = cdrSource;
        this.cdrSchema = cdrSchema;
        this.sendCdrOnEvents = sendCdrOnEvents;
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
        else if (data == GET_ACTIVE_CDR_COUNT) return cdrs.size();
        else if (data == GET_COMPLETED_CDR_COUNT) return completedCdrCount;
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
                if (System.currentTimeMillis() - entry.getKey().getCallStartTime() > cdrCompleteTimeout) {
                    if (getLogger().isWarnEnabled())
                        getLogger().warn("Found CDR that forms more than {} minutes. CDR: {}", cdrCompleteTimeout/1000/60, entry.getValue());
                    it.remove();
                    completeCdr(entry.getKey(), entry.getValue());
                    trySendCdrToConcumers(entry.getValue(), CallEventType.CONVERSATION_FINISHED);
                }
            }
        }
        return VOID;
    }
        
    private void trySendCdrToConcumers(final Record cdr, final CallEventType eventType) throws Exception {
        if (sendCdrOnEvents.contains(eventType)) {
            final Record _cdr = cdrSchema.createRecord();
            _cdr.copyFrom(cdr);
            _cdr.setTag("eventType", eventType.name());
            DataSourceHelper.sendDataToConsumers(cdrSource, _cdr, new DataContextImpl());                
        }
    }

    private void completeCdr(IvrEndpointConversation conv, Record cdr) throws RecordException {
        processConversationFinishedEvent(conv, cdr);
        cdr.setValue(COMPLETION_CODE, CDR_COMPLETION_TOO_LONG);
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
            case CONNECTION_ESTABLISHED:    
                cdr = cdrs.get(callEvent.conversation);
                if (cdr==null) {
                    if (getLogger().isErrorEnabled())
                        getLogger().error("Can't process ({}) call event because CDR not initialized.", callEvent);
                    return VOID;
                }
                switch (callEvent.eventType) {
                    case CONVERSATION_STARTED: processConversationStartedEvent(callEvent.conversation, cdr); break;
                    case CONNECTION_ESTABLISHED: processConnectionEstablishedEvent(callEvent.conversation, cdr); break;
                    case CONVERSATION_FINISHED: 
                        cdrs.remove(callEvent.conversation);
                        processConversationFinishedEvent(callEvent.conversation, cdr);
                        break;
                    default: return UNHANDLED;
                }
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
        cdr.setValue(CALL_START_TIME, conv.getCallStartTime());
        return cdr;        
    }
    
    private void processConversationStartedEvent(IvrEndpointConversation conv, Record cdr) throws RecordException {
//        final IvrEndpointConversation conv = callEvent.conversation;
        
        cdr.setValue(CALLING_NUMBER, conv.getCallingNumber());
        cdr.setValue(CALLED_NUMBER, conv.getCalledNumber());
        cdr.setValue(LAST_REDIRECTED_NUMBER, conv.getLastRedirectedNumber());
        cdr.setValue(CONVERSATION_START_TIME, conv.getConversationStartTime());
        
        final IncomingRtpStream inRtp = conv.getIncomingRtpStream();
        if (inRtp!=null) {
            cdr.setValue(IN_RTP_LOCAL_ADDR, inRtp.getAddress().getHostAddress());
            cdr.setValue(IN_RTP_LOCAL_PORT, inRtp.getPort());
            cdr.setValue(IN_RTP_REMOTE_ADDR, inRtp.getRemoteHost());
            cdr.setValue(IN_RTP_REMOTE_PORT, inRtp.getRemotePort());            
        }
        
        final OutgoingRtpStream outRtp = conv.getOutgoingRtpStream();
        if (outRtp!=null) {
            cdr.setValue(OUT_RTP_LOCAL_ADDR, outRtp.getAddress().getHostAddress());
            cdr.setValue(OUT_RTP_LOCAL_PORT, outRtp.getPort());
            cdr.setValue(OUT_RTP_REMOTE_ADDR, outRtp.getRemoteHost());
            cdr.setValue(OUT_RTP_REMOTE_PORT, outRtp.getRemotePort());
        }
    }
    
    private void processConnectionEstablishedEvent(IvrEndpointConversation conv, Record cdr) throws RecordException {
        cdr.setValue(CONNECTION_ESTABLISHED_TIME, conv.getConnectionEstablishedTime());
    }

    private void processConversationFinishedEvent(IvrEndpointConversation conv, Record cdr) throws RecordException {
//        final IvrEndpointConversation conv = callEvent.conversation;     
        completedCdrCount++;
        cdr.setValue(CALLING_NUMBER, conv.getCallingNumber());
        cdr.setValue(CALLED_NUMBER, conv.getCalledNumber());
        cdr.setValue(LAST_REDIRECTED_NUMBER, conv.getLastRedirectedNumber());
        
        cdr.setValue(COMPLETION_CODE, conv.getCompletionCode());
        cdr.setValue(CALL_END_TIME, conv.getCallEndTime());
        cdr.setValue(CALL_DURATION, (conv.getCallEndTime()-conv.getCallStartTime())/1000);
        if (conv.getConnectionEstablishedTime()>0)
            cdr.setValue(CONVERSATION_DURATION, (conv.getCallEndTime() - conv.getConnectionEstablishedTime())/1000);
        cdr.setValues(conv.getIncomingRtpStat());
        cdr.setValues(conv.getOutgoingRtpStat());
        cdr.setValues(conv.getAudioStreamStat());
    }
   
    public static class CallEvent {        
        private final CallEventType eventType;
        private final IvrEndpointConversation conversation;        
        private final IvrTerminal endpoint;

        public CallEvent(CallEventType eventType, IvrEndpointConversation conversation, IvrTerminal endpoint) {
            this.eventType = eventType;
            this.conversation = conversation;
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return "CALL_EVENT: "+eventType+", "+conversation;            
        }        
    }
}
