/*
 *  Copyright 2010 Mikhail Titov.
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

import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface EndpointRequest
{
    /**
     * This method will be called when {@link IvrEndpointPool endpoint pool} will found the free enpoint or
     * when wait timeout will be reached.
     * @param endpoint the endpoint or null. If parameter value is null then endpoint pool does not
     *          contains the free endpoint
     */
    public void processRequest(IvrEndpoint endpoint);
    /**
     * Returns the timeout (ms) for waiting endpoint from the pool.
     */
    public long getWaitTimeout();
    /**
     * Returns the node owned request.
     */
    public Node getOwner();
    /**
     * Returns the current processing status message.
     */
    public String getStatusMessage();
}
