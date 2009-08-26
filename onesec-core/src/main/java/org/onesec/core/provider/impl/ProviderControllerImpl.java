/*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.core.provider.impl;

import java.util.concurrent.ExecutorService;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.events.ProvEv;
import javax.telephony.events.ProvInServiceEv;
import javax.telephony.events.ProvOutOfServiceEv;
import javax.telephony.events.ProvShutdownEv;
import org.onesec.core.provider.ProviderController;
import org.onesec.core.provider.ProviderControllerState;
import org.onesec.core.services.StateListenersCoordinator;

/**
 *
 * @author Mikhail Titov
 */
public class ProviderControllerImpl implements ProviderController, ProviderObserver{
    
    public final static String APPINFO = "OneSec server";
    public static final String OBJECT_NAME = "ProviderController";
    
    private int id;
    private int fromNumber;
    private int toNumber;
    private String user;
    private String password;
    private String host;
    private String name;
    
    private ProviderControllerStateImpl state;
    private Provider provider;
    private JtapiPeer jtapiPeer;
    
    private ExecutorService executor;

    public ProviderControllerImpl(
            StateListenersCoordinator stateListenersCoordinator
            , int id, String name, int fromNumber, int toNumber
            , String user, String password, String host) 
    {
        this.id = id;
        this.name = name;
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
        this.user = user;
        this.password = password;
        this.host = host;
        
        state = new ProviderControllerStateImpl(this);
        stateListenersCoordinator.addListenersToState(state);
        state.setState(ProviderControllerState.CONNECTING);
    }

    public void setExecutor(ExecutorService executor)
    {
        this.executor = executor;
    }

    public JtapiPeer getJtapiPeer()
    {
        return jtapiPeer;
    }

    public void setJtapiPeer(JtapiPeer jtapiPeer)
    {
        this.jtapiPeer = jtapiPeer;
    }

    public ProviderControllerState connect() 
    {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    state.setState(ProviderControllerState.CONNECTING);
                    if (provider!=null)
                    {
                        provider.removeObserver(ProviderControllerImpl.this);
                        provider.shutdown();
                    }
                    System.out.println("!!!Getting pear: "+name);
//                    JtapiPeer jtapiPeer = JtapiPeerFactory.getJtapiPeer(null);
                    System.out.println("!!!Getting pear: "+name+": ok");
					
                    System.out.println("!!!Getting provider: "+name);
                    provider = jtapiPeer.getProvider(String.format(
                            "%s;login=%s;passwd=%s;appinfo=%s"
                            , host, user, password, APPINFO));
                    provider.addObserver(ProviderControllerImpl.this);
                    System.out.println("!!!Getting provider: "+name+": ok");
                    password = null;
                } catch (Exception e) {
                    state.setState(ProviderControllerState.OUT_OF_SERVICE, e.getMessage(), e);
                }
            };
        });
        
        return state;
    }

    public void shutdown() {
//        if (!executor.isShutdown())
//            executor.shutdownNow();
        if (provider!=null) {
            provider.removeObserver(this);
            provider.shutdown();
            provider = null;
        }
        state.setState(ProviderControllerState.STOPED);
    }

    public Provider getProvider() {
        return provider;
    }
    
    public ProviderControllerState getState() {
        return state;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getFromNumber() {
        return fromNumber;
    }

    public String getHost() {
        return host;
    }

    public Integer getToNumber() {
        return toNumber;
    }

    public String getUser() {
        return user;
    }

    public void providerChangedEvent(ProvEv[] events) {
        for (ProvEv event: events)
            if (event instanceof ProvInServiceEv)
                state.setState(ProviderControllerState.IN_SERVICE);
            else if (event instanceof ProvShutdownEv)
                state.setState(ProviderControllerState.STOPED);
            else if (event instanceof ProvOutOfServiceEv)
                state.setState(ProviderControllerState.OUT_OF_SERVICE);
    }
    
    @Override
    public String toString() {
        return String.format(
                "Provider: id (%d); numbers block [%d, %d]; user (%s); host (%s). "
                , id, fromNumber, toNumber, user, host);
    }

    public String getObjectDescription() {
        return name;
    }

    public String getObjectName() {
        return OBJECT_NAME;
    }
}
