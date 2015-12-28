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

import java.util.List;
import java.util.Set;
import javax.media.protocol.DataSource;
import mockit.Delegate;
import mockit.Expectations;
import org.onesec.raven.OnesecRavenModule;
import org.onesec.raven.OnesecRavenTestCase;
import org.onesec.raven.ivr.Action;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrActionNode;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.RavenFuture;
import org.raven.dp.impl.CompletedFuture;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.TestDataProcessorFacade;
import org.raven.test.TestDataProcessorFacadeConfig;

/**
 *
 * @author Mikhail Titov
 */
public class ActionTestCase extends OnesecRavenTestCase {
    protected ExecutorServiceNode executor;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder); 
        OnesecRavenModule.ENABLE_LOADING_SOUND_RESOURCE = false;
        OnesecRavenModule.ENABLE_LOADING_TEMPLATES = false;
    }

    @Override
    public void initTest() throws Exception {
        super.initTest(); 
        executor = createExecutor();
    }
    
    protected TestDataProcessorFacade createActionExecutor(final DataProcessor actionExecutorDP) {
        TestDataProcessorFacade actionExecutor = new TestDataProcessorFacadeConfig(
                "Action executor", testsNode, actionExecutorDP, executor, logger
        ).buildTestFacade();
        return actionExecutor;
    }
    
    protected DataProcessorFacade createAction(TestDataProcessorFacade actionExecutor, IvrActionNode actionNode) {
        Action actionDP = actionNode.createAction();
        return createAction(actionExecutor, actionDP);
    }
    
    protected DataProcessorFacade createAction(TestDataProcessorFacade actionExecutor, Action actionDP) {
        DataProcessorFacade action = new DataProcessorFacadeConfig(
                actionDP.getName(), executor, actionDP, executor, logger
        ).withParent(actionExecutor).build();
        return action;
    }
    
    protected void trainAudioStreamAddSource(final AudioStream audioStream) throws Exception {
        new Expectations(){{
            audioStream.addSource((DataSource) any); minTimes=0; result = new Delegate() {
                RavenFuture addSource(DataSource source) {
                    return new CompletedFuture(null, executor);
                }
            }; 
            audioStream.addSource((InputStreamSource)any); minTimes=0; result = new Delegate() {
                RavenFuture addSource(InputStreamSource source) {
                    return new CompletedFuture(null, executor);
                }
            };
            audioStream.addSource(anyString, anyLong, (DataSource)any); minTimes=0; result = new Delegate() {
                RavenFuture addSource(String key, long checksum, DataSource source) {
                    return new CompletedFuture(null, executor);
                }
            };
            audioStream.addSource(anyString, anyLong, (InputStreamSource)any); minTimes=0; result = new Delegate() {
                RavenFuture addSource(String key, long checksum, InputStreamSource source) {
                    return new CompletedFuture(null, executor);
                }
            };
            audioStream.playContinuously((List<AudioFile>) any, anyLong); result = new Delegate() {
                RavenFuture playContinuously(List<AudioFile> files, long trimPeriod) {
                    return new CompletedFuture(null, executor);
                }
            };
        }};
    }
}
