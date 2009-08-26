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

import java.io.IOException;
import javax.media.DataSink;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;

/**
 *
 * @author Mikhail Titov
 */
public class RTPSession
{
    private final DataSink session;
    private final ConcatDataSource source;

    public RTPSession(String host, int port, ConcatDataSource source) throws NoDataSinkException
    {
        this.source = source;
        session = Manager.createDataSink(
                source, new MediaLocator("rtp://"+host+":"+port+"/audio/1"));
    }

    public void start() throws IOException
    {
        session.open();
        session.start();
    }

    public void stop() throws IOException
    {
        try
        {
            session.stop();
            session.close();
        }
        finally
        {
            source.close();
        }
    }
}
