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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.core.StateWaitResult;
import org.onesec.raven.ivr.*;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;

/**
 * Stores {@link IvrInformer ivr informer} session information
 * @author Mikhail Titov
 */
public class IvrInformerSession 
        extends ConversationCdrRegistrator 
        implements EndpointRequest, ConversationCompletionCallback
{
    @Service
    private static TypeConverter converter;

    private final List<Record> records;
    private final AsyncIvrInformer informer;
    private final int maxCallDuration;
    private final int inviteTimeout;
    private final IvrConversationScenario scenario;

    private final Condition abonentInformed;
    private final Lock informLock;
    private final long endpointWaitTimeout;
    private final DataContext dataContext;
    private final int requestPriority;

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
        
        informLock = new ReentrantLock();
        abonentInformed = informLock.newCondition();
    }

    public int getPriority()
    {
        return requestPriority;
    }

    public IvrEndpoint getEndpoint()
    {
        return endpoint;
    }

    public Record getCurrentRecord()
    {
        return currentRecord;
    }

    public List<Record> getRecords()
    {
        return records;
    }

    public boolean containsAbonentNumber(Object abonentNumber) throws RecordException
    {
        for (Record record: records)
            if (ObjectUtils.equals(abonentNumber, record.getValue(ABONENT_NUMBER_FIELD)))
                return true;

        return false;
    }

    public Node getOwner()
    {
        return informer;
    }

    public long getWaitTimeout()
    {
        return endpointWaitTimeout;
    }

    public String getStatusMessage()
    {
        return statusMessage+" ("+informer.getRecordInfo(currentRecord)
                +(abonentNumber==null?"":", translated number - "+abonentNumber)+")";
    }

    public void processRequest(IvrEndpoint endpoint) {
        try {
            statusMessage.set("Processing call request");
            this.endpoint = endpoint;
            try {
                if (endpoint==null)
                    skipInforming();
                else
                    inform(endpoint);
            } catch(Throwable e) {
                if (informer.isLogLevelEnabled(LogLevel.ERROR))
                    informer.getLogger().error("Error informing abonent.", e);
            }
        } finally {
            informer.incInformedAbonents();
            informer.removeSession(this);
        }
    }

    public void conversationCompleted(ConversationCdr conversationResult)
    {
        this.conversationResult = conversationResult;
        informLock.lock();
        try
        {
            abonentInformed.signal();
        }
        finally
        {
            informLock.unlock();
        }
    }

    private void skipRecord(Record record) throws RecordException
    {
        statusMessage.set("Skiping record: "+informer.getRecordInfo(record));
        record.setValue(COMPLETION_CODE_FIELD, AsyncIvrInformer.SKIPPED_STATUS);
        Timestamp curTs = new Timestamp(System.currentTimeMillis());
        record.setValue(CALL_START_TIME_FIELD, curTs);
        record.setValue(CALL_END_TIME_FIELD, curTs);
    }

    private void restartEndpoint(IvrEndpoint endpoint, Record rec) throws Exception
    {
        if (informer.isLogLevelEnabled(LogLevel.DEBUG))
            informer.getLogger().debug(String.format(
                    "The %s is too long! Restaring endpoint (%s)"
                    , rec.getValue(COMPLETION_CODE_FIELD)==null? "call":"invite", endpoint.getPath()));

        endpoint.stop();

        if (informer.isLogLevelEnabled(LogLevel.DEBUG))
            informer.getLogger().debug(String.format("Endpoint (%s) stoped", endpoint.getPath()));
        TimeUnit.SECONDS.sleep(5);
        endpoint.start();
        StateWaitResult res = endpoint.getEndpointState().waitForState(new int[]{IvrEndpointState.IN_SERVICE}, 20000);
//
//        long startTime = System.currentTimeMillis();
//        while (  (   !Status.STARTED.equals(endpoint.getStatus())
//                  || IvrEndpointState.IN_SERVICE!=endpoint.getEndpointState().getId())
//               && System.currentTimeMillis()-startTime<20000)
//        {
//            Thread.sleep(100);
//        }
//        TimeUnit.SECONDS.sleep(10);
        if (   Status.STARTED.equals(endpoint.getStatus())
            && IvrEndpointState.IN_SERVICE == endpoint.getEndpointState().getId())
        {
            if (informer.isLogLevelEnabled(LogLevel.DEBUG))
                informer.getLogger().debug(String.format(
                        "Endpoint (%s) successfully restarted", endpoint.getPath()));
        } else if (informer.isLogLevelEnabled(LogLevel.ERROR))
            informer.getLogger().error(String.format(
                    "Error restarting endpoint (%s)", endpoint.getPath()));
    }

