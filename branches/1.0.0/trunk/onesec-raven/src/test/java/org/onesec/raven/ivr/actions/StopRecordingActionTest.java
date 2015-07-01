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
package org.onesec.raven.ivr.actions;

import javax.script.Bindings;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onesec.raven.OnesecRavenTestCase;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.onesec.raven.ivr.CallRecorder;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.conv.ConversationScenarioState;

/**
 *
 * @author Mikhail Titov
 */
public class StopRecordingActionTest extends OnesecRavenTestCase {
    
    private StopRecordingActionNode actionNode;
    
    @Before
    public void prepare() {
        actionNode = new StopRecordingActionNode();
        actionNode.setName("stop recording action node");
        tree.getRootNode().addAndSaveChildren(actionNode);
        assertTrue(actionNode.start());
    }
    
    @Test
    public void test() throws Exception {
        IMocksControl control = createControl();
        IvrEndpointConversation conv = control.createMock(IvrEndpointConversation.class);
        ConversationScenarioState state = control.createMock(ConversationScenarioState.class);
        Bindings bindings = control.createMock(Bindings.class);
        CallRecorder recorder = control.createMock(CallRecorder.class);
        
        expect(conv.getConversationScenarioState()).andReturn(state);
        expect(conv.getOwner()).andReturn(actionNode).anyTimes();
        expect(state.getBindings()).andReturn(bindings);
        expect(bindings.remove(StartRecordingAction.RECORDER_BINDING)).andReturn(recorder);
        recorder.stopRecording(false);
        
        control.replay();
        
        StopRecordingAction action = (StopRecordingAction) actionNode.createAction();
        assertNotNull(action);
        action.doExecute(conv);
        
        control.verify();
    }
}
