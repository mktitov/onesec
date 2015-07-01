/*
 * Copyright 2013 Mikhail Titov.
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

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoAddress;
import java.util.Map;
import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.events.AddrEv;
import org.onesec.core.services.ProviderRegistry;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.BindingSupport;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.LoggerHelper;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class MessageWaitingIndicatorSwitcher extends AbstractSafeDataPipe {
    public static final String ADDRESS_FIELD = "address";
    public static final String INDICATOR_FIELD = "indicator";
    
    @Service
    protected static ProviderRegistry providerRegistry;
    
    @NotNull @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue = "500")
    private Long switchTimeout;

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Long getSwitchTimeout() {
        return switchTimeout;
    }

    public void setSwitchTimeout(Long switchTimeout) {
        this.switchTimeout = switchTimeout;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data!=null) {
            Map<String, Object> req = converter.convert(Map.class, data, null);
            String address = converter.convert(String.class, req.get(ADDRESS_FIELD), null);
            if (address==null)
                throw new Exception("Field 'address' can not be null");
            Boolean switchStatus = converter.convert(Boolean.class, req.get(INDICATOR_FIELD), null);
            if (switchStatus==null)
                throw new Exception("Field 'switch' can not be null");
            Address addr = providerRegistry.getProviderController(address).getProvider().getAddress(address);
            Switcher switcher = new Switcher((CiscoAddress)addr, switchStatus);
            executor.executeQuietly(switchTimeout, switcher);
            switcher.switchIndicator();
        }
        sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }
    
    private class Switcher extends AbstractTask implements AddressObserver {
        private final CiscoAddress address;
        private final boolean indicator;
        private final LoggerHelper logger;
        private volatile boolean finished = false;

        public Switcher(CiscoAddress address, boolean indicator) {
            super(MessageWaitingIndicatorSwitcher.this, "MWI switch timeout for "+address);
            this.address = address;
            this.indicator = indicator;
            this.logger = new LoggerHelper(MessageWaitingIndicatorSwitcher.this, 
                    String.format("(addr:%s; new indicator state:%s) ", address.getName(), indicator));
        }
        
        public void switchIndicator() throws Exception {
            if (logger.isDebugEnabled())
                logger.debug("Waiting for address IN_SERVICE");
            address.addObserver(this);
        }

        public void addressChangedEvent(AddrEv[] events) {
        if (logger.isDebugEnabled())
            logger.debug("Recieved address events: "+eventsToString(events));
        for (AddrEv ev: events)
            switch (ev.getID()) {
                case CiscoAddrInServiceEv.ID: doSwitch(); break;
//                case CiscoAddrOutOfServiceEv.ID: termAddressInService = false; checkState(); break;
            }
        }
        
        private void doSwitch() {
            try {
                try {
                    if (logger.isDebugEnabled())
                        logger.debug("Switching...");
                    address.setMessageWaiting(address.getName(), indicator);
                    if (logger.isDebugEnabled())
                        logger.debug("Ok");
                } catch (Throwable e) {
                    if (logger.isErrorEnabled())
                        logger.error("Switching error", e);
                }
            } finally {
                address.removeObserver(Switcher.this);
                finished = true;
                cancel();
            }
        }

        @Override
        public void doRun() throws Exception {
            if (!finished) {
                if (logger.isWarnEnabled())
                    logger.warn("Timeout waiting for switch MWI on "+address+". Canceling");
                address.removeObserver(this);
            }
        }

        private String eventsToString(Object[] events) {
            StringBuilder buf = new StringBuilder();
            for (int i=0; i<events.length; ++i)
                buf.append(i > 0 ? ", " : "").append(events[i].toString());
            return buf.toString();
        }
    }
}
