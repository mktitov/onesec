/*
 * Copyright 2012 Mikhail Titov.
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.media.protocol.FileTypeDescriptor;
import org.junit.*;
import org.onesec.raven.JMFHelper;
import org.onesec.raven.ivr.CodecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class PullToPushConverterDataSourceTest extends Assert {
    
    private CodecManager codecManager;
    private static Logger logger = LoggerFactory.getLogger(ContainerParserDataSource.class);
    private static volatile int taskFinished;
    private static volatile int taskExecuted;
    
    @Before
    public void prepare() throws IOException {
        taskFinished = 0;
        taskExecuted = 0;
        codecManager = new CodecManagerImpl(logger);
    }
    
    @Test
    public void test() throws Exception {
        ExecutorService executor = createMock("executor", ExecutorService.class);
        Node owner =  createMock("owner", Node.class);
        
        executor.execute(executeTask(owner));
        expectLastCall().atLeastOnce();
        replay(executor, owner);
        
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);
        ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, dataSource);
        PullToPushConverterDataSource conv = new PullToPushConverterDataSource(parser, executor, owner);
        conv.connect();
        JMFHelper.OperationController controller = JMFHelper.writeToFile(conv, "target/pull2push_file.wav");
        TimeUnit.SECONDS.sleep(5);
        controller.stop();
        
        verify(executor, owner);
        assertEquals(taskFinished, taskExecuted);
    }

    public static Task executeTask(final Node owner) {
        taskExecuted++;
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object argument) {
                final Task task = (Task) argument;
                assertSame(owner, task.getTaskNode());
                new Thread(new Runnable() {
                    public void run() {
                        System.out.println("!! Executing a task !!");
                        task.run();
                        taskFinished++;
                        System.out.println("!! Task executed !!");
                    }
                }).start();
                return true;
            }
            public void appendTo(StringBuffer buffer) { }
        });
        return null;
    }
}
