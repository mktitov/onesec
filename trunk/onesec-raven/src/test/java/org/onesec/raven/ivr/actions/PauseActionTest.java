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

import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.onesec.raven.ivr.IvrActionException;
import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class PauseActionTest extends Assert
{
    @Test()
    public void executeTest() throws ExecutorServiceException, IvrActionException
    {
        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
        ExecutorService executor = createMock(ExecutorService.class);
        expect(conversation.getExecutorService()).andReturn(executor);
        executor.execute(executeTask());
        replay(conversation, executor);

        PauseAction action = new PauseAction(100);
        long start = System.currentTimeMillis();
        action.execute(conversation);
        long interval = System.currentTimeMillis()-start;
        assertTrue(interval>=100);
        assertTrue(interval<=100+10);

        verify(conversation, executor);
    }

    @Test
    public void cancelTest() throws Exception
    {
        IvrEndpointConversation conversation = createMock(IvrEndpointConversation.class);
        ExecutorService executor = createMock(ExecutorService.class);
        expect(conversation.getExecutorService()).andReturn(executor);
        executor.execute(executeTaskInThread());
        replay(conversation, executor);

        PauseAction action = new PauseAction(100);
        long start = System.currentTimeMillis();
        assertEquals(IvrActionStatus.WAITING, action.getStatus());
        action.execute(conversation);
        assertEquals(IvrActionStatus.EXECUTING, action.getStatus());
        Thread.sleep(20);
        assertEquals(IvrActionStatus.EXECUTING, action.getStatus());
        action.cancel();
        Thread.sleep(15);
        long interval = System.currentTimeMillis()-start;
        assertTrue(interval<100);
        assertEquals(IvrActionStatus.EXECUTED, action.getStatus());

        verify(conversation, executor);
        
    }

    public static Task executeTask()
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(Object argument)
            {
                ((Task)argument).run();
                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });
        return null;
    }

    public static Task executeTaskInThread()
    {
        reportMatcher(new IArgumentMatcher()
        {
            public boolean matches(final Object argument)
            {
                new Thread()
                {
                    @Override
                    public void run() {
                        ((Task)argument).run();
                    }

                }.start();
                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });
        return null;
    }
}