//    private void handleConversationResult(Record rec) throws Exception {
//        String status = null;
//        if (conversationResult==null)
//        {
//            if (rec.getValue(COMPLETION_CODE_FIELD)==null)
//                rec.setValue(COMPLETION_CODE_FIELD, AsyncIvrInformer.PROCESSING_ERROR_STATUS);
//        }
//        else
//        {
//            boolean sucProc = false;
//            switch(conversationResult.getCompletionCode())
//            {
//                case COMPLETED_BY_ENDPOINT:
//                    status = AsyncIvrInformer.COMPLETED_BY_INFORMER_STATUS; sucProc = true; break;
//                case COMPLETED_BY_OPPONENT:
//                    status = AsyncIvrInformer.COMPLETED_BY_ABONENT_STATUS; sucProc = true; break;
//                case OPPONENT_BUSY: status = AsyncIvrInformer.NUMBER_BUSY_STATUS; break;
//                case OPPONENT_NOT_ANSWERED: status = AsyncIvrInformer.NUMBER_NOT_ANSWERED_STATUS; break;
//                case OPPONENT_UNKNOWN_ERROR: status = AsyncIvrInformer.PROCESSING_ERROR_STATUS; break;
//            }
//            if (sucProc)
//                informer.incSuccessfullyInformedAbonents();
//            rec.setValue(COMPLETION_CODE_FIELD, status);
//            rec.setValue(
//                    CALL_START_TIME_FIELD, conversationResult.getCallStartTime());
//            rec.setValue(
//                    CALL_END_TIME_FIELD, conversationResult.getCallEndTime());
//            rec.setValue(
//                    CALL_DURATION_FIELD, conversationResult.getCallDuration());
//            rec.setValue(
//                    CONVERSATION_START_TIME_FIELD
//                    , conversationResult.getConversationStartTime());
//            rec.setValue(
//                    CONVERSATION_DURATION_FIELD
//                    , conversationResult.getConversationDuration());
//
//            if (conversationResult.getTransferCompletionCode()!=null)
//            {
//                String transferStatus = null;
//                switch(conversationResult.getTransferCompletionCode())
//                {
//                    case ERROR : transferStatus = AsyncIvrInformer.TRANSFER_ERROR_STATUS; break;
//                    case NORMAL : transferStatus = AsyncIvrInformer.TRANSFER_SUCCESSFULL_STATUS; break;
//                    case NO_ANSWER :
//                        transferStatus = AsyncIvrInformer.TRANSFER_DESTINATION_NOT_ANSWER_STATUS;
//                        break;
//                }
//                rec.setValue(TRANSFER_COMPLETION_CODE_FIELD, transferStatus);
//                rec.setValue(
//                        TRANSFER_ADDRESS_FIELD, conversationResult.getTransferAddress());
//                rec.setValue(
//                        TRANSFER_TIME_FIELD, conversationResult.getTransferTime());
//                rec.setValue(TRANSFER_CONVERSATION_START_TIME_FIELD
//                        , conversationResult.getTransferConversationStartTime());
//                rec.setValue(TRANSFER_CONVERSATION_DURATION_FIELD
//                        , conversationResult.getTransferConversationDuration());
//            }
//        }
//    }
    
    private void inform(IvrEndpoint endpoint) throws Exception {
        Record rec = getCurrentRec();
        if (rec==null)
            return;
        abonentNumber = converter.convert(String.class, rec.getValue(ABONENT_NUMBER_FIELD), null);
        statusMessage.set(String.format("Calling to the abonent number (%s)", abonentNumber));
        abonentNumber = informer.translateNumber(abonentNumber, rec);
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put(AsyncIvrInformer.RECORD_BINDING, rec);
        bindings.put(AsyncIvrInformer.INFORMER_BINDING, this);
        rec.setValue(CALL_START_TIME_FIELD, new Timestamp(System.currentTimeMillis()));
        endpoint.invite(abonentNumber, inviteTimeout, maxCallDuration, this, scenario, bindings);
    }

    @Override
    public void conversationStopped(IvrEndpointConversationStoppedEvent event) {
        super.conversationStopped(event);
        
    }
    
    private Record getCurrentRec() {
        return records.get(recPos);
    }
    
    private Record getNextRecord() {
        return ++recPos>=records.size()? null : records.get(recPos);
    }
    private void handleConversationResult(Record rec, ConversationCdr cdr) throws Exception {
        String status = null;
        boolean sucProc = false;
        switch(cdr.getCompletionCode()) {
            case COMPLETED_BY_ENDPOINT:
                status = AsyncIvrInformer.COMPLETED_BY_INFORMER_STATUS; sucProc = true; break;
            case COMPLETED_BY_OPPONENT:
                status = AsyncIvrInformer.COMPLETED_BY_ABONENT_STATUS; sucProc = true; break;
            case OPPONENT_BUSY: status = AsyncIvrInformer.NUMBER_BUSY_STATUS; break;
            case OPPONENT_NOT_ANSWERED: status = AsyncIvrInformer.NUMBER_NOT_ANSWERED_STATUS; break;
            case OPPONENT_UNKNOWN_ERROR: status = AsyncIvrInformer.PROCESSING_ERROR_STATUS; break;
        }
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
    }

