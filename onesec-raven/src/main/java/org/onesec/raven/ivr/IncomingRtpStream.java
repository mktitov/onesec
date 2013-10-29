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

import javax.media.format.AudioFormat;

/**
 *
 * @author Mikhail Titov
 */
public interface IncomingRtpStream extends RtpStream
{
    /**
     * Open the incoming input stream
     * @param remoteHost the host, from which audio stream incoming
     * @param remotePort the port from which
     * @throws RtpStreamException
     */
    public void open(String remoteHost) throws RtpStreamException;
    public void open(String remoteHost, int remotePort) throws RtpStreamException;
    /**
     * Returns <b>true</b> if listener was successfully added or <b>false</b> if incoming rtp stream
     * is already closed.
     * @param listener the listener
     * @param format the audio format 
     * @throws RtpStreamException
     */
    public boolean addDataSourceListener(IncomingRtpStreamDataSourceListener listener, AudioFormat format)
            throws RtpStreamException;
}
