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
package org.onesec.raven.ivr.impl;

import org.onesec.raven.ivr.SipContext;
import static org.onesec.raven.ivr.SipTerminalState.*;
import org.onesec.raven.ivr.SipTerminal;
import org.raven.tree.Node;


/**
 *
 * @author Mikhail Titov
 */
public class SipTerminalImpl implements SipTerminal {
    private final String address;
    private final Node owner;
    private final SipTerminalStateImpl state;
    private final String password;
    private final String protocol;
    private volatile SipContext sipContext;

    public SipTerminalImpl(String address, Node owner, String password, String protocol) {
        this.address = address;
        this.owner = owner;
        this.state = new SipTerminalStateImpl(this);
        this.password = password;
        this.protocol = protocol;
        this.state.setState(OUT_OF_SERVICE);
    }

    public String getAddress() {
        return address;
    }

    public Node getOwner() {
        return owner;
    }

    public void registered(SipContext context) {
        this.sipContext = context;
    }

    public SipTerminalStateImpl getState() {
        return state;
    }

    public void unregistered() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void registerTerminal() {
        
    }

    public String getObjectName() {
        return address;
    }

    public String getObjectDescription() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public String getProtocol() {
        return protocol;
    }
    
}
