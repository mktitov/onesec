/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr;

import java.util.Iterator;
import java.util.List;
import org.onesec.raven.ivr.impl.AudioFileInputStreamSource;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.FutureCallback;
import org.raven.dp.RavenFuture;
import org.raven.dp.UnbecomeFailureException;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;

/**
 *
 * @author Mikhail Titov
 */
public class PlayAudioDP extends AbstractDataProcessorLogic {
    public final static String PLAYED = "PLAYED";
        
    private final AudioStream audioStream;
    private DataProcessorFacade requester;

    public PlayAudioDP(AudioStream audioStream) {
        this.audioStream = audioStream;
    }

    @Override
    public Object processData(Object message) throws Exception {
        if (message instanceof PlayAudio) {
            if (audioStream==null) {
                if (getLogger().isDebugEnabled())
                    getLogger().debug("Can't play audio. Audio stream not ready");
                return PLAYED;
            } else {
                requester = getSender();
                become(playing, false);
                if      (message instanceof PlayAudioFile)          playAudioFile((PlayAudioFile)message);
                else if (message instanceof PlayInputStreamSource)  playInputStreamSource((PlayInputStreamSource)message);
                else if (message instanceof PlayAudioFiles)         playAudioFiles((PlayAudioFiles)message);
                return VOID;    
            }
        }
        return UNHANDLED;
    }
    
    private final Behaviour playing = new Behaviour("Playing") {
        @Override public Object processData(Object message) throws Exception {
            if (message==PLAYED) 
                played();
            else if (message instanceof PlayNext) {
                PlayNext playNext = (PlayNext)message;
                if (playNext.files.hasNext()) {
                    final AudioFile nextFile = playNext.files.next();
                    playAudio(new AudioFileInputStreamSource(nextFile, getContext().getOwner()), nextFile, playNext, playNext.pauseBetweenFragments);
                } else 
                    played();
            } else
                return UNHANDLED;
            return VOID;
        }
        private void played() throws UnbecomeFailureException {
            requester.send(PLAYED);
            requester = null;
            unbecome();
        }
    };

    
    public void playAudio(InputStreamSource audio, Cacheable cacheInfo, final Object message, final long delay) {
        RavenFuture playFuture = cacheInfo==null || !cacheInfo.isCacheable()?
                audioStream.addSource(audio) :
                audioStream.addSource(cacheInfo.getCacheKey(), cacheInfo.getCacheChecksum(), audio);
        waitForFuture(playFuture, message, delay);
    }

    private void waitForFuture(RavenFuture playFuture, final Object message, final long delay) {
        playFuture.onComplete(new FutureCallback() {
            @Override public void onSuccess(Object result) {
                audioPlayed(message, delay);
            }
            @Override public void onError(Throwable error) {
                audioPlayed(message, delay);
            }
            @Override public void onCanceled() {
                audioPlayed(message, delay);
            }
        });
    }
    
    private void audioPlayed(final Object message, final long delay) {
        if (delay>0)
            getFacade().sendDelayed(delay, message);
        else
            getFacade().send(message);
    }

    private void playAudioFile(PlayAudioFile mess) {
        playAudio(new AudioFileInputStreamSource(mess.audioFile, getContext().getOwner()), mess.audioFile, PLAYED, 0);
    }

    private void playInputStreamSource(PlayInputStreamSource mess) {
        playAudio(mess.streamSource, mess.cacheInfo, PLAYED, 0);
    }

    private void playAudioFiles(PlayAudioFiles mess) {
        if (mess.pauseBetweenFragments<0) {
            final RavenFuture playFuture = audioStream.playContinuously(mess.audioFiles, Math.abs(mess.pauseBetweenFragments));
            waitForFuture(playFuture, PLAYED, 0);
        } else {
            final PlayNext playNextMess = new PlayNext(mess.audioFiles.iterator(), mess.pauseBetweenFragments);
            getFacade().send(playNextMess);
        }
    }
    
    private interface PlayAudio {};
    
    public static class PlayAudioFile implements PlayAudio {
        private final AudioFile audioFile;

        public PlayAudioFile(AudioFile audioFile) {
            this.audioFile = audioFile;
        }

        @Override
        public String toString() {
            return "PLAY_AUDIO_FILE: "+audioFile==null?null:audioFile.getName();
        }
    }
    
    public static class PlayInputStreamSource implements PlayAudio {
        private final InputStreamSource streamSource;
        private final Cacheable cacheInfo;

        public PlayInputStreamSource(InputStreamSource streamSource, Cacheable cacheInfo) {
            this.streamSource = streamSource;
            this.cacheInfo = cacheInfo;
        }

        @Override
        public String toString() {
            return "PLAY_INPUT_STREAM_SOURCE (cacheable="+(cacheInfo!=null && cacheInfo.isCacheable())+")";
        }
    }
    
    public static class PlayAudioFiles implements PlayAudio {
        private final List<AudioFile> audioFiles;
        private final long pauseBetweenFragments;

        public PlayAudioFiles(List<AudioFile> audioFiles, long pauseBetweenFragments) {
            this.audioFiles = audioFiles;
            this.pauseBetweenFragments = pauseBetweenFragments;
        }

        @Override
        public String toString() {
            return "PLAY_AUDIO_FILES (count="+audioFiles.size()+")";
        }
    }
    
    private static class PlayNext {
        private final Iterator<AudioFile> files;
        private final long pauseBetweenFragments;

        public PlayNext(Iterator<AudioFile> files, long pauseBetweenFragments) {
            this.files = files;
            this.pauseBetweenFragments = pauseBetweenFragments;
        }

        @Override
        public String toString() {
            return "PLAY_NEXT";
        }
    }
}
