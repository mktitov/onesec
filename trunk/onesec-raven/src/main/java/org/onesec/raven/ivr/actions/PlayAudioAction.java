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

package org.onesec.raven.ivr.actions;

import java.io.FileInputStream;
import java.io.InputStream;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpoint;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.log.LogLevel;
import org.raven.tree.DataFileException;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioAction extends AsyncAction implements InputStreamSource
{
    public final static String NAME = "Play audio action";
    private final AudioFileNode audioFile;
    private IvrEndpoint endpoint;

    public PlayAudioAction(AudioFileNode audioFile)
    {
        super(NAME);
        this.audioFile = audioFile;
    }

    @Override
    protected void doExecute(IvrEndpoint endpoint) throws Exception
    {
        if (endpoint.isLogLevelEnabled(LogLevel.DEBUG))
            endpoint.getLogger().debug(
                    String.format("Action. Playing audio from source (%s)", audioFile.getPath()));
        this.endpoint = endpoint;
        AudioStream stream = endpoint.getAudioStream();
        stream.addSource(this);
        Thread.sleep(3000);
        while (!hasCancelRequest() && stream.isPlaying())
            Thread.sleep(1000);
        if (endpoint.isLogLevelEnabled(LogLevel.DEBUG))
            endpoint.getLogger().debug(String.format(
                    "Action. Audio source (%s) successfuly played ", audioFile.getPath()));
    }

    public InputStream getInputStream()
    {
        try
        {
            return audioFile.getAudioFile().getDataStream();
//            return new FileInputStream("src/test/wav/test.wav");
        }
        catch (Exception ex)
        {
            if (endpoint.isLogLevelEnabled(LogLevel.ERROR))
                endpoint.getLogger().error(
                    String.format(
                        "Action. Error geting audio stream from audio file node (%s) ",
                        audioFile.getPath())
                    , ex);
            return null;
        }
    }
}
