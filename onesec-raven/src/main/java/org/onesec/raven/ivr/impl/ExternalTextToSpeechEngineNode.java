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

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ExternalTextToSpeechEngineNode extends AbstractDataSource {
    
    public final static String TEXT_ATTR = "text";
    public final static String TEXT_ATTR_DESC_KEY = "textAttrDescription";
    
    @Service
    private static MessagesRegistry messReg;
    
    @NotNull @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String commandLine;
    @NotNull @Parameter(defaultValue="utf-8")
    private Charset textFileEncoding;
    @NotNull
    private String cacheDir;
    
    private Map<String, String> cache;

    @Override
    protected void initFields() {
        super.initFields();
        cache = new ConcurrentHashMap<String, String>();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        readCache();
    }

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
        NodeAttributeImpl textAttr = new NodeAttributeImpl(TEXT_ATTR, String.class, null, null);
        MessageComposer comp = new MessageComposer(messReg)
                .append(messReg.createMessageKeyForStringValue(
                    getClass().getCanonicalName(), TEXT_ATTR_DESC_KEY));
        textAttr.setDescriptionContainer(comp);
        textAttr.setRequired(true);
        consumerAttributes.add(textAttr);
    }
    
    private void readCache() throws Exception {
        File cacheFile = new File(cacheDir);
        if (!cacheFile.exists() || !cacheFile.isDirectory() || !cacheFile.canRead() || !cacheFile.canWrite())
            throw new Exception(String.format("Not valid path for cache (%s). It's not a directory or "
                    + "file not exists or can't read/write to this directory", cacheDir));
        File[] textFiles = cacheFile.listFiles((FilenameFilter)new SuffixFileFilter(".txt"));
        if (textFiles!=null && textFiles.length>0)
            for (File textFile: textFiles) {
                String text = FileUtils.readFileToString(textFile, textFileEncoding.name());
                String wavFileName = FilenameUtils.removeExtension(textFile.getPath())+".wav";
                if (!(new File(wavFileName).exists())) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error("Found text file ({}) but not found WAV file ({}). "
                                + "Removing invalid cached entry", textFile.getPath(), wavFileName);
                    textFile.delete();
                } else 
                    cache.put(text, wavFileName);
            }
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public Charset getTextFileEncoding() {
        return textFileEncoding;
    }

    public void setTextFileEncoding(Charset textFileEncoding) {
        this.textFileEncoding = textFileEncoding;
    }
}
