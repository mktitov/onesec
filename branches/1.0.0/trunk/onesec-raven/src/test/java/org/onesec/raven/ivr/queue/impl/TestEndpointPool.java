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

package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.EndpointRequest;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointPool;
import org.onesec.raven.ivr.ReserveEndpointRequest;
import org.raven.conv.ConversationScenario;
import org.raven.sched.ExecutorServiceException;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestEndpointPool extends BaseNode implements IvrEndpointPool
{
    private IvrEndpointPool endpointPool;

    public IvrEndpointPool getEndpointPool() {
        return endpointPool;
    }

    public void setEndpointPool(IvrEndpointPool endpointPool) {
        this.endpointPool = endpointPool;
    }

    public void requestEndpoint(EndpointRequest request) throws ExecutorServiceException {
        endpointPool.requestEndpoint(request);
    }

    public void releaseEndpoint(IvrEndpoint endpoint) {
        endpointPool.releaseEndpoint(endpoint);
    }

    public IvrEndpoint reserveEndpoint(long timeout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IvrEndpoint reserveEndpoint(ReserveEndpointRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ConversationScenario getConversationScenario(IvrEndpoint endpoint) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
