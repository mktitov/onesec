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
public interface IvrConversationsBridge
{
    /**
     * Returns the current status of the bridge
     */
    public IvrConversationsBridgeStatus getStatus();
    /**
     * Returns the time in milliseconds when bridge was created
     */
    public long getCreatedTimestamp();
    /**
     * Returns the time in milliseconds when bridge activation begun
     */
    public long getActivatingTimestamp();
    /**
     * Returns the time in milliseconds when bridge were activated
     */
    public long getActivatedTimestamp();
    /**
     * Returns the first conversation in the bridge
     */
    public IvrEndpointConversation getConversation1();
    /**
     * Returns the second conversation in the bridge
     */
    public IvrEndpointConversation getConversation2();
    /**
     * Activates bridge
     */
    public void activateBridge();
    /** 
     * Sends bridgeDeactivated event and then bridgeActivated event
     */
    public void reactivateBridge();
    /**
     * Deactivates bridge
     */
    public void deactivateBridge();
    /**
     * Adds listener to the bridge
     */
    public void addBridgeListener(IvrConversationsBridgeListener listener);
}
