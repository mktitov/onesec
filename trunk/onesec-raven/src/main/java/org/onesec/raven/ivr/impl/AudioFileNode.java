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

package org.onesec.raven.ivr.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.onesec.raven.ivr.AudioFile;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.BindingSupport;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class AudioFileNode extends BaseNode implements Viewable, AudioFile
{
    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile audioFile;

    @NotNull @Parameter(defaultValue="true")
    private Boolean cacheEnabled;

    public DataFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(DataFile audioFile) {
        this.audioFile = audioFile;
    }

    public Boolean getCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(Boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public String getCacheKey() {
        return ""+getId();
    }

    public boolean isCacheable() {
        return cacheEnabled;
    }

    public long getCacheChecksum() {
        try {
            return audioFile.getChecksum();
        } catch (DataFileException ex) {
            return 0l;
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        if (Status.STARTED!=getStatus() 
            || audioFile.getMimeType()==null || audioFile.getMimeType().isEmpty())
        {
            return null;
        }

        ViewableObject obj = new DataFileViewableObject(audioFile, this);

        return Arrays.asList(obj);
    }

    public BindingSupport getBindingSupport() {
        return null;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }
}
