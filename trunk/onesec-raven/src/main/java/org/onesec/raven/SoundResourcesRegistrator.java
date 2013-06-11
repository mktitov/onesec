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
        "beeps/phone_beep_ru",
        "beeps/long_phone_beep_ru",
        "status/current_status_ru",
        "status/press_1_to_change_status_ru",
        "status/status_available_ru",
        "status/status_unavailable_ru",
        "vmail/male/greeting_ru",
        "vmail/male/and_ru",
        "vmail/male/message_recorded_ru",
        "vmail/male/you_have_ru",
        "vmail/male/received_ru",
        "vmail/male/no_messages_ru",
        "vmail/male/new_message1_ru",
        "vmail/male/new_message2_ru",
        "vmail/male/new_message3_ru",
        "vmail/male/saved_message1_ru",
        "vmail/male/saved_message2_ru",
        "vmail/male/saved_message3_ru",
        "vmail/male/press_1_to_listen_new_messages_ru",
        "vmail/male/press_2_to_listen_saved_messages_ru",
        "vmail/male/new_message_from_number_ru",
        "vmail/male/saved_message_from_number_ru",
        "vmail/male/and_go_to_next_ru",
        "vmail/male/press_1_to_delete_ru",
        "vmail/male/press_2_to_save_ru",
        "vmail/male/press_3_to_replay_message_ru",
        "vmail/male/press_start_for_main_menu_ru",
        "vmail/male/1'_ru",
        "vmail/male/_20_ru",
        "vmail/male/_30_ru",
        "conference/male/stop_after_1min_ru",
        "conference/male/stopped_ru",
        "conference/male/join_error_ru",
        "conference/male/too_many_participants_ru",
        "conference/male/connected_to_conference_ru",
        "conference/male/invalid_auth_ru",
        "conference/male/enter_access_code_ru",
        "conference/male/enter_id_ru",
        "conference/male/greeting_ru",
        "time words/male/года_ru",
        "time words/male/прошлого_ru",
        "time words/male/позавчера_ru",
        "time words/male/вчера_ru",
        "time words/male/сегодн€_ru",
        "time words/male/минут_ru",
        "time words/male/минуту_ru",
        "time words/male/минуты_ru",
        "time words/male/минута_ru",
        "time words/male/часов_ru",
        "time words/male/часа_ru",
        "time words/male/час_ru",
        "months/male/1_ru",
        "months/male/2_ru",
        "months/male/3_ru",
        "months/male/4_ru",
        "months/male/5_ru",
        "months/male/6_ru",
        "months/male/7_ru",
        "months/male/8_ru",
        "months/male/9_ru",
        "months/male/10_ru",
        "months/male/11_ru",
        "months/male/12_ru",
        "numbers2/male/1_ru",
        "numbers2/male/2_ru",
        "numbers2/male/3_ru",
        "numbers2/male/4_ru",
        "numbers2/male/5_ru",
        "numbers2/male/6_ru",
        "numbers2/male/7_ru",
        "numbers2/male/8_ru",
        "numbers2/male/9_ru",
        "numbers2/male/10_ru",
        "numbers2/male/11_ru",
        "numbers2/male/12_ru",
        "numbers2/male/13_ru",
        "numbers2/male/14_ru",
        "numbers2/male/15_ru",
        "numbers2/male/16_ru",
        "numbers2/male/17_ru",
        "numbers2/male/18_ru",
        "numbers2/male/19_ru",
        "numbers2/male/20_ru",
        "numbers2/male/30_ru",
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
        "numbers/female/копеек_ru",
        "numbers/female/копейка_ru",
        "numbers/female/копейки_ru",
        "numbers/female/рублей_ru",
        "numbers/female/рубль_ru",
        "numbers/female/рубл€_ru",
        "numbers/female/тыс€ч_ru",
        "numbers/female/тыс€ча_ru",
        "numbers/female/тыс€чи_ru",        
        "numbers/male/0_ru",
        "numbers/male/1'_ru",
        "numbers/male/1''_ru",
        "numbers/male/100_ru",
        "numbers/male/10_ru",
        "numbers/male/11_ru",
        "numbers/male/12_ru",
        "numbers/male/13_ru",
        "numbers/male/14_ru",
        "numbers/male/15_ru",
        "numbers/male/16_ru",
        "numbers/male/17_ru",
        "numbers/male/18_ru",
        "numbers/male/19_ru",
        "numbers/male/1_ru",
        "numbers/male/2'_ru",
        "numbers/male/200_ru",
        "numbers/male/20_ru",
        "numbers/male/2_ru",
        "numbers/male/300_ru",
        "numbers/male/30_ru",
        "numbers/male/3_ru",
        "numbers/male/400_ru",
        "numbers/male/40_ru",
        "numbers/male/4_ru",
        "numbers/male/500_ru",
        "numbers/male/50_ru",
        "numbers/male/5_ru",
        "numbers/male/600_ru",
        "numbers/male/60_ru",
        "numbers/male/6_ru",
        "numbers/male/700_ru",
        "numbers/male/70_ru",
        "numbers/male/7_ru",
        "numbers/male/800_ru",
        "numbers/male/80_ru",
        "numbers/male/8_ru",
        "numbers/male/900_ru",
        "numbers/male/90_ru",
        "numbers/male/9_ru",
        "numbers/male/копеек_ru",
        "numbers/male/копейка_ru",
        "numbers/male/копейки_ru",
        "numbers/male/рублей_ru",
        "numbers/male/рубль_ru",
        "numbers/male/рубл€_ru",
        "numbers/male/тыс€ч_ru",
        "numbers/male/тыс€ча_ru",
        "numbers/male/тыс€чи_ru"        
    };

    public void registerResources(ResourceManager resourceManager) {
        for (String name: names) 
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
