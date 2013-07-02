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

package org.onesec.raven.sms;

import com.logica.smpp.Data;
import com.logica.smpp.Receiver;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.Session;
import com.logica.smpp.SmppException;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.pdu.BindReceiver;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.BindTransciever;
import com.logica.smpp.pdu.BindTransmitter;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitSM;
import java.util.concurrent.atomic.AtomicReference;
import static org.onesec.raven.sms.BindMode.RECEIVER;
import static org.onesec.raven.sms.BindMode.TRANSMITTER;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class SmsAgent {
    private enum Status {INITIALIZING, IN_SERVICE, OUT_OF_SERVICE};
    private final static long QUEUE_WAIT_TIMEOUT = 100;
    private final SmsConfig config;
    private final LoggerHelper logger;
    private final SmsAgentListener agentListener;
    private final ExecutorService executor;
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.INITIALIZING);
    private final Session session;
    private final Node owner;
    private final MessageListener messageListener;
    
    private volatile long lastInteractionTime = 0;

    public SmsAgent(SmsConfig config, SmsAgentListener agentListener, ExecutorService executor
            , Node owner, LoggerHelper logger) 
        throws Exception 
    {
        this.config = config;
        this.agentListener = agentListener;
        this.executor = executor;
        this.logger = new LoggerHelper(logger, "Agent. ");
        this.owner = owner;
        this.messageListener = new MessageListener();
        this.session = createSession();
        bind();
        executeCheckConnectionTask();
    }
    
    public void unbind() {
        changeStatusToOutOfService();
    }
    
    public void submit(SubmitSM request) throws Exception {
        try {
            if (config.getBindMode()== BindMode.RECEIVER) 
                throw new SmppException("Can't transmitte message in RECEIVER mode");
            if (logger.isDebugEnabled()) 
                logger.debug("Submiting request {}", request.debugString());
            session.submit(request);
        } catch (Exception e) {
            if (logger.isErrorEnabled())
                logger.error("Submiting error", e);
            throw e;
        }
    }
    
    private void bind() {
        executor.executeQuietly(new BindTask(owner));
    }
    
    private Session createSession() {
        TCPIPConnection connection = new TCPIPConnection(config.getBindAddr(), config.getBindPort());
        connection.setReceiveTimeout(config.getBindTimeout());
        connection.setCommsTimeout(config.getSoTimeout());
        Session _session = new Session(connection);
        _session.setQueueWaitTimeout(QUEUE_WAIT_TIMEOUT);
        return _session;
    }
    
    private void changeStatusToInService() {
        if (status.compareAndSet(Status.INITIALIZING, Status.IN_SERVICE)) 
            fireInServiceEvent();
    }
    
    private void changeStatusToOutOfService() {
        executor.executeQuietly(new UnbindTask());
    }
    
    private void executeCheckConnectionTask() {
        executor.executeQuietly(new CheckConnectionTask());
    }
    
    private void fireOutOfServiceEvent() {
        if (logger.isErrorEnabled())
            logger.info("Out of service");
        executor.executeQuietly(new AbstractTask(owner, "Pushing OUT_OF_SERVICE event") {
            @Override public void doRun() throws Exception {
                agentListener.outOfService(SmsAgent.this);
            }
        });
    }
    
    private void fireInServiceEvent() {
        if (logger.isErrorEnabled())
            logger.info("In service");
        executor.executeQuietly(new AbstractTask(owner, "Pushing IN_SERVICE event") {
            @Override public void doRun() throws Exception {
                agentListener.inService(SmsAgent.this);
            }
        });
    }
    
    private void fireMessageReceived(final PDU message, final boolean response) {
        executor.executeQuietly(new AbstractTask(owner, "Delivering message to listener") {
            @Override public void doRun() throws Exception {
                if (response) agentListener.responseReceived(SmsAgent.this, (Response)message);
                else agentListener.requestReceived(SmsAgent.this, (Request)message);
            }
        });
    }
    
    private class BindTask extends AbstractTask {

        public BindTask(Node taskNode) {
            super(taskNode, "Binding to SMSC server");
        }

        @Override
        public void doRun() throws Exception {
            try {
                if (logger.isDebugEnabled()) 
                    logger.debug("Binding to SMSC: addr={} port={}", config.getBindAddr(), config.getBindPort());
                session.bind(createBindRequest(), messageListener);
            } catch (Throwable e) {
                logger.error(String.format("Bind operation failed: %s", e.getMessage()), e);
                changeStatusToOutOfService();
            }
        }
        
        private BindRequest createBindRequest() throws SmppException {
            final BindRequest request;
            switch (config.getBindMode()) {
                case RECEIVER    : request = new BindReceiver(); break;
                case TRANSMITTER : request = new BindTransmitter(); break;
                default          : request = new BindTransciever(); break;
            }
            request.setSystemId(config.getSystemId());
            request.setPassword(config.getPassword());
            request.setSystemType(config.getSystemType());
            request.setInterfaceVersion((byte) 0x34);
            request.setAddressRange(config.getServeAddr());
            if (logger.isDebugEnabled()) 
                logger.debug("Created bind request {}", request.debugString());
            return request;
        }
    }
    
    private class MessageListener implements ServerPDUEventListener {

        public void handleEvent(ServerPDUEvent event) {
            final PDU pdu = event.getPDU();
            if (logger.isDebugEnabled())
                logger.debug("Processing received event: "+pdu.debugString());
            if (pdu.isRequest()) 
                processRequest((Request)pdu);
            else if (pdu.isRequest()) 
                processResponse((Response)pdu);
        }
        
        private void processRequest(Request req) {
            switch (req.getCommandId()) {
                case Data.UNBIND: changeStatusToOutOfService(); break;
                case Data.DATA_SM:
                case Data.DELIVER_SM: 
                    lastInteractionTime = System.currentTimeMillis();
                    fireMessageReceived(req, false); 
                    break;
                default:
                    if (logger.isDebugEnabled())
                        logger.debug("No handler for request: {}", req.debugString());
            }
        }

        private void processResponse(Response resp) {
            switch (resp.getCommandId()) {
                case Data.BIND_RECEIVER_RESP:
                case Data.BIND_TRANSCEIVER_RESP:
                case Data.BIND_TRANSMITTER_RESP:
                    lastInteractionTime = System.currentTimeMillis();
                    if (resp.getCommandStatus() == Data.ESME_ROK) changeStatusToInService();
                    else changeStatusToOutOfService();
                    break;
                case Data.ENQUIRE_LINK_RESP: 
                    if (resp.getCommandStatus() != Data.ESME_ROK) {
                        if (logger.isErrorEnabled())
                            logger.error("Problem with connection testing. Link is down. Unbinding");
                        changeStatusToOutOfService();
                    } else { 
                        if (logger.isDebugEnabled())
                            logger.debug("Link is alive.");
                        lastInteractionTime = System.currentTimeMillis();
                    }
                    break;    
                default: 
                    lastInteractionTime = System.currentTimeMillis();
                    fireMessageReceived(resp, true);
            }
        }
    }
    
    private class UnbindTask extends AbstractTask {

        public UnbindTask() {
            super(owner, logger.getPrefix()+"Unbinding...");
        }

        @Override
        public void doRun() throws Exception {
            if (status.compareAndSet(Status.IN_SERVICE, Status.OUT_OF_SERVICE)) {
                try {
                    session.unbind();
                } finally {
                    closeSession();
                }
            } else if (status.compareAndSet(Status.INITIALIZING, Status.OUT_OF_SERVICE)) 
                closeSession();
        }
        
        private void closeSession() throws Exception {
            try {
                Receiver receiver = session.getReceiver();
                if (receiver!=null)
                    receiver.stop();
            } finally {
                try {
                    session.close();
                } finally {
                    fireOutOfServiceEvent();
                }
            }
        }
    }
    
    private class CheckConnectionTask extends AbstractTask {

        public CheckConnectionTask() {
            super(owner, logger.logMess("Checking connection"));
        }

        @Override
        public void doRun() throws Exception {
            if (lastInteractionTime + config.getEnquireTimeout() <= System.currentTimeMillis()
                && status.get() == Status.IN_SERVICE) 
            {
                if (logger.isDebugEnabled()) 
                    logger.debug("Enquiring link");
                session.enquireLink();
            }
            if (status.get() != Status.OUT_OF_SERVICE)
                executeCheckConnectionTask();
        }
    }
}
