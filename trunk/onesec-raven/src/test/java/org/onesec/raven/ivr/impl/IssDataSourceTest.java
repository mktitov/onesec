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

import java.util.concurrent.TimeUnit;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import org.junit.Test;
import org.onesec.raven.ivr.InputStreamSource;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class IssDataSourceTest implements ControllerListener
{
    @Test
    public void test() throws Exception
    {
        InputStreamSource source = new TestInputStreamSource("src/test/wav/test.wav");
//        InputStreamSource source = new TestInputStreamSource("/home/tim/tmp/sound/silence.wav");
        IssDataSource dataSource = new IssDataSource(source, FileTypeDescriptor.WAVE);

        Processor processor = Manager.createProcessor(dataSource);
        processor.addControllerListener(this);
        processor.configure();
        waitForState(processor, Processor.Configured);
        Format format = new AudioFormat("ALAW", 8000d, 8, 1);
        TrackControl[] tracks = processor.getTrackControls();
        tracks[0].setFormat(format);
        processor.realize();
        waitForState(processor, Processor.Realized);

//        Player player = Manager.createPlayer(dataSource);
        PushBufferDataSource ds = (PushBufferDataSource) processor.getDataOutput();
        Player player = Manager.createPlayer(ds);
        player.start();
        processor.start();
        TimeUnit.SECONDS.sleep(5);
        System.out.println("Content type: "+ds.getContentType());
        System.out.println("Format: "+ds.getStreams()[0].getFormat());
//        fail();
    }

    private static void waitForState(Processor p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(50);
            if (System.currentTimeMillis()-startTime>5000)
                throw new Exception("Processor state wait timeout");
        }
    }

    public void controllerUpdate(ControllerEvent event)
    {
    }

}