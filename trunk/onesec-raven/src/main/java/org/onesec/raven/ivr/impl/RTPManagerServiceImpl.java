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

import com.sun.media.codec.audio.ulaw.Packetizer;
import java.io.IOException;
import javax.media.Format;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.rtp.RTPManager;
import org.onesec.raven.codec.AlawEncoder;
import org.onesec.raven.codec.AlawPacketizer;
import org.onesec.raven.codec.UlawPacketizer;
import org.onesec.raven.codec.g729.G729Encoder;
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

    public RTPManagerServiceImpl(Logger logger) throws IOException
    {
        this.logger = logger;

        if (PlugInManager.removePlugIn(Packetizer.class.getName(), PlugInManager.CODEC))
            logger.debug("ULAW packetizier codec (with getControls() bug) ({}) successfully removed", Packetizer.class.getName());
        UlawPacketizer up = new UlawPacketizer();
        PlugInManager.addPlugIn(UlawPacketizer.class.getName()
                , up.getSupportedInputFormats(), up.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        logger.debug("New ULAW packetizier codec ({}) successfully added", UlawPacketizer.class.getName());

        AlawEncoder en = new AlawEncoder();
        PlugInManager.addPlugIn(AlawEncoder.class.getName()
                , en.getSupportedInputFormats()
				, en.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        logger.debug("ALAW codec ({}) successfully added", AlawEncoder.class.getName());

        AlawPacketizer p = new AlawPacketizer();
        PlugInManager.addPlugIn(AlawPacketizer.class.getName()
                , p.getSupportedInputFormats()
				, p.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        logger.debug("ALAW packetizer codec ({}) successfully added", AlawPacketizer.class.getName());

        G729Encoder g = new G729Encoder();
        PlugInManager.addPlugIn(G729Encoder.class.getName()
                , g.getSupportedInputFormats()
                , g.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        logger.debug("G729 encoder/packetizer ({}) successfully added", G729Encoder.class.getName());

//
//        RTPManager tempManager = RTPManager.newInstance();
        alawRtpFormat = p.getSupportedOutputFormats(null)[0];
        g729RtpFormat = g.getSupportedOutputFormats(null)[0];
        System.out.println("G729 computed duration: "+((AudioFormat)g729RtpFormat).computeDuration(100));
//        tempManager.addFormat(alawRtpFormat, 8);
//        tempManager.dispose();
//
        PlugInManager.commit();
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
