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

package org.onesec.raven.ivr;

import java.util.List;
import javax.media.protocol.DataSource;

/**
 *
 * @author Mikhail Titov
 */
public interface AudioStream
{
    public void addSource(InputStreamSource source);
    public void addSource(InputStreamSource source, AudioStreamSourceListener listener);
    public void addSource(DataSource source);
    public void addSource(DataSource source, AudioStreamSourceListener listener);
    public void addSource(String key, long checksum, DataSource source);
    public void addSource(String key, long checksum, DataSource source, AudioStreamSourceListener listener);
    public void addSource(String key, long checksum, InputStreamSource source);
    public void addSource(String key, long checksum, InputStreamSource source, AudioStreamSourceListener listener);
    /**
     * Play passed audio files continuously. 
     * @param files - files which will be played continuously
     * @param trimPeriod - period in milliseconds which will be dropped from the end of all audio files exclude the last 
     */
    public void playContinuously(List<AudioFile> files, long trimPeriod);
    public void playContinuously(List<AudioFile> files, long trimPeriod, AudioStreamSourceListener sourceListener);
    /**
     * Returns true if audio stream has buffers that not played yet.
     */
    public boolean isPlaying();
    /**
     * Returns audio source
     */
    public DataSource getDataSource();
    /**
     * Resets the stream. This means that all audio data is not played for now will be cleared.
     */
    public void reset();
    /**
     * Closes audio source.
     */
    public void close();
}
