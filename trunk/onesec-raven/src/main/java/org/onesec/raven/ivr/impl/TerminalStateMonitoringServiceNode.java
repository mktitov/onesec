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

package org.onesec.raven.ivr.impl;

import java.util.concurrent.atomic.AtomicLong;
import org.onesec.raven.ivr.IvrTerminal;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class TerminalStateMonitoringServiceNode extends BaseNode 
{
    public final static String NAME = "Terminal state monitoring";
    
    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    private AtomicLong restartedCount;
    private AtomicLong stoppedCount;

//    @Service
//    private static TerminalStateMonitoringService service;

    public TerminalStateMonitoringServiceNode() {
        super(NAME);
    }
    
    @Override
    protected void initFields() {
        super.initFields();
        restartedCount = new AtomicLong();
        stoppedCount = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        restartedCount.set(0);
        stoppedCount.set(0);
    }

    @Parameter(readOnly=true)
    public long getRestartedCount() {
        return restartedCount.get();
    }
    
    @Parameter(readOnly=true)
    public long getStoppedCount() {
        return stoppedCount.get();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
    
    void startTerminal(final IvrTerminal term) {
        if (!isStarted()) return;
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Restarting terminal ({})", term);
        executor.executeQuietly(new AbstractTask(this, "Restarting terminal: "+term) {
            @Override public void doRun() throws Exception {
                if (!ObjectUtils.in(term.getStatus(), Status.INITIALIZED, Status.STARTED)) return;
                if (term.isStarted()) term.stop();
                if (term.isAutoStart()) {
                    if (term.start()) {
                        restartedCount.incrementAndGet();
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            getLogger().debug("Terminal ({}) successfully restarted", term);
                    }
                } else if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn("Can't start terminal ({}) because of autoStart==false", term.getPath());        
            }
        });
        
    }
    
    void stopTerminal(final IvrTerminal term) {
        if (!isStarted()) return;
        executor.executeQuietly(new AbstractTask(this, "Restarting terminal: "+term) {
            @Override public void doRun() throws Exception {
                if (term.isStarted()) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        getLogger().debug("Stopping terminal ({})", term.getPath());
                    term.stop();
                    stoppedCount.incrementAndGet();
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        getLogger().debug("Terminal stopped ({})", term.getPath());
                }
            }
        });
    }
}
