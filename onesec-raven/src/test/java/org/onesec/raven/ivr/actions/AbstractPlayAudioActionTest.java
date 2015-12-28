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
package org.onesec.raven.ivr.actions;

import java.io.InputStream;
import javax.script.Bindings;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.CompletedFuture;
import org.raven.expr.BindingSupport;
import org.raven.test.TestDataProcessorFacade;
import org.raven.tree.DataFile;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class AbstractPlayAudioActionTest extends ActionTestCase {
    
    @Test
    public void playAudioFileTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final AudioFile audioFile,
            @Mocked final DataFile audioData,
            @Mocked final InputStream audioIs) 
        throws Exception 
    {
        new Expectations() {{
            executeMessage.getConversation(); result = conv;
            conv.getAudioStream(); result = audioStream;
            audioFile.isCacheable(); result = true;
            audioFile.getAudioFile(); result = audioData;
            audioData.getDataStream(); result = audioIs;
            audioStream.addSource(anyString, anyLong, (InputStreamSource) any); result = new Delegate() {
                RavenFuture addSource(String key, long checksum, InputStreamSource source) {
                    source.getInputStream();
                    return new CompletedFuture(null, executor);
                }
            };
        }};
        
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestPlayAudioAction(null, audioFile, null));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
    }
    
    @Test
    public void getBindingSupportTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final ConversationScenarioState state,
            @Mocked final Bindings bindings,
            @Mocked final BindingSupport bindingSupport,
            @Mocked final AudioStream audioStream,
            @Mocked final AudioFile audioFile,
            @Mocked final DataFile audioData,
            @Mocked final InputStream audioIs) 
        throws Exception 
    {
        new Expectations() {{
            executeMessage.getConversation(); result = conv;
            conv.getAudioStream(); result = audioStream;
            conv.getConversationScenarioState().getBindings(); result = bindings;
            bindingSupport.putAll(bindings);
            bindingSupport.reset();
            audioFile.isCacheable(); result = true; minTimes=1;
            audioFile.getAudioFile(); result = audioData;
            audioData.getDataStream(); result = audioIs;
            audioStream.addSource(anyString, anyLong, (InputStreamSource) any); result = new Delegate() {
                RavenFuture addSource(String key, long checksum, InputStreamSource source) {
                    source.getInputStream();
                    return new CompletedFuture(null, executor);
                }
            };
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestPlayAudioAction(null, audioFile, bindingSupport));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
    }
    
    @Test
    public void playInputStreamSourceTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final InputStreamSource audioFile,
            @Mocked final InputStream audioInputStream,
            @Mocked final InputStream audioIs,
            @Mocked final TypeConverter converter) 
        throws Exception 
    {
        new Expectations() {{
            executeMessage.getConversation(); result = conv;
            conv.getAudioStream(); result = audioStream;
            converter.convert(InputStreamSource.class, audioFile, null); result = audioFile;
            audioFile.getInputStream(); result = audioInputStream;
            audioStream.addSource((InputStreamSource) any); result = new Delegate() {
                RavenFuture addSource(InputStreamSource source) {
                    source.getInputStream();
                    return new CompletedFuture(null, executor);
                }
            };
        }};
        
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestPlayAudioAction(converter, audioFile, null));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
    }
    
    @Test
    public void nullAudioTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream
) 
        throws Exception 
    {
        new Expectations() {{
            executeMessage.getConversation(); result = conv;
            conv.getAudioStream(); result = audioStream;
        }};
        
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestPlayAudioAction(null, null, null));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
    }
    
    @Test
    public void cancelTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final AudioFile audioFile
) 
        throws Exception 
    {
        new Expectations() {{
            executeMessage.getConversation(); result = conv;
            conv.getAudioStream(); result = audioStream;
            audioFile.isCacheable(); result = true;
            
        }};
        
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestPlayAudioAction(null, audioFile, null));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        playAudioAction.send(Action.CANCEL);
        assertTrue(actionExecutor.waitForMessage(500));
    }
    
    private class TestPlayAudioAction extends AbstractPlayAudioAction {
        private final Object audioFile;
        private final BindingSupport bindingSupport;

        public TestPlayAudioAction(TypeConverter converter, Object audioFile, BindingSupport support) {
            super("Test play action", converter);
            this.audioFile = audioFile;
            this.bindingSupport = support;
        }        

        @Override
        protected Object getAudio(IvrEndpointConversation conversation) {
            return audioFile;
        }

        @Override
        protected BindingSupport getBindingSupport() {
            return bindingSupport;
        }
    }
}
