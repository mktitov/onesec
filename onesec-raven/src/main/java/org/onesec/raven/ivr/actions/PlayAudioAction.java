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

import java.io.InputStream;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioAction extends AsyncAction implements InputStreamSource
{
    public final static String NAME = "Play audio action";
    private final AudioFileNode audioFile;

    public PlayAudioAction(AudioFileNode audioFile)
    {
        super(NAME);
        this.audioFile = audioFile;
    }

    public boolean isFlowControlAction() {
        return false;
    }

    @Override
    protected void doExecute(IvrEndpointConversation conversation) throws Exception
    {
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(
                    logMess("Playing audio from source (%s)", audioFile.getPath()));
        AudioStream stream = conversation.getAudioStream();
        if (stream!=null){
            stream.addSource(this);
            Thread.sleep(100);
            while (!hasCancelRequest() && stream.isPlaying())
                Thread.sleep(10);
        }
        if (conversation.getOwner().isLogLevelEnabled(LogLevel.DEBUG))
            conversation.getOwner().getLogger().debug(logMess(
                    "Audio source (%s) successfuly played ", audioFile.getPath()));
    }

    public InputStream getInputStream()
    {
        try
        {
            return audioFile.getAudioFile().getDataStream();
        }
        catch (Exception ex)
        {
            if (conversation.getOwner().isLogLevelEnabled(LogLevel.ERROR))
                conversation.getOwner().getLogger().error(
                    logMess("Error geting audio stream from audio file node (%s) ", audioFile.getPath())
                    , ex);
            return null;
        }
    }
}
