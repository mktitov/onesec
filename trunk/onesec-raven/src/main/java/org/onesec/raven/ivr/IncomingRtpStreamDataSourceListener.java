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

package org.onesec.raven.ivr;

import javax.media.protocol.DataSource;

/**
 * The data source listener of the {@link IncomingRtpStream incoming rtp stream}
 * @author Mikhail Titov
 */
public interface IncomingRtpStreamDataSourceListener
{
    /**
     * Fires when the data source was created for this listener (for each listener creating unique
     * data source)
     * @param dataSource unique data source for this listener.
     * @see IncomingRtpStream
     */
    public void dataSourceCreated(DataSource dataSource);
    /**
     * Fires when the incoming rtp stream closing.
     * @param dataSource
     * @see IncomingRtpStream
     */
    public void streamClosing();
}
