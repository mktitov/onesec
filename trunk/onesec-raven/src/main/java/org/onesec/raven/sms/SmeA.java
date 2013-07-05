package org.onesec.raven.sms;

/**
 * @author psn
 */
import java.net.SocketException;
import com.logica.smpp.Data;
import com.logica.smpp.Receiver;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.Session;
import com.logica.smpp.SmppException;
import com.logica.smpp.SmppObject;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.AddressRange;
import com.logica.smpp.pdu.BindReceiver;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.BindResponse;
import com.logica.smpp.pdu.BindTransciever;
import com.logica.smpp.pdu.BindTransmitter;
import com.logica.smpp.pdu.EnquireLink;
import com.logica.smpp.pdu.EnquireLinkResp;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.SubmitSMResp;
import com.logica.smpp.pdu.UnbindResp;
import com.logica.smpp.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author psn
 */
public class SmeA {

    private static Logger log = LoggerFactory.getLogger(SmeA.class);
    /**
     * This is the SMPP session used for communication with SMSC.
     */
    private Session session = null;
    /**
     * If the application is bound to the SMSC.
     */
    private boolean bound = false;
    /**
     * If the application has to keep reading commands from the keyboard and to do what's requested.
     */
    private boolean asynchronous = true;
    /**
     * IP Address of the SMSC.
     */
    private String smscIpAddr = null;
    /**
     * The port number to bind to on the SMSC server.
     */
    private int smscIpPort = 0;
    /**
     * The name which identifies you to SMSC.
     */
    private String systemId = null;
    /**
     * The password for authentication to SMSC.
     */
    private String password = null;
    /**
     * How you want to bind to the SMSC: transmitter (t), receiver (r) or transceiver (tr).
     * Transceiver can both send messages and receive messages.
     */
    private String bindMode = "tr";
    /**
     * The range of addresses the smpp session will serve.
     */
    private AddressRange serveAddr = new AddressRange();
    /*
     * for information about these variables have a look in SMPP 3.4 specification
     */
    private String systemType = "";
    private Address smscAddr = null;
    private byte registeredDelivery = 0;
    private PduEventListener pduListener = null;
    private int cntEnqLinkAttempt = 0;
    private long lastBindAttemptTime = 0;
    private long lastEnqLinkAttemptTime = 0;
    private long lastPduRcvTime = 0;
    private ISmeConfig smePar;
    private boolean needUnbind = false;
    private boolean anyPduClearEnqLinkStatus = true;
    /**
     * If you attemt to receive message, how long will the application wait for data.
     */
    protected int bindTimeout = 30 * 1000;
    private long receiveTimeout = Data.RECEIVE_BLOCKING;
    private long soTimeout = 100;
    private long queueWaitTimeout = 50;

    public SmeA(ISmeConfig smeParams) throws SmppException {
        this.smePar = smeParams;
        setServeAddr(new AddressRange((byte) smePar.getBindTon(), (byte) smePar.getBindNpi(), smePar.getAddrRange()));
    }

    public void setSmscIpAddr(String ipa) {
        smscIpAddr = ipa;
    }

    public String getSmscIpAddr() {
        return smscIpAddr;
    }

    public void setSmscIpPort(int ipp) {
        smscIpPort = ipp;
    }

    public int getSmscIpPort() {
        return smscIpPort;
    }

