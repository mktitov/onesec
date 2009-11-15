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

import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrEndpointPool extends Node
{
    /**
     * Returns the endpoint acquired from the pool or null if no IN_SERVICE endpoints in the pool.
     * @param timeout if all endpoints in the pool are busy the pool will waits this timeout (in the milliseconds)
     *      until any endpoint will released.
     */
    public IvrEndpoint getEndpoint(long timeout);
    /**
     * Releases the endpoint back to the pool
     * @param endpoint the endpoint which must be released.
     */
    public void releaseEndpoint(IvrEndpoint endpoint);
}
