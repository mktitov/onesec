    /*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.core.call.impl;

import org.onesec.core.call.CallController;
import org.onesec.core.call.CallState;
import org.onesec.core.impl.BaseState;

/**
 *
 * @author Mikhail Titov
 */
public class CallStateImpl extends BaseState<CallState, CallController> implements CallState {
    
    private boolean addressReady;
    private boolean terminalReady;
    
    private boolean sideAConnected;
    private boolean sideBConnected;
    
    public CallStateImpl(CallController callController) {
        super(callController);
        addIdName(PREPARING, "PREPARING");
        addIdName(PREPARED, "PREPARED");
        addIdName(CALLING, "CALLING");
        addIdName(TALKING, "TALKING");
        addIdName(FINISHED, "FINISHED");
    }

    public boolean isAddressReady() {
        return addressReady;
    }

    public void setAddressReady(boolean addressReady) {
        this.addressReady = addressReady;
        trySetPreparedState();
    }

    public boolean isTerminalReady() {
        return terminalReady;
    }

    public void setTerminalReady(boolean terminalReady) {
        this.terminalReady = terminalReady;
        trySetPreparedState();
    }

    public boolean isSideAConnected() {
        return sideAConnected;
    }

    public void setSideAConnected(boolean sideAConnected) {
        this.sideAConnected = sideAConnected;
        trySetTalkingState();
    }

    public boolean isSideBConnected() {
        return sideBConnected;
    }

    public void setSideBConnected(boolean sideBConnected) {
        this.sideBConnected = sideBConnected;
        trySetTalkingState();
    }
    
    private synchronized void trySetPreparedState(){
        if (getId()==PREPARING && addressReady && terminalReady)
            setState(PREPARED);
    }

    private synchronized void  trySetTalkingState() {
        if (getId()==CALLING && sideAConnected && sideBConnected)
            setState(TALKING);
    }
    
}
