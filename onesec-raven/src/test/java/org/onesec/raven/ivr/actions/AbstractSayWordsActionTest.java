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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import mockit.Delegate;
import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.CompletedFuture;
import org.raven.test.TestDataProcessorFacade;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class AbstractSayWordsActionTest extends ActionTestCase {
    
    @Test
    public void nullWordsGroupsTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage) 
        throws Exception
    {
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestSayWordsAction(null, 0, 0, null, null));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(100));
    }
    
    @Test
    public void audioFilesPlayTest (
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final AudioFileNode file1,
            @Mocked final AudioFileNode file2,
            @Mocked final AudioFileNode file3)
        throws Exception
    {
        final List group1 = Arrays.asList(file1, file2);
        final List group2 = Arrays.asList(file3);
        final long pauseBetweebWords = -10;
        final AtomicLong firstPlay = new AtomicLong();
        final AtomicLong lastPlay = new AtomicLong();
        new StrictExpectations() {{
            executeMessage.getConversation(); times=2; result = conv;
            conv.getAudioStream(); result = audioStream;
            audioStream.playContinuously(withEqual(group1), Math.abs(pauseBetweebWords)); result = new Delegate() {
                RavenFuture playContinuously(List<AudioFile> files, long trimPeriod) {
                    firstPlay.set(System.currentTimeMillis());
                    return new CompletedFuture(null, executor);
                };
            };
            audioStream.playContinuously(withEqual(group2), Math.abs(pauseBetweebWords)); result = new Delegate() {
                RavenFuture playContinuously(List<AudioFile> files, long trimPeriod) {
                    lastPlay.set(System.currentTimeMillis());
                    return new CompletedFuture(null, executor);
                };
            };
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        List<List> audioFiles = Arrays.asList(
                group1,
                Collections.EMPTY_LIST,
                group2
            );
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestSayWordsAction(
                null, pauseBetweebWords, 100, null, audioFiles));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
        assertTrue(lastPlay.get()-firstPlay.get()>=100);
        assertTrue(lastPlay.get()-firstPlay.get()<=120);
    }
    
    @Test
    public void stringRefToaudioFilesPlayTest (
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final ResourceManager resourceManager,
            @Injectable final AudioContainer source1,
            @Injectable final AudioContainer source2,
            @Mocked final AudioFileNode file1,
            @Mocked final AudioFileNode file2)
        throws Exception
    {
        final List group1 = Arrays.asList(file1, file2);
        final long pauseBetweebWords = -10;
        new StrictExpectations() {{
            executeMessage.getConversation(); times=2; result = conv;
            conv.getAudioStream(); result = audioStream;
            resourceManager.getResource(source1, "audio1", null); result = file1;
            resourceManager.getResource(source1, "audio2", null); result = null;
            resourceManager.getResource(source2, "audio2", null); result = file2;
            audioStream.playContinuously(withEqual(group1), Math.abs(pauseBetweebWords)); result = new Delegate() {
                RavenFuture playContinuously(List<AudioFile> files, long trimPeriod) {
                    return new CompletedFuture(null, executor);
                };
            };
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        List<List> audioFiles = Arrays.asList((List)Arrays.asList("audio1", "audio2"));
        final TestSayWordsAction action = new TestSayWordsAction(
                        Arrays.asList((Node)source1, source2), pauseBetweebWords, 100, resourceManager, audioFiles);
        DataProcessorFacade playAudioAction = createAction(actionExecutor, action);
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
    }
    
    @Test
    public void sentencesTest(
            @Mocked final DataProcessor actionExecutorDP,
            @Mocked final Action.Execute executeMessage,
            @Mocked final IvrEndpointConversation conv,
            @Mocked final AudioStream audioStream,
            @Mocked final AudioFile file1,
            @Mocked final AudioFile file2,
            @Mocked final AudioFile file3)
        throws Exception
    {
        final List group1 = Arrays.asList(file1, file2);
        final List group2 = Arrays.asList(file3);
        
        final long pauseBetweebWords = -10;
        final AtomicLong firstPlay = new AtomicLong();
        final AtomicLong lastPlay = new AtomicLong();
        new StrictExpectations() {{
            executeMessage.getConversation(); times=2; result = conv;
            conv.getAudioStream(); result = audioStream;
            audioStream.playContinuously(withEqual(group1), 1); result = new Delegate() {
                RavenFuture playContinuously(List<AudioFile> files, long trimPeriod) {
                    firstPlay.set(System.currentTimeMillis());
                    return new CompletedFuture(null, executor);
                };
            };
            audioStream.playContinuously(withEqual(group2), 10); result = new Delegate() {
                RavenFuture playContinuously(List<AudioFile> files, long trimPeriod) {
                    lastPlay.set(System.currentTimeMillis());
                    return new CompletedFuture(null, executor);
                };
            };
        }};
        TestDataProcessorFacade actionExecutor = createActionExecutor(actionExecutorDP);
        List sentences = Arrays.asList(
                new AbstractSayWordsAction.Sentence(100, -1, group1),
                new AbstractSayWordsAction.Pause(200),
                new AbstractSayWordsAction.Sentence(0, -10, group2)
        );
        DataProcessorFacade playAudioAction = createAction(actionExecutor, new TestSayWordsAction(
                null, pauseBetweebWords, 100, null, sentences));
        actionExecutor.setWaitForMessage(AbstractAction.ACTION_EXECUTED_then_EXECUTE_NEXT, playAudioAction);
        playAudioAction.send(executeMessage);
        assertTrue(actionExecutor.waitForMessage(500));
        assertTrue(lastPlay.get()-firstPlay.get()>=300);
        assertTrue(lastPlay.get()-firstPlay.get()<=320);
    }
    
    private class TestSayWordsAction extends AbstractSayWordsAction {
        private final List wordsGroups;

        public TestSayWordsAction(Collection<Node> wordsNodes, long pauseBetweenWords, 
                long pauseBetweenWordsGroups, ResourceManager resourceManager, List wordsGroups) 
        {
            super("Say words", wordsNodes, pauseBetweenWords, pauseBetweenWordsGroups, resourceManager);
            this.wordsGroups = wordsGroups;
        }

        @Override
        protected List formWords(IvrEndpointConversation conversation) {
            return wordsGroups;
        }
    }
    
    public interface AudioContainer extends Node {
        
    }
}
