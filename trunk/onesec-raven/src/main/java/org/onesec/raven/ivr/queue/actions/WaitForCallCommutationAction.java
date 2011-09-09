/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.raven.ivr.queue.actions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.onesec.raven.ivr.impl.ResourceInputStreamSource;
import org.onesec.raven.ivr.queue.CallsCommutationManager;
import org.onesec.raven.ivr.queue.CallsCommutationManagerListener;

/**
 * Цель: проинформировать {@link CallsCommutationManager} о том, что оператор готов к коммутации и ждать
 *       до тех пор пока оператор не положит трубку или пока коммутация активна (абонент не положит трубку)
 * @author Mikhail Titov
 */
public class WaitForCallCommutationAction extends AsyncAction implements CallsCommutationManagerListener
{
    private final static String NAME = "Wait for commutation action";
    private final static String BEEP_RESOURCE_NAME = "/org/onesec/raven/ivr/phone_beep.wav";
    private final static int WAIT_TIMEOUT = 100;

    private final Lock lock = new ReentrantLock();
    private final Condition abonentReadyCondition = lock.newCondition();

    public WaitForCallCommutationAction() {
        super(NAME);
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        CallsCommutationManager commutationManager = (CallsCommutationManager) conversation
                .getConversationScenarioState()
                .getBindings()
                .get(CallsCommutationManager.CALLS_COMMUTATION_MANAGER_BINDING);

        if (commutationManager==null)
            throw new Exception("CallsCommutationManager not found in the conversation scenario state");

        commutationManager.addListener(this);
        try {
            commutationManager.operatorReadyToCommutate(conversation);

            boolean preamblePlayed = false;
            while (!hasCancelRequest() && commutationManager.isCommutationValid()){
                if (!preamblePlayed) {
                    lock.lock();
                    try {
                        if (abonentReadyCondition.await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                            preamblePlayed = true;
                            IvrUtils.playAudioInAction(
                                    this, conversation, new ResourceInputStreamSource(BEEP_RESOURCE_NAME));
                        }
                    } finally {
                        lock.unlock();
                    }
                } else
                    TimeUnit.MILLISECONDS.sleep(WAIT_TIMEOUT);
            }
        } finally {
            commutationManager.removeListener(this);
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }

    public void abonentReady()
    {
        lock.lock();
        try {
            abonentReadyCondition.signal();
        } finally {
            lock.unlock();
        }
    }

}