    public void setSystemId(String arg) {
        systemId = arg;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setPassword(String arg) {
        password = arg;
    }

    public String getPassword() {
        return password;
    }

    public void setBindMode(String arg) {
        bindMode = arg;
    }

    public String getBindMode() {
        return bindMode;
    }

    public void setReceiveTimeout(long arg) {
        receiveTimeout = arg;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

//  public void setBindTimeout(int arg) { bindTimeout = arg; }
//  public int getBindTimeout() { return bindTimeout; }
    public void setSmscAddr(Address a) {
        smscAddr = a;
    }

    public Address getSmscAddr() {
        return smscAddr;
    }

//  public void setSrcAddr(Address a) { srcAddr = a; }
//  public void setDstAddr(Address a) { dstAddr = a; }
    public void setServeAddr(AddressRange a) {
        serveAddr = a;
    }

    public void setRegisteredDelivery(byte a) {
        if (a != 0) {
            a = 1;
        }
        registeredDelivery = a;
    }

    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }
    /*
     * public void setMessageCP(String arg) { if(arg.compareToIgnoreCase("UCS2")==0 ||
     * arg.compareToIgnoreCase("UTF-16")==0) { messageCP = Data.ENC_UTF16_BE; dataCoding = (byte)8;
     * } else try { "somestr".getBytes(arg); messageCP = arg; } catch (Exception e) { messageCP =
     * "US-ASCII"; log.error("Bad codepage ("+arg+"), replace to US-ASCII");} log.info("Set
     * cp="+messageCP+" dc="+dataCoding); }
     */
//  public String getMessageCP() { return messageCP; }

    /*
     * public SmeParams getSmeParams(int x) { SmeParams sp = new SmeParams();
     * sp.setSystemType(systemType); sp.setValidityPeriod(validityPeriod);
     * sp.setRegisteredDelivery(registeredDelivery); //sp.setUseSarTags(useSarTags); return sp; }
     */
    //------------------------------------------------------------------------------
    private BindRequest prepareBindRequest() throws SmppException {
        BindRequest request;
        if (isReceiver() && isTransmitter()) {
            request = new BindTransciever();
        } else if (isTransmitter()) {
            request = new BindTransmitter();
        } else {
            request = new BindReceiver();
        }
        request.setSystemId(systemId);
        request.setPassword(password);
        request.setSystemType(systemType);
        request.setInterfaceVersion((byte) 0x34);
        request.setAddressRange(serveAddr);
        if (log.isDebugEnabled()) {
            log.debug("Bind request {}", request.debugString());
        }
        return request;
    }

    private Session prepareSession() {
        TCPIPConnection connection = new TCPIPConnection(smscIpAddr, smscIpPort);
        connection.setReceiveTimeout(bindTimeout);
        connection.setCommsTimeout(soTimeout);
        Session session = new Session(connection);
//        session.setQueueWaitTimeout(queueWaitTimeout);
        return session;
    }

    /**
     */
    public boolean bind() {
        try {
            if (isBound()) {
                log.warn("Already bound, unbind first.");
                return true;
            }
            if (log.isDebugEnabled()) {
                log.debug("Try bind to SMSC: addr={} port={}", smscIpAddr, smscIpPort);
            }
            setLastPduRcvTime(0);
            clearEnqLinkAttempts();
            lastBindAttemptTime = System.currentTimeMillis();
            session = prepareSession();
            //session.g
            BindRequest request = prepareBindRequest();
            BindResponse response;
            if (asynchronous) {
                pduListener = new PduEventListener(session, log);
                response = session.bind(request, pduListener);
            } else {
                response = session.bind(request);
            }
            if (log.isInfoEnabled()) {
                log.info("Bind response {}", response.debugString());
            }
            if (response.getCommandStatus() == Data.ESME_ROK) {
                lastPduRcvTime = System.currentTimeMillis();
                setBound(true);
                session.getConnection().setReceiveTimeout(receiveTimeout);
                log.warn("Bind OK");
            }
        } catch (Exception e) {
            log.error("Bind operation failed: {}", e.getMessage());
        }
        return isBound();
    } //end bind()

    /**
     */
    public void unbind() {
        if (!isBound()) {
            log.warn("Not bound, can't unbind.");
            return;
        }
        log.debug("Going to unbind");
        try {
            UnbindResp response = session.unbind();
            log.debug("Unbind response {}", response.debugString());
        } catch (Exception e) {
            log.error("on unbind:", e);
        }
        setBound(false);
        //pduListener.
        try {
            Receiver rr = session.getReceiver();
            if (rr != null) {
                rr.stop();
            }
        } catch (Exception e) {
            log.error("Hmm...", e);
        }
        try {
            session.close();
        } catch (Exception e) {
            log.error("Hmmm...", e);
        } finally {
            setLastBindAttemptTime(0);
            log.info("Session closed");
        }
    } // end unbind()

    /**
     * If bound, unbinds and then exits this application.
     */
    protected void exit() {
        if (isBound()) {
            unbind();
        }
        log.warn("Exit...");
    }

    /**
     */
    protected void finalize() {
        exit();
    }

    private void clearEnqLinkAttempts() {
        setCntEnqLinkAttempt(0);
        lastEnqLinkAttemptTime = 0;
    }

    private void updateEnqLinkAttempts() {
        setCntEnqLinkAttempt(getCntEnqLinkAttempt() + 1);
        lastEnqLinkAttemptTime = System.currentTimeMillis();
    }

    public boolean enquireLink() {
        final String mes = "enquireLink failed. ";
        try {
            EnquireLink request = new EnquireLink();
            if (log.isInfoEnabled()) {
                log.info("Enquire Link request {}", request.debugString());
            }
            updateEnqLinkAttempts();
            EnquireLinkResp response = session.enquireLink(request);
            if (asynchronous) {
                return false;
            }
            if (response != null) {
                if (log.isInfoEnabled()) {
                    log.info("Enquire Link response {}", response.debugString());
                }
            } else {
                log.warn(mes);
            }
        } catch (SocketException ee) {
            log.error("lost connection, unbind", ee);
            unbind();
        } catch (Exception e) {
            log.error(mes, e);
        }
        return isBound();
    } // end enquireLink()

    public boolean isTransmitter() {
        return bindMode.indexOf("t") != -1;
    }

    public boolean isReceiver() {
        return bindMode.indexOf("r") != -1;
    }

    public int init() {
        return 0;
    }
    /**
     * Отправляет сообщение
     * <code>shortMessage</code>. Максимальная длина: US-ASCII - 254 символа, UCS2 - 127 символов.
     * Возвращает 0 если OK.
     */
    private String errNotTr = "I'm not Transmitter :(";
    private String errSubmitFail = "Submit operation failed. ";

    public SubmitSMResp submit254(SubmitSM request) throws SmppException {
        SubmitSMResp response = null;
        if (isTransmitter() == false) {
            log.error(errNotTr);
            throw new SmppException(errNotTr);
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Submit request {}", request.debugString());
            }
            if (asynchronous == false) {
                response = session.submit(request);
                if (response != null && log.isDebugEnabled()) {
                    log.debug("Submit response {}", response.debugString());
                }
            } else {
                session.submit(request);
                response = new SubmitSMResp();
                response.setCommandStatus(666);
            }
        } catch (Exception e) {
            log.error(errSubmitFail, e);
            this.lastPduRcvTime = 0;
            //try {Thread.sleep(50); } catch(Exception ee) {}
            throw new SmppException();
        }
        if (response.getCommandStatus() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Submit OK. MessageID={}", response.getMessageId());
            }
        } else {
            if (response.getCommandStatus() == 666) {
                log.info("Submit async OK.");
            } else {
                log.error("Submit error. Status={}", Integer.toHexString(response.getCommandStatus()));
            }
        }
        return response;
    } // end submit()

