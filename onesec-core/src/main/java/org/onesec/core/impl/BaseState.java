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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onesec.core.ObjectDescription;
import org.onesec.core.State;
import org.onesec.core.StateListener;
import org.onesec.core.StateWaitResult;

/**
 *
 * @author Mikhail Titov
 */
public class BaseState<T extends State, O extends ObjectDescription> implements State<T, O>, Cloneable {    
    private volatile int state;
    private volatile long counter = 1;
    private String errorMessage;
    private Throwable errorException;
    private O observableObject;
    private List<StateListener> listeners;
    private final String listenersLock = "";
    private Map<Integer, String> idNames = new HashMap<Integer, String>();

    public BaseState(O observableObject) {
        this.observableObject = observableObject;
    }

    public O getObservableObject() {
        return observableObject;
    }
    
    public synchronized int getId() {
        return state;
    }

    public String getIdName() {
        return idNames.get(state);
    }

    public void setState(int state) {
        boolean stateChanged;
        synchronized(this) {
            stateChanged = state!=this.state;
            this.state = state;
            errorException = null;
            errorMessage = null;
            ++counter;
            notifyAll();
        }
        if (stateChanged)
            fireStateChangedEvent();
    }
    
    public long getCounter() {
        return counter;
    }
    
    public void setState(int state, String errorMessage, Throwable errorException) {
        boolean stateChanged;
        synchronized(this) {
            stateChanged = state!=this.state;
            this.state = state;
            this.errorException = errorException;
            this.errorMessage = errorMessage;
            notifyAll();
        }
        if (stateChanged)
            fireStateChangedEvent();
    }
    
    public boolean hasError() {
        return errorException!=null;
    }

    public Throwable getErrorException() {
        return errorException;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public synchronized StateWaitResult<T> waitForState(int[] states, long timeout) {
        return waitForState(states, 0L, timeout);
    }

    private void fireStateChangedEvent() {
        List<StateListener> _listeners = null;
        synchronized(listenersLock) {
            if (listeners!=null && !listeners.isEmpty()) 
                _listeners = new ArrayList<StateListener>(listeners);
        }
        if (_listeners!=null)
            for (StateListener listener: _listeners)
                listener.stateChanged(this);
    }
    
    private StateWaitResult<T> waitForState(int[] states, long stateCounter, long timeout) {
        try {
            long maxTime = System.currentTimeMillis()+timeout;
            while (true)
            {
                for (int st : states) 
                    if (st == state && counter > stateCounter) 
                        return new StateWaitResultImpl<T>(false, (T)clone());
                
                long curTime = System.currentTimeMillis();
                if (curTime >= maxTime)
                    return StateWaitResultImpl.WAIT_INTERRUPTED;
                
                long waitTimeout = maxTime - curTime;
                synchronized(this) {
                    wait(waitTimeout);
                }
            }
        } catch (CloneNotSupportedException ex) {
            throw new UnsupportedOperationException(ex);
        } catch (InterruptedException ex) {
            return StateWaitResultImpl.WAIT_INTERRUPTED;
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public StateWaitResult<T> waitForNewState(State state, long timeout) {
        return waitForState(new int[]{state.getId()}, state.getCounter(), timeout);
    }

    public void addStateListener(StateListener listener) {
        synchronized(listenersLock) {
            if (listeners==null)
                listeners = new ArrayList<StateListener>();
            listeners.add(listener);
        }
    }

    public void removeStateListener(StateListener listener) {
        synchronized(listenersLock) {
            if (listeners!=null)
                listeners.remove(listener);
        }
    }
    
    /**
     * Задает имя идентификатору сообщения 
     * @param id
     * @param name
     */
    protected void addIdName(Integer id, String name) {
        idNames.put(id, name);
    }
}
