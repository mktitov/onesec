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

import java.io.InputStream;
import org.onesec.raven.ivr.AudioFile;
import org.onesec.raven.ivr.InputStreamSource;
import org.raven.log.LogLevel;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AudioFileInputStreamSource implements InputStreamSource
{
    private final AudioFile fileNode;
    private final Node owner;

    public AudioFileInputStreamSource(AudioFile fileNode, Node owner)
    {
        this.fileNode = fileNode;
        this.owner = owner;
    }

    public InputStream getInputStream()
    {
        if (!fileNode.isStarted()) {
            if (owner.isLogLevelEnabled(LogLevel.WARN))
                owner.getLogger().warn(String.format(
                        "Can not extract audio stream from the (%s) because of node was not started"
                        , fileNode.getPath()));
            return null;
        } try {
            return fileNode.getAudioFile().getDataStream();
        } catch (DataFileException ex) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(
                    String.format(
                        "Error extracting audio stream from audio file node (%s) ",
                        fileNode.getPath())
                    , ex);
            return null;
        }
    }
}
