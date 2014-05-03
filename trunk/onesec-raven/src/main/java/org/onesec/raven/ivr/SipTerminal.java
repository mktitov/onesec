/*
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onesec.raven.ivr;

import org.onesec.core.ObjectDescription;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface SipTerminal extends ObjectDescription {
    public String getAddress();
    public String getPassword();
    public String getProtocol();
    public Node getOwner();
    public void registered(SipContext context);
    public void unregistered();
    public SipTerminalState getState();
}
