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

import java.util.Collection;
import org.raven.annotations.Parameter;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class IvrInformerScheduler extends BaseNode implements Schedulable
{
    public final static String START_SCHEDULER = "Start scheduler";
    public final static String STOP_SCHEDULER = "Stop scheduler";

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler scheduler;

    public IvrInformerScheduler()
    {
        super();
    }

    public IvrInformerScheduler(String name)
    {
        super(name);
    }

    public Scheduler getScheduler()
    {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
        IvrInformer informer = (IvrInformer) getParent();
        if (START_SCHEDULER.equals(getName()))
            informer.startProcessing();
        else
            informer.stopProcessing();
    }
}
