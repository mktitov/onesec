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

package org.onesec.core.impl;

import org.onesec.core.State;
import org.onesec.core.StateWaitResult;

/**
 *
 * @author Mikhail Titov
 */
public class StateWaitResultImpl<T extends State> implements StateWaitResult<T> 
{
    public final static StateWaitResult WAIT_INTERRUPTED = new StateWaitResultImpl(true, null);
    
    private boolean waitInterrupted;
    private T state;

    public StateWaitResultImpl(boolean waitInterrupted, T state) {
        this.waitInterrupted = waitInterrupted;
        this.state = state;
    }

    public boolean isWaitInterrupted() {
        return waitInterrupted;
    }
    
    public T getState() {
        return state;
    }

}
