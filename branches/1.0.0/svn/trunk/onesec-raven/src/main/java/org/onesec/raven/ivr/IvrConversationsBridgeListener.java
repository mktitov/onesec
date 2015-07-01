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

package org.onesec.raven.ivr;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrConversationsBridgeListener
{
    /**
     * Fires when bridge was activated
     */
    public void bridgeActivated(IvrConversationsBridge bridge);
    /**
     * Fires when bridge was reactivated. For instance, when call were transfered or call hold activated or
     * something that stops rtp and starts it again
     */
    public void bridgeReactivated(IvrConversationsBridge bridge);
    /**
     * Fires when bridge was deactivated
     */
    public void bridgeDeactivated(IvrConversationsBridge bridge);
}
