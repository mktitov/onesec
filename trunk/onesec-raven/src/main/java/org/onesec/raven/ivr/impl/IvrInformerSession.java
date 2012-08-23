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

package org.onesec.raven.ivr.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.ivr.*;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;
import org.raven.BindingNames;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 * Stores {@link IvrInformer ivr informer} session information
 * @author Mikhail Titov
 */
//TODO: test class
public class IvrInformerSession implements EndpointRequest {
    @Service
    private static TypeConverter converter;

    private final List<Record> records;
    private final AsyncIvrInformer informer;
    private final int maxCallDuration;
    private final int inviteTimeout;
    private final IvrConversationScenario scenario;

//    private final Condition abonentInformed;
//    private final Lock informLock;
    private final long endpointWaitTimeout;
    private final DataContext dataContext;
    private final int requestPriority;
    private final AtomicBoolean active = new AtomicBoolean(true);

    private AtomicReference<String> statusMessage;
    private ConversationCdr conversationResult;
    private Record currentRecord;
    private IvrEndpoint endpoint;
    private String abonentNumber;
    private int recPos = 0;

    public IvrInformerSession(
            List<Record> records, AsyncIvrInformer informer
            , Integer inviteTimeout, Integer maxCallDuration
            , IvrConversationScenario scenario, long endpointWaitTimeout, DataContext dataContext
            , int requestPriority)
    {
        this.records = records;
        this.informer = informer;
        this.inviteTimeout = inviteTimeout==null? 0 : inviteTimeout;
        this.maxCallDuration = maxCallDuration==null? 0 : maxCallDuration;
        this.scenario = scenario;
        this.endpointWaitTimeout = endpointWaitTimeout;
        this.dataContext = dataContext;
        this.requestPriority = requestPriority;

        this.statusMessage = new AtomicReference<String>("Queued (waiting for terminal)");
        
//        informLock = new ReentrantLock();
//        abonentInformed = informLock.newCondition();
    }

    public int getPriority() {
        return requestPriority;
    }

    public IvrEndpoint getEndpoint() {
        return endpoint;
    }

    public Record getCurrentRecord() {
        return getCurrentRec();
    }

    public List<Record> getRecords() {
        return records;
    }

    public boolean containsAbonentNumber(Object abonentNumber) throws RecordException {
        for (Record record: records)
            if (ObjectUtils.equals(abonentNumber, record.getValue(ABONENT_NUMBER_FIELD)))
                return true;
        return false;
    }

    public Node getOwner() {
        return informer;
    }

    public long getWaitTimeout() {
        return endpointWaitTimeout;
    }

    public String getStatusMessage() {
        return statusMessage+" ("+informer.getRecordInfo(currentRecord)
                +(abonentNumber==null?"":", translated number - "+abonentNumber)+")";
    }

    public void processRequest(IvrEndpoint endpoint) {
//        try {
            statusMessage.set("Processing call request");
            this.endpoint = endpoint;
            try {
                if (endpoint==null)
                    skipInforming();
                else
                    inform(getCurrentRec());
            } catch(Throwable e) {
                if (informer.isLogLevelEnabled(LogLevel.ERROR))
                    informer.getLogger().error("Error informing abonent.", e);
                closeSession(false);
            }
//        } finally {
//            informer.incInformedAbonents();
//            informer.removeSession(this);
//        }
    }
    
    private void inform(Record rec) throws Exception {
        if (rec==null || !informer.getInformAllowed()) {
            closeSession(rec==null);
            return;
        }
        String _abonentNumber = converter.convert(String.class, rec.getValue(ABONENT_NUMBER_FIELD), null);
        if (_abonentNumber==null && informer.isLogLevelEnabled(LogLevel.WARN))
            informer.getLogger().warn("Record has NULL abonent number");
        statusMessage.set(String.format("Calling to the abonent number (%s)", _abonentNumber));
        _abonentNumber = informer.translateNumber(_abonentNumber, rec);
        if (_abonentNumber==null && informer.isLogLevelEnabled(LogLevel.WARN))
            informer.getLogger().warn("Abonent number translated to NULL");
        abonentNumber = _abonentNumber;
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put(AsyncIvrInformer.RECORD_BINDING, rec);
        bindings.put(AsyncIvrInformer.INFORMER_BINDING, this);
        bindings.put(BindingNames.DATA_CONTEXT_BINDING, dataContext);
        rec.setValue(CALL_START_TIME_FIELD, new Timestamp(System.currentTimeMillis()));
        
        endpoint.invite(_abonentNumber, inviteTimeout, maxCallDuration, new ConversationListener(), scenario, bindings);
    }

//    @Override
//    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
//        super.conversationStopped(event);
//        try {
//            Record rec = getCurrentRec();
//            boolean groupInformed = handleConversationResult(rec, getCdr());
//            informer.sendRecordToConsumers(rec, dataContext);
//            if (!groupInformed)
//                inform(getNextRec());
//            else 
//                skipRestRec(getNextRec());
//        } catch (Throwable ex) {
//            if (informer.isLogLevelEnabled(LogLevel.ERROR))
//                informer.getLogger().error("Error informing abonent.", ex);
//            closeSession(false);
//        }
//    }
    
