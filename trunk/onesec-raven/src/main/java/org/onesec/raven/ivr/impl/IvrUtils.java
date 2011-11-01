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
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.AudioStream;
import org.onesec.raven.ivr.Cacheable;
import org.onesec.raven.ivr.InputStreamSource;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.actions.AsyncAction;

/**
 *
 * @author Mikhail Titov
 */
public class IvrUtils
{
    private IvrUtils()     {
    }

    public static void playAudioInAction(AsyncAction action, IvrEndpointConversation conversation
            , InputStreamSource audio)
        throws InterruptedException
    {
        playAudioInAction(action, conversation, audio, null);
    }

    public static void playAudioInAction(AsyncAction action, IvrEndpointConversation conversation
            , InputStreamSource audio, Cacheable cacheInfo)
        throws InterruptedException
    {
        AudioStream stream = conversation.getAudioStream();
        if (stream!=null){
            if (cacheInfo==null || !cacheInfo.isCacheable())
                stream.addSource(audio);
            else
                stream.addSource(cacheInfo.getCacheKey(), cacheInfo.getCacheChecksum(), audio);
//            Thread.sleep(200);
            while (!action.hasCancelRequest() && stream.isPlaying())
                TimeUnit.MILLISECONDS.sleep(10);
            TimeUnit.MILLISECONDS.sleep(20);
        }
    }

    public static void playAudioInAction(AsyncAction action, IvrEndpointConversation conversation
            , AudioFile audio)
        throws InterruptedException
    {
        AudioFileInputStreamSource source = new AudioFileInputStreamSource(audio, conversation.getOwner());
        playAudioInAction(action, conversation, source, audio);
    }
}
