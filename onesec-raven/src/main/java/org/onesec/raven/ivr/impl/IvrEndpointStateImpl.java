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

package org.onesec.raven.ivr.impl;

import org.onesec.core.impl.BaseState;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.IvrEndpointState;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointStateImpl 
        extends BaseState<IvrEndpointState, IvrEndpoint>
        implements IvrEndpointState
{
    public IvrEndpointStateImpl(IvrEndpoint observableObject)
    {
        super(observableObject);
        addIdName(OUT_OF_SERVICE, "OUT_OF_SERVICE");
        addIdName(IN_SERVICE, "IN_SERVICE");
        addIdName(ACCEPTING_CALL, "ACCEPTING_CALL");
        addIdName(INVITING, "INVITING");
        addIdName(TALKING, "TALKING");
    }
}
