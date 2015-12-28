/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.actions;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Mikhail Titov
 */
public class PauseAction extends AbstractAction {
    private final static String UNPAUSE = "UNPAUSE";
    
    private final long interval;

    public PauseAction(long interval, TimeUnit intervalTimeUnit) {
        super("Pause: "+intervalTimeUnit.toMillis(interval)+" ms");
        this.interval = intervalTimeUnit.toMillis(interval);
    }

    @Override
    protected ActionExecuted processExecuteMessage(Execute message) throws Exception {
        getFacade().sendDelayed(interval, UNPAUSE);
        return null;
    }

    @Override
    protected void processCancelMessage() throws Exception {
        getFacade().sendTo(getContext().getParent(), ACTION_EXECUTED_then_EXECUTE_NEXT);
    }
    
    @Override
    public Object processData(Object message) throws Exception {
        if (message==UNPAUSE) {
            processCancelMessage();
            return VOID;
        } else
            return super.processData(message);          
    }
}
