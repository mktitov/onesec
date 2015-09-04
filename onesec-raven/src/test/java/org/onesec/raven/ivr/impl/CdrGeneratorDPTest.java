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

import java.net.InetAddress;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.CompletionCode;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrTerminal;
import org.onesec.raven.ivr.OutgoingRtpStream;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.LoggerFactory;
import static org.onesec.raven.ivr.impl.CallCdrRecordSchemaNode.*;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.RecordException;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import org.raven.test.PushDataSource;
/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class CdrGeneratorDPTest extends OnesecRavenTestCase {
    private static LoggerHelper logger = new LoggerHelper(LogLevel.TRACE, "[Send CDR] ", "", LoggerFactory.getLogger("TEST"));
    private DataCollector cdrReceiver;
    private PushDataSource cdrSource;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder);
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
    }

    @Before
    public void prepare() {
        cdrSource = new PushDataSource();
        cdrSource.setName("cdr source");
        testsNode.addAndSaveChildren(cdrSource);
        assertTrue(testsNode.start());
        
        cdrReceiver = new DataCollector();
        cdrReceiver.setName("cdr receiver");
        testsNode.addAndSaveChildren(cdrReceiver);
        cdrReceiver.setDataSource(cdrSource);
        assertTrue(cdrReceiver.start());
    }
    
    @Test
    public void postInitTest(
            final @Mocked DataProcessorFacade dpFacade,
            final @Mocked DataProcessorContext dpContext) 
        throws Exception 
    {
//        prepareFacade(dpFacade);
//        prepareContext(dpContext);
        CdrGeneratorDP dp = new CdrGeneratorDP(null, null, null, 10);
        dp.init(dpFacade, dpContext);
        new Verifications() {{
            dpFacade.sendRepeatedly(
                    CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL, 
                    CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL, 0, CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT);
            times = 1;
            
        }};
    }
    
    @Test
    public void callWithoutConversationTest(
            final @Mocked DataProcessorFacade dpFacade,
            final @Mocked DataProcessorContext dpContext,
//            final @Mocked DataSource cdrSource,
            final @Mocked RecordSchema cdrSchema,
            final @Mocked IvrEndpointConversation conv,
            final @Mocked IvrTerminal term,
            final @Mocked Record cdr,
            final @Mocked DataConsumer consumer,
            final @Mocked DataHandler dataHandler,
            final @Mocked Map<String, Object> inRtpStat,
            final @Mocked Map<String, Object> outRtpStat,
            final @Mocked Map<String, Object> asStat) 
        throws Exception 
    {
        final long callStartTime = System.currentTimeMillis();
        final long callEndTime = callStartTime+10000;
        prepareContext(dpContext);
        CdrGeneratorDP dp = new CdrGeneratorDP(cdrSource, cdrSchema, EnumSet.allOf(CdrGeneratorDP.CallEventType.class), 10);
        
        //phase 1. CDR INITIALIZATION        
        initAndCheckCallInit(dp, dataHandler, cdrSchema, cdr, conv, term, callStartTime, 
                dpFacade, dpContext);
        
        recordCallFinishedExpectations(conv, cdrSchema, cdr, callEndTime, callStartTime, inRtpStat, outRtpStat, asStat);
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_FINISHED, conv, term));
        new Verifications() {{
            dataHandler.handleData(cdr, withInstanceOf(DataContext.class));            
        }};
    }

    @Test
    public void callWithConversationTest(
            final @Mocked DataProcessorFacade dpFacade,
            final @Mocked DataProcessorContext dpContext,
//            final @Mocked DataSource cdrSource,
            final @Mocked RecordSchema cdrSchema,
            final @Mocked IvrEndpointConversation conv,
            final @Mocked IvrTerminal term,
            final @Mocked Record cdr,
            final @Mocked DataConsumer consumer,
            final @Mocked DataHandler dataHandler,
            final @Mocked Map<String, Object> inRtpStat,
            final @Mocked Map<String, Object> outRtpStat,
            final @Mocked Map<String, Object> asStat,
            final @Mocked IncomingRtpStream inRtp,
            final @Mocked OutgoingRtpStream outRtp) 
        throws Exception 
    {
        final long callStartTime = System.currentTimeMillis();
        final long callEndTime = callStartTime+10000;
        prepareContext(dpContext);
        CdrGeneratorDP dp = new CdrGeneratorDP(cdrSource, cdrSchema, EnumSet.allOf(CdrGeneratorDP.CallEventType.class), 10);
        
        //phase 1. CDR INITIALIZATION
        initAndCheckCallInit(dp, dataHandler, cdrSchema, cdr, conv, term, callStartTime, 
                dpFacade, dpContext);
        
        //pahse 2. CALL STARTED        
        final InetAddress localAddr1 = InetAddress.getLoopbackAddress();
        final InetAddress localAddr2 = InetAddress.getByName("192.168.1.1");
        final long conversationStartTime = callStartTime + 5000;
        new Expectations(){{
            conv.getCallingNumber(); result = "1234";
            cdr.setValue(CALLING_NUMBER, "1234");
            conv.getCalledNumber(); result = "4321";
            cdr.setValue(CALLED_NUMBER, "4321");
            conv.getLastRedirectedNumber(); result = "1111";
            cdr.setValue(LAST_REDIRECTED_NUMBER, "1111");
            
            conv.getConversationStartTime(); result = conversationStartTime;
            cdr.setValue(CONVERSATION_START_TIME, conversationStartTime);

            conv.getIncomingRtpStream(); result = inRtp;
            inRtp.getAddress(); result = localAddr1;
            cdr.setValue(IN_RTP_LOCAL_ADDR, localAddr1.getHostAddress());
            inRtp.getPort(); result = 1;
            cdr.setValue(IN_RTP_LOCAL_PORT, 1);                        
            inRtp.getRemoteHost(); result = "1.1.1.1";
            cdr.setValue(IN_RTP_REMOTE_ADDR, "1.1.1.1");
            inRtp.getRemotePort(); result = 10;
            cdr.setValue(IN_RTP_REMOTE_PORT, 10);
            
            conv.getOutgoingRtpStream(); result = outRtp;
            outRtp.getAddress(); result = localAddr2;
            cdr.setValue(OUT_RTP_LOCAL_ADDR, localAddr2.getHostAddress());
            outRtp.getPort(); result = 2;
            cdr.setValue(OUT_RTP_LOCAL_PORT, 2);
            outRtp.getRemoteHost(); result = "2.2.2.2";
            cdr.setValue(OUT_RTP_REMOTE_ADDR, "2.2.2.2");
            outRtp.getRemotePort(); result = 20;
            cdr.setValue(OUT_RTP_REMOTE_PORT, 20);
        }};
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_STARTED, conv, term));
        new Verifications() {{
            dataHandler.handleData(cdr, withInstanceOf(DataContext.class));            
        }};
        
        //phase 3. CONNECTION_ESTABLISHIED
        final long connectionEstablishedTime = conversationStartTime+1000;
        new Expectations(){{
            conv.getConnectionEstablishedTime(); result = connectionEstablishedTime;
            cdr.setValue(CONNECTION_ESTABLISHED_TIME, connectionEstablishedTime);
        }};
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONNECTION_ESTABLISHED, conv, term));
        new Verifications() {{
            dataHandler.handleData(cdr, withInstanceOf(DataContext.class));            
        }};
        
        //phase 4. CALL FINISH
        recordCallFinishedExpectations(conv, cdrSchema, cdr, callEndTime, callStartTime, inRtpStat, outRtpStat, asStat);
        //adding conversation duration
        new Expectations() {{
            conv.getConnectionEstablishedTime(); result = connectionEstablishedTime;
            cdr.setValue(CONVERSATION_DURATION, (callEndTime-connectionEstablishedTime)/1000);
        }};
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_FINISHED, conv, term));
        new Verifications() {{
            dataHandler.handleData(cdr, withInstanceOf(DataContext.class));            
        }};
    }
    
    @Test
    public void sendCdrOnEventsTest(
            final @Mocked DataProcessorFacade dpFacade,
            final @Mocked DataProcessorContext dpContext,
//            final @Mocked DataSource cdrSource,
            final @Mocked RecordSchema cdrSchema,
            final @Mocked IvrEndpointConversation conv,
            final @Mocked IvrTerminal term,
            final @Mocked Record cdr,
            final @Mocked DataConsumer consumer,
            final @Mocked DataHandler dataHandler,
            final @Mocked Map<String, Object> inRtpStat,
            final @Mocked Map<String, Object> outRtpStat,
            final @Mocked Map<String, Object> asStat) 
        throws Exception 
    {
        final long callStartTime = System.currentTimeMillis();
        final long callEndTime = callStartTime+10000;
        prepareContext(dpContext);
        CdrGeneratorDP dp = new CdrGeneratorDP(cdrSource, cdrSchema, EnumSet.of(CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED), 10);
        
        //phase 1. CDR INITIALIZATION
        initAndCheckCallInit(dp, dataHandler, cdrSchema, cdr, conv, term, callStartTime, dpFacade, dpContext);
        
        recordCallFinishedExpectations(conv, null, cdr, callEndTime, callStartTime, inRtpStat, outRtpStat, asStat);
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_FINISHED, conv, term));
        new Verifications() {{
            dataHandler.handleData(any, (DataContext) any); times=0;
        }};
    }
    
    @Test
    public void cdrCompleteTimeoutTest(
            final @Mocked DataProcessorFacade dpFacade,
            final @Mocked DataProcessorContext dpContext,
//            final @Mocked DataSource cdrSource,
            final @Mocked RecordSchema cdrSchema,
            final @Mocked DataHandler dataHandler,
            final @Mocked IvrEndpointConversation conv1,
            final @Mocked IvrEndpointConversation conv2,
            final @Mocked IvrTerminal term1,
            final @Mocked IvrTerminal term2,
            final @Mocked Record cdr1,
            final @Mocked Record cdr2) 
        throws Exception 
    {
        prepareContext(dpContext);
        CdrGeneratorDP dp = new CdrGeneratorDP(cdrSource, cdrSchema, EnumSet.of(CdrGeneratorDP.CallEventType.CONVERSATION_FINISHED), 1000);
        cdrReceiver.setDataHandler(dataHandler);
        new Expectations(){{
            cdrSchema.createRecord(); result = cdr1;
            conv1.getConversationId(); result ="conv_1";
            conv1.getCallStartTime(); result = System.currentTimeMillis()-1500;
            //
        }};
        dp.init(dpFacade, dpContext);
        
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED, conv1, term1));
//        dp.processData(new SendCallCdrDP.CallEvent(SendCallCdrDP.CallEventType.CONVERSATION_INITIALIZED, conv2, term2));        
        dp.processData(CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT);
        
        new Verifications() {{
            cdr1.setValue(COMPLETION_CODE, CdrGeneratorDP.CDR_COMPLETION_TOO_LONG);
            dataHandler.handleData(cdr1, withInstanceOf(DataContext.class));            
        }};
        
        new Expectations(){{
            cdrSchema.createRecord(); result = cdr2;
            conv2.getConversationId(); result ="conv_2";
            conv2.getCallStartTime(); result = System.currentTimeMillis();
        }};
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED, conv2, term2));        
        dp.processData(CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT);
        new Verifications() {{
            cdr2.setValue(COMPLETION_CODE, CdrGeneratorDP.CDR_COMPLETION_TOO_LONG); times=0;
            dataHandler.handleData(cdr2, withInstanceOf(DataContext.class)); times=0;
        }};
        
    }
    
    private void recordCallFinishedExpectations(final IvrEndpointConversation conv, final RecordSchema cdrSchema, final Record cdr, final long callEndTime, final long callStartTime, final Map<String, Object> inRtpStat, final Map<String, Object> outRtpStat, final Map<String, Object> asStat) throws RecordException {
        //phase 2. CALL FINISH
        new Expectations() {{
            conv.getCallingNumber(); result = "1234";
            cdr.setValue(CALLING_NUMBER, "1234");
            conv.getCalledNumber(); result = "4321";
            cdr.setValue(CALLED_NUMBER, "4321");
            conv.getLastRedirectedNumber(); result = "1111";
            cdr.setValue(LAST_REDIRECTED_NUMBER, "1111");
            
            conv.getCompletionCode(); result = CompletionCode.COMPLETED_BY_ENDPOINT;
            cdr.setValue(COMPLETION_CODE, CompletionCode.COMPLETED_BY_ENDPOINT);
            conv.getConnectionEstablishedTime(); result = 0l;
            conv.getCallEndTime(); result = callEndTime;
            cdr.setValue(CALL_END_TIME, callEndTime);
            cdr.setValue(CALL_DURATION, (callEndTime-callStartTime)/1000);
            conv.getIncomingRtpStat(); result = inRtpStat;
            cdr.setValues(inRtpStat);
            conv.getOutgoingRtpStat(); result = outRtpStat;
            cdr.setValues(outRtpStat);            
            conv.getAudioStreamStat(); result = asStat;
            cdr.setValues(asStat);
            
        }};
            //send cdr
        if (cdrSchema!=null) {
            new Expectations(){{
                cdrSchema.createRecord(); result = cdr;
                cdr.copyFrom(cdr);
                cdr.setTag("eventType", CdrGeneratorDP.CallEventType.CONVERSATION_FINISHED.name());
            }};
        }            
        
    }
    
    private void initAndCheckCallInit(CdrGeneratorDP dp, final DataHandler dataHandler, final RecordSchema cdrSchema, 
            final Record cdr, final IvrEndpointConversation conv, final IvrTerminal term, 
            final long callStartTime, final DataProcessorFacade dpFacade, 
            final DataProcessorContext dpContext) 
        throws Exception, RecordException 
    {
        cdrReceiver.setDataHandler(dataHandler);
        new Expectations(){{
            cdrSchema.createRecord(); result = cdr;
            conv.getConversationId(); result ="conv_1";
            cdr.setValue(ID, "conv_1");
            term.getAddress(); result = "0000";
            cdr.setValue(ENDPOINT_ADDRESS, "0000");
            conv.getCallStartTime(); result = callStartTime;
            cdr.setValue(CALL_START_TIME, callStartTime);
            //send cdr
            cdrSchema.createRecord(); result = cdr;
            cdr.copyFrom(cdr);
            cdr.setTag("eventType", CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED.name());
        }};
        dp.init(dpFacade, dpContext);
        dp.processData(new CdrGeneratorDP.CallEvent(CdrGeneratorDP.CallEventType.CONVERSATION_INITIALIZED, conv, term));
        new Verifications() {{
            dataHandler.handleData(cdr, withInstanceOf(DataContext.class));
        }};
    }
    
    public void prepareFacade(final DataProcessorFacade facade) throws Exception {
        new Expectations() {{
            facade.sendRepeatedly(
                    CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL, 
                    CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT_INTERVAL, 0, CdrGeneratorDP.CHECK_CDR_COMPLETE_TIMEOUT);
        }};
    }
    
    public void prepareContext(final DataProcessorContext context) {
        new Expectations() {{
            context.getLogger(); result = logger; minTimes=0;
        }};
    }

}
