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
import org.onesec.raven.ivr.IvrEndpoint;
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

    @Override
    protected void doExecute(IvrEndpoint endpoint) throws Exception
    {
        if (endpoint.isLogLevelEnabled(LogLevel.DEBUG))
            endpoint.getLogger().debug("Action. Pausing on "+interval+" ms");
        long start = System.currentTimeMillis();
        do
        {
                TimeUnit.MILLISECONDS.sleep(10);
        }
        while (System.currentTimeMillis()-start<interval && !hasCancelRequest());
    }
    
//
//    private class PauseActionTask implements Task
//    {
//        private final Node initiator;
//
//        public PauseActionTask(Node initiator) {
//            this.initiator = initiator;
//        }
//
//        public Node getTaskNode() {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        public String getStatusMessage() {
//            return PauseAction.this.getStatusMessage();
//        }
//
//        public void run()
//        {
//            try
//            {
//                long start = System.currentTimeMillis();
//                do {
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(10);
//                    } catch (InterruptedException ex) {
//                        if (initiator.isLogLevelEnabled(LogLevel.ERROR))
//                            initiator.getLogger().error("Pause action was itnterrupted");
//                    }
//
//                } while (System.currentTimeMillis()-start<interval && !isMustCancel());
//            }
//            finally
//            {
//                setStatus(IvrActionStatus.EXECUTED);
//            }
//        }
//    }
}
