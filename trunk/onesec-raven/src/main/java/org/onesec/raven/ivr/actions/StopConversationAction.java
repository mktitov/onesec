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

import org.onesec.raven.ivr.IvrActionStatus;
import org.onesec.raven.ivr.IvrEndpoint;

/**
 *
 * @author Mikhail Titov
 */
public class StopConversationAction extends AsyncAction
{
    public static final String NAME = "Stop conversation action";

    public StopConversationAction()
    {
        super(NAME);
    }

    @Override
    protected void doExecute(IvrEndpoint endpoint) throws Exception
    {
        setStatus(IvrActionStatus.EXECUTED);
        endpoint.stopConversation();
    }
}
