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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

/**
 *
 * @author Mikhail Titov
 */
public class ControllerStateWaiter implements ControllerListener
{
    private final int[] states;
    private final Controller controller;
    private final Lock lock;
    private final Condition condition;
    private final int timeout;

    private int stateIndex;

    public ControllerStateWaiter(int[] states, Controller controller, int timeout)
    {
        this.states = states;
        this.controller = controller;
        this.timeout = timeout;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();

        stateIndex = 0;
        this.controller.addControllerListener(this);
    }

    public void waitForNextState() throws Exception
    {
        if (stateIndex>=states.length)
            return;
        lock.lock();
        try {
            if (controller.getState()==states[stateIndex])
                return;
            condition.await(timeout, TimeUnit.MILLISECONDS);
            if (controller.getState()!=states[stateIndex])
                throw new Exception("Controller state wait timeout");
            ++stateIndex;
        } finally {
            lock.unlock();
        }
        if (stateIndex==states.length)
            controller.removeControllerListener(this);
    }

    public void controllerUpdate(ControllerEvent event)
    {
        boolean found = false;
        for (int state: states)
            if (state==event.getSourceController().getState()) {
                found = true;
                break;
            }

        if (!found) return;

        System.out.println("Received event: "+event.toString());

        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public static Processor createRealizedProcessor(
            DataSource source, Format format, int stateWaitTimeout, ContentDescriptor contentDescriptor)
        throws Exception
    {
        Processor p = Manager.createProcessor(source);
        ControllerStateWaiter waiter = new ControllerStateWaiter(
                new int[]{Processor.Configured, Processor.Realized}, p, stateWaitTimeout);
        p.configure();
        waiter.waitForNextState();
        if (contentDescriptor!=null)
            p.setContentDescriptor(contentDescriptor);
        TrackControl[] tracks = p.getTrackControls();
        tracks[0].setFormat(format);
        p.realize();
        waiter.waitForNextState();
        
        return p;
    }
    
    public static Processor createRealizedProcessor(
            DataSource source, Format format, int stateWaitTimeout)
        throws Exception
    {
        return createRealizedProcessor(source, format, stateWaitTimeout, null);
    }
}
