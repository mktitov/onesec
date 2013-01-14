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
import javax.script.Bindings;
import org.onesec.raven.ivr.Cacheable;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;
import org.onesec.raven.ivr.impl.IvrUtils;
import org.onesec.raven.ivr.impl.ResourceInputStreamSource;
import org.onesec.raven.ivr.queue.CallsCommutationManagerListener;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.raven.RavenUtils;
import org.raven.tree.Node;

/**
 * Цель: проинформировать {@link CommutationManagerCall} о том, что оператор готов к коммутации и ждать
 *       до тех пор пока оператор не положит трубку или пока коммутация активна (абонент не положит трубку)
 * @author Mikhail Titov
 */
public class WaitForCallCommutationAction extends AsyncAction 
        implements CallsCommutationManagerListener, Cacheable
{
    private final static String NAME = "Wait for commutation action";
    private final static String BEEP_RESOURCE_NAME = "/org/onesec/raven/ivr/phone_beep.wav";
    private final static int WAIT_TIMEOUT = 100;
    private final static int ABONENT_READY_WAIT_TIMEOUT = 1500;

    private final Lock lock = new ReentrantLock();
    private final Condition abonentReadyCondition = lock.newCondition();
    private final Condition preamblePlayedCondition = lock.newCondition();
    private volatile boolean abonentReady = false;
    private final Node owner;

    public WaitForCallCommutationAction(Node owner) {
        super(NAME);
        this.owner = owner;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conv) throws Exception
    {
        Bindings bindings = conv.getConversationScenarioState().getBindings();
        CommutationManagerCall commutationManager = (CommutationManagerCall) 
                bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING);

        if (commutationManager==null)
            throw new Exception("CallsCommutationManager not found in the conversation scenario state");
        
        bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, commutationManager.isCommutationValid());
        String executedFlagKey = RavenUtils.generateKey("executed_"+this.getClass().getName(), owner);

        boolean executed = bindings.containsKey(executedFlagKey);
        if (!executed)
            commutationManager.addListener(this);
        try {
            if (!executed) {
                commutationManager.operatorReadyToCommutate(conv);
                bindings.put(executedFlagKey, true);
            }
            
            boolean commutated = false;
            while (!executed && !hasCancelRequest() && commutationManager.isCommutationValid() && !commutated) {
//                conv.getOwner().getLogger().debug("Playing beep");
                IvrUtils.playAudioInAction(this, conv, new ResourceInputStreamSource(BEEP_RESOURCE_NAME), this);
                lock.lock();
                try {
                    if (!abonentReady)
                        abonentReadyCondition.await(ABONENT_READY_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (abonentReady) {
                        preamblePlayedCondition.signal();
                        commutated = true;
                    }
                } finally {
                    lock.unlock();
                }
            }

//            conv.getOwner().getLogger().debug("Wating for commutation finish");
            while (!hasCancelRequest() && commutationManager.isCommutationValid())
                TimeUnit.MILLISECONDS.sleep(WAIT_TIMEOUT);
//            conv.getOwner().getLogger().debug("Commutation finished");
        } finally {
            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, commutationManager.isCommutationValid());
            commutationManager.removeListener(this);
        }
    }

    public boolean isFlowControlAction() {
        return false;
    }

    public void abonentReady() {
        lock.lock();
        try {
            abonentReady = true;
            abonentReadyCondition.signal();
            try {
                preamblePlayedCondition.await();
            } catch (InterruptedException ex) { }
        } finally {
            lock.unlock();
        }
    }

    public String getCacheKey() {
        return BEEP_RESOURCE_NAME;
    }

    public long getCacheChecksum() {
        return 1;
    }

    public boolean isCacheable() {
        return true;
    }
}