//    private void inform(IvrEndpoint endpoint) throws Exception {
//        boolean groupInformed = false;
//        for (Record record: records) {
//            if (!informer.getInformAllowed())
//                return;
//            statusMessage.set("Starting inform abonent");
//            record.setValue(CALL_START_TIME_FIELD, new Timestamp(System.currentTimeMillis()));
//            currentRecord = record;
//            Map<String, Object> bindings = new HashMap<String, Object>();
//            bindings.put(AsyncIvrInformer.RECORD_BINDING, record);
//            bindings.put(AsyncIvrInformer.INFORMER_BINDING, this);
//            statusMessage.set("Calling to the abonent");
//            abonentNumber = converter.convert(String.class, record.getValue(ABONENT_NUMBER_FIELD), null);
//            abonentNumber = informer.translateNumber(abonentNumber, record);
//            conversationResult = null;
//            if (groupInformed)
//                skipRecord(record);
//            else
//            {
//                //TODO:fix invite
////                endpoint.invite(abonentNumber, scenario, this, bindings);
//                boolean restartEndpoint = false;
//                informLock.lock();
//                try
//                {
//                    //разговор может завершиться не начавшись
//                    if (conversationResult==null)
//                    {
//                        long callStartTime = System.currentTimeMillis();
//                        if (inviteTimeout!=null && inviteTimeout>0)
//                            if (   !abonentInformed.await(inviteTimeout, TimeUnit.SECONDS)
//                                && endpoint.getEndpointState().getId()==IvrEndpointState.INVITING)
//                            {
//                                restartEndpoint = true;
//                                record.setValue(COMPLETION_CODE_FIELD
//                                        , AsyncIvrInformer.NUMBER_NOT_ANSWERED_STATUS);
//                            }
//
//                        if (!restartEndpoint && conversationResult==null)
//                        {
//                            if (maxCallDuration!=null)
//                            {
//                                long timeout = maxCallDuration - (System.currentTimeMillis()-callStartTime)/1000;
//                                if (timeout<=0 || !abonentInformed.await(timeout, TimeUnit.SECONDS))
//                                    restartEndpoint = true;
//                            }
//                            else
//                                abonentInformed.await();
//                        }
//                    }
//                    handleConversationResult(record);
//                    String completionCode = (String) record.getValue(COMPLETION_CODE_FIELD);
//                    Long duration = (Long) record.getValue(CONVERSATION_DURATION_FIELD);
//                    if (ObjectUtils.in(completionCode, AsyncIvrInformer.COMPLETED_BY_INFORMER_STATUS
//                            , AsyncIvrInformer.COMPLETED_BY_ABONENT_STATUS)
//                        && duration!=null && duration>0)
//                    {
//                        groupInformed = true;
//                    }
//                }
//                finally
//                {
//                    informLock.unlock();
//                }
//                if (restartEndpoint)
//                    restartEndpoint(endpoint, record);
//            }
//            abonentNumber = null;
//            informer.sendRecordToConsumers(record, dataContext);
//        }
//    }

    private void skipInforming() throws Exception
    {
        statusMessage.set("Skiping infroming because of no free terminal in the pool");
        for (Record record: records)
        {
            record.setValue(COMPLETION_CODE_FIELD
                    , AsyncIvrInformer.ERROR_NO_FREE_ENDPOINT_IN_THE_POOL);
            informer.sendRecordToConsumers(record, dataContext);
        }
    }
}
