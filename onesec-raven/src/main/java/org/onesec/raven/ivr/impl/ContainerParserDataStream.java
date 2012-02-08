/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.ivr.impl;

import java.io.IOException;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Track;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;

/**
 *
 * @author Mikhail Titov
 */
public class ContainerParserDataStream implements PullBufferStream {
    
    private final Track track;
    private final ContentDescriptor contentDescriptor;
//    private final ContainerParserDataSource dataSource;
    private boolean endOfStream = false;

    public ContainerParserDataStream(Track track, ContainerParserDataSource dataSource) {
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
        this.track = track;
    }
    
//    void setTrack(Track track) {
//        this.track = track;
//    }
//
    public boolean willReadBlock() {
        return track!=null && track.isEnabled() && !endOfStream;
    }

    public void read(Buffer buffer) throws IOException {
        track.readFrame(buffer);
        if (buffer!=null && buffer.isEOM())
            endOfStream = true;
    }
    
    public Format getFormat() {
        return track==null? contentDescriptor : track.getFormat();
    }

    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public boolean endOfStream() {
        return endOfStream;
    }

    public Object[] getControls() {
        return null;
    }

    public Object getControl(String controlType) {
        return null;
    }
}
