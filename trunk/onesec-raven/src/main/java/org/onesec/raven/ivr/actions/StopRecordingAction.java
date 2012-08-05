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
import org.onesec.raven.ivr.CallRecorder;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class StopRecordingAction extends AsyncAction {
    
    public final static String NAME = "Stop recording action";

    public StopRecordingAction() {
        super(NAME);
    }
    
    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception {
        Bindings bindings = conversation.getConversationScenarioState().getBindings();
        CallRecorder recorder = (CallRecorder) bindings.remove(
                StartRecordingAction.RECORDER_BINDING);
        if (recorder==null) {
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.WARN))
                conversation.getOwner().getLogger().warn(logMess("Can't stop recording because of recorder not found"));
        } else 
            recorder.stopRecording(false);
    }

    public boolean isFlowControlAction() {
        return false;
    }
}
