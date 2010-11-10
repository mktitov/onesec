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

import javax.media.MediaLocator;
import org.onesec.raven.ivr.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.media.Duration;
import javax.media.protocol.PullDataSource;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Mikhail Titov
 */
public class IssDataSource extends PullDataSource
{

    private final InputStreamSource source;
    private final String contentType;
    private IssStream[] sources;
    private boolean connected;
    private boolean started;

    public IssDataSource(InputStreamSource source, String contentType) throws IOException
    {
        this.source = source;
        this.contentType = contentType;
        connected = true;
        sources = new IssStream[1];
        sources[0] = new IssStream();
    }

    @Override
    public MediaLocator getLocator()
    {
        return super.getLocator();
    }

    public void connect() throws java.io.IOException
    {
        connected = true;
    }

    public void disconnect()
    {
        if (connected)
        {
            sources[0].close();
            connected = false;
        }
    }

    public String getContentType()
    {
        return contentType;
    }

    public Object getControl(String str)
    {
        return null;
    }

    public Object[] getControls()
    {
        return new Object[0];
    }

    public javax.media.Time getDuration()
    {
        return Duration.DURATION_UNKNOWN;
    }

    public javax.media.protocol.PullSourceStream[] getStreams()
    {
        return sources;
    }

    public void start() throws IOException
    {
        if (started)
            return;
        started = true;
//        System.out.println("!!!Converting input stream source to bytes");
        InputStream is = source.getInputStream();
        byte[] bytes = null;
        try
        {
            bytes = IOUtils.toByteArray(is);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
//        System.out.println("!!!Readed "+bytes.length+" bytes");
        ByteBuffer inputBuffer = ByteBuffer.wrap(bytes);
        inputBuffer.position(0);
        sources[0].setInputBuffer(inputBuffer);
//        System.out.println("!!!Byte buffer created");
    }

    public void stop() throws IOException
    {
        sources[0].close();
    }
}
