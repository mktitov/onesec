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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javax.media.Codec;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import org.onesec.raven.codec.AlawEncoder;
import org.onesec.raven.codec.AlawPacketizer;
import org.onesec.raven.codec.UlawPacketizer;
import org.onesec.raven.codec.g729.G729Decoder;
import org.onesec.raven.codec.g729.G729Encoder;
import org.onesec.raven.ivr.CodecConfig;
import org.onesec.raven.ivr.CodecManager;
import org.onesec.raven.ivr.CodecManagerException;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class CodecManagerImpl implements CodecManager {
    
    private final Logger logger;
    private final Format alawRtpFormat;
    private final Format g729RtpFormat;
    private final Map<Format/*inFormat*/, Map<Format/*outFormat*/, CodecConfigMeta[]>> cache =
            new HashMap<Format, Map<Format, CodecConfigMeta[]>>();
    private final Map<String, Class> parsers = new HashMap<String, Class>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
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
        getDemultiplexorsInfo();
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
    
    private void getDemultiplexorsInfo() {
        Collection<String> demuxs = PlugInManager.getPlugInList(null, null, PlugInManager.DEMULTIPLEXER);
        for (String className: demuxs) {
            try {
                Demultiplexer demux = (Demultiplexer) Class.forName(className).newInstance();
                ContentDescriptor[] descs = demux.getSupportedInputContentDescriptors();
                if (descs!=null)
                    for (ContentDescriptor desc: descs)
                        parsers.put(desc.getContentType(), demux.getClass());
            } catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error("Error creating instance of codec ({})", className);
            }
        }
    }
    
    public Format getAlawRtpFormat() {
        return alawRtpFormat;
    }

    public Format getG729RtpFormat() {
        return g729RtpFormat;
    }

    public Demultiplexer buildDemultiplexer(String contentType) {
        Class parserClass = parsers.get(contentType);
        try {
            return parserClass==null? null : (Demultiplexer)parserClass.newInstance();
        } catch (Exception ex) {
            if (logger.isErrorEnabled())
                logger.error("Error creating instance of demultiplexor class", ex);
            return null;
        }
    }
    
    public CodecConfig[] buildCodecChain(Format inFormat, Format outFormat) throws CodecManagerException {
        try {
            CodecConfig[] codecs = getChainFromCache(inFormat, outFormat);
            if (codecs!=null)
                return codecs;
            TailHolder tailHolder = new TailHolder();
            getChainTail(null, inFormat, outFormat, tailHolder);
            if (tailHolder.tail==null)
                throw new CodecManagerException(String.format(
                        "Can't find codec chain to convert data from (%s) to (%s)"
                        , inFormat, outFormat));
            //creating result and caching chain
            CodecNode tail = tailHolder.tail;
            CodecConfigMeta[] meta = new CodecConfigMeta[tail.level];
            codecs = new CodecConfig[meta.length];
            int i=0;
            while (tail!=null) {
                meta[i] = new CodecConfigMeta(tail.codec, tail.inFormat, tail.outFormat);
                codecs[i] = meta[i].createCodecConfig();
                tail = tail.parent;
                i++;
            }
            cacheChain(inFormat, outFormat, meta);
            return codecs;
        } catch (Exception ex) {
            throw new CodecManagerException("Error creating codec chain", ex);
        }
    }
    
    private CodecConfig[] getChainFromCache(Format inFormat, Format outFormat) throws Exception {
        CodecConfigMeta[] meta = null;
        lock.readLock().lock();
        try {
            Map<Format, CodecConfigMeta[]> m = cache.get(inFormat);
            if (m!=null)
                meta = m.get(outFormat);
        } finally {
            lock.readLock().unlock();
        }
        if (meta==null)
            return null;
        CodecConfig[] res = new CodecConfig[meta.length];
        for (int i=0; i<meta.length; ++i)
            res[i] = meta[i].createCodecConfig();
        return res;
    }
    
    private void cacheChain(Format inFormat, Format outFormat, CodecConfigMeta[] chainConfig) {
        if (lock.writeLock().tryLock()) {
            try {
                Map<Format, CodecConfigMeta[]> ref = cache.get(inFormat);
                if (ref==null) {
                    ref = new HashMap<Format, CodecConfigMeta[]>();
                    cache.put(inFormat, ref);
                }
                ref.put(outFormat, chainConfig);
            } finally {
                lock.writeLock().unlock();
            }
        } else if (logger.isWarnEnabled())
            logger.warn("Can't cache chain configuration because of timeout on cache write lock.");
            
    }
    
    private void getChainTail(CodecNode parent, Format inFormat, Format outFormat, TailHolder tailHolder) {
        if (tailHolder.continueSearch(parent))
            for (FormatInfo format: formats)
                if (format.outFormat.matches(outFormat) && !isChainContainsCodec(parent, format)) {
                    CodecNode node = new CodecNode(parent, format.codec, format.inFormat, outFormat);
                    if (format.inFormat.matches(inFormat)) {
                        tailHolder.setTail(node);
                        break;
                    } else 
                        getChainTail(node, inFormat, format.inFormat, tailHolder);
                }        
    }
    
    private boolean isChainContainsCodec(CodecNode tail, FormatInfo fmt) {
        while (tail!=null) 
            if (tail.codec.equals(fmt.codec) || tail.inFormat.matches(fmt.inFormat) || tail.outFormat.matches(fmt.outFormat))
                return true;
            else
                tail = tail.parent;
        return false;
    }
    
    private final class CodecNode {
        private final CodecNode parent;
        private final Class codec;
        private final Format inFormat;
        private final Format outFormat;
        private int level = 1;

        public CodecNode(CodecNode parent, Class codec, Format inFormat, Format outFormat) {
            this.parent = parent;
            this.codec = codec;
            this.inFormat = inFormat;
            this.outFormat = outFormat;
            if (parent!=null)
                level = parent.level+1;
        }
    }
    
    private final class CodecConfigMeta {
        private final Class codecClass;
        private final Format inputFormat;
        private final Format outputFormat;

        public CodecConfigMeta(Class codecClass, Format inputFormat, Format outputFormat) {
            this.codecClass = codecClass;
            this.inputFormat = inputFormat;
            this.outputFormat = outputFormat;
        }
        
        public CodecConfig createCodecConfig() throws Exception {
            return new CodecConfigImpl((Codec)codecClass.newInstance(), outputFormat, inputFormat);
        }
    }
    
    private final class FormatInfo {
        private final Format outFormat;
        private final Format inFormat;
        private final Class codec;

        public FormatInfo(Format outFormat, Format inFormat, Class codec) {
            this.outFormat = outFormat;
            this.inFormat = inFormat;
            this.codec = codec;
        }
    }
    
    private final class TailHolder {
        private CodecNode tail;
        
        private void setTail(CodecNode node) {
            if (tail==null || tail.level>node.level)
                tail = node;
        }
        
        private boolean continueSearch(CodecNode parent) {
            return tail==null || tail.level>parent.level;
        }
    }
}