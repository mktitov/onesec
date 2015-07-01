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
package org.onesec.raven.ivr.vmail.impl;

import java.util.Collection;
import org.onesec.raven.ivr.vmail.VMailBox;
import static org.raven.RavenUtils.*;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
/**
 *
 * @author Mikhail Titov
 */
public class VMailBoxStatusChannel extends BaseNode implements DataSource {
    public static final String NAME = "VBoxes status channel";
    public static final String EVENT_TYPE_FIELD = "eventType";
    public static final String VMAIL_BOX_FIELD = "vmailBox";
    
    public enum EventType {NBOX_BECAME_EMPTY, NBOX_BECAME_NON_EMPTY, SBOX_BECAME_EMPTY, SBOX_BECAME_NON_EMPTY}
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    public VMailBoxStatusChannel() {
        super(NAME);
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this dataSource");
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }
    
    public void pushEvent(VMailBox vbox, EventType eventType) {
        if (isStarted()) {
            DataSourceHelper.sendDataToConsumers(
                    executor, this, 
                    asMap(pair(EVENT_TYPE_FIELD, (Object)eventType.name()), pair(VMAIL_BOX_FIELD, (Object)vbox)), 
                    new DataContextImpl());
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
