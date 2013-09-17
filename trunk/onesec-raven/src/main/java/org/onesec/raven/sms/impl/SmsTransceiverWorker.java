/*
 * Copyright 2013 Mikhail Titov.
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

import com.logica.smpp.Data;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.onesec.raven.sms.MessageUnit;
import org.onesec.raven.sms.ShortMessageListener;
import org.onesec.raven.sms.ShortTextMessage;
import org.onesec.raven.sms.SmsAgentListener;
import org.onesec.raven.sms.SmsConfig;
import org.onesec.raven.sms.SmsMessageEncoder;
import org.onesec.raven.sms.queue.OutQueue;
import org.onesec.raven.sms.queue.ShortTextMessageImpl;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class SmsTransceiverWorker implements ShortMessageListener {
    public final static long CYCLE_PAUSE_INTERVAL = 100;
//    public final static long RESTART_AGENT_ON_ERROR = 30; //secs
    public final static long SMS_AGENT_BIND_TIMEOUT = 10000; //ms
    /**
     * @see OutQueue.factorQF
     */
//    private final long mesWaitQF = 60 * 1000;
    /**
     * @see OutQueue.factorTH
     */
//    private final long mesWaitTH = 60 * 1000;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 14 - QUEUE_FULL (мс). If QUEUE_FULL
     * : waitInterval = mesWaitQF * ( 1+factorQF*attempsCount )
     */
    private final int factorQF = 0;
    /**
     * На сколько отложить попытки отправки сообщения при ошибке 58 - THROTTLED (мс). If THROTTLED :
     * waitInterval = mesWaitTH * ( 1+factorTH*attempsCount )
     */
    private final int factorTH = 0;
    
    
    public static final long rebuindInterval = 30000;
    private final SmsConfig config;
    private final ExecutorService executor;
    private final OutQueue queue;
    private final LoggerHelper logger;
    private final SmsMessageEncoder messageEncoder;