    private void skipRestRec(Record rec) {
        if (rec==null || !informer.getInformAllowed()) {
            closeSession(rec==null);
            return;
        }
        try {
            statusMessage.set("Skiping record: "+informer.getRecordInfo(rec));
            rec.setValue(COMPLETION_CODE_FIELD, AsyncIvrInformer.SKIPPED_STATUS);
            Timestamp curTs = new Timestamp(System.currentTimeMillis());
            rec.setValue(CALL_START_TIME_FIELD, curTs);
            rec.setValue(CALL_END_TIME_FIELD, curTs);
            informer.sendRecordToConsumers(rec, dataContext);
            skipRestRec(getNextRec());
        } catch (Throwable e) {
            if (informer.isLogLevelEnabled(LogLevel.ERROR))
                informer.getLogger().error("Error informing abonent.", e);
            closeSession(false);
        }
    }
    
    private Record getCurrentRec() {
        return records.get(recPos);
    }
    
    private Record getNextRec() {
        return ++recPos>=records.size()? null : records.get(recPos);
    }
    
    private void skipInforming() throws Exception {
        statusMessage.set("Skiping infroming because of no free terminal in the pool");
        for (Record rec: records) {
            rec.setValue(COMPLETION_CODE_FIELD, AsyncIvrInformer.ERROR_NO_FREE_ENDPOINT_IN_THE_POOL);
            informer.sendRecordToConsumers(rec, dataContext);
        }
        closeSession(false);
    }

    private boolean handleConversationResult(Record rec, ConversationCdr cdr) throws Exception {
        String status = null;
        boolean sucProc = false;
        switch(cdr.getCompletionCode()) {
            case COMPLETED_BY_ENDPOINT:
                status = AsyncIvrInformer.COMPLETED_BY_INFORMER_STATUS; sucProc = true; break;
            case COMPLETED_BY_OPPONENT:
                status = AsyncIvrInformer.COMPLETED_BY_ABONENT_STATUS; sucProc = true; break;
            case OPPONENT_BUSY: status = AsyncIvrInformer.NUMBER_BUSY_STATUS; break;
            case OPPONENT_NOT_ANSWERED: status = AsyncIvrInformer.NUMBER_NOT_ANSWERED_STATUS; break;
            case TERMINAL_NOT_READY:
            case CALL_DURATION_TOO_LONG:    
            case OPPONENT_UNKNOWN_ERROR: status = AsyncIvrInformer.PROCESSING_ERROR_STATUS; break;
        }
        sucProc = cdr.getConversationDuration()>0 && sucProc;
        if (sucProc)
            informer.incSuccessfullyInformedAbonents();
        rec.setValue(COMPLETION_CODE_FIELD, status);
        rec.setValue(CALL_START_TIME_FIELD, cdr.getCallStartTime());
        rec.setValue(CALL_END_TIME_FIELD, cdr.getCallEndTime());
        rec.setValue(CALL_DURATION_FIELD, cdr.getCallDuration());
        rec.setValue(CONVERSATION_START_TIME_FIELD, cdr.getConversationStartTime());
        rec.setValue(CONVERSATION_DURATION_FIELD, cdr.getConversationDuration());

        if (cdr.getTransferCompletionCode()!=null) {
            String transferStatus = null;
            switch(conversationResult.getTransferCompletionCode()) {
                case ERROR : transferStatus = AsyncIvrInformer.TRANSFER_ERROR_STATUS; break;
                case NORMAL : transferStatus = AsyncIvrInformer.TRANSFER_SUCCESSFULL_STATUS; break;
                case NO_ANSWER :
                    transferStatus = AsyncIvrInformer.TRANSFER_DESTINATION_NOT_ANSWER_STATUS;
                    break;
            }
            rec.setValue(TRANSFER_COMPLETION_CODE_FIELD, transferStatus);
            rec.setValue(TRANSFER_ADDRESS_FIELD, cdr.getTransferAddress());
            rec.setValue(TRANSFER_TIME_FIELD, cdr.getTransferTime());
            rec.setValue(TRANSFER_CONVERSATION_START_TIME_FIELD , cdr.getTransferConversationStartTime());
            rec.setValue(TRANSFER_CONVERSATION_DURATION_FIELD, cdr.getTransferConversationDuration());
        }
        return sucProc;
    }
   
    private void closeSession(boolean success) {
        if (!active.compareAndSet(true, false))
            return;
//        for (Record rec: records)
//            informer.sendRecordToConsumers(rec, dataContext);
        informer.incInformedAbonents();
//        if (success)
//            informer.incSuccessfullyInformedAbonents();
        informer.removeSession(this);
    }
    
    private class ConversationListener extends ConversationCdrRegistrator {
        private final AtomicBoolean active = new AtomicBoolean(true);

        @Override
        public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
            if (!active.compareAndSet(true, false))
                return;
            super.conversationStopped(event);
            try {
                Record rec = getCurrentRec();
                boolean groupInformed = handleConversationResult(rec, getCdr());
                informer.sendRecordToConsumers(rec, dataContext);
                if (!groupInformed)
                    inform(getNextRec());
                else 
                    skipRestRec(getNextRec());
            } catch (Throwable ex) {
                if (informer.isLogLevelEnabled(LogLevel.ERROR))
                    informer.getLogger().error("Error informing abonent.", ex);
                closeSession(false);
            }
        }
    }
}
