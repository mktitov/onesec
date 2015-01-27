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
package org.onesec.raven.ivr;

import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public interface CodecManager {
    public CodecConfig[] buildCodecChain(AudioFormat inFormat, AudioFormat outFormat) throws CodecManagerException;
    public Demultiplexer buildDemultiplexer(String contentType);
    public Multiplexer buildMultiplexer(String contentType);
    public Format getAlawRtpFormat();
    public Format getG729RtpFormat();    
}
