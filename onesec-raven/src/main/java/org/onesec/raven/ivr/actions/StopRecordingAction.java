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

/**
 *
 * @author Mikhail Titov
 */
public class StopRecordingAction extends AbstractAction {
    
    public final static String NAME = "Stop recording";

    public StopRecordingAction() {
        super(NAME);
    }

    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        Bindings bindings = message.getConversation().getConversationScenarioState().getBindings();
        CallRecorder recorder = (CallRecorder) bindings.remove(StartRecordingAction.RECORDER_BINDING);
        if (recorder==null) {
            if (getLogger().isWarnEnabled())
                getLogger().warn("Can't stop recording because of recorder not found");
        } else 
            recorder.stopRecording(false);
        return ACTION_EXECUTED_then_EXECUTE_NEXT;
    }
}
