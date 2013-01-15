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
package org.onesec.raven;

import java.io.InputStream;
import java.util.Locale;
import org.apache.commons.io.IOUtils;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.raven.tree.DataFile;
import org.raven.tree.ResourceManager;
import org.raven.tree.ResourceRegistrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SoundResourcesRegistrator implements ResourceRegistrator, Constants {
    
    public static final String RES_BASE = "/org/onesec/raven/sounds/";
    
    private final static Logger logger = LoggerFactory.getLogger(SoundResourcesRegistrator.class);
    
    private final static String[] names = new String[] {
        "hello_ru",
        "status/current_status_ru",
        "status/press_1_to_change_status_ru",
        "status/status_available_ru",
        "status/status_unavailable_ru",
        "numbers/female/1'_ru",
        "numbers/female/100_ru",
        "numbers/female/10_ru",
        "numbers/female/11_ru",
        "numbers/female/12_ru",
        "numbers/female/13_ru",
        "numbers/female/14_ru",
        "numbers/female/15_ru",
        "numbers/female/16_ru",
        "numbers/female/17_ru",
        "numbers/female/18_ru",
        "numbers/female/19_ru",
        "numbers/female/1_ru",
        "numbers/female/2'_ru",
        "numbers/female/200_ru",
        "numbers/female/20_ru",
        "numbers/female/2_ru",
        "numbers/female/300_ru",
        "numbers/female/30_ru",
        "numbers/female/3_ru",
        "numbers/female/400_ru",
        "numbers/female/40_ru",
        "numbers/female/4_ru",
        "numbers/female/500_ru",
        "numbers/female/50_ru",
        "numbers/female/5_ru",
        "numbers/female/600_ru",
        "numbers/female/60_ru",
        "numbers/female/6_ru",
        "numbers/female/700_ru",
        "numbers/female/70_ru",
        "numbers/female/7_ru",
        "numbers/female/800_ru",
        "numbers/female/80_ru",
        "numbers/female/8_ru",
        "numbers/female/900_ru",
        "numbers/female/90_ru",
        "numbers/female/9_ru",
        "numbers/female/������_ru",
        "numbers/female/�������_ru",
        "numbers/female/�������_ru",
        "numbers/female/������_ru",
        "numbers/female/�����_ru",
        "numbers/female/�����_ru",
        "numbers/female/�����_ru",
        "numbers/female/������_ru",
        "numbers/female/������_ru"        
    };

    public void registerResources(ResourceManager resourceManager) {
        for (String name: names) {
            try {
                ResInfo resInfo = new ResInfo(name);
                if (!resourceManager.containsResource(resInfo.ravenResName, resInfo.locale)) {
                    InputStream is = this.getClass().getResourceAsStream(resInfo.resPath);
                    try {
                        if (is==null)
                            throw new Exception(String.format("Resource (%s) does not exists", resInfo.resPath));
                        AudioFileNode node = resInfo.createNode();
                        if (!resourceManager.registerResource(resInfo.ravenResName, resInfo.locale, node))
                            throw new Exception("Resource manager can't register resource");
                        DataFile file = node.getAudioFile();
                        file.setFilename(resInfo.fileName);
                        file.setDataStream(is);
                        if (logger.isDebugEnabled())
                            logger.debug("Registered new resource ({})", resInfo.toString());
                    } finally  {
                        IOUtils.closeQuietly(is);
                    }
                }
            } catch (Throwable e) {
                if (logger.isErrorEnabled())
                    logger.error(String.format("Error registering resource (%s)", SOUNDS_RESOURCES_BASE+name), e);
            }
        }
    }
    
    private AudioFileNode createFromRes(String resPath) {
        return null;
    }
    
    private class ResInfo {
        private final String name;
        private final Locale locale;
        private final String resPath;
        private final String ravenResName;
        private final String fileName;
        private AudioFileNode node;

        public ResInfo(String name) throws Exception {
            this.name = name;
            String[] elems = name.split("_");
            if (elems==null || elems.length<=1)
                throw new Exception("Can't detect resource locale");
            String nameWoLocale = name.substring(0, name.lastIndexOf("_"));
            this.locale = new Locale(elems[elems.length-1]);
            resPath = RES_BASE+name+".wav";
            ravenResName = SOUNDS_RESOURCES_BASE+nameWoLocale;
            elems = name.split("/");
            fileName = elems[elems.length-1]+".wav";
        }
        
        private AudioFileNode createNode() {
            return node = new AudioFileNode();
        }

        @Override
        public String toString() {
            return String.format("path in library: %s; path in the tree: %s", resPath, node.getPath());
        }
    }
}