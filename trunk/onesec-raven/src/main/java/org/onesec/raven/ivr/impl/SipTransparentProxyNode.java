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
package org.onesec.raven.ivr.impl;

import gov.nist.javax.sip.SipStackImpl;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.onesec.raven.ivr.SipContext;
import org.onesec.raven.ivr.SipProxy;
import org.onesec.raven.ivr.SipProxyException;
import org.onesec.raven.ivr.SipTerminal;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.LoggerHelper;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SipTransparentProxyNode extends BaseNode implements SipProxy {
    private static enum TerminalStatus { UNREGISTERED, REGISTERING, REGISTERED };    
    
    @NotNull @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue = "5060")
    private Integer port;
    
    @NotNull @Parameter(defaultValue = "true")
    private Boolean enableTcp;
    
    @NotNull @Parameter(defaultValue = "true")
    private Boolean enableUdp;
    
    @NotNull
    private String proxyIp;
    
    @NotNull @Parameter(defaultValue = "5060")
    private Integer proxyPort;
    
    @NotNull @Parameter(defaultValue = "UDP")
    private String proxyProtocol;
    
    @Parameter
    private String ip;
    
    private AtomicReference<SipProxyContext> context;

    @Override
    protected void initFields() {
        super.initFields();
        context = new AtomicReference<SipProxyContext>();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        context.set(new SipProxyContext(executor, ip, port, proxyIp, proxyPort, proxyProtocol, enableTcp, enableUdp));
    }
    

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        SipProxyContext _context = context.getAndSet(null);
        if (_context!=null)
            _context.stop();
        //TODO: unregister all terminals and clear terminals
    }

    public synchronized boolean register(SipTerminal terminal) throws SipProxyException {
        if (!isStarted())
            return false;
        SipProxyContext _context = context.get();
        if (_context!=null)
            _context.register(terminal);
        return true;
    }

    public boolean unregister(SipTerminal terminal) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getEnableTcp() {
        return enableTcp;
    }

    public void setEnableTcp(Boolean enableTcp) {
        this.enableTcp = enableTcp;
    }

    public Boolean getEnableUdp() {
        return enableUdp;
    }

    public void setEnableUdp(Boolean enableUdp) {
        this.enableUdp = enableUdp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getProxyIp() {
        return proxyIp;
    }

    public void setProxyIp(String proxyHost) {
        this.proxyIp = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyProtocol() {
        return proxyProtocol;
    }

    public void setProxyProtocol(String proxyProtocol) {
        this.proxyProtocol = proxyProtocol;
    }

    private class Registered extends AbstractTask {
        private final SipTerminal terminal;
        private final SipProxyContext context;

        public Registered(SipTerminal terminal, SipProxyContext context) {
            super(SipTransparentProxyNode.this, "Delivering registered message");
            this.terminal = terminal;
            this.context = context;
        }

        @Override
        public void doRun() throws Exception {
            terminal.registered(context);
        }
    }
    
    private class Register extends AbstractTask {
        private final SipTerminal terminal;
        private final SipProxyContext context;

        public Register(SipTerminal terminal, SipProxyContext context, Node taskNode) {
            super(SipTransparentProxyNode.this, "Trying to register terminal "+terminal.getAddress());
            this.terminal = terminal;
            this.context = context;
        }

        @Override
        public void doRun() throws Exception {
//            Request request = context.getMessageFactory().
//            context.getSipProvider().getNewClientTransaction(null);
        }
        
    }
    
    private class LoggerAppender implements Appender {
        private final LoggerHelper logger;

        public LoggerAppender(LoggerHelper logger) {
            this.logger = new LoggerHelper(logger, "Stack. ");
        }

        public void addFilter(Filter newFilter) {
//            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Filter getFilter() {
            return null;
//            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void clearFilters() {
//                throw new UnsupportedOperationException("Not supported yet.");
        }

        public void close() {
//                throw new UnsupportedOperationException("Not supported yet.");
        }

        public void doAppend(LoggingEvent event) {
            logger.debug("Appending event: "+event.getRenderedMessage());
            final Level lvl = event.getLevel();
            if ( (lvl==Level.FATAL || lvl==Level.ERROR) && logger.isErrorEnabled())
                logger.error(event.getRenderedMessage());
            else if (lvl==Level.WARN && logger.isWarnEnabled())
                logger.warn(event.getRenderedMessage());
            else if (lvl==Level.INFO && logger.isInfoEnabled())
                logger.info(event.getRenderedMessage());
            else if (lvl==Level.DEBUG && logger.isDebugEnabled())
                logger.debug(event.getRenderedMessage());
        }

        public String getName() {
            return getPath();
//                throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setErrorHandler(ErrorHandler errorHandler) {
//                throw new UnsupportedOperationException("Not supported yet.");
        }

        public ErrorHandler getErrorHandler() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setLayout(Layout layout) {
//                throw new UnsupportedOperationException("Not supported yet.");
        }

        public Layout getLayout() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setName(String name) {
//                throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean requiresLayout() {
            return false;
        }

    }
        
    
    private class SipProxyContext implements SipListener, SipContext {
        private final ConcurrentHashMap<String, TerminalHolder> terminals = new ConcurrentHashMap<String, TerminalHolder>();
        private final SipStack sipStack;
        private final ListeningPoint tcpPoint;
        private final ListeningPoint udpPoint;
        private final SipProvider sipProvider;
        private final MessageFactory messageFactory;
        private final AddressFactory addressFactory;
        private final HeaderFactory headerFactory;
        private final LoggerHelper logger = new LoggerHelper(SipTransparentProxyNode.this, null);
        private final String ip;
        private final int port;
        private final String proxyIp;
        private final int proxyPort;
        private final String proxyProtocol;
        private final ExecutorService executor;        
        private volatile boolean valid = true;
        private final Random tagRandom = new Random();
        private final String tagPrefix = "ra-"+SipTransparentProxyNode.this.getId()+"-";
        
        public SipProxyContext(ExecutorService executor, String ip, int port, String proxyIp, int proxyPort, 
                String proxyProtocol, boolean enableTcp, boolean enableUdp) 
            throws Exception 
        {
            this.executor = executor;
            SipFactory factory = SipFactory.getInstance();
            Properties props = new Properties();
//            if (ip!=null)
//                props.put("javax.sip.IP_ADDRESS", ip);
            this.ip = ip;
            this.port = port;
            this.proxyIp = proxyIp;
            this.proxyPort = proxyPort;
            this.proxyProtocol = proxyProtocol;
            props.put("javax.sip.STACK_NAME", "raven-ivr");
            props.put("gov.nist.javax.sip.TRACE_LEVEL", "TRACE");
            props.put("javax.sip.OUTBOUND_PROXY", proxyIp+":"+proxyPort+"/"+proxyProtocol);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Creating sip stack");
            messageFactory = factory.createMessageFactory();
            headerFactory = factory.createHeaderFactory();
            addressFactory = factory.createAddressFactory();
            sipStack = factory.createSipStack(props);
            ((SipStackImpl)sipStack).addLogAppender(new LoggerAppender(logger));
            tcpPoint = createListeningPoint("tcp", enableTcp);
            udpPoint = createListeningPoint("udp", enableUdp);
//            factory.
            SipProvider _sipProvider = null;
            for (ListeningPoint point: Arrays.asList(tcpPoint, udpPoint))
                if (point!=null) {
                    if (_sipProvider==null)
                        _sipProvider = sipStack.createSipProvider(point);
                    else 
                        _sipProvider.addListeningPoint(point);
                }
            if (_sipProvider==null)
                throw new Exception("Can't start sip stack because of all transports disabled");
            sipProvider = _sipProvider;
            sipProvider.addSipListener(this);
            sipStack.start();
            if (logger.isInfoEnabled())
                logger.info("Sip stack successfully started");
            
        }

        public String generateTag() {
            return tagPrefix+Math.abs(tagRandom.nextInt());
        }
        
        public SipURI createSipURI(String address) throws ParseException {
            return addressFactory.createSipURI(address, ip);
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public ExecutorService getExecutor() {
            return executor;
        }

        public String getProxyHost() {
            return proxyIp;
        }
        
//        public create
        
        public void register(SipTerminal terminal) throws SipProxyException {
            try {
                if (logger.isDebugEnabled())
                    logger.debug(String.format("Trying to register terminal (%s) owned by (%s)", 
                        terminal.getAddress(), terminal.getOwner()));
                TerminalHolder newTerm = new TerminalHolder(terminal, this, logger);
                TerminalHolder existingTerm = terminals.putIfAbsent(
                        terminal.getAddress(), newTerm);
                if (existingTerm!=null) {
                    String mess = String.format("Terminal (%s) already registered by (%s)", 
                            terminal.getAddress(), terminal.getAddress());
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error(mess);
                    throw new SipProxyException(mess);
                }
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug(String.format("Terminal (%s) owned by (%s) successfully added", 
                            terminal.getAddress(), terminal.getOwner()));
                newTerm.register();
            } catch (SipProxyException se) {
                throw se;
            } catch (Exception e) {
                throw new SipProxyException(e);
                
            }
//            executor.executeQuietly(new Registered(terminal, this));
        }
        
        public void stop() {
            valid = false;
            sipStack.stop();
        }

        private ListeningPoint createListeningPoint(String proto, boolean flag) throws Exception {
            if (!flag) 
                return null;
            if (logger.isDebugEnabled())
                logger.debug(String.format("Creating %s listening point %s:%s", proto, ip, port));
            return sipStack.createListeningPoint(ip, port, proto);
        }

        public SipProvider getSipProvider() {
            return sipProvider;
        }

        public MessageFactory getMessageFactory() {
            return messageFactory;
        }

        public HeaderFactory getHeaderFactory() {
            return headerFactory;
        }

        public AddressFactory getAddressFactory() {
            return addressFactory;
        }
    
        public void processRequest(RequestEvent re) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processResponse(ResponseEvent re) {
//            re.getResponse().get
        }

        public void processTimeout(TimeoutEvent te) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processIOException(IOExceptionEvent ioee) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processTransactionTerminated(TransactionTerminatedEvent tte) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processDialogTerminated(DialogTerminatedEvent dte) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private abstract class ProxyTask extends AbstractTask {
        public ProxyTask(String status) {
            super(SipTransparentProxyNode.this, status);
        }
    }
    
    private class TerminalHolder {
        private volatile boolean registered = false;
        private final SipTerminal terminal;
        private final SipContext context;
        private final RegInfo regInfo;
        private volatile TerminalStatus status = TerminalStatus.UNREGISTERED;
        private final LoggerHelper logger;

        public TerminalHolder(SipTerminal terminal, SipContext context, LoggerHelper logger) throws Exception {
            this.terminal = terminal;
            this.context = context;
            this.regInfo = new RegInfo();
            this.logger = new LoggerHelper(logger, terminal.getAddress()+". ");
        }
        
        public void register() throws Exception {
            context.getExecutor().execute(new ProxyTask("Registering sip terminal "+terminal.getAddress()) {
                @Override public void doRun() throws Exception {
                    if (logger.isDebugEnabled())
                        logger.debug("Trying to register terminal on proxy ({})", context.getProxyHost());
                    SipURI requestURI = context.getAddressFactory().createSipURI(null, context.getProxyHost());
                    requestURI.setTransportParam(terminal.getProtocol());
                    
                    Address fromAddr = context.getAddressFactory().createAddress(
                            context.createSipURI(terminal.getAddress()));
                    FromHeader fromHeader = context.getHeaderFactory().createFromHeader(fromAddr, context.generateTag()); 
                    
                    Address toAddr = context.getAddressFactory().createAddress(
                            context.createSipURI(terminal.getAddress()));
                    ToHeader toHeader = context.getHeaderFactory().createToHeader(toAddr, null);
                    MaxForwardsHeader maxForwards = context.getHeaderFactory().createMaxForwardsHeader(70);
                    ViaHeader via = context.getHeaderFactory().createViaHeader(context.getIp(), context.getPort(), 
                            terminal.getProtocol(), null);
                    List<ViaHeader> viaHeaders = Arrays.asList(via);
                    Request request = context.getMessageFactory().createRequest(requestURI, Request.REGISTER, 
                            regInfo.callId, regInfo.cseq, fromHeader, toHeader, viaHeaders, maxForwards);
//                    request.s
                    ClientTransaction t = context.getSipProvider().getNewClientTransaction(request);
                    t.sendRequest();
//                    SipURI toAddress = context.createSipUri
//                    context.getHeaderFactory().createF
                }
            });
        }
        
        private class RegInfo {
            private final CallIdHeader callId;
            private final CSeqHeader cseq;
            private volatile long registrationTime = 0l;
            private volatile long lastRegistrationCheck = 0l;

            public RegInfo() throws Exception {
                this.callId = context.getSipProvider().getNewCallId();
                this.cseq = context.getHeaderFactory().createCSeqHeader(1l, Request.REGISTER);
            }
        }
        
//        private class RavenStackLogger implements StackLogger {
//            private final LoggerHelper logger;
//
//            public RavenStackLogger(LoggerHelper logger) {
//                this.logger = new LoggerHelper(logger, "Stack. ");
//            }
//
//            public void logStackTrace() {
//            }
//
//            public void logStackTrace(int traceLevel) {
//            }
//
//            public int getLineCount() {
//                return 0;
//            }
//
//            public void logException(Throwable ex) {
//                if (logger.isErrorEnabled())
//                    logger.error("Error", ex);
//            }
//
//            public void logDebug(String message) {
//                if (logger.isDebugEnabled())
//                    logger.debug(message);
//            }
//
//            public void logTrace(String message) {
//                if (logger.isTraceEnabled())
//                    logger.trace(message);
//            }
//
//            public void logFatalError(String message) {
////                if (logger.isErrorEnabled())
////                    logger.e
//            }
//
//            public void logError(String message) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public boolean isLoggingEnabled() {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public boolean isLoggingEnabled(int logLevel) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void logError(String message, Exception ex) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void logWarning(String string) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void logInfo(String string) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void disableLogging() {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void enableLogging() {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void setBuildTimeStamp(String buildTimeStamp) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void setStackProperties(Properties stackProperties) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public String getLoggerName() {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//            
//        }
        
    }
    
}