//    private final AtomicReference<SmsAgent> agent = new AtomicReference<SmsAgent>();
    private final AtomicReference<MessageProcessor> messageProcessor = new AtomicReference<MessageProcessor>();
    private final SmsTransceiverNode owner;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicLong suspendProcessorUntil = new AtomicLong();

    public SmsTransceiverWorker(SmsTransceiverNode owner, SmsConfig config, ExecutorService executor) 
        throws Exception 
    {
        this.owner = owner;
        this.config = config;
        this.executor = executor;
        this.logger = new LoggerHelper(owner, "Transceiver. ");
        this.messageEncoder = new SmsMessageEncoderImpl(config, logger);
        this.queue = new OutQueue(config, this.logger);
//        createAgent();
    }
    
    public OutQueue getQueue() {
        return queue;
    }
    
    public boolean isProcessorActive() {
        return messageProcessor.get()!=null;
    }
    
    public String getSmsAgentStatus() {
        MessageProcessor _processor = messageProcessor.get();
        if (_processor==null) return "NOT_CREATED";
        else {
            SmsAgent agent = _processor.agent.get();
            return agent==null? "NOT_CREATED" : agent.getStatus().toString();
        }
    }
    
    public boolean addMessage(Long messageId, String message, String dstAddr, Object tag) {
        try {
            ShortTextMessageImpl msg = new ShortTextMessageImpl(
                    dstAddr, message, tag, messageId, messageEncoder, config, logger);
            msg.addListener(this);
            if (!queue.addMessage(msg)) return false;
            else {
                startMessageProcessor();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    public void stop() {
        synchronized(stopped) {
            stopped.set(true);
            stopMessageProcessor();
//            stopAgent();
        }
    }
    
    private void startMessageProcessor() {
        if (!stopped.get() && !isProcessorSuspended() && running.compareAndSet(false, true)) {
            stopMessageProcessor();
            messageProcessor.set(new MessageProcessor());
            if (!executor.executeQuietly(messageProcessor.get()))
                running.set(false);
        }
    }
    
    private boolean isProcessorSuspended() {
        return System.currentTimeMillis() < suspendProcessorUntil.get();
    }
    
    private void startMessageProcessor(long delay) {
        executor.executeQuietly(delay, new AbstractTask(owner, "Waiting for processor start") {
            @Override public void doRun() throws Exception {
                startMessageProcessor();
            }
        });
    }
    
    private void restartMessageProcessor(long delay) {
        suspendProcessorUntil.set(System.currentTimeMillis()+delay);
        stopMessageProcessor();
        startMessageProcessor(delay);
        if (logger.isInfoEnabled())
            logger.info(String.format("Message processor suspended on %s secs", delay/1000));
    }
    
    private void stopMessageProcessor() {
        MessageProcessor _processor = messageProcessor.getAndSet(null);
        if (_processor!=null)
            _processor.stop();
    }
    
    public void messageHandled(ShortTextMessage msg, boolean success, Object tag) {
        if (!stopped.get())
            owner.messageHandled(success, tag);
    }

    private class MessageProcessor extends AbstractTask {
        private final AtomicBoolean needStop = new AtomicBoolean();
        private final LoggerHelper logger = new LoggerHelper(SmsTransceiverWorker.this.logger, "Processor. ");
        private final AtomicReference<SmsAgent> agent = new AtomicReference<SmsAgent>();
        private final AtomicBoolean binding = new AtomicBoolean(false);
        private final AtomicBoolean unbinding = new AtomicBoolean(false);
        
        public MessageProcessor() {
            super(owner, "Processing messages");
        }
        
        public void stop() {
            needStop.set(true);
        }

        @Override
        public void doRun() throws Exception {
            if (logger.isDebugEnabled())
                logger.debug("Message processor task STARTED");
            try {
                while (true) {
                    long cyclePause = 0;
                    int counter = 0;
                    while (!needStop.get() && !queue.isEmpty()) {
                       if (cyclePause > 0) {
                           Thread.sleep(cyclePause);
                            cyclePause = 0;
                       }
                       cyclePause = CYCLE_PAUSE_INTERVAL;
                       if (trySendMessageUnit()) {
                           ++counter;
                           if (counter > config.getOnceSend()) {
                               if (logger.isTraceEnabled())
                                   logger.trace("Packet sent. Messages in packet - {}", counter);
                               counter = 0;
                           }
                           else cyclePause = 0;
                       }
                    }                    
                    running.set(false);
                    if (needStop.get() || queue.isEmpty() || !running.compareAndSet(false, true)) {
                        if (logger.isDebugEnabled())
                            logger.debug("Message processor task FINISHED. Queue empty flag: {}; Stopped flag: {}", 
                                    queue.isEmpty(), needStop.get());
                        return;
                    }
                }
            } finally {
                running.compareAndSet(true, false);
                synchronized(unbinding) {
                    unbinding.set(true);
                    SmsAgent _agent = agent.getAndSet(null);
                    if (_agent != null) {
                        _agent.unbind();
                    }
                }
            }
        }

        private boolean trySendMessageUnit() {
            MessageUnit unit = queue.getNext();
            if (unit==null) {
                if (logger.isTraceEnabled())
                    logger.trace("Queue doesn't have message unit READY to submit. Waiting");
                return false;
            }
            SmsAgent _agent = getAgent();
            if (_agent!=null) { 
                try {
                    _agent.submit(unit.getPdu());
                    unit.submitted();
                    return true;
                } catch (Exception ex) {
                    if (logger.isErrorEnabled())
                        logger.error(
                            String.format("Error submitting message unit: %s", unit.getPdu().debugString())
                            , ex);
                    return false;
                }
            } else {
                if (logger.isWarnEnabled())
                    logger.warn("SmsAgent not active");
                return false;
            }
        }
        
        private SmsAgent getAgent() {
            if (agent.get()!=null) return agent.get();
            else {
                try {
                    if (binding.compareAndSet(false, true)) {
                        SmsAgent _agent = new SmsAgent(config, new AgentListener(this), executor, executor, logger);
                        if (logger.isDebugEnabled())
                            logger.debug("SmsAgent created. Wating for IN_SERVICE");
                        synchronized(this) {
                            wait(SMS_AGENT_BIND_TIMEOUT);
                        }
                        if (agent.get()!=null) return agent.get();
                        else throw new Exception("Bind timeout");
                    }
                    return null;
                } catch (Exception ex) {
                    if (logger.isErrorEnabled())
                        logger.error(
                                String.format("Error creating SMS agent. Will try again via %s seconds"
                                    , config.getRebindOnTimeoutInterval()/1000)
                                , ex);
                    restartMessageProcessor(config.getRebindOnTimeoutInterval());
                    return null;
                }
            }
        }
        
        private void setAgent(SmsAgent agent) {
            boolean needNotify = false;
            synchronized(unbinding) {
                if (unbinding.get()) {
                    if (agent!=null) agent.unbind();
                } else {
                    this.agent.set(agent);
                    if (agent!=null) needNotify = true;
                    else {
                        if (logger.isWarnEnabled())
                            logger.warn("SMS agent unexpected chaneged state to OUT_OF_SERVICE. "
                                    + "Restarting processor and sms agent via {} seconds", 
                                    config.getRebindOnTimeoutInterval()/1000);
                        restartMessageProcessor(config.getRebindOnTimeoutInterval());
                    }
                }
            }
            if (needNotify)
                synchronized(this) {
                    notify();
                }
        }
    }
    
    private class AgentListener implements SmsAgentListener {
        private final AtomicBoolean valid = new AtomicBoolean(true);
        private final MessageProcessor processor;

        public AgentListener(MessageProcessor processor) {
            this.processor = processor;
        }
        
        public void inService(SmsAgent _agent) {
            processor.setAgent(_agent);
        }

        public void responseReceived(SmsAgent agent, Response pdu) {
            if (valid.get()) {
                int sq = pdu.getSequenceNumber();
//                long time = System.currentTimeMillis();
                MessageUnit unit;
                switch (pdu.getCommandStatus()) {
                    case Data.ESME_ROK:
                        unit = queue.getMessageUnit(sq);
                        if (unit!=null) unit.confirmed();
                        break;
                    case Data.ESME_RTHROTTLED:
                        if (config.getThrottledDelay() > 0) {
                            restartMessageProcessor(config.getThrottledDelay());
                            unit = queue.getMessageUnit(sq);
                            if (unit!=null) unit.delay(
                                    config.getMesThrottledDelay() * (1 + factorTH * unit.getAttempts()));
                        }
                        break;
                    case Data.ESME_RMSGQFUL:
                        if (config.getQueueFullDelay() > 0) {
                            restartMessageProcessor(config.getQueueFullDelay());
                            unit = queue.getMessageUnit(sq);
                            if (unit!=null) unit.delay(
                                    config.getMesQueueFullDelay() * (1 + factorQF * unit.getAttempts()));
                        }
                        break;
                    default:
                        unit = queue.getMessageUnit(sq);
                        if (unit!=null) unit.fatal();
                }
            }
        }

        public void requestReceived(SmsAgent agent, Request pdu) {
            if (valid.get()) {
                
            }
        }

        public void outOfService(SmsAgent _agent) {
            valid.set(false);
            processor.setAgent(null);
        }
    }
    
//    private class TaskRebuindTask extends AbstractTask {
//
//        public TaskRebuindTask() {
//            super(owner, "Agent rebuind task");
//        }
//        
//        @Override
//        public void doRun() throws Exception {
//            if (!stopped.get()) 
//                createAgent();
//        }
//    }
}
