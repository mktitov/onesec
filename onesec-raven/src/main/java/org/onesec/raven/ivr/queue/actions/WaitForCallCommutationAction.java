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

import org.onesec.raven.ivr.Cacheable;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.PlayAudioDP;
import org.onesec.raven.ivr.actions.AbstractAction;
import org.onesec.raven.ivr.impl.ResourceInputStreamSource;
import org.onesec.raven.ivr.queue.CallsCommutationManagerListener;
import org.onesec.raven.ivr.queue.CommutationManagerCall;
import org.raven.conv.ConversationScenarioState;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.Behaviour;

/**
 * Цель: проинформировать {@link CommutationManagerCall} о том, что оператор готов к коммутации и ждать
 *       до тех пор пока оператор не положит трубку или пока коммутация активна (абонент не положит трубку)
 * @author Mikhail Titov
 */
public class WaitForCallCommutationAction extends AbstractAction implements CallsCommutationManagerListener
{
    private final static String NAME = "Operator queue commutator";
    public final static String BEEP_RESOURCE_NAME = "/org/onesec/raven/ivr/phone_beep.wav";
    public final static int BEEP_CHECK_SUM = 1;

    public final static Cacheable BEEP_CACHE_INFO = new Cacheable() {
        @Override public String getCacheKey() {
            return BEEP_RESOURCE_NAME;
        }
        @Override public long getCacheChecksum() {
            return BEEP_CHECK_SUM;
        }
        @Override public boolean isCacheable() {
            return true;
        }
    };
    public final static int PAUSE_BETWEEN_BEEPS = 1500;
    
    public static final String COMMUTATION_INVALIDATED = "COMMUTATION_INVALIDATED";
    public static final String SEND_OPERATOR_READY = "SEND_OPERATOR_READY";
    public final static String PLAY_BEEP = "PLAY_BEEP";
    public final static String ABONENT_READY = "ABONENT_READY";
    public final static String DISCONNECTED = "DISCONNECTED";
    
    private CommutationManagerCall commutationManager;
    private IvrEndpointConversation conversation;
    private ConversationScenarioState state;

//    private final Lock lock = new ReentrantLock();
//    private final Condition abonentReadyCondition = lock.newCondition();
//    private final Condition preamblePlayedCondition = lock.newCondition();
//    private volatile boolean abonentReady = false;
//    private final Node owner;
    private final CancelBehaviour executeNextCancelBehaviour = new CancelBehaviour(ACTION_EXECUTED_then_EXECUTE_NEXT);

    public WaitForCallCommutationAction() {
        super(NAME);
//        this.owner = owner;
    }

    @Override
    public void postStop() {
        super.postStop(); 
        if (commutationManager!=null)
            commutationManager.removeListener(this);
        if (state!=null)
            state.enableDtmfProcessing();
    }

    @Override
    public Object processData(Object message) throws Exception {
        if (message==SEND_OPERATOR_READY) {
            state.getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, true);            
            commutationManager.operatorReadyToCommutate(conversation);
            become(waitingForAbonent);
            getFacade().send(PLAY_BEEP);
            return VOID;
        } else if (message==ABONENT_READY) {
            become(waitingForAbonent);
            getContext().stash(); getContext().unstashAll();
            return VOID;
        } else if (message==COMMUTATION_INVALIDATED) {
            become(waitForDisconnect);
            getContext().stash(); getContext().unstashAll();
            return VOID;
        } else            
            return super.processData(message); 
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute execMessage) throws Exception {
        // есть ли необходимость в хранения executed?
        //3 - фазы
        //1 - шлем operatorReadyToCommutate
        //2 - проигрываем beep до тех пор пока не придет abonentReady. В этой фазе необходимо запретить обработку DTMF.
        //3 - ждем когда завершится commutation. Для этого в CallsCommutationManagerListener нужно добавить метод commutationInvalidated
        conversation = execMessage.getConversation();
        state = conversation.getConversationScenarioState();
        commutationManager = (CommutationManagerCall) state.getBindings().get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING);

        if (commutationManager==null)
            throw new Exception("CallsCommutationManager not found in the conversation scenario state");
        
        commutationManager.addListener(this);
        
        return null;
    }
    
    @Override
    protected void processCancelMessage() throws Exception {
        sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
    }
    
    private final Behaviour waitingForAbonent = new  Behaviour("Commutating") {        
        @Override public Object processData(Object message) throws Exception {
            if (message==PLAY_BEEP) {
                state.disableDtmfProcessing();
                getFacade().sendTo(getPlayer(true), new PlayAudioDP.PlayInputStreamSource(
                        new ResourceInputStreamSource(BEEP_RESOURCE_NAME), BEEP_CACHE_INFO));
                return VOID;
            } else if (message.equals(PlayAudioDP.PLAYED)) {
                getFacade().sendDelayed(PAUSE_BETWEEN_BEEPS, PLAY_BEEP);
                return VOID;
            } else if (message==ABONENT_READY) {
                state.enableDtmfProcessing();
                DataProcessorFacade player = getPlayer(false);
                if (player!=null)
                    player.stop();
                become(waitForDisconnect);
                return VOID;
            }
            return UNHANDLED;
        }        
        private DataProcessorFacade getPlayer(boolean create) throws Exception {
            DataProcessorFacade player = getContext().getChild("Player");
            return player!=null || !create? 
                        player :
                        getContext().addChild(getContext().createChild("Player", new PlayAudioDP(conversation.getAudioStream())));
        }        
    }.andThen(executeNextCancelBehaviour);
    
    private final Behaviour waitForDisconnect = new Behaviour("Commutated") {
        @Override public Object processData(Object message) throws Exception {
            if (message==COMMUTATION_INVALIDATED) {
                state.getBindings().put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, false);            
                sendExecuted(ACTION_EXECUTED_then_EXECUTE_NEXT);
                return VOID;
            }
            return UNHANDLED;
        }
    }.andThen(executeNextCancelBehaviour);
    
    @Override
    public void stateChanged(CommutationManagerCall.State state) {
        switch(state) {
            case INVITING: getFacade().send(SEND_OPERATOR_READY); break;
            case ABONENT_READY: getFacade().send(ABONENT_READY); break;
            case INVALID: getFacade().send(COMMUTATION_INVALIDATED); break;
        }
    }

