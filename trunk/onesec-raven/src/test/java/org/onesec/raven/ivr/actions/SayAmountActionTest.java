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

package org.onesec.raven.ivr.actions;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class SayAmountActionTest extends OnesecRavenTestCase
{
    private TestEndpointConversationNode conv;
    private ExecutorServiceNode executor;
    private Node numbers;

    @Before
    public void prepare() throws Exception
    {
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        executor.setCorePoolSize(10);
        executor.setMaximumPoolSize(10);
        assertTrue(executor.start());

        conv = new TestEndpointConversationNode();
        conv.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(conv);
        conv.setExecutorService(executor);
        conv.setFileName("target/amount.wav");
        assertTrue(conv.start());

        numbers = createNumbersNode(tree);
    }

    @Test(timeout=15000)
    public void test() throws IvrActionException, InterruptedException
    {
        SayAmountAction action = new SayAmountAction(numbers, 11239.42, 0);
        action.execute(conv);
        Thread.sleep(10000);
    }

    public static Node createNumbersNode(Tree tree) throws Exception
    {
        ContainerNode numbers = new ContainerNode("numbers");
        tree.getRootNode().addAndSaveChildren(numbers);
        assertTrue(numbers.start());

        File[] sounds = new File("src/test/wav/numbers").listFiles();
        for (File sound: sounds)
        {
            if (sound.isDirectory())
                continue;
            AudioFileNode audioNode = new AudioFileNode();
            audioNode.setName(FilenameUtils.getBaseName(sound.getName()));
            numbers.addAndSaveChildren(audioNode);
            audioNode.getAudioFile().setDataStream(FileUtils.openInputStream(sound));
            assertTrue(audioNode.start());
        }

        return numbers;
    }
}