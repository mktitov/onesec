/*
 *  Copyright 2010 Mikhail Titov.
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

package org.onesec.raven.codec.g729;

import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public class G729AudioFormat extends AudioFormat
{
    private double koef = 0d;

    public G729AudioFormat(Format format)
    {
        super(G729_RTP);
        copy(format);
    }

    @Override
    public long computeDuration(long length)
    {
        if (koef==0d)
            koef = (64000000 / sampleSizeInBits / channels / sampleRate);
        return (long) (length * koef * 1000L);
    }
}
