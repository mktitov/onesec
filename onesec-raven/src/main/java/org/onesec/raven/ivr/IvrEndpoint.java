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

package org.onesec.raven.ivr;

import java.util.Map;
import org.raven.tree.Node;

/**
 * @author Mikhail Titov
 */
public interface IvrEndpoint extends IvrMediaTerminal, Node
{
    public IvrEndpointState getEndpointState();

    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , IvrEndpointConversationListener listener
            , IvrConversationScenario scenario, Map<String, Object> bindings, String callingNumber);
//    public RtpAddress getRtpAddress();
    /**
     * Adds conversation listener
     */
    public void addConversationListener(IvrEndpointConversationListener listener);
    /**
     * Removes conversation listener
     */
    public void removeConversationListener(IvrEndpointConversationListener listener);
    /**
     * Returns active calls count for the terminal
     */
    public int getActiveCallsCount();
}