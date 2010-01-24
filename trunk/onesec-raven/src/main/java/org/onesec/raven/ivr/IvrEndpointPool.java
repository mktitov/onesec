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

import org.raven.sched.ExecutorServiceException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrEndpointPool extends Node
{
    /**
     * Send request for endpoint. When pool will found free endpoint or when
     * {@link EndpointRequest#getWaitTimeout() timeout} will reached pool executes
     * {@link EndpointRequest#processRequest(org.onesec.raven.ivr.IvrEndpointPool) callback method}
     * in the async manner (in the separate thread)
     */
    public void requestEndpoint(EndpointRequest request) throws ExecutorServiceException;
}
