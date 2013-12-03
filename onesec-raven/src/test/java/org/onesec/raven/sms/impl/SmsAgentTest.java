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
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.AddressRange;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitSM;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.onesec.raven.NodeAdapter;
import static org.onesec.raven.ivr.conference.impl.RealTimeConferenceMixerTest.executeTask;
import org.onesec.raven.ivr.impl.ContainerParserDataSource;
import org.onesec.raven.sms.BindMode;
import org.onesec.raven.sms.SmsAgentListener;
import org.onesec.raven.sms.SmsConfig;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.test.ServiceTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SmsAgentTest extends ServiceTestCase {
    private final static Logger logger = LoggerFactory.getLogger(ContainerParserDataSource.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "SMS. ", null, logger);
    private IMocksControl mainMocks;
    private IMocksControl testMocks;
    private Node owner;
    private ExecutorService executorMock;
    private ExecutorService executor;
    private volatile boolean binded;
    private volatile boolean unbinded;
    
    @Before
    public void prepare() throws Exception {
        testMocks = createControl();
        trainMocks();
        mainMocks.replay();
        executor = new Executor();
//        replay(executor, owner);
    }
    
    @After
    public void finish() throws Exception {
        mainMocks.verify();
        testMocks.verify();
    }
    
//    @Test(timeout = 10000)
    public void bindTest() throws Exception {
        SmsConfig config = trainConfig();
        SmsAgentListener listener = trainAgentListener();
        testMocks.replay();
        binded = false;
        unbinded = false;
        SmsAgent agent = new SmsAgent(config, listener, executor, owner, loggerHelper);
        while (!binded) Thread.sleep(100);
        while (!unbinded) Thread.sleep(100);
    }
    
    @Test(timeout = 10000)
//    @Test
    public void submitTest() throws Exception {
        SmsConfig config = trainConfigForSubmit();
        SmsAgentListener listener = trainAgentListenerForSubmit(config);
        testMocks.replay();
        binded = false;
        unbinded = false;
        SmsAgent agent = new SmsAgent(config, listener, executor, owner, loggerHelper);
        while (!binded) Thread.sleep(100);
        while (!unbinded) Thread.sleep(100);
        
    }
    
    private SmsConfig trainConfig() throws Exception {
        SmsConfig config = testMocks.createMock(SmsConfig.class);
        expect(config.getBindAddr()).andReturn(privateProperties.getProperty("smsc_address")).atLeastOnce();
        expect(config.getBindPort()).andReturn(Integer.parseInt(privateProperties.getProperty("smsc_port"))).atLeastOnce();
        expect(config.getBindTimeout()).andReturn(9000);
        expect(config.getSoTimeout()).andReturn(100);
        expect(config.getReceiveTimeout()).andReturn(100);
        expect(config.getBindMode()).andReturn(BindMode.RECEIVER_AND_TRANSMITTER).atLeastOnce();
        expect(config.getSystemId()).andReturn(privateProperties.getProperty("sms_system_id"));
        expect(config.getPassword()).andReturn(privateProperties.getProperty("sms_passwd"));
        expect(config.getSystemType()).andReturn("");
        expect(config.getServeAddr()).andReturn(new AddressRange((byte)5, (byte)0, 
                privateProperties.getProperty("sms_addr_range")));
        expect(config.getEnquireTimeout()).andReturn(90000).atLeastOnce();
        return config;
    }
    
    private SmsConfig trainConfigForSubmit() throws Exception {
        SmsConfig config = trainConfig();
        expect(config.getDstTon()).andReturn((byte)1);
        expect(config.getDstNpi()).andReturn((byte)1);
        expect(config.getLongSmMode()).andReturn(3).anyTimes();
        expect(config.getDataCoding()).andReturn((byte)8).anyTimes();
        expect(config.getSrcAddr())
            .andReturn(new Address((byte)5, (byte)0, privateProperties.getProperty("sms_from_addr")))
            .atLeastOnce();
        expect(config.getReplaceIfPresentFlag()).andReturn((byte)0).anyTimes();
        expect(config.getEsmClass()).andReturn((byte)0).anyTimes();
        expect(config.getProtocolId()).andReturn((byte)0).anyTimes();
        expect(config.getPriorityFlag()).andReturn((byte)0).anyTimes();
        expect(config.getRegisteredDelivery()).andReturn((byte)0).anyTimes();
        expect(config.getSmDefaultMsgId()).andReturn((byte)0).anyTimes();
        expect(config.getServiceType()).andReturn("").anyTimes();
        expect(config.getMessageCP()).andReturn("cp1251").anyTimes();
        expect(config.isUse7bit()).andReturn(false).anyTimes();
        return config;
    }
    
    private SmsAgentListener trainAgentListener() {
        SmsAgentListener res = testMocks.createMock(SmsAgentListener.class);
        res.inService(isA(SmsAgent.class));
        expectLastCall().andDelegateTo(new SmsAgentListenerAdapter() {
            @Override public void inService(SmsAgent agent) {
                binded = true;
                agent.unbind();
            }
        });
        res.outOfService(isA(SmsAgent.class));
        expectLastCall().andDelegateTo(new SmsAgentListenerAdapter(){
            @Override public void outOfService(SmsAgent agent) {
                unbinded = true;
            }
        });
        return res;
    }
    
    private SmsAgentListener trainAgentListenerForSubmit(final SmsConfig config) {
        SmsAgentListener res = testMocks.createMock(SmsAgentListener.class);
        res.inService(isA(SmsAgent.class));
        expectLastCall().andDelegateTo(new SmsAgentListenerAdapter(){
            @Override public void inService(SmsAgent agent) {
                binded = true;
                SmsMessageEncoderImpl encoder = new SmsMessageEncoderImpl(config, loggerHelper);
                try {
//                    agent.submit(encoder.encode("Test message", privateProperties.getProperty("sms_dst_addr"))[0]);
//                    SubmitSM[] frags = encoder.encode("Тестовое сообщение 1 Тестовое сообщение 2 Тестовое сообщение 3 Тестовое сообщение 4 Тестовое сообщение 5"
//                            , privateProperties.getProperty("sms_dst_addr"));
                    SubmitSM[] frags = encoder.encode(
                            "Test", privateProperties.getProperty("sms_dst_addr"), config.getSrcAddr());
                    System.out.println(String.format("\n\nSUBMITING (%s) fragments.\n\n", frags.length));
                    for (SubmitSM frag: frags)
                        agent.submit(frag);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        });
        res.responseReceived(isA(SmsAgent.class), isA(Response.class));
        expectLastCall().andDelegateTo(new SmsAgentListenerAdapter(){
            @Override public void responseReceived(SmsAgent agent, Response pdu) {
                agent.unbind();
                if (pdu.getCommandStatus() != Data.ESME_ROK)
                    fail("Received error on submit");
            }
        });
        res.outOfService(isA(SmsAgent.class));
        expectLastCall().andDelegateTo(new SmsAgentListenerAdapter(){
            @Override public void outOfService(SmsAgent agent) {
                unbinded = true;
            }
        });
        return res;
    }
    
    private void trainMocks() throws ExecutorServiceException {
        mainMocks = createControl();
        executorMock = mainMocks.createMock("executor", ExecutorService.class);
        owner = mainMocks.createMock("owner", Node.class);
        
        executorMock.execute(executeTask(owner));
        expectLastCall().anyTimes();
//        expect(executor.executeQuietly(executeTask(owner))).andReturn(true);
//        expect(executor.executeQuietly(anyLong(), executeTask(owner))).andReturn(true);
//        expectLastCall().anyTimes();
        expect(owner.getLogger()).andReturn(logger).anyTimes();
        expect(owner.getLogLevel()).andReturn(LogLevel.TRACE).anyTimes();
        expect(owner.getName()).andReturn("owner").anyTimes();
        expect(owner.isLogLevelEnabled(anyObject(LogLevel.class))).andReturn(Boolean.TRUE).anyTimes();
    }
    
    public static Task executeTask(final Node owner) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                final Task task = (Task) argument;
                assertSame(owner, task.getTaskNode());
                new Thread(new Runnable() {
                    public void run() {
                        task.run();
//                        ++tasksFinished;
                    }
                }).start();
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
        });
        return null;
    }

    public static Task executeTask(final Node owner, final long delay) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                final Task task = (Task) argument;
                assertSame(owner, task.getTaskNode());
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        task.run();
//                        ++tasksFinished;
                    }
                }).start();
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
        });
        return null;
    }
    
    private class Executor extends NodeAdapter implements ExecutorService {

        public void execute(Task task) throws ExecutorServiceException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void execute(long delay, Task task) throws ExecutorServiceException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean executeQuietly(final Task task) {
            new Thread(new Runnable() {
                public void run() {
                    task.run();
                }
            }).start();
            return true;
        }

        public boolean executeQuietly(final long delay, final Task task) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(delay);
                        task.run();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
//                        ++tasksFinished;
                }
            }).start();
            return true;
        }
    }
    
    private class SmsAgentListenerAdapter implements SmsAgentListener {

        public void inService(SmsAgent agent) {
        }

        public void responseReceived(SmsAgent agent, Response pdu) {
        }

        public void requestReceived(SmsAgent agent, Request pdu) {
        }

        public void outOfService(SmsAgent agent) {
        }
    }
}