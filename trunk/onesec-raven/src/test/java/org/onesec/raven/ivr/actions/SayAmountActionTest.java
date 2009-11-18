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
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class SayAmountActionTest extends OnesecRavenTestCase
{
    private TestEndpointNode term;
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

        term = new TestEndpointNode();
        term.setName("endpoint");
        tree.getRootNode().addAndSaveChildren(term);
        term.setExecutorService(executor);
        assertTrue(term.start());

        numbers = createNumbersNode();
    }

    @Test(timeout=15000)
    public void test() throws IvrActionException, InterruptedException
    {
        SayAmountAction action = new SayAmountAction(numbers, 195912.12, 100);
        action.execute(term);
        Thread.sleep(10000);
    }

    private Node createNumbersNode() throws Exception
    {
        ContainerNode numbers = new ContainerNode("numbers");
        tree.getRootNode().addAndSaveChildren(numbers);
        assertTrue(numbers.start());

        File[] sounds = new File("src/test/wav/numbers").listFiles();
        for (File sound: sounds)
        {
            AudioFileNode audioNode = new AudioFileNode();
            audioNode.setName(FilenameUtils.getBaseName(sound.getName()));
            numbers.addAndSaveChildren(audioNode);
            audioNode.getAudioFile().setDataStream(FileUtils.openInputStream(sound));
            assertTrue(audioNode.start());
        }

        return numbers;
    }
}