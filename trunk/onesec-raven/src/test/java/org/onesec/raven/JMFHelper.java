/*
 *  Copyright 2010 Mikhail Titov.
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

package org.onesec.raven;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.media.Controller;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import org.apache.commons.io.FileUtils;
import org.onesec.raven.ivr.impl.ControllerStateWaiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class JMFHelper
{
    private final static Logger logger = LoggerFactory.getLogger(JMFHelper.class);

    public static OperationController writeToFile(DataSource dataSource, final String filename)
            throws Exception
    {
        logger.debug("Creating new file writer ({})", filename);
        File file = new File(filename);
        if (file.exists())
            FileUtils.forceDelete(file);
        DataSink _writer = null;
        MediaLocator dest = new MediaLocator(file.toURI().toURL());
        Processor p = null;
        try{
            _writer = Manager.createDataSink(dataSource, dest);
        }catch(Exception e){
            logger.warn("Error creating data sink directly from the data source, so creating a processor");
            AudioFormat format = new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN
                    , AudioFormat.SIGNED);
            p = ControllerStateWaiter.createRealizedProcessor(dataSource, format, 4000
                    , new ContentDescriptor(FileTypeDescriptor.WAVE));
//            p = Manager.createRealizedProcessor(new ProcessorModel(
//                    dataSource
//                    , new Format[]{new AudioFormat(
//                        AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)}
//                    , new ContentDescriptor(FileTypeDescriptor.WAVE)));
            _writer = Manager.createDataSink(p.getDataOutput(), dest);
            p.start();
        }
        final DataSink writer = _writer;
        final Processor processor = p;
        writer.open();
        logger.debug("Opening file writer for a file ({})", filename);
        writer.start();
        logger.debug("Starting writing to a file ({})", filename);

        return new OperationController() {
            public void stop() {
                try {
                    logger.debug("Stoping writing to a file ({})", filename);
                    if (processor!=null){
                        processor.stop();
                        processor.close();
                    }
                    writer.stop();
                } catch (IOException ex) {
                    logger.error("Error stoping writeToFile process", ex);
                }
            }
        };
    }

    private static void waitForState(Controller p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(5);
            if (System.currentTimeMillis()-startTime>2000)
                throw new Exception("Processor state wait timeout");
        }
    }

    public interface OperationController
    {
        public void stop();
    }

}
