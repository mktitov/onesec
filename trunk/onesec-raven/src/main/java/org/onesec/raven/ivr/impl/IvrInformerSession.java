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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.ConversationCompletionCallback;
import org.onesec.raven.ivr.ConversationResult;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointState;
import org.raven.ds.Record;
import org.raven.log.LogLevel;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;
import static org.onesec.raven.ivr.impl.IvrInformerRecordSchemaNode.*;

/**
 * Stores {@link IvrInformer ivr informer} session information
 * @author Mikhail Titov
 */
public class IvrInformerSession implements Task, ConversationCompletionCallback
{
    @Service
    private static TypeConverter converter;

    private final Record record;
    private final AsyncIvrInformer informer;
    private final IvrEndpoint endpoint;
    private final Integer maxCallDuration;
    private final IvrConversationScenario scenario;

    private final Condition abonentInformed;
    private final Lock informLock;

    private String statusMessage;
    private ConversationResult conversationResult;

    public IvrInformerSession(
            Record record, AsyncIvrInformer informer, IvrEndpoint endpoint, Integer maxCallDuration
            , IvrConversationScenario scenario)
    {
        this.record = record;
        this.informer = informer;
        this.endpoint = endpoint;
        this.maxCallDuration = maxCallDuration;
        this.scenario = scenario;
        
        informLock = new ReentrantLock();
        abonentInformed = informLock.newCondition();
    }

    public IvrEndpoint getEndpoint()
    {
        return endpoint;
    }

    public Record getRecord()
    {
        return record;
    }

    public Node getTaskNode()
    {
        return informer;
    }

    public String getStatusMessage()
    {
        return statusMessage+" ("+informer.getRecordInfo(record)+")";
    }

    public void run()
    {
        try
        {
            try
            {
                statusMessage = "Starting inform abonent";
                Map<String, Object> bindings = new HashMap<String, Object>();
                bindings.put(AsyncIvrInformer.RECORD_BINDING, record);
                bindings.put(AsyncIvrInformer.INFORMER_BINDING, this);
                statusMessage = "Calling to the abonent";
                String abonentNumber = converter.convert(String.class, record.getValue(ABONENT_NUMBER_FIELD), null);
                informLock.lock();
                try
                {
                    endpoint.invite(abonentNumber, scenario, this, bindings);
                    if (maxCallDuration!=null && maxCallDuration>0)
                    {
                        if (!abonentInformed.await(maxCallDuration, TimeUnit.SECONDS))
                            restartEndpoint();
                    }
                    else
                        abonentInformed.await();
                    handleConversationResult();
                    informer.sendRecordToConsumers(record);
                }
                finally
                {
                    informLock.unlock();
                }
            }
            catch(Exception e)
            {
                if (informer.isLogLevelEnabled(LogLevel.ERROR))
                    informer.getLogger().error(
                            "Error informing abonent: "+informer.getRecordInfo(record));
            }
        }
        finally
        {
            informer.removeSession(this);
        }
    }

    public void conversationCompleted(ConversationResult conversationResult)
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

    private void restartEndpoint() throws Exception
    {
        if (informer.isLogLevelEnabled(LogLevel.WARN))
            informer.getLogger().warn(String.format(
                    "The call is too long! Restaring endpoint (%s)", endpoint.getPath()));

        endpoint.stop();

        if (informer.isLogLevelEnabled(LogLevel.DEBUG))
            informer.getLogger().debug(String.format("Endpoint (%s) stoped", endpoint.getPath()));
        TimeUnit.SECONDS.sleep(5);
        endpoint.start();
        TimeUnit.SECONDS.sleep(10);
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

    private void handleConversationResult() throws Exception
    {
        String status = null;
        if (conversationResult==null)
            record.setValue(COMPLETION_CODE_FIELD, AsyncIvrInformer.PROCESSING_ERROR_STATUS);
        else
        {
            boolean sucProc = false;
            switch(conversationResult.getCompletionCode())
            {
                case COMPLETED_BY_ENDPOINT:
                    status = AsyncIvrInformer.COMPLETED_BY_INFORMER_STATUS; sucProc = true; break;
                case COMPLETED_BY_OPPONENT:
                    status = AsyncIvrInformer.COMPLETED_BY_ABONENT_STATUS; sucProc = true; break;
                case OPPONENT_BUSY: status = AsyncIvrInformer.NUMBER_BUSY_STATUS; break;
                case OPPONENT_NO_ANSWERED: status = AsyncIvrInformer.NUMBER_NOT_ANSWERED_STATUS; break;
                case OPPONENT_UNKNOWN_ERROR: status = AsyncIvrInformer.PROCESSING_ERROR_STATUS; break;
            }
            if (sucProc)
                informer.incSuccessfullyInformedAbonents();
            informer.incInformedAbonents();
            record.setValue(COMPLETION_CODE_FIELD, status);
            record.setValue(
                    CALL_START_TIME_FIELD, conversationResult.getCallStartTime());
            record.setValue(
                    CALL_END_TIME_FIELD, conversationResult.getCallEndTime());
            record.setValue(
                    CALL_DURATION_FIELD, conversationResult.getCallDuration());
            record.setValue(
                    CONVERSATION_START_TIME_FIELD
                    , conversationResult.getConversationStartTime());
            record.setValue(
                    CONVERSATION_DURATION_FIELD
                    , conversationResult.getConversationDuration());

            if (conversationResult.getTransferCompletionCode()!=null)
            {
                String transferStatus = null;
                switch(conversationResult.getTransferCompletionCode())
                {
                    case ERROR : transferStatus = AsyncIvrInformer.TRANSFER_ERROR_STATUS; break;
                    case NORMAL : transferStatus = AsyncIvrInformer.TRANSFER_SUCCESSFULL_STATUS; break;
                    case NO_ANSWER :
                        transferStatus = AsyncIvrInformer.TRANSFER_DESTINATION_NOT_ANSWER_STATUS;
                        break;
                }
                record.setValue(TRANSFER_COMPLETION_CODE_FIELD, transferStatus);
                record.setValue(
                        TRANSFER_ADDRESS_FIELD, conversationResult.getTransferAddress());
                record.setValue(
                        TRANSFER_TIME_FIELD, conversationResult.getTransferTime());
                record.setValue(TRANSFER_CONVERSATION_START_TIME_FIELD
                        , conversationResult.getTransferConversationStartTime());
                record.setValue(TRANSFER_CONVERSATION_DURATION_FIELD
                        , conversationResult.getTransferConversationDuration());
            }
        }
    }
}