//
//    @Override
//    public void abonentReady() {
//        this.getFacade().send(ABONENT_READY);
//    }

//    private class CommutationListener implements CallsCommutationManagerListener {
//        @Override public void abonentReady() {
//            WaitForCallCommutationAction.this.getFacade().send(ABONENT_READY);
//        }        
//    }
    

//    @Override
//    protected void doExecute(IvrEndpointConversation conv) throws Exception
//    {
//        Bindings bindings = conv.getConversationScenarioState().getBindings();
//        CommutationManagerCall commutationManager = (CommutationManagerCall) 
//                bindings.get(CommutationManagerCall.CALLS_COMMUTATION_MANAGER_BINDING);
//
//        if (commutationManager==null)
//            throw new Exception("CallsCommutationManager not found in the conversation scenario state");
//        
//        bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, commutationManager.isCommutationValid());
//        String executedFlagKey = RavenUtils.generateKey("executed_"+this.getClass().getName(), owner);
//
//        boolean executed = bindings.containsKey(executedFlagKey);
//        if (!executed)
//            commutationManager.addListener(this);
//        try {
//            if (!executed) {
//                commutationManager.operatorReadyToCommutate(conv);
//                bindings.put(executedFlagKey, true);
//            }
//            
//            boolean commutated = false;
//            while (!executed && !hasCancelRequest() && commutationManager.isCommutationValid() && !commutated) {
////                conv.getOwner().getLogger().debug("Playing beep");
//                IvrUtils.playAudioInAction(this, conv, new ResourceInputStreamSource(BEEP_RESOURCE_NAME), this);
//                lock.lock();
//                try {
//                    if (!abonentReady)
//                        abonentReadyCondition.await(ABONENT_READY_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
//                    if (abonentReady) {
//                        preamblePlayedCondition.signal();
//                        commutated = true;
//                    }
//                } finally {
//                    lock.unlock();
//                }
//            }
//
////            conv.getOwner().getLogger().debug("Wating for commutation finish");
//            while (!hasCancelRequest() && commutationManager.isCommutationValid())
//                TimeUnit.MILLISECONDS.sleep(WAIT_TIMEOUT);
////            conv.getOwner().getLogger().debug("Commutation finished");
//        } finally {
//            bindings.put(IvrEndpointConversation.DISABLE_AUDIO_STREAM_RESET, commutationManager.isCommutationValid());
//            commutationManager.removeListener(this);
//        }
//    }

//    public boolean isFlowControlAction() {
//        return false;
//    }

//    public void abonentReady() {
//        lock.lock();
//        try {
//            abonentReady = true;
//            abonentReadyCondition.signal();
//            try {
//                preamblePlayedCondition.await();
//            } catch (InterruptedException ex) { }
//        } finally {
//            lock.unlock();
//        }
//    }

//    public String getCacheKey() {
//        return BEEP_RESOURCE_NAME;
//    }
//
//    public long getCacheChecksum() {
//        return 1;
//    }
//
//    public boolean isCacheable() {
//        return true;
//    }
}
