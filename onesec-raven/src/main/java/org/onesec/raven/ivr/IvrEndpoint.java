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
import org.onesec.core.ObjectDescription;
import org.raven.tree.Node;

/**
 * @author Mikhail Titov
 */
public interface IvrEndpoint extends IvrEndpointConversation, Node, ObjectDescription
{
    public IvrEndpointState getEndpointState();
    /**
     * Returns the executor service
     */
    public void invite(
            String opponentNumber, IvrConversationScenario conversationScenario
            , ConversationCompletionCallback callback
            , Map<String, Object> bindings) throws IvrEndpointException;
    /**
     * Transfers current call to the address passed in the parameter.
     * @param address The destination telephone address string to where the Call is being
     *      transferred
     * @param monitorTransfer if <code>true</code> then method will monitor call transfer, etc will
     *      wait until transfered call end
     * @param callStartTimeout
     * @param callEndTimeout 
     */
}