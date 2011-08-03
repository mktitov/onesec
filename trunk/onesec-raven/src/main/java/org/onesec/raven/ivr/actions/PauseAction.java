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
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class PauseAction extends AsyncAction
{
    public static final String ACTION_NAME = "Pause action";
    private final long interval;

    public PauseAction(long interval)
    {
        super(ACTION_NAME);
        this.interval = interval;
        setStatusMessage("Pausing");
    }

    public boolean isFlowControlAction() {
        return false;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(logMess("Pausing on "+interval+" ms"));
        long start = System.currentTimeMillis();
        do {
            TimeUnit.MILLISECONDS.sleep(10);
        } while (System.currentTimeMillis()-start<interval && !hasCancelRequest());
    }
}
