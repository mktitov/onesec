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

import com.sun.media.codec.audio.ulaw.Packetizer;
import java.io.IOException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import javax.media.Codec;
import javax.media.Format;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import org.onesec.raven.codec.AlawEncoder;
import org.onesec.raven.codec.AlawPacketizer;
import org.onesec.raven.codec.UlawPacketizer;
import org.onesec.raven.codec.g729.G729Decoder;
import org.onesec.raven.codec.g729.G729Encoder;
import org.onesec.raven.ivr.CodecManager;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class CodecManagerImpl implements CodecManager {
    
    private final Logger logger;
    private final Format alawRtpFormat;
    private final Format g729RtpFormat;
    private FormatInfo[] formats;

    public CodecManagerImpl(Logger logger) throws IOException {
        this.logger = logger;
        if (PlugInManager.removePlugIn(Packetizer.class.getName(), PlugInManager.CODEC))
            logger.debug("ULAW packetizier codec (with getControls() bug) ({}) successfully removed"
                    , Packetizer.class.getName());
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

        G729Decoder d = new G729Decoder();
        PlugInManager.addPlugIn(G729Decoder.class.getName()
                , d.getSupportedInputFormats()
                , d.getSupportedOutputFormats(null)
                , PlugInManager.CODEC);
        logger.debug("G729 decoder/depacketizer ({}) successfully added", G729Decoder.class.getName());

        alawRtpFormat = p.getSupportedOutputFormats(null)[0];
        g729RtpFormat = g.getSupportedOutputFormats(null)[0];

        PlugInManager.commit();
        
        getFormatInfo();
    }
    
    private void getFormatInfo() {
        ArrayList<FormatInfo> _formats = new ArrayList<FormatInfo>(512);
        Collection<String> codecs = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);
        for (String className: codecs) {
            try {
                Codec codec = (Codec) Class.forName(className).newInstance();
                Format[] inFormats = codec.getSupportedInputFormats();
                if (inFormats==null || inFormats.length==0 || !(inFormats[0] instanceof AudioFormat))
                    continue;
                for (Format inFormat: inFormats) {
                    Format[] outFormats = codec.getSupportedOutputFormats(inFormat);
                    if (outFormats!=null && outFormats.length>0)
                        for (Format outFormat: outFormats)
                            _formats.add(new FormatInfo(outFormat, inFormat, codec.getClass()));
                }
                    
            } catch (Exception e) {
                if (logger.isErrorEnabled())
                    logger.error("Error creating instance of codec ({})", className);
            }
        }
        formats = new FormatInfo[_formats.size()];
        _formats.toArray(formats);
    }
    
    public Format getAlawRtpFormat() {
        return alawRtpFormat;
    }

    public Format getG729RtpFormat() {
        return g729RtpFormat;
    }
    
    public Codec[] buildCodecChain(Format inFormat, Format outFormat) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private CodecNode getChainTail(CodecNode parent, Format inFormat, Format outFormat) {
        return null;
    }
    
    private class CodecNode {
        private final CodecNode parent;
        private final Class codec;

        public CodecNode(CodecNode parent, Class codec) {
            this.parent = parent;
            this.codec = codec;
        }
    }
    
    private class FormatInfo {
        private final Format outFormat;
        private final Format inFormat;
        private final Class codec;

        public FormatInfo(Format outFormat, Format inFormat, Class codec) {
            this.outFormat = outFormat;
            this.inFormat = inFormat;
            this.codec = codec;
        }
    }
}
