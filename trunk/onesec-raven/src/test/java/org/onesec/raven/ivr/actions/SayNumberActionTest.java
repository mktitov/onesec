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
package org.onesec.raven.ivr.actions;

import java.io.File;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.ResourcesNode;

/**
 *
 * @author Mikhail Titov
 */
public class SayNumberActionTest extends OnesecRavenTestCase {
    private TestEndpointConversationNode conv;
    private ExecutorServiceNode executor;
    private Node numbers;
    private SayNumberActionNode actionNode;
    
    @Before
    public void prepare() throws Exception {
        ResourcesNode resources = (ResourcesNode) tree.getRootNode().getChildren(ResourcesNode.NAME);
        resources.setDefaultLocale(new Locale("ru"));
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(40);
        executor.setMaximumPoolSize(40);
        assertTrue(executor.start());

        conv = new TestEndpointConversationNode();
        conv.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(conv);
        conv.setExecutorService(executor);

//        numbers = createNumbersNode(tree);
        
        actionNode = new SayNumberActionNode();
        actionNode.setName("say number");
        tree.getRootNode().addAndSaveChildren(actionNode);
    }
    
    @After
    public void afterTest() {
        conv.stop();
    }
    
//    @Test
    public void withoutPatternsTest() throws Exception {
        conv.setFileName("target/number.wav");
        assertTrue(conv.start());
        actionNode.setNumber("124");
        assertTrue(actionNode.start());
        SayNumberAction action = (SayNumberAction) actionNode.createAction();
        assertNotNull(action);
        action.execute(conv);
        Thread.sleep(10000);
    }
    
    @Test
    public void withPatternsTest() throws Exception {
        conv.setFileName("target/numbers.wav");
        assertTrue(conv.start());
        actionNode.setNumber("9128672947");
        assertTrue(actionNode.start());
        RegexpPattern pattern = new RegexpPattern();
        pattern.setName("pattern1");
        actionNode.addAndSaveChildren(pattern);
        pattern.setPattern("(\\d\\d\\d)(\\d\\d\\d)(\\d\\d)(\\d\\d)");
        assertTrue(pattern.start());
        SayNumberAction action = (SayNumberAction) actionNode.createAction();
        assertNotNull(action);
        action.execute(conv);
        Thread.sleep(20000);
    }
    
    public static Node createNumbersNode(Tree tree) throws Exception {
        ContainerNode numbers = new ContainerNode("numbers");
        tree.getRootNode().addAndSaveChildren(numbers);
        assertTrue(numbers.start());

        File[] sounds = new File("src/test/wav/numbers").listFiles();
        for (File sound: sounds)  {
            if (sound.isDirectory()) continue;
            AudioFileNode audioNode = new AudioFileNode();
            audioNode.setName(FilenameUtils.getBaseName(sound.getName()));
            numbers.addAndSaveChildren(audioNode);
            audioNode.getAudioFile().setDataStream(FileUtils.openInputStream(sound));
            assertTrue(audioNode.start());
        }

        return numbers;
    }
    
}
