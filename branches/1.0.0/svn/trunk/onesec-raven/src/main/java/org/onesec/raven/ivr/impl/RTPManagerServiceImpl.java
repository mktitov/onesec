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

package org.onesec.raven.ivr.impl;

import javax.media.Format;
import javax.media.rtp.RTPManager;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.RTPManagerService;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class RTPManagerServiceImpl implements RTPManagerService
{
    private final Logger logger;
    private final Format alawRtpFormat;
    private final Format g729RtpFormat;

    public RTPManagerServiceImpl(Logger logger, CodecManager codecManager) {
        this.logger = logger;
        this.alawRtpFormat = codecManager.getAlawRtpFormat();
        this.g729RtpFormat = codecManager.getG729RtpFormat();
    }

    public RTPManager createRtpManager()
    {
        if (logger.isDebugEnabled())
            logger.debug("Creating new RTPManager");
        RTPManager manager = RTPManager.newInstance();
        manager.addFormat(alawRtpFormat, 8);
        manager.addFormat(g729RtpFormat, 8);

        return manager;
    }
}
