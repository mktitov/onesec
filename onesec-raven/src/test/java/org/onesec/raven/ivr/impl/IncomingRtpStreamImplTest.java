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

package org.onesec.raven.ivr.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.rtp.SendStream;
import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.RtpManagerTestCase;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IncomingRtpStream;
import org.onesec.raven.ivr.IncomingRtpStreamDataSourceListener;

/**
 *
 * @author Mikhail Titov
 */
public class IncomingRtpStreamImplTest extends RtpManagerTestCase 
{
    @Before
    public void prepare()
    {
    }

    public void sendOverRtpTest() throws Exception
    {
        OperationState state = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, "10.50.1.85", 1234);
        state.join();
    }

    @Test
    public void simpleTest() throws Exception
    {
        IncomingRtpStream irtp = manager.getIncomingRtpStream(manager);
        String address = getInterfaceAddress().getHostName();
        irtp.open(address);
        DataSourceListener fileWriter = new DataSourceListener("target/recorded.wav");
        irtp.addDataSourceListener(fileWriter, new ContentDescriptor(FileTypeDescriptor.WAVE));
        OperationState sendControl = sendOverRtp(
                "src/test/wav/test.wav", Codec.G711_MU_LAW, address, irtp.getPort());
        sendControl.join();
        irtp.release();
    }

    private class DataSourceListener implements IncomingRtpStreamDataSourceListener
    {
        private final String filename;
        private OperationController writeControl;

        public DataSourceListener(String filename) {
            this.filename = filename;
            this.writeControl = null;
        }

        public void dataSourceCreated(DataSource dataSource)
        {
            if (dataSource!=null)
                try {
                    logger.debug("Creating new file writer ({})", filename);
                    writeControl = writeToFile(dataSource, filename);
                } catch (Exception ex) {
                    logger.error("Error writing to file ({})", filename, ex);
                }

        }

        public void streamClosing()
        {
            if (writeControl!=null)
                writeControl.stop();
        }
    }
}