//	------------------------------------------------------------------------------    
    public PDU receive() {
        //if(isReceiver()==false) { log.error("I'm not Receiver !"); return null; }
        PDU pdu = null;
        try {
            if (log.isTraceEnabled()) {
                log.trace("Going to receive a PDU. Async={}", asynchronous);
                if (receiveTimeout == Data.RECEIVE_BLOCKING) {
                    log.trace("Application wait incoming PDU");
                } else {
                    log.trace("The receive timeout is {} sec.", receiveTimeout / 1000);
                }
            }
            if (asynchronous) {
                ServerPDUEvent pduEvent = pduListener.getRequestEvent(receiveTimeout / 10);
                if (pduEvent != null) {
                    pdu = pduEvent.getPDU();
                }
            } else {
                pdu = session.receive(receiveTimeout);
            }

            if (pdu == null) {
                log.trace("No PDU received this time.");
                return null;
            }
            lastPduRcvTime = System.currentTimeMillis();
            if (anyPduClearEnqLinkStatus) {
                clearEnqLinkAttempts();
            }
            if (pdu.isRequest()) {
                return onRcvRequest((Request) pdu);
            }
            if (pdu.isResponse()) {
                return onRcvResponse((Response) pdu);
            }
            log.info("Rcv: {}. Received PDU is unknown !", pdu.debugString());
        } catch (Exception e) {
            log.error("Receiving failed. ", e);
        }
        return null;
    }

    private Request onRcvRequest(Request pdu) {
        log.debug("PDU is request !");
        try {
            if (pdu.canResponse()) {
                Response response = pdu.getResponse();
                if (log.isDebugEnabled()) {
                    log.debug("Going to send default response to request {}", response.debugString());
                }
                session.respond(response);
            }
            log.info("Rcv: {}", pdu.debugString());
            switch (pdu.getCommandId()) {
                case Data.UNBIND:
                    needUnbind = true;
                case Data.ENQUIRE_LINK:
                    return null;
                case Data.DATA_SM:
                case Data.DELIVER_SM:
                    return pdu;
                default:
                    log.warn("Hmm.., received request with CommandId={}", pdu.getCommandId());
                    return null;
            }
        } catch (Exception e) {
            log.error("Hmm...", e);
            return null;
        }
    }

    private Response onRcvResponse(Response resp) {
        if (log.isInfoEnabled()) {
            log.info("Rcv resp: {}", resp.debugString());
        }
        switch (resp.getCommandId()) {
            case Data.BIND_RECEIVER_RESP:
            case Data.BIND_TRANSCEIVER_RESP:
            case Data.BIND_TRANSMITTER_RESP:
                if (resp.getCommandStatus() == Data.ESME_ROK) {
                    setBound(true);
                    log.warn("Bind OK (async)");
                } else {
                    setBound(false);
                    log.warn("Bind Error (async): {}", resp.getCommandStatus());
                }
                return null;
            case Data.ENQUIRE_LINK_RESP:
                if (resp.getCommandStatus() == Data.ESME_ROK) {
                    log.info("EnquireLink OK (async)");
                    clearEnqLinkAttempts();
                } else {
                    log.warn("Hmm..., enquireLinkResp status = {}", resp.getCommandStatus());
                }
                return null;
        }
        return resp;
    }

    private class PduEventListener extends SmppObject implements ServerPDUEventListener {
        //private static Log log = LogFactory.getLog(SMPPTestPDUEventListener.class);

        Logger log;
        Session session;
        Queue requestEvents = new Queue();

        public PduEventListener(Session session, Logger log) {
            this.session = session;
            this.log = log;
        }

        public void handleEvent(ServerPDUEvent event) {
            PDU pdu = event.getPDU();
            if (pdu.isRequest() || pdu.isResponse()) {
                synchronized (requestEvents) {
                    requestEvents.enqueue(event);
                    requestEvents.notify();
                }
                return;
            }
            log.warn("pdu of unknown class received, discarding: {}", pdu.debugString());
        }

        /**
         * Returns received pdu from the queue. If the queue is empty, the method blocks for the
         * specified timeout.
         */
        public ServerPDUEvent getRequestEvent(long timeout) {
            ServerPDUEvent pduEvent = null;
            synchronized (requestEvents) {
                if (requestEvents.isEmpty()) {
                    try {
                        requestEvents.wait(timeout);
                    } catch (InterruptedException e) {
                    }
                    return null;
                }
                if (!requestEvents.isEmpty()) {
                    pduEvent = (ServerPDUEvent) requestEvents.dequeue();
                }
            }
            return pduEvent;
        }
    }

    /**
     * @return Returns the queueWaitTimeout.
     */
    public long getQueueWaitTimeout() {
        return queueWaitTimeout;
    }

    /**
     * @param queueWaitTimeout The queueWaitTimeout to set.
     */
    public void setQueueWaitTimeout(long queueWaitTimeout) {
        this.queueWaitTimeout = queueWaitTimeout;
    }

    public long getLastBindAttemptTime() {
        return lastBindAttemptTime;
    }

    public void setLastBindAttemptTime(long lastBindAttemptTime) {
        this.lastBindAttemptTime = lastBindAttemptTime;
    }

    public long getLastEnqLinkAttemptTime() {
        return lastEnqLinkAttemptTime;
    }

    public void setLastEnqLinkAttemptTime(long lastEnqLinkAttemptTime) {
        this.lastEnqLinkAttemptTime = lastEnqLinkAttemptTime;
    }

    public long getLastPduRcvTime() {
        return lastPduRcvTime;
    }

    public void setLastPduRcvTime(long lastPduRcvTime) {
        this.lastPduRcvTime = lastPduRcvTime;
    }

    public synchronized boolean isBound() {
        return bound;
    }

    private synchronized void setBound(boolean bound) {
        this.bound = bound;
    }

    public synchronized int getCntEnqLinkAttempt() {
        return cntEnqLinkAttempt;
    }

    private synchronized void setCntEnqLinkAttempt(int cntEnqLinkAttempt) {
        this.cntEnqLinkAttempt = cntEnqLinkAttempt;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

//public ISmeConfig getSmePar() {	return smePar; }
/*
     * public void setSmePar(ISmeConfig smePar) throws SmppException { this.smePar = smePar;
     * setServeAddr(new
     * AddressRange((byte)smePar.getBindTon(),(byte)smePar.getBindNpi(),smePar.getAddrRange()));
     *
     * }
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    public boolean isNeedUnbind() {
        return needUnbind;
    }

    public void setNeedUnbind(boolean needUnbind) {
        this.needUnbind = needUnbind;
    }
    private static final String replArTo[][] = {
        {"\\^", "\u001b\u0014"},
        {"\\{", "\u001b\u0028"},
        {"\\}", "\u001b\u0029"},
        {"\\\\", "\u001b\u002f"},
        {"\\[", "\u001b\u003c"},
        {"~", "\u001b\u003d"},
        {"\\]", "\u001b\u003e"},
        {"\\|", "\u001b\u0040"}
// ,"\u00a4","\u001b\u0065"
    };
    private static final String replArFrom[][] = {
        {"\u001b\u0014", "^"},
        {"\u001b\\\u0028", "{"},
        {"\u001b\\\u0029", "}"},
        {"\u001b\u002f", "\\"},
        {"\u001b\u003c", "["},
        {"\u001b\u003d", "~"},
        {"\u001b\u003e", "]"},
        {"\u001b\u0040", "|"}
// ,"\u00a4","\u001b\u0065"
    };

    /*
     * {"\u000c" , "\\e\\n", "\\^" , "\\e\u0014" , "\\\\" , "\\e\u002f" , "\\[" , "\\e\u003c" , "~"
     * , "\\e\u003d", "\\]" , "\\e\u003e", "|" , "\\e\u0040", "\u00a4" , "\\e\u0065"};
     */
//tmp = tmp.replaceAll("\\{","\u001b\u0028");
    public static String toFromGSM_0338(String in, String[][] mx) {
        String out = in;
//	if(log.isTraceEnabled())
//		log.trace("before convert='{}' to GSM_0338 ",out);
        //String[][] mx = to ? replArTo : replArFrom;
        for (int i = 0; i < mx.length; i++) {
            String[] ra = mx[i];
            try {
                out = out.replaceAll(ra[0], ra[1]);
            } catch (Exception e) {
                log.error("error on replaceAll '{}' to '{}' at {} : {}", new Object[]{ra[0], ra[1], i, e});
            }
        }
//	if(log.isTraceEnabled())
//		log.trace("after replace={}",out);
        return out;
    }

    public static String fromGSM_0338(String in) {
        return toFromGSM_0338(in, replArFrom);
    }

    public static String toGSM_0338(String in) {
        return toFromGSM_0338(in, replArTo);
    }

    public long getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(long soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getBindTimeout() {
        return bindTimeout;
    }

    public void setBindTimeout(int bindTimeout) {
        this.bindTimeout = bindTimeout;
    }
}
